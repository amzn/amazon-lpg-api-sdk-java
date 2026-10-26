/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amazon.lending.security.impl;

import com.amazon.lending.security.exception.LPGSignatureException;
import com.amazon.lending.testsupport.SdkMessages;
import com.amazon.lending.testsupport.TestKeys;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Signing on one side, verification on the other, and the algorithms the verifier refuses. */
class LPGMessageSignerAndVerifierTest {
    private static final String KEY_ID = "signing";
    private static final String PAYLOAD = "{\"a\":1}";

    private static KeyPair keyPair;
    private static LPGMessageSignerAndVerifier signerAndVerifier;

    @BeforeAll
    static void generateKeyMaterial() {
        keyPair = TestKeys.generateRsaKeyPair();
        signerAndVerifier = new LPGMessageSignerAndVerifier(
                TestKeys.publicKeyProvider(keyPair, KEY_ID), TestKeys.decryptionKey(keyPair, KEY_ID));
    }

    @Test
    void signWithPsaProducesPs256JwsCarryingTheKeyIdAndPayloadClaim() throws Exception {
        final SignedJWT jws = signerAndVerifier.signWithPsa(PAYLOAD);

        assertEquals(JWSAlgorithm.PS256, jws.getHeader().getAlgorithm());
        assertEquals(KEY_ID, jws.getHeader().getKeyID());
        assertEquals(PAYLOAD, jws.getJWTClaimsSet().getStringClaim("payload"));
        assertTrue(jws.verify(new RSASSAVerifier(TestKeys.publicKey(keyPair))));
        signerAndVerifier.verify(jws);
    }

    @Test
    void legacySignProducesRs256JwsThatNimbusAccepts() throws Exception {
        final SignedJWT jws = signerAndVerifier.sign(PAYLOAD);

        assertEquals(JWSAlgorithm.RS256, jws.getHeader().getAlgorithm());
        assertEquals(KEY_ID, jws.getHeader().getKeyID());
        assertEquals(PAYLOAD, jws.getJWTClaimsSet().getStringClaim("payload"));
        assertTrue(jws.verify(new RSASSAVerifier(TestKeys.publicKey(keyPair))));
        signerAndVerifier.verify(jws);
    }

    @Test
    void legacySignIsDeterministicWhilePs256IsNot() throws Exception {
        // PKCS#1 v1.5 (RS256) has no salt; RSASSA-PSS (PS256) draws a fresh 32-byte salt, so two
        // signatures coincide with probability 2^-256, which this test accepts.
        assertEquals(signerAndVerifier.sign(PAYLOAD).getSignature(), signerAndVerifier.sign(PAYLOAD).getSignature());
        assertNotEquals(signerAndVerifier.signWithPsa(PAYLOAD).getSignature(), signerAndVerifier.signWithPsa(PAYLOAD).getSignature());
    }

    @Test
    void verifyRefusesAlgorithmsOtherThanRs256AndPs256() throws Exception {
        final SignedJWT hmacSigned = new SignedJWT(
                Base64URL.encode("{\"alg\":\"HS256\",\"kid\":\"" + KEY_ID + "\"}"),
                Base64URL.encode("{\"payload\":\"x\"}"),
                Base64URL.encode("not-checked".getBytes(StandardCharsets.UTF_8)));

        final LPGSignatureException e = assertThrows(LPGSignatureException.class, () -> signerAndVerifier.verify(hmacSigned));
        assertEquals(SdkMessages.INVALID_JWS_ALGORITHM + "HS256", e.getMessage());
    }

    @Test
    void verifyRefusesSignatureMadeWithAnotherKey() throws Exception {
        final KeyPair other = TestKeys.generateRsaKeyPair();
        final SignedJWT jws = new LPGMessageSignerAndVerifier(
                TestKeys.publicKeyProvider(other, KEY_ID), TestKeys.decryptionKey(other, KEY_ID)).signWithPsa(PAYLOAD);

        final LPGSignatureException e = assertThrows(LPGSignatureException.class, () -> signerAndVerifier.verify(jws));
        assertEquals(SdkMessages.FAILED_TO_VERIFY, e.getMessage());
    }

    @Test
    void verifyReportsWhenTheKeyIdIsUnknown() throws Exception {
        // A composite provider throws for an unknown kid, which the verifier wraps.
        final LPGMessageSignerAndVerifier compositeBacked = new LPGMessageSignerAndVerifier(
                new CompositeSecurityKeyProvider(Collections.singletonList(TestKeys.publicKeyProvider(keyPair, KEY_ID))),
                TestKeys.decryptionKey(keyPair, "rotated-away"));
        final SignedJWT jws = compositeBacked.signWithPsa(PAYLOAD);

        final LPGSignatureException e = assertThrows(LPGSignatureException.class, () -> compositeBacked.verify(jws));
        assertEquals(SdkMessages.FAILED_TO_GET_VERIFICATION_KEY, e.getMessage());
    }

    @Test
    void nullArgumentsAreRejected() {
        assertThrows(NullPointerException.class, () -> signerAndVerifier.signWithPsa(null));
        assertThrows(NullPointerException.class, () -> signerAndVerifier.sign(null));
        assertThrows(NullPointerException.class, () -> signerAndVerifier.verify(null));
    }

    @Test
    void opaquePrivateKeyThatTheJcaCannotUseIsReportedAsSigningFailure() {
        // An HSM-style handle exposes the modulus but not the private exponent. The JDK's SunRsaSign
        // provider rejects it on initSign (a different provider might reject it elsewhere); either way
        // both signing paths must surface the failure as the SDK's exception.
        final LPGMessageSignerAndVerifier opaque = new LPGMessageSignerAndVerifier(
                TestKeys.publicKeyProvider(keyPair, KEY_ID),
                LPGDecryptionKey.builder().rsaPrivateKey(new OpaqueRsaPrivateKey(TestKeys.privateKey(keyPair))).keyId(KEY_ID).build());

        assertEquals(SdkMessages.FAILED_TO_SIGN, assertThrows(LPGSignatureException.class, () -> opaque.signWithPsa(PAYLOAD)).getMessage());
        assertEquals(SdkMessages.FAILED_TO_SIGN_LEGACY, assertThrows(LPGSignatureException.class, () -> opaque.sign(PAYLOAD)).getMessage());
    }

    @Test
    void verificationKeyTooSmallForPs256IsReportedAsVerificationFailure() throws Exception {
        // RFC 8017 section 9.1.1: RSASSA-PSS needs emLen >= hLen + sLen + 2, i.e. 66 bytes for
        // SHA-256 with a 32-byte salt, so every compliant provider refuses a 512-bit (64-byte) key.
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(512);
        final LPGMessageSignerAndVerifier weakVerifier = new LPGMessageSignerAndVerifier(
                TestKeys.publicKeyProvider(generator.generateKeyPair(), KEY_ID), TestKeys.decryptionKey(keyPair, KEY_ID));
        final SignedJWT jws = signerAndVerifier.signWithPsa(PAYLOAD);

        final LPGSignatureException e = assertThrows(LPGSignatureException.class, () -> weakVerifier.verify(jws));
        assertEquals(SdkMessages.FAILED_TO_VERIFY, e.getMessage());
    }

    @Test
    void signingKeyBelow2048BitsCurrentlyLeaksNimbusIllegalArgument() throws Exception {
        // Known gap: new RSASSASigner(...) runs outside the try in signWithPsa, and Nimbus refuses RSA
        // keys under 2048 bits there, so the caller sees IllegalArgumentException instead of
        // LPGSignatureException. The fix CR flips this assertion.
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        final LPGMessageSignerAndVerifier weakSigner = new LPGMessageSignerAndVerifier(
                TestKeys.publicKeyProvider(keyPair, KEY_ID), TestKeys.decryptionKey(generator.generateKeyPair(), KEY_ID));

        assertThrows(IllegalArgumentException.class, () -> weakSigner.signWithPsa(PAYLOAD));
    }

    @Test
    void verifyWithASingleKeyProviderCurrentlyThrowsNullPointerForAnUnknownKeyId() throws Exception {
        // Known gap: a String or File provider configured with an explicit kid returns null for any
        // other kid, and verify dereferences it instead of throwing LPGSignatureException. Reachable
        // through the TKZ verifier, which is handed the raw provider. The fix CR flips this assertion.
        final SignedJWT jws = new LPGMessageSignerAndVerifier(
                TestKeys.publicKeyProvider(keyPair, KEY_ID), TestKeys.decryptionKey(keyPair, "rotated-away")).signWithPsa(PAYLOAD);

        assertThrows(NullPointerException.class, () -> signerAndVerifier.verify(jws));
    }

    /** RSA private key that only discloses its modulus, as a hardware-backed key handle would. */
    private static final class OpaqueRsaPrivateKey implements RSAPrivateKey {
        private static final long serialVersionUID = 1L;
        private final BigInteger modulus;

        OpaqueRsaPrivateKey(final RSAPrivateKey real) {
            this.modulus = real.getModulus();
        }

        @Override
        public BigInteger getModulus() {
            return modulus;
        }

        @Override
        public BigInteger getPrivateExponent() {
            return null;
        }

        @Override
        public String getAlgorithm() {
            return "RSA";
        }

        @Override
        public String getFormat() {
            return null;
        }

        @Override
        public byte[] getEncoded() {
            return null;
        }
    }
}

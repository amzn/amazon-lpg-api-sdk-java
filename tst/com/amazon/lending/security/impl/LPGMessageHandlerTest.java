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

import com.amazon.lending.security.MessageHandler;
import com.amazon.lending.security.SecurityKeyProvider;
import com.amazon.lending.security.exception.LPGEncryptionException;
import com.amazon.lending.security.exception.LPGSignatureException;
import com.amazon.lending.testsupport.SdkMessages;
import com.amazon.lending.testsupport.TestKeys;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.security.KeyPair;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The request/response envelope as both sides see it: the partner signs with its signing key and
 * encrypts to Amazon's encryption key; Amazon decrypts, verifies, and answers the same way back.
 * Every key below is generated at runtime.
 */
class LPGMessageHandlerTest {
    private static final String REQUEST_JSON = "{\"offerId\":\"offer-1\",\"amount\":12.5}";
    private static final String RESPONSE_JSON = "{\"status\":\"ACCEPTED\"}";

    private static Party partner;
    private static Party amazon;
    private static KeyPair stranger;

    @BeforeAll
    static void generateKeyMaterial() {
        partner = new Party("partner");
        amazon = new Party("amazon");
        stranger = TestKeys.generateRsaKeyPair();
    }

    @Test
    void requestSignedByPartnerIsReadByAmazon() throws Exception {
        final String envelope = partner.handlerTalkingTo(amazon).signAndEncrypt(REQUEST_JSON);

        assertEquals(REQUEST_JSON, amazon.handlerTalkingTo(partner).decryptAndVerify(envelope));
    }

    @Test
    void responseSignedByAmazonIsReadByPartner() throws Exception {
        final String envelope = amazon.handlerTalkingTo(partner).signAndEncrypt(RESPONSE_JSON);

        assertEquals(RESPONSE_JSON, partner.handlerTalkingTo(amazon).decryptAndVerify(envelope));
    }

    @Test
    void envelopeUsesRsaOaep256WithA256GcmAroundAPs256Jws() throws Exception {
        final String envelope = partner.handlerTalkingTo(amazon).signAndEncrypt(REQUEST_JSON);

        final JWEObject jwe = JWEObject.parse(envelope);
        assertEquals(JWEAlgorithm.RSA_OAEP_256, jwe.getHeader().getAlgorithm());
        assertEquals(EncryptionMethod.A256GCM, jwe.getHeader().getEncryptionMethod());
        assertEquals("JWT", jwe.getHeader().getContentType());
        assertEquals(amazon.encryptionKeyId(), jwe.getHeader().getKeyID());

        final SignedJWT jws = amazon.decryptWithNimbus(jwe);
        assertEquals(JWSAlgorithm.PS256, jws.getHeader().getAlgorithm());
        assertEquals(partner.signingKeyId(), jws.getHeader().getKeyID());
        assertEquals(REQUEST_JSON, jws.getJWTClaimsSet().getStringClaim("payload"));
    }

    @Test
    void legacyRs256SignatureStillVerifies() throws Exception {
        final SignedJWT legacyJws = partner.signerTalkingTo(amazon).sign(REQUEST_JSON);
        final String envelope = partner.encrypterTo(amazon).encrypt(legacyJws);

        assertEquals(JWSAlgorithm.RS256, legacyJws.getHeader().getAlgorithm());
        assertEquals(partner.signingKeyId(), legacyJws.getHeader().getKeyID());
        assertEquals(REQUEST_JSON, amazon.handlerTalkingTo(partner).decryptAndVerify(envelope));
    }

    @Test
    void responseSignedWithStandbyKeyIsStillVerified() throws Exception {
        // Key rotation: Amazon signs with its standby key and encrypts to the partner's standby key.
        final MessageHandler rotatedAmazon = new LPGMessageHandler(
                new LPGMessageEncrypter(TestKeys.encryptionKey(partner.standbyEncryption, partner.standbyEncryptionKeyId())),
                amazon.decrypter(),
                new LPGMessageSignerAndVerifier(amazon.verifierFor(partner), TestKeys.decryptionKey(amazon.standbySigning, amazon.standbySigningKeyId())));
        final String envelope = rotatedAmazon.signAndEncrypt(RESPONSE_JSON);

        assertEquals(RESPONSE_JSON, partner.handlerTalkingTo(amazon).decryptAndVerify(envelope));
    }

    @ParameterizedTest(name = "tampered {0}")
    @CsvSource(delimiter = '|', value = {
            "header             | 0 | " + SdkMessages.FAILED_TO_PARSE_PAYLOAD,
            "encrypted key      | 1 | " + SdkMessages.FAILED_TO_DECRYPT,
            "IV                 | 2 | " + SdkMessages.FAILED_TO_DECRYPT,
            "ciphertext         | 3 | " + SdkMessages.FAILED_TO_DECRYPT,
            "authentication tag | 4 | " + SdkMessages.FAILED_TO_DECRYPT,
    })
    void tamperedEnvelopeIsRejected(final String part, final int index, final String expectedMessage) throws Exception {
        // Compact JWE: header.encryptedKey.iv.ciphertext.tag. A256GCM authenticates the protected
        // header (as additional authenticated data), the IV, the ciphertext and the tag, and RSA-OAEP
        // rejects a damaged encrypted key. Flipping the header's first character also breaks its
        // base64 JSON, so that case fails one step earlier, at parsing.
        final String envelope = partner.handlerTalkingTo(amazon).signAndEncrypt(REQUEST_JSON);
        final String[] parts = envelope.split("\\.");
        parts[index] = flipFirstCharacter(parts[index]);

        final LPGEncryptionException e = assertThrows(
                LPGEncryptionException.class,
                () -> amazon.handlerTalkingTo(partner).decryptAndVerify(String.join(".", parts)));
        assertEquals(expectedMessage, e.getMessage(), part);
    }

    @Test
    void tamperedSignatureIsRejected() throws Exception {
        final SignedJWT genuine = partner.signerTalkingTo(amazon).signWithPsa(REQUEST_JSON);
        final SignedJWT forged = new SignedJWT(
                genuine.getHeader().toBase64URL(),
                genuine.getPayload().toBase64URL(),
                new Base64URL(flipFirstCharacter(genuine.getSignature().toString())));
        final String envelope = partner.encrypterTo(amazon).encrypt(forged);

        final LPGSignatureException e = assertThrows(
                LPGSignatureException.class,
                () -> amazon.handlerTalkingTo(partner).decryptAndVerify(envelope));
        assertEquals(SdkMessages.FAILED_TO_VERIFY, e.getMessage());
    }

    @Test
    void payloadReSignedByAnotherKeyIsRejected() throws Exception {
        // Same kid as the partner's signing key, so lookup succeeds and only the signature check fails.
        final SignedJWT jws = new LPGMessageSignerAndVerifier(amazon.verifierFor(partner),
                TestKeys.decryptionKey(stranger, partner.signingKeyId())).signWithPsa(REQUEST_JSON);
        final String envelope = partner.encrypterTo(amazon).encrypt(jws);

        final LPGSignatureException e = assertThrows(
                LPGSignatureException.class,
                () -> amazon.handlerTalkingTo(partner).decryptAndVerify(envelope));
        assertEquals(SdkMessages.FAILED_TO_VERIFY, e.getMessage());
    }

    @Test
    void envelopeEncryptedToAnotherKeyIsRejected() throws Exception {
        // Same kid as Amazon's encryption key, so lookup succeeds and only the decryption fails.
        final MessageHandler misdirected = new LPGMessageHandler(
                new LPGMessageEncrypter(TestKeys.encryptionKey(stranger, amazon.encryptionKeyId())),
                partner.decrypter(),
                partner.signerTalkingTo(amazon));
        final String envelope = misdirected.signAndEncrypt(REQUEST_JSON);

        final LPGEncryptionException e = assertThrows(
                LPGEncryptionException.class,
                () -> amazon.handlerTalkingTo(partner).decryptAndVerify(envelope));
        assertEquals(SdkMessages.FAILED_TO_DECRYPT, e.getMessage());
    }

    @Test
    void unknownEncryptionKeyIdIsRejected() throws Exception {
        final MessageHandler misdirected = new LPGMessageHandler(
                new LPGMessageEncrypter(TestKeys.encryptionKey(amazon.encryption, "no-such-kid")),
                partner.decrypter(),
                partner.signerTalkingTo(amazon));
        final String envelope = misdirected.signAndEncrypt(REQUEST_JSON);

        final LPGEncryptionException e = assertThrows(
                LPGEncryptionException.class,
                () -> amazon.handlerTalkingTo(partner).decryptAndVerify(envelope));
        assertEquals(SdkMessages.FAILED_TO_GET_ENCRYPTION_KEY, e.getMessage());
    }

    @Test
    void unknownSigningKeyIdIsRejected() throws Exception {
        final SignedJWT jws = new LPGMessageSignerAndVerifier(amazon.verifierFor(partner),
                TestKeys.decryptionKey(partner.signing, "no-such-kid")).signWithPsa(REQUEST_JSON);
        final String envelope = partner.encrypterTo(amazon).encrypt(jws);

        final LPGSignatureException e = assertThrows(
                LPGSignatureException.class,
                () -> amazon.handlerTalkingTo(partner).decryptAndVerify(envelope));
        assertEquals(SdkMessages.FAILED_TO_GET_VERIFICATION_KEY, e.getMessage());
    }

    @Test
    void missingPayloadClaimIsRejected() throws Exception {
        // Decryption and verification both succeed here; the failure is in reading the claims.
        // LPGEncryptionException is the type the SDK uses for that today, and partners catch on it.
        final SignedJWT jws = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.PS256).keyID(partner.signingKeyId()).build(),
                new JWTClaimsSet.Builder().claim("somethingElse", REQUEST_JSON).build());
        jws.sign(new RSASSASigner(TestKeys.privateKey(partner.signing)));
        final String envelope = partner.encrypterTo(amazon).encrypt(jws);

        final LPGEncryptionException e = assertThrows(
                LPGEncryptionException.class,
                () -> amazon.handlerTalkingTo(partner).decryptAndVerify(envelope));
        assertEquals(SdkMessages.FAILED_TO_RETRIEVE_PAYLOAD, e.getMessage());
    }

    @Test
    void signedPayloadThatIsNotAClaimsSetIsRejected() throws Exception {
        // Validly signed, but the JWS payload is plain text rather than a JSON claims object.
        // As above, the SDK reports a claims-parsing failure as LPGEncryptionException.
        final JWSObject jws = new JWSObject(
                new JWSHeader.Builder(JWSAlgorithm.PS256).keyID(partner.signingKeyId()).build(),
                new Payload("not a claims set"));
        jws.sign(new RSASSASigner(TestKeys.privateKey(partner.signing)));
        final String envelope = partner.encrypterTo(amazon).encrypt(
                new SignedJWT(jws.getHeader().toBase64URL(), jws.getPayload().toBase64URL(), jws.getSignature()));

        final LPGEncryptionException e = assertThrows(
                LPGEncryptionException.class,
                () -> amazon.handlerTalkingTo(partner).decryptAndVerify(envelope));
        assertEquals(SdkMessages.FAILED_TO_PARSE_PAYLOAD, e.getMessage());
    }

    @Test
    void envelopeWithoutAKeyIdIsDecryptedWithTheActiveKey() throws Exception {
        // A missing kid resolves to the first provider's key (its configured kid is the default), so
        // an envelope that names no key is still accepted; pinned so a change here is deliberate.
        final SignedJWT jws = partner.signerTalkingTo(amazon).signWithPsa(REQUEST_JSON);
        final String envelope = amazon.encryptWithNimbus(new Payload(jws),
                new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM).contentType("JWT").build());

        assertEquals(REQUEST_JSON, amazon.handlerTalkingTo(partner).decryptAndVerify(envelope));
    }

    @Test
    void signatureWithoutAKeyIdIsVerifiedAgainstTheActiveKeyOnly() throws Exception {
        final String fromPartner = partner.encrypterTo(amazon).encrypt(signWithoutKeyId(partner.signing));
        final String fromStranger = partner.encrypterTo(amazon).encrypt(signWithoutKeyId(stranger));

        assertEquals(REQUEST_JSON, amazon.handlerTalkingTo(partner).decryptAndVerify(fromPartner));
        final LPGSignatureException e = assertThrows(
                LPGSignatureException.class,
                () -> amazon.handlerTalkingTo(partner).decryptAndVerify(fromStranger));
        assertEquals(SdkMessages.FAILED_TO_VERIFY, e.getMessage());
    }

    @Test
    void encryptedPlainTextWhereTheJwsShouldBeCurrentlyFailsWithNullPointer() throws Exception {
        assertNotASignedJwsCurrentlyFailsWithNullPointer("just text");
    }

    @Test
    void encryptedNestedJweWhereTheJwsShouldBeCurrentlyFailsWithNullPointer() throws Exception {
        // Five dot-separated segments: a JWE inside the JWE instead of a JWS.
        assertNotASignedJwsCurrentlyFailsWithNullPointer(partner.handlerTalkingTo(amazon).signAndEncrypt(REQUEST_JSON));
    }

    @Test
    void encryptedAlgNoneTokenCurrentlyFailsWithNullPointer() throws Exception {
        // The classic unsigned-token trick: a JWS-shaped string with alg "none" and an empty signature.
        final String algNone = Base64URL.encode("{\"alg\":\"none\",\"kid\":\"" + partner.signingKeyId() + "\"}")
                + "." + Base64URL.encode("{\"payload\":\"" + REQUEST_JSON.replace("\"", "\\\"") + "\"}") + ".";

        assertNotASignedJwsCurrentlyFailsWithNullPointer(algNone);
    }

    /**
     * Known gap: the JWE decrypts, but its content is not a JWS (Nimbus refuses alg "none"), so
     * toSignedJWT() yields null and verify's @NonNull check throws instead of the SDK raising
     * LPGEncryptionException. The fix CR flips this assertion.
     */
    private static void assertNotASignedJwsCurrentlyFailsWithNullPointer(final String jwePayload) throws Exception {
        final String envelope = amazon.encryptWithNimbus(new Payload(jwePayload),
                new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM).keyID(amazon.encryptionKeyId()).build());

        assertThrows(NullPointerException.class, () -> amazon.handlerTalkingTo(partner).decryptAndVerify(envelope));
    }

    @Test
    void bankAccountNumberIsReadableOnlyWithAmazonsKey() throws Exception {
        final String accountNumber = "000123456789";
        final String envelope = partner.handlerTalkingTo(amazon).encryptBankAccountNumber(accountNumber);

        final JWEObject jwe = JWEObject.parse(envelope);
        jwe.decrypt(new RSADecrypter(TestKeys.privateKey(amazon.encryption)));
        assertEquals(accountNumber, jwe.getPayload().toString());

        final JWEObject again = JWEObject.parse(envelope);
        assertThrows(JOSEException.class, () -> again.decrypt(new RSADecrypter(TestKeys.privateKey(stranger))));
    }

    @Test
    void sideBySideEnvelopesForTheSamePayloadDiffer() throws Exception {
        // RSA-OAEP and A256GCM both draw fresh randomness, so a replayed envelope is never a byte-copy.
        final MessageHandler handler = partner.handlerTalkingTo(amazon);

        assertNotEquals(handler.signAndEncrypt(REQUEST_JSON), handler.signAndEncrypt(REQUEST_JSON));
    }

    private static SignedJWT signWithoutKeyId(final KeyPair signingKey) throws Exception {
        final SignedJWT jws = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.PS256).build(),
                new JWTClaimsSet.Builder().claim("payload", REQUEST_JSON).build());
        jws.sign(new RSASSASigner(TestKeys.privateKey(signingKey)));
        return jws;
    }

    /** Any change will do: GCM authenticates every bit, and RSA signatures cover the whole input. */
    private static String flipFirstCharacter(final String base64Url) {
        final char first = base64Url.charAt(0);
        final char replacement = first == 'A' ? 'B' : 'A';
        return replacement + base64Url.substring(1);
    }

    /**
     * One side of the conversation: a signing pair and an encryption pair, each with a standby
     * for rotation, wired through the same provider classes the SDK uses.
     */
    static final class Party {
        private final String name;
        final KeyPair signing = TestKeys.generateRsaKeyPair();
        final KeyPair standbySigning = TestKeys.generateRsaKeyPair();
        final KeyPair encryption = TestKeys.generateRsaKeyPair();
        final KeyPair standbyEncryption = TestKeys.generateRsaKeyPair();

        Party(final String name) {
            this.name = name;
        }

        String signingKeyId() {
            return name + "-signing";
        }

        String standbySigningKeyId() {
            return name + "-signing-standby";
        }

        String encryptionKeyId() {
            return name + "-encryption";
        }

        String standbyEncryptionKeyId() {
            return name + "-encryption-standby";
        }

        /** Looks up this party's private encryption keys (active then standby) by kid. */
        LPGMessageDecrypter decrypter() {
            return new LPGMessageDecrypter(new CompositeSecurityKeyProvider(Arrays.asList(
                    TestKeys.privateKeyProvider(encryption, encryptionKeyId()),
                    TestKeys.privateKeyProvider(standbyEncryption, standbyEncryptionKeyId()))));
        }

        /** Looks up the other party's public signing keys (active then standby) by kid. */
        SecurityKeyProvider verifierFor(final Party other) {
            return new CompositeSecurityKeyProvider(Arrays.asList(
                    TestKeys.publicKeyProvider(other.signing, other.signingKeyId()),
                    TestKeys.publicKeyProvider(other.standbySigning, other.standbySigningKeyId())));
        }

        LPGMessageEncrypter encrypterTo(final Party other) {
            return new LPGMessageEncrypter(TestKeys.encryptionKey(other.encryption, other.encryptionKeyId()));
        }

        LPGMessageSignerAndVerifier signerTalkingTo(final Party other) {
            return new LPGMessageSignerAndVerifier(verifierFor(other), TestKeys.decryptionKey(signing, signingKeyId()));
        }

        MessageHandler handlerTalkingTo(final Party other) {
            return new LPGMessageHandler(encrypterTo(other), decrypter(), signerTalkingTo(other));
        }

        SignedJWT decryptWithNimbus(final JWEObject jwe) throws JOSEException {
            jwe.decrypt(new RSADecrypter(TestKeys.privateKey(encryption)));
            return jwe.getPayload().toSignedJWT();
        }

        /** Encrypts to this party with an arbitrary header, as a sender outside the SDK would. */
        String encryptWithNimbus(final Payload payload, final JWEHeader header) throws JOSEException {
            final JWEObject jwe = new JWEObject(header, payload);
            jwe.encrypt(new RSAEncrypter(TestKeys.publicKey(encryption)));
            return jwe.serialize();
        }
    }
}

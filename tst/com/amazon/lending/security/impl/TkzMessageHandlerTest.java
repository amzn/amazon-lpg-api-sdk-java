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

import com.amazon.lending.security.exception.LPGEncryptionException;
import com.amazon.lending.security.exception.LPGSignatureException;
import com.amazon.lending.testsupport.SdkMessages;
import com.amazon.lending.testsupport.TestKeys;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The bank-account verification (TKZ) envelope: signs RS256 only, and has no separate bank-account encryption. */
class TkzMessageHandlerTest {
    private static final String PARTNER_KEY_ID = "partner-encryption";
    private static final String TKZ_KEY_ID = "tkz-verification";
    private static final String PAYLOAD = "{\"secureHash\":\"abc\"}";

    private static KeyPair partnerKeys;
    private static KeyPair tkzKeys;
    private static TkzMessageHandler partnerSide;

    @BeforeAll
    static void generateKeyMaterial() {
        partnerKeys = TestKeys.generateRsaKeyPair();
        tkzKeys = TestKeys.generateRsaKeyPair();
        partnerSide = new TkzMessageHandler(
                new LPGMessageEncrypter(TestKeys.encryptionKey(tkzKeys, TKZ_KEY_ID)),
                new LPGMessageDecrypter(TestKeys.privateKeyProvider(partnerKeys, PARTNER_KEY_ID)),
                new LPGMessageSignerAndVerifier(TestKeys.publicKeyProvider(tkzKeys, TKZ_KEY_ID),
                        TestKeys.decryptionKey(partnerKeys, PARTNER_KEY_ID)));
    }

    @Test
    void requestIsSignedWithRs256AndEncryptedToTkz() throws Exception {
        final JWEObject jwe = JWEObject.parse(partnerSide.signAndEncrypt(PAYLOAD));
        jwe.decrypt(new RSADecrypter(TestKeys.privateKey(tkzKeys)));
        final SignedJWT jws = jwe.getPayload().toSignedJWT();

        assertEquals(JWSAlgorithm.RS256, jws.getHeader().getAlgorithm());
        assertEquals(PARTNER_KEY_ID, jws.getHeader().getKeyID());
        assertEquals(PAYLOAD, jws.getJWTClaimsSet().getStringClaim("payload"));
    }

    @Test
    void responseFromTkzIsDecryptedAndVerified() throws Exception {
        assertEquals(PAYLOAD, partnerSide.decryptAndVerify(tkzResponse(new JWTClaimsSet.Builder().claim("payload", PAYLOAD).build())));
    }

    @Test
    void responseWithoutPayloadClaimIsRejected() throws Exception {
        final String envelope = tkzResponse(new JWTClaimsSet.Builder().claim("other", PAYLOAD).build());

        final LPGEncryptionException e = assertThrows(LPGEncryptionException.class, () -> partnerSide.decryptAndVerify(envelope));
        assertEquals(SdkMessages.FAILED_TO_RETRIEVE_PAYLOAD, e.getMessage());
    }

    @Test
    void responseWhosePayloadIsNotAClaimsSetIsRejected() throws Exception {
        final JWSObject jws = new JWSObject(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(TKZ_KEY_ID).build(), new Payload("plain text"));
        jws.sign(new RSASSASigner(TestKeys.privateKey(tkzKeys)));
        final String envelope = new LPGMessageEncrypter(TestKeys.encryptionKey(partnerKeys, PARTNER_KEY_ID)).encrypt(
                new SignedJWT(jws.getHeader().toBase64URL(), jws.getPayload().toBase64URL(), jws.getSignature()));

        final LPGEncryptionException e = assertThrows(LPGEncryptionException.class, () -> partnerSide.decryptAndVerify(envelope));
        assertEquals(SdkMessages.FAILED_TO_PARSE_PAYLOAD, e.getMessage());
    }

    @Test
    void responseSignedByAnotherKeyIsRejected() throws Exception {
        final TkzMessageHandler impostor = new TkzMessageHandler(
                new LPGMessageEncrypter(TestKeys.encryptionKey(partnerKeys, PARTNER_KEY_ID)),
                new LPGMessageDecrypter(TestKeys.privateKeyProvider(tkzKeys, TKZ_KEY_ID)),
                new LPGMessageSignerAndVerifier(TestKeys.publicKeyProvider(partnerKeys, PARTNER_KEY_ID),
                        TestKeys.decryptionKey(TestKeys.generateRsaKeyPair(), TKZ_KEY_ID)));
        final String envelope = impostor.signAndEncrypt(PAYLOAD);

        final LPGSignatureException e = assertThrows(LPGSignatureException.class, () -> partnerSide.decryptAndVerify(envelope));
        assertEquals(SdkMessages.FAILED_TO_VERIFY, e.getMessage());
    }

    @Test
    void bankAccountEncryptionIsNotPartOfTheTkzFlow() {
        final LPGEncryptionException e = assertThrows(LPGEncryptionException.class,
                () -> partnerSide.encryptBankAccountNumber("000123456789"));
        assertEquals(SdkMessages.TKZ_NO_BANK_ACCOUNT_ENCRYPTION, e.getMessage());
    }

    /** What TKZ sends back: RS256-signed with its own key, encrypted to the partner's encryption key. */
    private static String tkzResponse(final JWTClaimsSet claims) throws Exception {
        final SignedJWT jws = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(TKZ_KEY_ID).build(), claims);
        jws.sign(new RSASSASigner(TestKeys.privateKey(tkzKeys)));
        return new LPGMessageEncrypter(TestKeys.encryptionKey(partnerKeys, PARTNER_KEY_ID)).encrypt(jws);
    }
}

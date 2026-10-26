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
import com.amazon.lending.testsupport.SdkMessages;
import com.amazon.lending.testsupport.TestKeys;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The two JWE shapes the encrypter produces: the request envelope and the standalone bank account number. */
class LPGMessageEncrypterTest {
    private static final String KEY_ID = "amazon-encryption";

    private static KeyPair keyPair;
    private static LPGMessageEncrypter encrypter;

    @BeforeAll
    static void generateKeyMaterial() {
        keyPair = TestKeys.generateRsaKeyPair();
        encrypter = new LPGMessageEncrypter(TestKeys.encryptionKey(keyPair, KEY_ID));
    }

    @Test
    void bankAccountJweIdentifiesTheKeyByItsPublicMaterialNotByConfiguredKeyId() throws Exception {
        final byte[] publicMaterial = keyPair.getPublic().getEncoded();

        final JWEObject jwe = JWEObject.parse(encrypter.encryptBankAccountNumber("000123456789"));

        assertEquals(JWEAlgorithm.RSA_OAEP_256, jwe.getHeader().getAlgorithm());
        assertEquals(EncryptionMethod.A256GCM, jwe.getHeader().getEncryptionMethod());
        assertEquals(DigestUtils.sha256Hex(publicMaterial), jwe.getHeader().getKeyID());
        // x5t#S256 is specified as a certificate thumbprint; the SDK deliberately puts the hash of
        // the bare public key there, since partners hold Amazon's key, not a certificate.
        assertEquals(Base64URL.encode(DigestUtils.sha256(publicMaterial)), jwe.getHeader().getX509CertSHA256Thumbprint());
        assertNull(jwe.getHeader().getContentType());

        jwe.decrypt(new RSADecrypter(TestKeys.privateKey(keyPair)));
        assertEquals("000123456789", jwe.getPayload().toString());
    }

    @Test
    void keyTooSmallForRsaOaep256IsReportedAsEncryptionFailure() throws Exception {
        // RFC 8017 section 7.1.1: RSA-OAEP needs k >= 2 * hLen + 2 + mLen, i.e. 98 bytes to wrap a
        // 32-byte A256GCM key under SHA-256, so every compliant provider refuses a 512-bit (64-byte) key.
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(512);
        final LPGMessageEncrypter weak = new LPGMessageEncrypter(TestKeys.encryptionKey(generator.generateKeyPair(), KEY_ID));

        final LPGEncryptionException e = assertThrows(LPGEncryptionException.class, () -> weak.encryptBankAccountNumber("1"));
        assertEquals(SdkMessages.UNABLE_TO_PERFORM_ENCRYPTION, e.getMessage());
        final LPGEncryptionException e2 = assertThrows(LPGEncryptionException.class, () -> weak.encrypt(anySignedJwt()));
        assertEquals(SdkMessages.FAILED_TO_ENCRYPT, e2.getMessage());
    }

    @Test
    void nullArgumentsAreRejected() {
        assertThrows(NullPointerException.class, () -> encrypter.encrypt(null));
        assertThrows(NullPointerException.class, () -> encrypter.encryptBankAccountNumber(null));
    }

    private static SignedJWT anySignedJwt() throws Exception {
        return new LPGMessageSignerAndVerifier(TestKeys.publicKeyProvider(keyPair, KEY_ID),
                TestKeys.decryptionKey(keyPair, KEY_ID)).signWithPsa("x");
    }
}

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
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSAEncrypter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Header checks on inbound JWEs: anything but RSA-OAEP-256 + A256GCM is refused before any key is touched. */
class LPGMessageDecrypterTest {
    private static final String KEY_ID = "partner-encryption";

    private static KeyPair keyPair;
    private static LPGMessageDecrypter decrypter;

    @BeforeAll
    static void generateKeyMaterial() {
        keyPair = TestKeys.generateRsaKeyPair();
        decrypter = new LPGMessageDecrypter(TestKeys.privateKeyProvider(keyPair, KEY_ID));
    }

    @Test
    void rejectsJweWithAnotherKeyManagementAlgorithm() throws Exception {
        final String envelope = encrypt(JWEAlgorithm.RSA_OAEP, EncryptionMethod.A256GCM);

        final LPGEncryptionException e = assertThrows(LPGEncryptionException.class, () -> decrypter.decrypt(envelope));
        assertEquals(SdkMessages.INVALID_JWE_ALGORITHM + "RSA-OAEP", e.getMessage());
    }

    @Test
    void rejectsJweWithAnotherContentEncryptionMethod() throws Exception {
        final String envelope = encrypt(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A128GCM);

        final LPGEncryptionException e = assertThrows(LPGEncryptionException.class, () -> decrypter.decrypt(envelope));
        assertEquals(SdkMessages.INVALID_ENCRYPTION_METHOD + "A128GCM", e.getMessage());
    }

    @Test
    void rejectsTextThatIsNotAJwe() {
        final LPGEncryptionException e = assertThrows(LPGEncryptionException.class, () -> decrypter.decrypt("not.a.jwe"));
        assertEquals(SdkMessages.FAILED_TO_PARSE_PAYLOAD, e.getMessage());
    }

    @Test
    void singleKeyProviderCurrentlyThrowsNullPointerForAnUnknownKeyId() throws Exception {
        // Known gap: a String or File provider configured with an explicit kid returns null for any
        // other kid, and decrypt dereferences it instead of throwing LPGEncryptionException. The fix
        // CR flips this assertion.
        final JWEObject jwe = new JWEObject(
                new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM).keyID("rotated-away").build(),
                new Payload("anything"));
        jwe.encrypt(new RSAEncrypter(TestKeys.publicKey(keyPair)));

        assertThrows(NullPointerException.class, () -> decrypter.decrypt(jwe.serialize()));
    }

    @Test
    void rejectsNullPayload() {
        assertThrows(NullPointerException.class, () -> decrypter.decrypt(null));
    }

    private static String encrypt(final JWEAlgorithm algorithm, final EncryptionMethod method) throws Exception {
        final JWEObject jwe = new JWEObject(
                new JWEHeader.Builder(algorithm, method).keyID(KEY_ID).build(),
                new Payload("anything"));
        jwe.encrypt(new RSAEncrypter(TestKeys.publicKey(keyPair)));
        return jwe.serialize();
    }
}

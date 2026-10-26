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

package com.amazon.lending.security.util;

import com.amazon.lending.security.EncryptionKey;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("deprecation")
class DeprecatedKeyUtilShimTest {
    private static KeyPair keyPair;
    private static String publicPem;

    @BeforeAll
    static void generateKeyMaterial() throws Exception {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        publicPem = toPublicPem(keyPair);
    }

    @Test
    void shimGetMd5ChecksumEqualsReal() throws Exception {
        final byte[] pemBytes = publicPem.getBytes(StandardCharsets.UTF_8);

        assertEquals(
                KeyUtil.getMd5Checksum(pemBytes),
                main.java.com.amazon.lending.security.util.KeyUtil.getMd5Checksum(pemBytes)
        );
    }

    @Test
    void shimGeneratePublicKeyEqualsReal() throws Exception {
        final byte[] keyBytes = keyPair.getPublic().getEncoded();

        assertArrayEquals(
                KeyUtil.generatePublicKey(keyBytes).getEncoded(),
                main.java.com.amazon.lending.security.util.KeyUtil.generatePublicKey(keyBytes).getEncoded()
        );
    }

    @Test
    void shimGetKeyByIdReturnsEquivalentKey() throws Exception {
        final EncryptionKey realKey = KeyUtil.getKeyById(null, publicPem, EncryptionKey.class);
        final EncryptionKey shimKey =
                main.java.com.amazon.lending.security.util.KeyUtil.getKeyById(
                        null,
                        publicPem,
                        EncryptionKey.class
                );

        assertArrayEquals(realKey.getKey().getEncoded(), shimKey.getKey().getEncoded());
        assertEquals(realKey.getKeyId(), shimKey.getKeyId());
    }

    private static String toPublicPem(final KeyPair pair) {
        final Base64.Encoder encoder = Base64.getMimeEncoder(
                64,
                "\n".getBytes(StandardCharsets.UTF_8)
        );
        return "-----BEGIN PUBLIC KEY-----\n"
                + encoder.encodeToString(pair.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----\n";
    }
}

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

import com.amazon.lending.security.DecryptionKey;
import com.amazon.lending.security.EncryptionKey;
import com.amazon.lending.security.SecurityKey;
import com.amazon.lending.security.exception.LPGKeyException;
import com.amazon.lending.security.impl.FileSecurityKeyProvider;
import com.amazon.lending.security.impl.StringSecurityKeyProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyUtilTest {
    private static KeyPair keyPair;
    private static String publicPem;
    private static String privatePem;

    @BeforeAll
    static void generateKeyMaterial() throws Exception {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        publicPem = toPem("PUBLIC KEY", keyPair.getPublic().getEncoded(), 64);
        privatePem = toPem("PRIVATE KEY", keyPair.getPrivate().getEncoded(), 64);
    }

    @Test
    void derivedKidIsUppercaseHexMd5OfRawPemText() throws Exception {
        final byte[] pemBytes = publicPem.getBytes(StandardCharsets.UTF_8);
        final String expected = uppercaseHex(MessageDigest.getInstance("MD5").digest(pemBytes));
        final String actual = KeyUtil.getMd5Checksum(pemBytes);

        assertEquals(expected, actual);
        assertTrue(actual.matches("^[0-9A-F]{32}$"));
    }

    @Test
    void derivedKidChangesWhenPemWhitespaceChanges() throws Exception {
        final String differentlyWrappedPem = toPem("PUBLIC KEY", keyPair.getPublic().getEncoded(), 48);

        assertNotEquals(
                KeyUtil.getMd5Checksum(publicPem.getBytes(StandardCharsets.UTF_8)),
                KeyUtil.getMd5Checksum(differentlyWrappedPem.getBytes(StandardCharsets.UTF_8))
        );
    }

    @Test
    void publicKeyRoundTrip() throws Exception {
        final EncryptionKey derivedKey = KeyUtil.getKeyById(null, publicPem, EncryptionKey.class);
        final EncryptionKey explicitKey = KeyUtil.getKeyById("abc", publicPem, EncryptionKey.class);

        assertArrayEquals(keyPair.getPublic().getEncoded(), derivedKey.getKey().getEncoded());
        assertEquals(KeyUtil.getMd5Checksum(publicPem.getBytes(StandardCharsets.UTF_8)), derivedKey.getKeyId());
        assertEquals("abc", explicitKey.getKeyId());
    }

    @Test
    void privateKeyRoundTrip() throws Exception {
        // KeyUtil treats both PEM header styles as PKCS#8 key content.
        final String pkcs8WithRsaPrivateKeyHeaderPem =
                toPem("RSA PRIVATE KEY", keyPair.getPrivate().getEncoded(), 64);
        final DecryptionKey privateHeaderKey = KeyUtil.getKeyById(null, privatePem, DecryptionKey.class);
        final DecryptionKey rsaHeaderKey =
                KeyUtil.getKeyById(null, pkcs8WithRsaPrivateKeyHeaderPem, DecryptionKey.class);

        assertArrayEquals(keyPair.getPrivate().getEncoded(), privateHeaderKey.getKey().getEncoded());
        assertArrayEquals(keyPair.getPrivate().getEncoded(), rsaHeaderKey.getKey().getEncoded());
        assertEquals(KeyUtil.getMd5Checksum(privatePem.getBytes(StandardCharsets.UTF_8)), privateHeaderKey.getKeyId());
        assertEquals(
                KeyUtil.getMd5Checksum(pkcs8WithRsaPrivateKeyHeaderPem.getBytes(StandardCharsets.UTF_8)),
                rsaHeaderKey.getKeyId()
        );
    }

    @Test
    void unknownTypeThrowsLPGKeyException() {
        assertThrows(
                LPGKeyException.class,
                () -> KeyUtil.getKeyById(null, publicPem, SecurityKey.class)
        );
    }

    @Test
    void stringProviderWithoutKeyIdWorksOnCurrentJdk() throws Exception {
        final EncryptionKey key = new StringSecurityKeyProvider(publicPem)
                .getKeyById(null, EncryptionKey.class);

        assertNotNull(key);
        assertArrayEquals(keyPair.getPublic().getEncoded(), key.getKey().getEncoded());
    }

    @Test
    void fileProviderWithoutKeyIdWorksOnCurrentJdk(@TempDir final Path tempDir) throws Exception {
        final Path keyFile = tempDir.resolve("public-key.pem");
        Files.write(keyFile, publicPem.getBytes(StandardCharsets.UTF_8));

        final EncryptionKey key = new FileSecurityKeyProvider(keyFile.toString())
                .getKeyById(null, EncryptionKey.class);

        assertNotNull(key);
        assertArrayEquals(keyPair.getPublic().getEncoded(), key.getKey().getEncoded());
    }

    @Test
    void compiledClassDoesNotReferenceJavaxXmlBind() throws Exception {
        final byte[] classBytes;
        try (InputStream input = KeyUtil.class.getResourceAsStream("KeyUtil.class")) {
            assertNotNull(input);
            classBytes = readAllBytes(input);
        }

        assertFalse(contains(classBytes, "javax/xml/bind".getBytes(StandardCharsets.UTF_8)));
    }

    private static String toPem(final String type, final byte[] encoded, final int lineLength) {
        final Base64.Encoder encoder = Base64.getMimeEncoder(
                lineLength,
                "\n".getBytes(StandardCharsets.UTF_8)
        );
        return "-----BEGIN " + type + "-----\n"
                + encoder.encodeToString(encoded)
                + "\n-----END " + type + "-----\n";
    }

    private static String uppercaseHex(final byte[] bytes) {
        final char[] hex = "0123456789ABCDEF".toCharArray();
        final char[] result = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            final int value = bytes[i] & 0xFF;
            result[i * 2] = hex[value >>> 4];
            result[i * 2 + 1] = hex[value & 0x0F];
        }
        return new String(result);
    }

    private static byte[] readAllBytes(final InputStream input) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final byte[] buffer = new byte[1024];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static boolean contains(final byte[] source, final byte[] target) {
        for (int i = 0; i <= source.length - target.length; i++) {
            boolean matches = true;
            for (int j = 0; j < target.length; j++) {
                if (source[i + j] != target[j]) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return true;
            }
        }
        return false;
    }
}

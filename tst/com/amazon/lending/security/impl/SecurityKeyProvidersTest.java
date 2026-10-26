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

import com.amazon.lending.security.DecryptionKey;
import com.amazon.lending.security.EncryptionKey;
import com.amazon.lending.security.SecurityKeyProvider;
import com.amazon.lending.security.exception.LPGKeyException;
import com.amazon.lending.security.util.KeyUtil;
import com.amazon.lending.testsupport.SdkMessages;
import com.amazon.lending.testsupport.TestKeys;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Key lookup by kid through the String, File and Composite providers, including rotation fallback. */
class SecurityKeyProvidersTest {
    /** Mirrors MAX_KEY_SIZE_BYTES / MAX_FILE_SIZE_BYTES in the String and File providers (private there). */
    private static final int MAX_KEY_SIZE_BYTES = 8192;

    private static KeyPair activeKeys;
    private static KeyPair standbyKeys;
    private static String activePublicPem;
    private static String activePrivatePem;

    @BeforeAll
    static void generateKeyMaterial() {
        activeKeys = TestKeys.generateRsaKeyPair();
        standbyKeys = TestKeys.generateRsaKeyPair();
        activePublicPem = TestKeys.toPublicPem(activeKeys);
        activePrivatePem = TestKeys.toPrivatePem(activeKeys);
    }

    @Test
    void stringProviderWithKeyIdAnswersOnlyForThatKeyId() throws Exception {
        final StringSecurityKeyProvider provider = new StringSecurityKeyProvider(activePublicPem, "active");

        final EncryptionKey matched = provider.getKeyById("active", EncryptionKey.class);
        assertEquals("active", matched.getKeyId());
        assertArrayEquals(activeKeys.getPublic().getEncoded(), matched.getKey().getEncoded());
        assertNull(provider.getKeyById("someone-else", EncryptionKey.class));
    }

    @Test
    void stringProviderWithKeyIdUsesItAsTheDefaultWhenNoneIsRequested() throws Exception {
        final DecryptionKey key = new StringSecurityKeyProvider(activePrivatePem, "active").getKeyById(null, DecryptionKey.class);

        assertEquals("active", key.getKeyId());
        assertArrayEquals(activeKeys.getPrivate().getEncoded(), key.getKey().getEncoded());
    }

    @Test
    void stringProviderWithoutKeyIdAnswersForAnyKeyIdAndDerivesTheDefaultFromThePem() throws Exception {
        final StringSecurityKeyProvider provider = new StringSecurityKeyProvider(activePublicPem);

        assertEquals("whatever", provider.getKeyById("whatever", EncryptionKey.class).getKeyId());
        assertEquals(
                KeyUtil.getMd5Checksum(activePublicPem.getBytes(StandardCharsets.UTF_8)),
                provider.getKeyById(null, EncryptionKey.class).getKeyId());
    }

    @Test
    void stringProviderAcceptsExactlyTheMaximumAndRefusesOneByteMore() throws Exception {
        // The limit is measured on the raw UTF-8 text before any whitespace is stripped.
        final String atLimit = padToBytes(activePublicPem, MAX_KEY_SIZE_BYTES);
        final String overLimit = atLimit + '\n';

        assertArrayEquals(activeKeys.getPublic().getEncoded(),
                new StringSecurityKeyProvider(atLimit).getKeyById(null, EncryptionKey.class).getKey().getEncoded());
        final LPGKeyException e = assertThrows(LPGKeyException.class,
                () -> new StringSecurityKeyProvider(overLimit).getKeyById(null, EncryptionKey.class));
        assertEquals(SdkMessages.KEY_TOO_LARGE, e.getMessage());
    }

    @Test
    void fileProviderReadsThePemFromDiskAndHonoursTheKeyId(@TempDir final Path tempDir) throws Exception {
        final Path keyFile = write(tempDir, "active.pem", activePrivatePem);
        final FileSecurityKeyProvider provider = new FileSecurityKeyProvider(keyFile.toString(), "active");

        final DecryptionKey matched = provider.getKeyById("active", DecryptionKey.class);
        assertEquals("active", matched.getKeyId());
        assertArrayEquals(activeKeys.getPrivate().getEncoded(), matched.getKey().getEncoded());
        assertEquals("active", provider.getKeyById(null, DecryptionKey.class).getKeyId());
        assertNull(provider.getKeyById("someone-else", DecryptionKey.class));
    }

    @Test
    void fileProviderAcceptsExactlyTheMaximumAndRefusesOneByteMore(@TempDir final Path tempDir) throws Exception {
        // The limit is the file's length on disk, before the content is read.
        final Path atLimit = write(tempDir, "at-limit.pem", padToBytes(activePublicPem, MAX_KEY_SIZE_BYTES));
        final Path overLimit = write(tempDir, "over-limit.pem", padToBytes(activePublicPem, MAX_KEY_SIZE_BYTES) + '\n');

        assertArrayEquals(activeKeys.getPublic().getEncoded(),
                new FileSecurityKeyProvider(atLimit.toString()).getKeyById(null, EncryptionKey.class).getKey().getEncoded());
        final LPGKeyException e = assertThrows(LPGKeyException.class,
                () -> new FileSecurityKeyProvider(overLimit.toString()).getKeyById(null, EncryptionKey.class));
        assertEquals(SdkMessages.fileTooLarge(overLimit.toString()), e.getMessage());
    }

    @Test
    void fileProviderReportsAMissingFile(@TempDir final Path tempDir) {
        final Path missing = tempDir.resolve("missing.pem");

        assertThrows(IOException.class,
                () -> new FileSecurityKeyProvider(missing.toString()).getKeyById(null, EncryptionKey.class));
    }

    @Test
    void compositeProviderFallsBackFromActiveToStandby() throws Exception {
        final SecurityKeyProvider rotation = new CompositeSecurityKeyProvider(Arrays.asList(
                TestKeys.privateKeyProvider(activeKeys, "active"),
                TestKeys.privateKeyProvider(standbyKeys, "standby")));

        assertArrayEquals(activeKeys.getPrivate().getEncoded(),
                rotation.getKeyById("active", DecryptionKey.class).getKey().getEncoded());
        assertArrayEquals(standbyKeys.getPrivate().getEncoded(),
                rotation.getKeyById("standby", DecryptionKey.class).getKey().getEncoded());
    }

    @Test
    void compositeProviderReportsAnUnknownKeyIdWithEveryProviderFailureAttached(@TempDir final Path tempDir) {
        final Path missing = tempDir.resolve("missing.pem");
        final SecurityKeyProvider rotation = new CompositeSecurityKeyProvider(Arrays.asList(
                TestKeys.privateKeyProvider(activeKeys, "active"),
                null,
                new FileSecurityKeyProvider(missing.toString())));

        final Exception e = assertThrows(Exception.class, () -> rotation.getKeyById("gone", DecryptionKey.class));

        assertEquals("Key not found for keyId: gone", e.getMessage());
        assertEquals(1, e.getSuppressed().length);
        assertInstanceOf(IOException.class, e.getSuppressed()[0]);
    }

    @Test
    void compositeProviderRequiresAProviderList() {
        assertThrows(NullPointerException.class, () -> new CompositeSecurityKeyProvider(null));
    }

    @Test
    void compositeProviderRequiresTheKeyType() {
        assertThrows(NullPointerException.class,
                () -> new CompositeSecurityKeyProvider(Collections.emptyList()).getKeyById("x", null));
    }

    private static Path write(final Path dir, final String name, final String content) throws IOException {
        return Files.write(dir.resolve(name), content.getBytes(StandardCharsets.UTF_8));
    }

    /** Appends trailing newlines, which KeyUtil strips, until the UTF-8 text is exactly {@code bytes} long. */
    private static String padToBytes(final String pem, final int bytes) {
        final char[] padding = new char[bytes - pem.getBytes(StandardCharsets.UTF_8).length];
        Arrays.fill(padding, '\n');
        return pem + new String(padding);
    }
}

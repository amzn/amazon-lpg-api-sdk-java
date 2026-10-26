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

package com.amazon.lending.testsupport;

import com.amazon.lending.security.DecryptionKey;
import com.amazon.lending.security.EncryptionKey;
import com.amazon.lending.security.SecurityKeyProvider;
import com.amazon.lending.security.impl.LPGDecryptionKey;
import com.amazon.lending.security.impl.LPGEncryptionKey;
import com.amazon.lending.security.impl.StringSecurityKeyProvider;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

/**
 * Runtime-generated RSA key material for tests. Nothing here is ever a real
 * partner or Amazon key; every pair is created by {@link KeyPairGenerator}.
 */
public final class TestKeys {
    private static final int KEY_SIZE_BITS = 2048;

    private TestKeys() {}

    public static KeyPair generateRsaKeyPair() {
        try {
            final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(KEY_SIZE_BITS);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA is mandatory in every JRE", e);
        }
    }

    public static RSAPublicKey publicKey(final KeyPair keyPair) {
        return (RSAPublicKey) keyPair.getPublic();
    }

    public static RSAPrivateKey privateKey(final KeyPair keyPair) {
        return (RSAPrivateKey) keyPair.getPrivate();
    }

    public static String toPublicPem(final KeyPair keyPair) {
        return toPem("PUBLIC KEY", keyPair.getPublic().getEncoded());
    }

    public static String toPrivatePem(final KeyPair keyPair) {
        return toPem("PRIVATE KEY", keyPair.getPrivate().getEncoded());
    }

    public static EncryptionKey encryptionKey(final KeyPair keyPair, final String keyId) {
        return LPGEncryptionKey.builder().rsaPublicKey(publicKey(keyPair)).keyId(keyId).build();
    }

    public static DecryptionKey decryptionKey(final KeyPair keyPair, final String keyId) {
        return LPGDecryptionKey.builder().rsaPrivateKey(privateKey(keyPair)).keyId(keyId).build();
    }

    /** A provider that answers only for {@code keyId}, as the SDK configures partners' standby keys. */
    public static SecurityKeyProvider publicKeyProvider(final KeyPair keyPair, final String keyId) {
        return new StringSecurityKeyProvider(toPublicPem(keyPair), keyId);
    }

    public static SecurityKeyProvider privateKeyProvider(final KeyPair keyPair, final String keyId) {
        return new StringSecurityKeyProvider(toPrivatePem(keyPair), keyId);
    }

    /** 64-column PEM with LF line ends: the shape partners paste from openssl, which KeyUtil must accept. */
    private static String toPem(final String type, final byte[] encoded) {
        final Base64.Encoder encoder = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8));
        return "-----BEGIN " + type + "-----\n"
                + encoder.encodeToString(encoded)
                + "\n-----END " + type + "-----\n";
    }
}

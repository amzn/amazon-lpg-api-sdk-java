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

import lombok.NonNull;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import com.amazon.lending.security.exception.LPGKeyException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

public class KeyUtil {
    private KeyUtil() {}

    public static <T extends com.amazon.lending.security.SecurityKey> T getKeyById(final String keyId, final String keyContent,
                                                                                   @NonNull final Class<T> type) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, LPGKeyException {
        String keyContentCopy = keyContent;
        final String md5Sum = KeyUtil.getMd5Checksum(keyContentCopy.getBytes(StandardCharsets.UTF_8));

        if (type.equals(com.amazon.lending.security.EncryptionKey.class)) {
            keyContentCopy = keyContentCopy.replace("-----BEGIN PUBLIC KEY-----", "");
            keyContentCopy = keyContentCopy.replace("-----END PUBLIC KEY-----", "");
            keyContentCopy = keyContentCopy.replaceAll("\\s", "");
            final RSAPublicKey publicKey = KeyUtil.generatePublicKey(Base64.getDecoder().decode(
                    keyContentCopy.getBytes(StandardCharsets.UTF_8)));
            return type.cast(
                    com.amazon.lending.security.impl.LPGEncryptionKey.builder()
                            .rsaPublicKey(publicKey)
                            .keyId(Optional.ofNullable(keyId).orElse(md5Sum))
                            .build()
            );
        } else if (type.equals(com.amazon.lending.security.DecryptionKey.class)) {
            keyContentCopy = keyContentCopy.replace("-----BEGIN PRIVATE KEY-----", "");
            keyContentCopy = keyContentCopy.replace("-----BEGIN RSA PRIVATE KEY-----", "");
            keyContentCopy = keyContentCopy.replace("-----END PRIVATE KEY-----", "");
            keyContentCopy = keyContentCopy.replace("-----END RSA PRIVATE KEY-----", "");
            keyContentCopy = keyContentCopy.replaceAll("\\s", "");
            final RSAPrivateKey privateKey = KeyUtil.generatePrivateKey(Base64.getDecoder().decode(
                    keyContentCopy.getBytes(StandardCharsets.UTF_8)));
            return type.cast(
                    com.amazon.lending.security.impl.LPGDecryptionKey.builder()
                            .rsaPrivateKey(privateKey)
                            .keyId(Optional.ofNullable(keyId).orElse(md5Sum))
                            .build()
            );
        } else {
            throw new LPGKeyException("Unknown type: " + type.getName());
        }
    }

    public static RSAPublicKey generatePublicKey(@NonNull final byte[] keyData) throws NoSuchAlgorithmException, InvalidKeySpecException {
        final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyData);
        final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }

    public static RSAPrivateKey generatePrivateKey(@NonNull final byte[] keyData) throws NoSuchAlgorithmException, InvalidKeySpecException {
        final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyData);
        final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }

    public static String getMd5Checksum(@NonNull final byte[] keyData) throws NoSuchAlgorithmException {
        // NoSuchAlgorithmException is retained in the signature for API compatibility.
        return DigestUtils.md5Hex(keyData).toUpperCase(Locale.ROOT);
    }
}

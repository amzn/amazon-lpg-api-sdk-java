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

package main.java.com.amazon.lending.security.util;

import com.amazon.lending.security.SecurityKey;
import com.amazon.lending.security.exception.LPGKeyException;
import lombok.NonNull;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;

/**
 * @deprecated Use {@link com.amazon.lending.security.util.KeyUtil}. Alias retained for source
 * compatibility; removed in 2.0.
 */
@Deprecated
public final class KeyUtil {
    private KeyUtil() {}

    public static <T extends SecurityKey> T getKeyById(final String keyId, final String keyContent,
                                                       @NonNull final Class<T> type) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, LPGKeyException {
        return com.amazon.lending.security.util.KeyUtil.getKeyById(keyId, keyContent, type);
    }

    public static RSAPublicKey generatePublicKey(@NonNull final byte[] keyData) throws NoSuchAlgorithmException, InvalidKeySpecException {
        return com.amazon.lending.security.util.KeyUtil.generatePublicKey(keyData);
    }

    public static RSAPrivateKey generatePrivateKey(@NonNull final byte[] keyData) throws NoSuchAlgorithmException, InvalidKeySpecException {
        return com.amazon.lending.security.util.KeyUtil.generatePrivateKey(keyData);
    }

    public static String getMd5Checksum(@NonNull final byte[] keyData) throws NoSuchAlgorithmException {
        return com.amazon.lending.security.util.KeyUtil.getMd5Checksum(keyData);
    }
}

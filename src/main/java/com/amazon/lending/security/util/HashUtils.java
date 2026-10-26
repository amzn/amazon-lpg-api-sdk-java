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
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class HashUtils {

    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private HashUtils() {}

    public static byte[] hmacSha256(@NonNull final byte[] key, @NonNull final String data) throws NoSuchAlgorithmException, InvalidKeyException {
        final Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
        mac.init(new SecretKeySpec(key, HMAC_SHA256_ALGORITHM));
        return mac.doFinal(data.getBytes(DEFAULT_CHARSET));
    }

    public static String hmacSha256Base64(@NonNull final byte[] key, @NonNull final String data) throws InvalidKeyException, NoSuchAlgorithmException {
        final byte[] hmacData = hmacSha256(key, data);
        return Base64.getEncoder().encodeToString(hmacData);
    }

    public static String sha256Hex(@NonNull final String data) {
        return sha256Hex(data.getBytes(DEFAULT_CHARSET));
    }

    public static String sha256Hex(@NonNull final byte[] data) {
        return DigestUtils.sha256Hex(data).toLowerCase();
    }

    public static String hexEncode(@NonNull final String data) {
        return hexEncode(data.getBytes(DEFAULT_CHARSET));
    }

    public static String hexEncode(@NonNull final byte[] data) {
        return Hex.encodeHexString(data);
    }
}

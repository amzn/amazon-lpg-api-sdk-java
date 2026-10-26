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

import com.amazon.lending.security.SecurityKey;
import com.amazon.lending.security.SecurityKeyProvider;
import com.amazon.lending.security.exception.LPGKeyException;
import com.amazon.lending.security.util.KeyUtil;

import lombok.NonNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class StringSecurityKeyProvider implements SecurityKeyProvider {

    private static final int MAX_KEY_SIZE_BYTES = 8192;

    private final String keyContent;
    private final String expectedKeyId;

    /**
     * Constructor with explicit keyId. When used with CompositeSecurityKeyProvider,
     * this provider will only return the key if the requested keyId matches.
     */
    public StringSecurityKeyProvider(String keyContent, String expectedKeyId) {
        this.keyContent = keyContent;
        this.expectedKeyId = expectedKeyId;
    }

    /**
     * Constructor without keyId (backward compatible).
     * Always returns the key regardless of requested keyId.
     */
    public StringSecurityKeyProvider(String keyContent) {
        this.keyContent = keyContent;
        this.expectedKeyId = null;
    }

    @Override
    public <T extends SecurityKey> T getKeyById(final String keyId, @NonNull final Class<T> type) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, LPGKeyException {
        if (expectedKeyId != null && keyId != null && !expectedKeyId.equals(keyId)) {
            return null;
        }

        validateKeySize();

        String effectiveKeyId = expectedKeyId != null ? expectedKeyId : keyId;
        return KeyUtil.getKeyById(effectiveKeyId, keyContent, type);
    }

    private void validateKeySize() throws LPGKeyException {
        int keySize = keyContent.getBytes(StandardCharsets.UTF_8).length;
        if (keySize > MAX_KEY_SIZE_BYTES) {
            throw new LPGKeyException("Key exceeds maximum allowed size");
        }
    }
}

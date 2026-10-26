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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public class FileSecurityKeyProvider implements SecurityKeyProvider {

    private static final int MAX_FILE_SIZE_BYTES = 8192;

    private final String fileName;
    private final String expectedKeyId;

    /**
     * Constructor with explicit keyId. When used with CompositeSecurityKeyProvider,
     * this provider will only return the key if the requested keyId matches.
     */
    public FileSecurityKeyProvider(String fileName, String expectedKeyId) {
        this.fileName = fileName;
        this.expectedKeyId = expectedKeyId;
    }

    /**
     * Constructor without keyId (backward compatible).
     * Always returns the key regardless of requested keyId.
     */
    public FileSecurityKeyProvider(String fileName) {
        this.fileName = fileName;
        this.expectedKeyId = null;
    }

    @Override
    public <T extends SecurityKey> T getKeyById(final String keyId, @NonNull final Class<T> type) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException, LPGKeyException {
        if (expectedKeyId != null && keyId != null && !expectedKeyId.equals(keyId)) {
            return null;
        }

        validateFileSize();

        final Path path = Paths.get(fileName);
        final byte[] fileBytes = Files.readAllBytes(path);
        String keyContent = new String(fileBytes, StandardCharsets.UTF_8);

        String effectiveKeyId = expectedKeyId != null ? expectedKeyId : keyId;
        return KeyUtil.getKeyById(effectiveKeyId, keyContent, type);
    }

    private void validateFileSize() throws LPGKeyException {
        final File file = new File(fileName);
        final long fileSize = file.length();
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            throw new LPGKeyException("File " + fileName + " exceeds maximum allowed file size");
        }
    }
}

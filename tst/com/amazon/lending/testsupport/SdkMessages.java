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

/**
 * The exception messages the SDK's security classes produce. They are string literals in
 * production, so the tests pin them here once; a wording change is one edit.
 */
public final class SdkMessages {
    public static final String FAILED_TO_ENCRYPT = "Failed to encrypt payload";
    public static final String UNABLE_TO_PERFORM_ENCRYPTION = "Unable to perform encryption";
    public static final String FAILED_TO_DECRYPT = "Failed to decrypt payload";
    public static final String FAILED_TO_PARSE_PAYLOAD = "Failed to parse payload";
    public static final String FAILED_TO_RETRIEVE_PAYLOAD = "Failed to retrieve payload";
    public static final String FAILED_TO_GET_ENCRYPTION_KEY = "Failed to get encryption key";
    public static final String FAILED_TO_GET_VERIFICATION_KEY = "Failed to get verification key";
    public static final String FAILED_TO_SIGN = "Failed to sign payload";
    public static final String FAILED_TO_SIGN_LEGACY = "Failed to sign payload with RSA signature";
    public static final String FAILED_TO_VERIFY = "Failed to verify signature";
    public static final String INVALID_JWE_ALGORITHM = "Invalid JWE algorithm: ";
    public static final String INVALID_ENCRYPTION_METHOD = "Invalid encryption method: ";
    public static final String INVALID_JWS_ALGORITHM = "Invalid JWS algorithm: ";
    public static final String TKZ_NO_BANK_ACCOUNT_ENCRYPTION = "encryptBankAccountNumber is not supported in TKZ flow";
    public static final String KEY_TOO_LARGE = "Key exceeds maximum allowed size";

    private SdkMessages() {}

    public static String fileTooLarge(final String fileName) {
        return "File " + fileName + " exceeds maximum allowed file size";
    }
}

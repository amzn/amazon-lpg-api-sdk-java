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

package com.amazon.lending.security;

import com.amazon.lending.security.exception.LPGEncryptionException;
import com.amazon.lending.security.exception.LPGSignatureException;

public interface MessageHandler {
    /**
     * Sign and encrypt the payload.
     * @param payload String payload to sign and encrypt
     * @return Signed and encrypted payload
     * @throws LPGEncryptionException in the event the payload cannot be encrypted
     * @throws LPGSignatureException in the event the payload cannot be signed
     */
    String signAndEncrypt(String payload) throws LPGEncryptionException, LPGSignatureException;

    /**
     * Encrypt the payload, specifically for Encrypting Bank Account Number.
     * @param bankAccountNumber String BankAccountNumber to encrypt
     * @return Encrypted Bank Account Number
     * @throws LPGEncryptionException in the event the payload cannot be encrypted
     */
    String encryptBankAccountNumber(String bankAccountNumber) throws LPGEncryptionException;

    /**
     * Decrypt and verify the payload.
     * @param payload Signed and encrypted payload
     * @return Decrypted payload
     * @throws LPGEncryptionException in the event the payload cannot be decrypted
     * @throws LPGSignatureException in the event the signature is invalid or cannot be verified
     */
    String decryptAndVerify(String payload) throws LPGEncryptionException, LPGSignatureException;
}

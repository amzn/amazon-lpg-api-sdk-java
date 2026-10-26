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

import com.amazon.lending.security.MessageDecrypter;
import com.amazon.lending.security.MessageEncrypter;
import com.amazon.lending.security.MessageHandler;
import com.amazon.lending.security.MessageSignerAndVerifier;
import com.amazon.lending.security.exception.LPGEncryptionException;
import com.amazon.lending.security.exception.LPGSignatureException;
import com.nimbusds.jwt.SignedJWT;
import lombok.AllArgsConstructor;

import java.text.ParseException;
import java.util.Map;

/**
 * Message handler for the TKZ bank account verification flow.
 * Uses RS256 signing (not PS256) because TKZ only supports RS256.
 */
@AllArgsConstructor
public class TkzMessageHandler implements MessageHandler {

    private static final String MESSAGE_CLAIMS_SET_KEY = "payload";

    private MessageEncrypter messageEncrypter;
    private MessageDecrypter messageDecrypter;
    private MessageSignerAndVerifier messageSignerAndVerifier;

    @Override
    public String signAndEncrypt(String payload) throws LPGEncryptionException, LPGSignatureException {
        final SignedJWT signedJWT = messageSignerAndVerifier.sign(payload);
        return messageEncrypter.encrypt(signedJWT);
    }

    @Override
    public String encryptBankAccountNumber(String bankAccountNumber) throws LPGEncryptionException {
        throw new LPGEncryptionException("encryptBankAccountNumber is not supported in TKZ flow");
    }

    @Override
    public String decryptAndVerify(String payload) throws LPGEncryptionException, LPGSignatureException {
        final SignedJWT signedJWT = messageDecrypter.decrypt(payload);
        messageSignerAndVerifier.verify(signedJWT);

        final Map<String, Object> claims;
        try {
            claims = signedJWT.getJWTClaimsSet().getClaims();
        } catch (ParseException e) {
            throw new LPGEncryptionException("Failed to parse payload", e);
        }
        if (!claims.containsKey(MESSAGE_CLAIMS_SET_KEY)) {
            throw new LPGEncryptionException("Failed to retrieve payload");
        }

        return (String) claims.get(MESSAGE_CLAIMS_SET_KEY);
    }
}

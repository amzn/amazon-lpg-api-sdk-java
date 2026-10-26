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
import com.amazon.lending.security.MessageDecrypter;
import com.amazon.lending.security.SecurityKeyProvider;
import com.amazon.lending.security.exception.LPGEncryptionException;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jwt.SignedJWT;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.text.ParseException;

@AllArgsConstructor
public class LPGMessageDecrypter implements MessageDecrypter {

    private static final JWEAlgorithm JWE_ALGORITHM = JWEAlgorithm.RSA_OAEP_256;
    private static final EncryptionMethod ENCRYPTION_METHOD = EncryptionMethod.A256GCM;

    private SecurityKeyProvider keyProvider;

    @Override
    public SignedJWT decrypt(@NonNull final String payload) throws LPGEncryptionException {
        final JWEObject jweObject;
        try {
            jweObject = JWEObject.parse(payload);
        } catch (ParseException e) {
            throw new LPGEncryptionException("Failed to parse payload", e);
        }

        validateHeader(jweObject);

        final String keyId = jweObject.getHeader().getKeyID();
        final DecryptionKey decryptionKey;
        try {
            decryptionKey = keyProvider.getKeyById(keyId, DecryptionKey.class);
        } catch (Exception e) {
            throw new LPGEncryptionException("Failed to get encryption key", e);
        }
        final JWEDecrypter jweDecrypter = new RSADecrypter(decryptionKey.getKey());

        try {
            jweObject.decrypt(jweDecrypter);
        } catch (JOSEException e) {
            throw new LPGEncryptionException("Failed to decrypt payload", e);
        }
        return jweObject.getPayload().toSignedJWT();
    }

    private void validateHeader(@NonNull final JWEObject jweObject) {
        final JWEHeader header = jweObject.getHeader();
        if (!JWE_ALGORITHM.equals(header.getAlgorithm())) {
            throw new LPGEncryptionException("Invalid JWE algorithm: " + header.getAlgorithm().getName());
        }
        if (!ENCRYPTION_METHOD.equals(header.getEncryptionMethod())) {
            throw new LPGEncryptionException("Invalid encryption method: " + header.getEncryptionMethod().getName());
        }
    }
}

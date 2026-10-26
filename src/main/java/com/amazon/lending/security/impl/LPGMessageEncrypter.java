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

import com.amazon.lending.security.EncryptionKey;
import com.amazon.lending.security.MessageEncrypter;
import com.amazon.lending.security.exception.LPGEncryptionException;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEEncrypter;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.SignedJWT;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.apache.commons.codec.digest.DigestUtils;

import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

@AllArgsConstructor
public class LPGMessageEncrypter implements MessageEncrypter {

    private static final JWEAlgorithm JWE_ALGORITHM = JWEAlgorithm.RSA_OAEP_256;
    private static final EncryptionMethod ENCRYPTION_METHOD = EncryptionMethod.A256GCM;
    private static final String JWT_CONTENT_TYPE = "JWT";

    private EncryptionKey encryptionKey;

    @Override
    public String encrypt(@NonNull final SignedJWT signedJWT) throws LPGEncryptionException {

        final RSAPublicKey publicKey = encryptionKey.getKey();
        final JWEEncrypter jweEncrypter = new RSAEncrypter(publicKey);
        final Payload payload = new Payload(signedJWT);

        final JWEHeader jweHeader = new JWEHeader.Builder(JWE_ALGORITHM, ENCRYPTION_METHOD)
                .contentType(JWT_CONTENT_TYPE)
                .keyID(encryptionKey.getKeyId())
                .build();

        final JWEObject jweObject = new JWEObject(jweHeader, payload);

        try {
            jweObject.encrypt(jweEncrypter);
        } catch (JOSEException e) {
            throw new LPGEncryptionException("Failed to encrypt payload", e);
        }
        return jweObject.serialize();
    }

    @Override
    public String encryptBankAccountNumber(@NonNull final String bankAccountNumber) throws LPGEncryptionException {
        final RSAPublicKey publicKey = encryptionKey.getKey();
        final RSAEncrypter rsaEncrypter = new RSAEncrypter(publicKey);
        final JWEHeader jweHeader = getJWEHeader(publicKey.getEncoded(), publicKey);
        final Payload payload = new Payload(bankAccountNumber);
        try {
            JWEObject jweObject = new JWEObject(jweHeader, payload);
            jweObject.encrypt(rsaEncrypter);
            return jweObject.serialize();
        } catch (JOSEException e) {
            throw new LPGEncryptionException("Unable to perform encryption", e);
        }
    }

    private JWEHeader getJWEHeader(@NonNull byte[] publicMaterial, @NonNull PublicKey publicKey) {
        String publicKeyMd5 = DigestUtils.sha256Hex(publicKey.getEncoded());
        Base64URL x5tSha256Hash = Base64URL.encode(DigestUtils.sha256(publicMaterial));
        return new JWEHeader.Builder(JWE_ALGORITHM,ENCRYPTION_METHOD)
                .x509CertSHA256Thumbprint(x5tSha256Hash)
                .keyID(publicKeyMd5)
                .build();
    }
}

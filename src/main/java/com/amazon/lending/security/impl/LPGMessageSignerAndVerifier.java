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
import com.amazon.lending.security.EncryptionKey;
import com.amazon.lending.security.MessageSignerAndVerifier;
import com.amazon.lending.security.SecurityKeyProvider;
import com.amazon.lending.security.exception.LPGSignatureException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.util.Base64URL;
import java.nio.charset.StandardCharsets;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.minidev.json.JSONValue;
import java.security.Signature;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@AllArgsConstructor
public class LPGMessageSignerAndVerifier implements MessageSignerAndVerifier {

    private static final String MESSAGE_CLAIMS_SET_KEY = "payload";
    private static final JWSAlgorithm JWS_ALGORITHM = JWSAlgorithm.RS256;
    private static final JWSAlgorithm JWS_ALGORITHM_PSA = JWSAlgorithm.PS256;
    private static final String SIGNING_ALGORITHM = "SHA256withRSA";
    // Freshness claims on the PS256 request JWS, so a replayed body can be rejected (RFC 7519).
    private static final JOSEObjectType REQUEST_JWS_TYPE = new JOSEObjectType("lpg-req+jwt");
    private static final long REQUEST_LIFETIME_SECONDS = 300;

    private SecurityKeyProvider keyProvider;
    private DecryptionKey signingKey;

    /**
     * Sign with PS256. Next to {@code payload}, the claims are {@code iat} and {@code nbf} (now,
     * whole seconds), {@code exp} ({@code iat} + 300 s) and a random {@code jti}; the header adds
     * {@code typ: lpg-req+jwt}.
     */
    @Override
    public SignedJWT signWithPsa(@NonNull final String payload) throws LPGSignatureException {
        final Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        final JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .claim(MESSAGE_CLAIMS_SET_KEY, payload)
                .issueTime(Date.from(issuedAt))
                .notBeforeTime(Date.from(issuedAt))
                .expirationTime(Date.from(issuedAt.plusSeconds(REQUEST_LIFETIME_SECONDS)))
                .jwtID(UUID.randomUUID().toString())
                .build();

        final JWSHeader jwsHeader = new JWSHeader.Builder(JWS_ALGORITHM_PSA)
                .type(REQUEST_JWS_TYPE)
                .keyID(signingKey.getKeyId())
                .build();
        final SignedJWT signedJWT = new SignedJWT(jwsHeader, claimsSet);
        final JWSSigner jwsSigner = new RSASSASigner(signingKey.getKey());

        try {
            signedJWT.sign(jwsSigner);
        } catch (JOSEException e) {
            throw new LPGSignatureException("Failed to sign payload", e);
        }

        return signedJWT;
    }

    @Override
    public SignedJWT sign(@NonNull final String payload) throws LPGSignatureException {

        Map<String, Object> claims = new HashMap<>();
        claims.put(MESSAGE_CLAIMS_SET_KEY, payload);

        // create JWS header
        Map<String, Object> headerClaims = new HashMap<>();
        headerClaims.put("kid", signingKey.getKeyId());
        headerClaims.put("alg", JWS_ALGORITHM.getName());
        final String headerJson = JSONValue.toJSONString(headerClaims);

        final Base64URL encodedHeader = Base64URL.encode(headerJson);

        final String jsonClaimsSet = JSONValue.toJSONString(claims);
        final Base64URL encodedPayload = Base64URL.encode(jsonClaimsSet);

        //Creating signing input String as done by Nimbus
        final String signingInput = encodedHeader + "." + encodedPayload;

        try {
            Signature signer = Signature.getInstance(SIGNING_ALGORITHM);
            signer.initSign(signingKey.getKey());
            signer.update(signingInput.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = signer.sign();
            return new SignedJWT(encodedHeader, encodedPayload, Base64URL.encode(signatureBytes));
        } catch (Exception e) {
            throw new LPGSignatureException("Failed to sign payload with RSA signature", e);
        }
    }

    @Override
    public void verify(@NonNull final SignedJWT signedJWT) throws LPGSignatureException {
        validateHeader(signedJWT);

        final String keyId = signedJWT.getHeader().getKeyID();
        final EncryptionKey verificationKey;
        try {
            verificationKey = keyProvider.getKeyById(keyId, EncryptionKey.class);
        } catch (Exception e) {
            throw new LPGSignatureException("Failed to get verification key", e);
        }
        final JWSVerifier jwsVerifier = new RSASSAVerifier(verificationKey.getKey());

        try {
            if (!signedJWT.verify(jwsVerifier)) {
                throw new LPGSignatureException("Failed to verify signature");
            }
        } catch (JOSEException e) {
            throw new LPGSignatureException("Failed to verify signature", e);
        }
    }

    private void validateHeader(@NonNull final SignedJWT signedJWT) {
        final JWSHeader header = signedJWT.getHeader();
        if (!JWS_ALGORITHM.equals(header.getAlgorithm()) && !JWS_ALGORITHM_PSA.equals(header.getAlgorithm())) {
            throw new LPGSignatureException("Invalid JWS algorithm: " + header.getAlgorithm().getName());
        }
    }
}

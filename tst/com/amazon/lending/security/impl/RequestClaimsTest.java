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

import com.amazon.lending.testsupport.TestKeys;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The freshness claims and header on the PS256 request JWS; the payload claim and TKZ's RS256 JWS are unchanged. */
class RequestClaimsTest {
    private static final String KEY_ID = "partner-signing";
    private static final String PAYLOAD = "{\"offerId\":\"offer-1\",\"amount\":12.5}";
    // signWithPsa(PAYLOAD)'s claims at 3373b2e, before this change, when payload was the only claim.
    private static final String BASE_PAYLOAD_MEMBER = "\"payload\":\"{\\\"offerId\\\":\\\"offer-1\\\",\\\"amount\\\":12.5}\"";

    private static KeyPair keyPair;
    private static LPGMessageSignerAndVerifier signer;

    @BeforeAll
    static void generateKeyMaterial() {
        keyPair = TestKeys.generateRsaKeyPair();
        signer = new LPGMessageSignerAndVerifier(TestKeys.publicKeyProvider(keyPair, KEY_ID), TestKeys.decryptionKey(keyPair, KEY_ID));
    }

    @Test
    void theClaimsArePresentWithTheirJsonTypesAndTheJwsStillVerifies() throws Exception {
        final long before = Instant.now().getEpochSecond();
        final SignedJWT jws = signer.signWithPsa(PAYLOAD);
        final long after = Instant.now().getEpochSecond();
        final Map<String, Object> claims = JSONObjectUtils.parse(jws.getPayload().toString());

        assertEquals(5, claims.size(), claims::toString);
        assertEquals(PAYLOAD, claims.get("payload"));
        assertTrue(claims.get("iat") instanceof Long && claims.get("exp") instanceof Long, "iat and exp are JSON numbers");
        final long iat = (Long) claims.get("iat");
        assertTrue(before <= iat && iat <= after, before + " <= " + iat + " <= " + after);
        assertEquals(300L, (Long) claims.get("exp") - iat);
        assertTrue(claims.get("nbf") instanceof Long, "nbf is a JSON number");
        assertEquals(iat, claims.get("nbf"));
        assertEquals(claims.get("jti"), UUID.fromString((String) claims.get("jti")).toString());
        signer.verify(jws);
    }

    @Test
    void theHeaderCarriesTypAlgAndKidOnly() throws Exception {
        final Map<String, Object> header = JSONObjectUtils.parse(signer.signWithPsa(PAYLOAD).getHeader().toString());
        assertEquals(3, header.size(), header::toString);
        assertEquals("lpg-req+jwt", header.get("typ"));
        assertEquals("PS256", header.get("alg"));
        assertEquals(KEY_ID, header.get("kid"));
    }

    @Test
    void jtiDiffersBetweenTwoSignings() throws Exception {
        assertNotEquals(signer.signWithPsa(PAYLOAD).getJWTClaimsSet().getJWTID(), signer.signWithPsa(PAYLOAD).getJWTClaimsSet().getJWTID());
    }

    @Test
    void thePayloadClaimSerialisesToTheSameBytesAsBefore() {
        assertTrue(signer.signWithPsa(PAYLOAD).getPayload().toString().contains(BASE_PAYLOAD_MEMBER));
    }

    @Test
    void tkzRs256SignIsUnchanged() throws Exception {
        final SignedJWT jws = signer.sign(PAYLOAD);
        assertNull(jws.getHeader().getType());
        assertEquals("{" + BASE_PAYLOAD_MEMBER + "}", jws.getPayload().toString());
        assertEquals(jws.serialize(), signer.sign(PAYLOAD).serialize());
    }
}

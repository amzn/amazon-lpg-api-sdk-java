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

package com.amazon.lending.http.impl;

import com.amazon.lending.aws.AWSSigner;
import com.amazon.lending.aws.impl.AWSSigV4Signer;
import com.amazon.lending.config.SdkVersion;
import com.amazon.lending.http.HttpMethod;
import com.amazon.lending.http.exception.LPGForbiddenException;
import com.amazon.lending.http.exception.LPGInvalidRequestException;
import com.amazon.lending.http.exception.LPGRequestException;
import com.amazon.lending.http.exception.impl.LPGExceptionConverter;
import com.amazon.lending.http.model.AccountLockDto;
import com.amazon.lending.http.model.PerformanceDataAuthTokenDto;
import com.amazon.lending.http.model.RedirectUrlDto;
import com.amazon.lending.model.AccountLock;
import com.amazon.lending.model.AccountLockAllowedUpdates;
import com.amazon.lending.model.AccountLockStatus;
import com.amazon.lending.model.BankIdCodeType;
import com.amazon.lending.security.MessageHandler;
import com.amazon.lending.security.exception.LPGEncryptionException;
import com.amazon.lending.security.exception.LPGSignatureException;
import com.amazon.lending.security.impl.CompositeSecurityKeyProvider;
import com.amazon.lending.security.impl.LPGMessageDecrypter;
import com.amazon.lending.security.impl.LPGMessageEncrypter;
import com.amazon.lending.security.impl.LPGMessageHandler;
import com.amazon.lending.security.impl.LPGMessageSignerAndVerifier;
import com.amazon.lending.testsupport.SdkMessages;
import com.amazon.lending.testsupport.TestKeys;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The whole outbound path over real HTTP: DTO mapping, JSON, JWS + JWE, SigV4, the wire, and
 * back through decryption, verification, JSON and mapping, against a gateway that checks each
 * step the way the real one does.
 */
class LPGRequestHandlerEndToEndTest {
    private static final String ACCOUNT_LOCK_PATH = "/v1.1/accountlock/lock-42";
    private static final String AUTH_TOKEN_PATH = "/v1.1/offers/offer-1/generatePerformanceDataAuthToken";

    private FakeLendingPartnerGateway gateway;
    private LPGRequestHandler handler;

    @BeforeEach
    void startGateway() throws IOException {
        gateway = new FakeLendingPartnerGateway();
        handler = new LPGRequestHandler(partnerMessageHandler(), partnerSigner(), FakeLendingPartnerGateway.API_KEY, new LPGExceptionConverter());
    }

    @AfterEach
    void stopGateway() {
        gateway.close();
    }

    @Test
    void getIsSignedAndTheResponseIsDecryptedVerifiedAndMapped() throws Exception {
        gateway.respondWith(200, "{\"redirectUrl\":\"https://partner.example/apply?token=abc\"}");

        final URL redirect = handler.makeRequest(gateway.url("/v1.1/offers/offer-1/redirect"), HttpMethod.GET, null, URL.class, RedirectUrlDto.class);

        assertEquals(new URL("https://partner.example/apply?token=abc"), redirect);
        assertEquals(Collections.emptyList(), gateway.failures());
        final FakeLendingPartnerGateway.ReceivedRequest request = gateway.onlyRequest();
        assertEquals("GET", request.method);
        assertEquals("/v1.1/offers/offer-1/redirect", request.path);
        assertNull(request.decryptedPayload);
        assertEquals(FakeLendingPartnerGateway.API_KEY, request.headers.get("x-api-key"));
        assertEquals(SdkVersion.SDK_VERSION, request.headers.get("x-amz-lpg-sdk-ver"));
        assertTrue(request.headers.get("Authorization").contains("SignedHeaders=host;x-amz-date;x-amz-security-token;x-api-key,"), request.headers.get("Authorization"));
        assertTrue(request.headers.get("x-amz-date").matches("\\d{8}T\\d{6}Z"), request.headers.get("x-amz-date"));
    }

    @Test
    void sdkSignsTheBareHostWhileTheWireCarriesHostAndPort() throws Exception {
        // HttpURLConnection sends Host: host:port and ignores the Host header the SDK set, while the
        // signature covers url.getHost() alone. The two agree only on the default port, which is
        // what every LPGEndpoint uses; the gateway here has to drop the port to verify.
        gateway.respondWith(200, "{\"authToken\":\"tok-1\"}");

        handler.makeRequest(gateway.url(AUTH_TOKEN_PATH), HttpMethod.POST, null, String.class, PerformanceDataAuthTokenDto.class);

        final FakeLendingPartnerGateway.ReceivedRequest request = gateway.onlyRequest();
        assertEquals(gateway.url("/").getAuthority(), request.headers.get("Host"));
        assertTrue(request.hostPortStrippedToVerify);
        assertEquals(Collections.emptyList(), gateway.failures());
    }

    @Test
    void postWithoutAPayloadSendsAnEmptyBody() throws Exception {
        gateway.respondWith(200, "{\"authToken\":\"tok-1\"}");

        final String token = handler.makeRequest(gateway.url(AUTH_TOKEN_PATH), HttpMethod.POST, null, String.class, PerformanceDataAuthTokenDto.class);

        assertEquals("tok-1", token);
        assertEquals(Collections.emptyList(), gateway.failures());
        assertEquals("POST", gateway.onlyRequest().method);
        assertNull(gateway.onlyRequest().decryptedPayload);
        assertEquals("0", gateway.onlyRequest().headers.get("Content-length"));
    }

    @Test
    void putWithAPayloadIsMappedSignedAndEncryptedAndTheTypedResponseComesBack() throws Exception {
        gateway.respondWith(200, "{\"accountLockId\":\"lock-42\",\"accountLockStatus\":\"UNLOCKED\",\"bankIdCodeType\":\"ROUTING_NUMBER\","
                + "\"allowedUpdates\":[\"RELOCK\"],\"lastUpdatedOn\":\"2026-09-04T10:15:30.000Z\",\"version\":7}");
        final AccountLock unlock = AccountLock.builder()
                .accountLockId("lock-42")
                .accountLockStatus(AccountLockStatus.UNLOCKED)
                .unlockReasonCode("CUSTOMER_REQUEST")
                .allowedUpdates(Collections.singletonList(AccountLockAllowedUpdates.UNLOCK))
                .build();

        final AccountLock result = handler.makeRequest(gateway.url(ACCOUNT_LOCK_PATH), HttpMethod.PUT, unlock, AccountLock.class, AccountLockDto.class);

        assertEquals(Collections.emptyList(), gateway.failures());
        final FakeLendingPartnerGateway.ReceivedRequest request = gateway.onlyRequest();
        assertEquals("PUT", request.method);
        assertEquals(ACCOUNT_LOCK_PATH, request.path);
        assertEquals(FakeLendingPartnerGateway.AMAZON_ENCRYPTION_KEY_ID, request.jweKeyId);
        assertEquals("RSA-OAEP-256/A256GCM", request.jweAlgorithms);
        assertEquals(FakeLendingPartnerGateway.PARTNER_SIGNING_KEY_ID, request.jwsKeyId);
        assertEquals("PS256", request.jwsAlgorithm);
        final GenericJson sent = JacksonFactory.getDefaultInstance().fromString(request.decryptedPayload, GenericJson.class);
        assertEquals("lock-42", sent.get("accountLockId"));
        assertEquals("UNLOCKED", sent.get("accountLockStatus"));
        assertEquals("CUSTOMER_REQUEST", sent.get("unlockReasonCode"));
        assertEquals(Collections.singletonList("UNLOCK"), sent.get("allowedUpdates"));

        assertEquals("lock-42", result.getAccountLockId());
        assertEquals(AccountLockStatus.UNLOCKED, result.getAccountLockStatus());
        assertEquals(BankIdCodeType.ROUTING_NUMBER, result.getBankIdCodeType());
        assertEquals(Collections.singletonList(AccountLockAllowedUpdates.RELOCK), result.getAllowedUpdates());
        assertEquals(Date.from(Instant.parse("2026-09-04T10:15:30.000Z")), result.getLastUpdatedOn());
        assertEquals(7L, result.getVersion());
    }

    @Test
    void aPostBodyCarriesTheFreshnessClaimsAndTheGatewayAcceptsThem() throws Exception {
        gateway.respondWith(200, "{\"accountLockId\":\"lock-42\"}");
        final long before = Instant.now().getEpochSecond();
        handler.makeRequest(gateway.url(ACCOUNT_LOCK_PATH), HttpMethod.POST, AccountLock.builder().accountLockId("lock-42").build(),
                AccountLock.class, AccountLockDto.class);
        final long after = Instant.now().getEpochSecond();

        assertEquals(Collections.emptyList(), gateway.failures());
        final FakeLendingPartnerGateway.ReceivedRequest request = gateway.onlyRequest();
        assertEquals("lpg-req+jwt", request.jwsType);
        final long iat = ((Date) request.jwsClaims.get("iat")).getTime() / 1000;
        assertTrue(before <= iat && iat <= after, before + " <= " + iat + " <= " + after);
        assertEquals(iat + 300, ((Date) request.jwsClaims.get("exp")).getTime() / 1000);
        assertTrue(request.jwsClaims.get("jti") instanceof String);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "400, com.amazon.lending.http.exception.LPGInvalidRequestException",
            "403, com.amazon.lending.http.exception.LPGForbiddenException",
            "404, com.amazon.lending.http.exception.LPGInvalidRequestException",
            "429, com.amazon.lending.http.exception.LPGThrottledRequestException",
            "500, com.amazon.lending.http.exception.LPGNonTransientServiceException",
            "502, com.amazon.lending.http.exception.LPGTransientServiceException",
            "418, com.amazon.lending.http.exception.LPGUnknownErrorException",
    })
    void errorStatusesBecomeTypedExceptions(final int status, final Class<? extends LPGRequestException> expected) throws Exception {
        gateway.respondWith(status, "{}");

        final LPGRequestException e = assertThrows(LPGRequestException.class,
                () -> handler.makeRequest(gateway.url(ACCOUNT_LOCK_PATH), HttpMethod.GET, null, AccountLock.class, AccountLockDto.class));

        assertSame(expected, e.getClass());
        assertEquals(Collections.emptyList(), gateway.failures());
        assertEquals(1, gateway.received().size());
    }

    @Test
    void wrongApiKeyIsRefusedByTheGatewayAsForbidden() throws Exception {
        gateway.expectApiKey("someone-elses-key");
        gateway.respondWith(200, "{}");

        assertThrows(LPGForbiddenException.class,
                () -> handler.makeRequest(gateway.url(ACCOUNT_LOCK_PATH), HttpMethod.GET, null, AccountLock.class, AccountLockDto.class));

        assertEquals(Collections.singletonList("x-api-key was " + FakeLendingPartnerGateway.API_KEY), gateway.failures());
    }

    @Test
    void responseSignedWithAnUnknownKeyIsRejected() throws Exception {
        gateway.signResponsesWith(TestKeys.generateRsaKeyPair());
        gateway.respondWith(200, "{\"authToken\":\"tok-1\"}");

        final LPGSignatureException e = assertThrows(LPGSignatureException.class,
                () -> handler.makeRequest(gateway.url(AUTH_TOKEN_PATH), HttpMethod.POST, null, String.class, PerformanceDataAuthTokenDto.class));

        assertEquals(SdkMessages.FAILED_TO_VERIFY, e.getMessage());
        assertEquals(Collections.emptyList(), gateway.failures());
    }

    @Test
    void responseEncryptedToSomeoneElseIsRejected() throws Exception {
        gateway.encryptResponsesTo(TestKeys.generateRsaKeyPair());
        gateway.respondWith(200, "{\"authToken\":\"tok-1\"}");

        final LPGEncryptionException e = assertThrows(LPGEncryptionException.class,
                () -> handler.makeRequest(gateway.url(AUTH_TOKEN_PATH), HttpMethod.POST, null, String.class, PerformanceDataAuthTokenDto.class));

        assertEquals(SdkMessages.FAILED_TO_DECRYPT, e.getMessage());
        assertEquals(Collections.emptyList(), gateway.failures());
    }

    @Test
    void unmappableTypesFailBeforeAnythingIsSent() throws Exception {
        final LPGInvalidRequestException requestSide = assertThrows(LPGInvalidRequestException.class,
                () -> handler.makeRequest(gateway.url(ACCOUNT_LOCK_PATH), HttpMethod.GET, null, String.class, AccountLockDto.class));
        final LPGInvalidRequestException responseSide = assertThrows(LPGInvalidRequestException.class,
                () -> handler.makeRequest(gateway.url(ACCOUNT_LOCK_PATH), HttpMethod.GET, null, AccountLock.class, AccountLockDto.class, RedirectUrlDto.class, AccountLock.class));

        assertTrue(requestSide.getMessage().startsWith("Unable to find mapper for"), requestSide.getMessage());
        assertTrue(responseSide.getMessage().startsWith("Unable to find mapper for"), responseSide.getMessage());
        assertEquals(Collections.emptyList(), gateway.received());
    }

    @Test
    void signerFailuresAreReportedAsInvalidRequests() {
        final AWSSigner noAlgorithm = (request, date, service) -> {
            throw new NoSuchAlgorithmException("HmacSHA256");
        };
        final AWSSigner badKey = (request, date, service) -> {
            throw new InvalidKeyException("empty secret");
        };

        assertThrows(LPGInvalidRequestException.class, () -> withSigner(noAlgorithm)
                .makeRequest(gateway.url(ACCOUNT_LOCK_PATH), HttpMethod.GET, null, AccountLock.class, AccountLockDto.class));
        assertThrows(LPGInvalidRequestException.class, () -> withSigner(badKey)
                .makeRequest(gateway.url(ACCOUNT_LOCK_PATH), HttpMethod.GET, null, AccountLock.class, AccountLockDto.class));
        assertEquals(Collections.emptyList(), gateway.received());
    }

    @Test
    void nullUrlOrMethodIsRejected() {
        assertThrows(NullPointerException.class, () -> handler.makeRequest(null, HttpMethod.GET, null, AccountLock.class, AccountLockDto.class));
        assertThrows(NullPointerException.class, () -> handler.makeRequest(gateway.url(ACCOUNT_LOCK_PATH), null, null, AccountLock.class, AccountLockDto.class));
    }

    private static LPGRequestHandler withSigner(final AWSSigner signer) {
        return new LPGRequestHandler(partnerMessageHandler(), signer, FakeLendingPartnerGateway.API_KEY, new LPGExceptionConverter());
    }

    /** The partner's side, wired exactly as DefaultLendingPartnerGatewayClient wires it. */
    private static MessageHandler partnerMessageHandler() {
        return new LPGMessageHandler(
                new LPGMessageEncrypter(TestKeys.encryptionKey(FakeLendingPartnerGateway.AMAZON_ENCRYPTION, FakeLendingPartnerGateway.AMAZON_ENCRYPTION_KEY_ID)),
                new LPGMessageDecrypter(new CompositeSecurityKeyProvider(Collections.singletonList(
                        TestKeys.privateKeyProvider(FakeLendingPartnerGateway.PARTNER_ENCRYPTION, FakeLendingPartnerGateway.PARTNER_ENCRYPTION_KEY_ID)))),
                new LPGMessageSignerAndVerifier(
                        new CompositeSecurityKeyProvider(Collections.singletonList(
                                TestKeys.publicKeyProvider(FakeLendingPartnerGateway.AMAZON_SIGNING, FakeLendingPartnerGateway.AMAZON_SIGNING_KEY_ID))),
                        TestKeys.decryptionKey(FakeLendingPartnerGateway.PARTNER_SIGNING, FakeLendingPartnerGateway.PARTNER_SIGNING_KEY_ID)));
    }

    private static AWSSigner partnerSigner() {
        return new AWSSigV4Signer(
                () -> FakeLendingPartnerGateway.ACCESS_KEY,
                () -> FakeLendingPartnerGateway.EXAMPLE_SECRET,
                () -> Optional.of(FakeLendingPartnerGateway.SESSION_TOKEN),
                () -> FakeLendingPartnerGateway.REGION);
    }
}

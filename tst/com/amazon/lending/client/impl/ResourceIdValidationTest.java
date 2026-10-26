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

package com.amazon.lending.client.impl;

import com.amazon.lending.client.LendingPartnerGatewayClient;
import com.amazon.lending.config.ApiVersion;
import com.amazon.lending.config.LPGEndpoint;
import com.amazon.lending.http.HttpMethod;
import com.amazon.lending.http.RequestHandler;
import com.amazon.lending.http.exception.LPGInvalidRequestException;
import com.amazon.lending.model.AccountLock;
import com.amazon.lending.model.Application;
import com.amazon.lending.model.CreditApproval;
import com.amazon.lending.model.Loan;
import com.amazon.lending.model.Offer;
import com.amazon.lending.model.OfferDetails;
import com.amazon.lending.model.RepaymentSchedule;
import com.amazon.lending.model.VerificationKey;
import com.amazon.lending.security.MessageHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Resource IDs go into URL paths only if they match [A-Za-z0-9][A-Za-z0-9._-]{0,127}; nothing else is sent. */
class ResourceIdValidationTest {
    private final List<URL> sent = new ArrayList<>();

    @ParameterizedTest
    @ValueSource(strings = {"..", ".", "../../x", "a/b", "a?b=c", "a#b", "%2e%2e", "a b", "a\r\nb", "", "_a"})
    void refusedIdsThrowBeforeAnyRequest(final String id) throws Exception {
        assertRefused(() -> client(LPGEndpoint.NA_BETA).getOffer(id));
        assertRefused(() -> client(LPGEndpoint.NA_BETA).inquireOffer(id));
        assertTrue(sent.isEmpty());
    }

    @Test
    void validRequestsBuildTheSameUrlsAsBefore() throws Exception {
        final LendingPartnerGatewayClient client = client(LPGEndpoint.NA_BETA);
        final String id = new String(new char[128]).replace('\0', 'a');
        client.createLoan(Loan.builder().build());
        client.createOffer(Offer.builder().details(OfferDetails.builder().expireOn(new Date(0)).build()).build(), "m", "k");
        client.inquireOffer(id);
        client.getRedirectUrl(id);
        client.generatePerformanceDataAuthToken(id);
        assertEquals(Arrays.asList("/v1.1/loans", "/test/offers", "/test/offers/" + id, "/test/offers/" + id + "/redirect",
                "/v1.1/offers/" + id + "/generatePerformanceDataAuthToken"), paths());
        assertRefused(() -> client.getLoan(id + "a"));
    }

    @ParameterizedTest // every token type LPG issues
    @ValueSource(strings = {"offer", "accountlock", "application", "creditapproval", "loan", "repaymentschedule",
            "merchant", "portfoliodata", "creditapprovaldata", "loandata", "repayment"})
    void everyTokenTypeIsSentUnchanged(final String type) throws Exception {
        client(LPGEndpoint.NA_BETA).getApplication("amzn1.lending." + type + ".us.q2w3e4r5t6y7u8i9o0p1a2s3d4f5g6h7");
        assertEquals(Arrays.asList("/v1.1/applications/amzn1.lending." + type + ".us.q2w3e4r5t6y7u8i9o0p1a2s3d4f5g6h7"), paths());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("idTakingMethods")
    void everyIdTakingMethodChecksTheId(final String name, final IdCall call) throws Exception {
        assertRefused(() -> call.invoke(client(LPGEndpoint.NA_BETA), "../x"));
        assertTrue(sent.isEmpty());
    }

    static Stream<Arguments> idTakingMethods() {
        final VerificationKey key = VerificationKey.builder().secretKey("c2VjcmV0").verificationPayload("p").build();
        return Stream.of(
                call("getOffer", (c, id) -> c.getOffer(id)),
                call("generatePerformanceDataAuthToken", (c, id) -> c.generatePerformanceDataAuthToken(id)),
                call("inquireOffer", (c, id) -> c.inquireOffer(id)),
                call("getRedirectUrl", (c, id) -> c.getRedirectUrl(id)),
                call("getApplication", (c, id) -> c.getApplication(id)),
                call("updateApplication", (c, id) -> c.updateApplication(Application.builder().applicationId(id).build())),
                call("confirmApplication", (c, id) -> c.confirmApplication(id, new Date(0))),
                call("lockAccountAndConfirmApplication", (c, id) -> c.lockAccountAndConfirmApplication(id)),
                call("getAccountLock", (c, id) -> c.getAccountLock(id)),
                call("updateAccountLock", (c, id) -> c.updateAccountLock(AccountLock.builder().accountLockId(id).build())),
                call("getCreditApproval", (c, id) -> c.getCreditApproval(id)),
                call("updateCreditApproval", (c, id) -> c.updateCreditApproval(CreditApproval.builder().creditApprovalId(id).build())),
                call("getLoan", (c, id) -> c.getLoan(id)),
                call("updateLoan", (c, id) -> c.updateLoan(Loan.builder().loanId(id).build())),
                call("updateLoan with a null model ID", (c, id) -> c.updateLoan(Loan.builder().build())),
                call("getVerificationKey", (c, id) -> c.getVerificationKey(id)),
                call("updateRepaymentSchedule", (c, id) -> c.updateRepaymentSchedule(RepaymentSchedule.builder().repaymentScheduleId(id).build())),
                call("getRepaymentSchedule", (c, id) -> c.getRepaymentSchedule(id)),
                call("getLenderPortfolioData", (c, id) -> c.getLenderPortfolioData(id)),
                call("getLenderCreditApprovalData", (c, id) -> c.getLenderCreditApprovalData(id)),
                call("getLenderLoanData", (c, id) -> c.getLenderLoanData(id)),
                call("getLoanRepaymentDetail", (c, id) -> c.getLoanRepaymentDetail(id)),
                call("getRepaymentCalculationMetadata", (c, id) -> c.getRepaymentCalculationMetadata(id)),
                call("verifyBankAccount", (c, id) -> c.verifyBankAccount(key, id, "000123456789")),
                call("verifyWithLockAndConfirm", (c, id) -> c.verifyWithLockAndConfirm(key, id, "000123456789")));
    }

    @Test
    void aProductionEndpointIsStillRefusedForTestRoutesFirst() throws Exception {
        assertEquals("Invalid endpoint; only non-production endpoints are supported for this API.",
                assertThrows(LPGInvalidRequestException.class, () -> client(LPGEndpoint.NA_PROD).inquireOffer("../x")).getMessage());
    }

    private static void assertRefused(final Executable call) {
        assertEquals("Invalid resource ID", assertThrows(LPGInvalidRequestException.class, call).getMessage());
    }

    /** A client whose request handler records URLs instead of sending them; initialize() would call STS. */
    private LendingPartnerGatewayClient client(final LPGEndpoint endpoint) throws Exception {
        final DefaultLendingPartnerGatewayClient client = new DefaultLendingPartnerGatewayClient();
        set(client, "requestHandler", new RequestHandler() {
            @Override
            public <A, B, C, D> D makeRequest(final URL url, final HttpMethod method, final A payload, final Class<A> a,
                                              final Class<B> b, final Class<C> c, final Class<D> d) {
                sent.add(url);
                return null;
            }
        });
        set(client, "lpgEndpoint", endpoint);
        set(client, "apiVersion", ApiVersion.V1_1);
        set(client, "fieldEncryptionHandler", new MessageHandler() {
            @Override public String signAndEncrypt(final String p) { return p; }
            @Override public String encryptBankAccountNumber(final String n) { return n; }
            @Override public String decryptAndVerify(final String p) { return p; }
        });
        return client;
    }

    private List<String> paths() {
        return sent.stream().map(URL::getPath).collect(Collectors.toList());
    }

    private static void set(final Object target, final String name, final Object value) throws Exception {
        final Field field = DefaultLendingPartnerGatewayClient.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Arguments call(final String name, final IdCall call) { return Arguments.of(name, call); }

    interface IdCall { void invoke(LendingPartnerGatewayClient client, String id) throws Exception; }
}

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

import com.amazon.lending.aws.impl.AWSSigV4Signer;
import com.amazon.lending.aws.impl.DefaultRoleManager;
import com.amazon.lending.config.Operation;
import com.amazon.lending.http.impl.LPGRequestHandler;
import com.amazon.lending.http.model.AccountLockDto;
import com.amazon.lending.http.model.CreateOfferAsyncRequestDto;
import com.amazon.lending.http.model.CreditApprovalDto;
import com.amazon.lending.http.model.LenderCreditApprovalDataDto;
import com.amazon.lending.http.model.LenderLoanDataDto;
import com.amazon.lending.http.model.LenderPortfolioDataDto;
import com.amazon.lending.http.model.LoanDto;
import com.amazon.lending.http.model.OfferDto;
import com.amazon.lending.http.model.PerformanceDataAuthTokenDto;
import com.amazon.lending.http.model.RedirectUrlDto;
import com.amazon.lending.http.model.RepaymentScheduleDto;
import com.amazon.lending.http.model.VerificationKeyDto;
import com.amazon.lending.http.model.VerificationRequestDto;
import com.amazon.lending.http.model.VerificationResultDto;
import com.amazon.lending.http.model.CreateLenderPortfolioDataRequestDto;
import com.amazon.lending.http.model.CreateLenderCreditApprovalDataRequestDto;
import com.amazon.lending.http.model.CreateLenderLoanDataRequestDto;
import com.amazon.lending.http.model.LoanRepaymentDetailDto;
import com.amazon.lending.http.model.RepaymentCalculationMetadataDto;
import com.amazon.lending.model.BankIdCodeType;
import com.amazon.lending.model.CreateOfferAsyncRequest;
import com.amazon.lending.model.CreateOfferAsyncResponse;
import com.amazon.lending.model.Repayment;
import com.amazon.lending.model.RepaymentSchedule;
import com.amazon.lending.model.RepaymentScheduleDetails;
import com.amazon.lending.model.LenderCreditApprovalData;
import com.amazon.lending.model.LenderLoanData;
import com.amazon.lending.model.LenderPortfolioData;
import com.amazon.lending.model.LoanRepaymentDetail;
import com.amazon.lending.model.RepaymentCalculationMetadata;
import com.amazon.lending.model.VerificationKey;
import com.amazon.lending.model.VerificationRequest;
import com.amazon.lending.model.VerificationType;
import com.amazon.lending.model.AccountLockAllowedUpdates;
import com.amazon.lending.security.DecryptionKey;
import com.amazon.lending.security.MessageDecrypter;
import com.amazon.lending.security.MessageHandler;
import com.amazon.lending.security.MessageSignerAndVerifier;
import com.amazon.lending.security.impl.CompositeSecurityKeyProvider;
import com.amazon.lending.security.impl.LPGMessageDecrypter;
import com.amazon.lending.security.SecurityKeyProvider;
import com.amazon.lending.aws.AWSSigner;
import com.amazon.lending.aws.RoleManager;
import com.amazon.lending.client.LendingPartnerGatewayClient;
import com.amazon.lending.config.AWSConfig;
import com.amazon.lending.config.ApiVersion;
import com.amazon.lending.config.LPGConfig;
import com.amazon.lending.config.LPGEndpoint;
import com.amazon.lending.config.Resource;
import com.amazon.lending.config.SecurityConfig;
import com.amazon.lending.http.HttpMethod;
import com.amazon.lending.http.RequestHandler;
import com.amazon.lending.http.exception.LPGInvalidRequestException;
import com.amazon.lending.http.exception.LPGRequestException;
import com.amazon.lending.http.exception.impl.LPGExceptionConverter;
import com.amazon.lending.http.model.ApplicationDto;
import com.amazon.lending.http.model.CreateOfferRequest;
import com.amazon.lending.model.CreateLenderPortfolioDataRequest;
import com.amazon.lending.model.CreateLenderCreditApprovalDataRequest;
import com.amazon.lending.model.CreateLenderLoanDataRequest;
import com.amazon.lending.model.AccountLock;
import com.amazon.lending.model.AccountLockStatus;
import com.amazon.lending.model.Application;
import com.amazon.lending.model.ApplicationAbandonCode;
import com.amazon.lending.model.ApplicationDetails;
import com.amazon.lending.model.ApplicationRejectionCategory;
import com.amazon.lending.model.ApplicationRejectionCode;
import com.amazon.lending.model.ApplicationStatus;
import com.amazon.lending.model.CreditApproval;
import com.amazon.lending.model.CreditApprovalDetails;
import com.amazon.lending.model.CreditApprovalStatus;
import com.amazon.lending.model.Loan;
import com.amazon.lending.model.LoanDetails;
import com.amazon.lending.model.LoanStatus;
import com.amazon.lending.model.Offer;
import com.amazon.lending.security.EncryptionKey;
import com.amazon.lending.security.MessageEncrypter;
import com.amazon.lending.security.impl.LPGMessageEncrypter;
import com.amazon.lending.security.impl.LPGMessageHandler;
import com.amazon.lending.security.impl.TkzMessageHandler;
import com.amazon.lending.security.impl.LPGMessageSignerAndVerifier;
import com.amazon.lending.security.util.HashUtils;

import com.google.api.client.util.DateTime;
import com.google.common.collect.ImmutableSet;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.Collections;
import java.util.regex.Pattern;

@NoArgsConstructor
public class DefaultLendingPartnerGatewayClient implements LendingPartnerGatewayClient {

    private RequestHandler requestHandler;
    private LPGEndpoint lpgEndpoint;
    private ApiVersion apiVersion;
    private MessageHandler fieldEncryptionHandler;
    private static List<AccountLockAllowedUpdates> ALLOWED_UPDATES_LIST_UNLOCK = Collections.singletonList(AccountLockAllowedUpdates.UNLOCK);
    private static List<AccountLockAllowedUpdates> ALLOWED_UPDATES_LIST_RELOCK = Collections.singletonList(AccountLockAllowedUpdates.RELOCK);
    private static final Pattern RESOURCE_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,127}");

    @Override
    public void initialize(@NonNull final SecurityConfig securityConfig, @NonNull final AWSConfig awsConfig, @NonNull final LPGConfig lpgConfig) throws Exception {
        // --- Main flow keys ---

        // Outbound: encrypt requests with Amazon's encryption public key
        final EncryptionKey encryptionKey = securityConfig.getAmazonEncryptionPublicKeyProvider()
                .getKeyById(securityConfig.getDefaultAmazonEncryptionKeyId(), EncryptionKey.class);
        final MessageEncrypter messageEncrypter = new LPGMessageEncrypter(encryptionKey);

        // Outbound: sign requests with partner's signing private key
        final DecryptionKey signingKey = securityConfig.getSigningPrivateKeyProvider()
                .getKeyById(securityConfig.getDefaultSigningKeyId(), DecryptionKey.class);

        // Inbound: decrypt responses — composite of active + standby partner encryption private keys
        final SecurityKeyProvider decryptionProvider = new CompositeSecurityKeyProvider(
                Arrays.asList(
                        securityConfig.getEncryptionPrivateKeyProvider(),
                        securityConfig.getStandbyEncryptionPrivateKeyProvider()
                ));
        final MessageDecrypter messageDecrypter = new LPGMessageDecrypter(decryptionProvider);

        // Inbound: verify Amazon signatures — composite of active + standby Amazon signing public keys
        final SecurityKeyProvider verificationProvider = new CompositeSecurityKeyProvider(
                Arrays.asList(
                        securityConfig.getAmazonSigningPublicKeyProvider(),
                        securityConfig.getAmazonStandbySigningPublicKeyProvider()
                ));
        final MessageSignerAndVerifier messageSignerAndVerifier = new LPGMessageSignerAndVerifier(verificationProvider, signingKey);

        final MessageHandler messageHandler = new LPGMessageHandler(messageEncrypter, messageDecrypter, messageSignerAndVerifier);
        final AWSSigner stsAwsSigner = new AWSSigV4Signer(awsConfig::getAwsAccessKey, awsConfig::getAwsSecretKey, Optional::empty, () -> "us-east-1");
        final RoleManager roleManager = new DefaultRoleManager(stsAwsSigner);
        final AWSSigner lpgAwsSigner = new AssumedRoleSigner(roleManager, awsConfig.getRoleArn(), awsConfig.getSessionName(),
                () -> lpgConfig.getEndpoint().getRegion());

        requestHandler = new LPGRequestHandler(messageHandler, lpgAwsSigner, awsConfig.getApiKey(), new LPGExceptionConverter());
        lpgEndpoint = lpgConfig.getEndpoint();
        apiVersion = lpgConfig.getApiVersion();

        // --- Verification flow keys ---
        EncryptionKey verificationEncryptionKey = null;
        if (securityConfig.getVerificationPublicKeyProvider() != null) {
            verificationEncryptionKey = securityConfig.getVerificationPublicKeyProvider().getKeyById(securityConfig.getVerificationPublicKeyId(), EncryptionKey.class);
        }

        // Verification: encrypt secureHash with verification public key (Amazon-owned, unchanged)
        final MessageEncrypter verificationMessageEncrypter = new LPGMessageEncrypter(verificationEncryptionKey);

        // Verification: decrypt TKZ secret key — uses same composite decryption provider as main flow
        // (TKZ encrypts with partner's encryption public key, so we decrypt with partner's encryption private key)

        // Verification/TKZ flow: sign with partner's encryption private key using RS256
        // (TKZ has partner's encryption public key stored and only supports RS256)
        final DecryptionKey partnerSigningKeyForTkz = securityConfig.getEncryptionPrivateKeyProvider()
                .getKeyById(securityConfig.getDefaultEncryptionKeyId(), DecryptionKey.class);
        final MessageSignerAndVerifier verificationMessageSignerAndVerifier = new LPGMessageSignerAndVerifier(securityConfig.getVerificationPublicKeyProvider(), partnerSigningKeyForTkz);

        fieldEncryptionHandler = new TkzMessageHandler(verificationMessageEncrypter, messageDecrypter, verificationMessageSignerAndVerifier);
    }

    @Override
    public Offer getOffer(@NonNull final String offerId) throws IOException, LPGRequestException {
        final URL url = generateUrl(Resource.OFFER, offerId);
        return requestHandler.makeRequest(url, HttpMethod.GET, null, Offer.class, OfferDto.class);
    }

    @Override
    public String generatePerformanceDataAuthToken(@NonNull final String offerId) throws IOException, LPGRequestException {
        final URL url = generateUrl(Resource.OFFER, offerId, Operation.GENERATE_PERFORMANCE_DATA_AUTH_TOKEN);
        return requestHandler.makeRequest(url, HttpMethod.POST, null, String.class, PerformanceDataAuthTokenDto.class);
    }

    @Override
    public Offer createOffer(@NonNull final Offer offer, @NonNull final String merchantId, @NonNull final String idempotencyKey) throws IOException, LPGRequestException {
        final CreateOfferRequest request = CreateOfferRequest.builder()
                .marketplaceCountry(offer.getMarketplaceCountry())
                .productCode(offer.getProductCode())
                .partnerCode(offer.getPartnerCode())
                .merchantId(merchantId)
                .idempotencyKey(idempotencyKey)
                .details(
                        CreateOfferRequest.CreateOfferRequestDetails.builder()
                                .term(offer.getDetails().getTerm())
                                .termUnit(offer.getDetails().getTermUnit())
                                .minRepaymentRate(offer.getDetails().getMinRepaymentRate())
                                .maxRepaymentRate(offer.getDetails().getMaxRepaymentRate())
                                .minApr(offer.getDetails().getMinApr())
                                .maxApr(offer.getDetails().getMaxApr())
                                .minInterest(offer.getDetails().getMinInterest())
                                .maxInterest(offer.getDetails().getMaxInterest())
                                .minAmount(offer.getDetails().getMinAmount())
                                .maxAmount(offer.getDetails().getMaxAmount())
                                .currency(offer.getDetails().getCurrency())
                                .expireOn(new DateTime(offer.getDetails().getExpireOn(), TimeZone.getTimeZone("UTC")))
                                .automatedRepaymentsRequired(offer.getDetails().getAutomatedRepaymentsRequired())
                                .build()
                )
                .build();

        final URL url = generateTestUrl(Resource.OFFER);
        return requestHandler.makeRequest(url, HttpMethod.POST, request, CreateOfferRequest.class, CreateOfferRequest.class, OfferDto.class, Offer.class);
    }

    @Override
    public CreateOfferAsyncResponse createOfferAsync(@NonNull final CreateOfferAsyncRequest createOfferRequest) throws IOException, LPGRequestException {

        CreateOfferAsyncRequestDto createOfferRequestDto =
                CreateOfferAsyncRequestDto.builder()
                        .offerType(createOfferRequest.getOfferType().name())
                        .idempotencyKey(createOfferRequest.getIdempotencyKey())
                        .merchantId(createOfferRequest.getMerchantId())
                        .partnerCode(createOfferRequest.getPartnerCode())
                        .productCode(createOfferRequest.getProductCode())
                        .referenceCreditApprovalId(createOfferRequest.getReferenceCreditApprovalId())
                        .marketplaceCountry(createOfferRequest.getMarketplaceCountry())
                        .details(
                                CreateOfferAsyncRequestDto.OfferDetails
                                        .builder()
                                        .minApr(createOfferRequest.getDetails().getMinApr())
                                        .minInterest(createOfferRequest.getDetails().getMinInterest())
                                        .maxApr(createOfferRequest.getDetails().getMaxApr())
                                        .maxInterest(createOfferRequest.getDetails().getMaxInterest())
                                        .minAmount(createOfferRequest.getDetails().getMinAmount())
                                        .maxAmount(createOfferRequest.getDetails().getMaxAmount())
                                        .minRepaymentRate(createOfferRequest.getDetails().getMinRepaymentRate())
                                        .maxRepaymentRate(createOfferRequest.getDetails().getMaxRepaymentRate())
                                        .term(createOfferRequest.getDetails().getTerm())
                                        .termUnit(createOfferRequest.getDetails().getTermUnit().name())
                                        .currency(createOfferRequest.getDetails().getCurrency())
                                        .automatedRepaymentsRequired(createOfferRequest.getDetails().getAutomatedRepaymentsRequired())
                                        .expireOn(createOfferRequest.getDetails().getExpireOn())
                                        .build()
                        )
                        .build();

        final URL url = generateUrl(Resource.OFFER);
        return requestHandler.makeRequest(url,
                HttpMethod.POST,
                createOfferRequestDto,
                CreateOfferAsyncRequestDto.class,
                CreateOfferAsyncRequestDto.class,
                CreateOfferAsyncResponse.class,
                CreateOfferAsyncResponse.class);
    }


    @Override
    public Offer inquireOffer(@NonNull final String offerId) throws IOException, LPGRequestException {
        final URL url = generateTestUrl(Resource.OFFER, offerId);
        return requestHandler.makeRequest(url, HttpMethod.PUT, null, Offer.class, OfferDto.class);
    }

    @Override
    public URL getRedirectUrl(@NonNull final String offerId) throws IOException, LPGRequestException {
        final URL url = generateTestUrl(Resource.OFFER, offerId, Operation.REDIRECT);
        return requestHandler.makeRequest(url, HttpMethod.GET, null, URL.class, RedirectUrlDto.class);
    }

    @Override
    public Application getApplication(@NonNull final String applicationId) throws IOException, LPGRequestException {
        final URL url = generateUrl(Resource.APPLICATION, applicationId);
        return requestHandler.makeRequest(url, HttpMethod.GET, null, Application.class, ApplicationDto.class);
    }

    @Override
    public Application createApplication(@NonNull final Application application) throws IOException, LPGRequestException {
        final URL url = generateUrl(Resource.APPLICATION);
        return requestHandler.makeRequest(url, HttpMethod.POST, application, Application.class, ApplicationDto.class);
    }

    @Override
    public Application createApplication(@NonNull final String offerId, @NonNull final Date createdOn) throws IOException, LPGRequestException {
        final Offer offer = getOffer(offerId);
        final Application application = Application.builder()
                .marketplaceCountry(offer.getMarketplaceCountry())
                .productCode(offer.getProductCode())
                .partnerCode(offer.getPartnerCode())
                .applicationStatus(ApplicationStatus.CREATED)
                .offerId(offerId)
                .details(ApplicationDetails.builder()
                        .createdOn(createdOn)
                        .offerTerm(offer.getDetails().getTerm())
                        .offerTermUnit(offer.getDetails().getTermUnit())
                        .offerMinRepaymentRate(offer.getDetails().getMinRepaymentRate())
                        .offerMaxRepaymentRate(offer.getDetails().getMaxRepaymentRate())
                        .offerMinAmount(offer.getDetails().getMinAmount())
                        .offerMaxAmount(offer.getDetails().getMaxAmount())
                        .offerMinApr(offer.getDetails().getMinApr())
                        .offerMaxApr(offer.getDetails().getMaxApr())
                        .offerMinInterest(offer.getDetails().getMinInterest())
                        .offerMaxInterest(offer.getDetails().getMaxInterest())
                        .currency(offer.getDetails().getCurrency())
                        .build())
                .build();
        return createApplication(application);
    }

    @Override
    public Application updateApplication(Application application) throws IOException, LPGRequestException {
        final URL url = generateUrl(Resource.APPLICATION, application.getApplicationId());
        return requestHandler.makeRequest(url, HttpMethod.PUT, application, Application.class, ApplicationDto.class);
    }

    @Override
    public Application receiveApplication(@NonNull final String applicationId, @NonNull final Date receivedOn, final int requestedTerm,
                                          @NonNull final String requestedTermUnit, @NonNull final BigDecimal requestedAmount,
                                          @NonNull final BigDecimal requestedApr) throws IOException, LPGRequestException {
        final Application application = getApplication(applicationId);
        application.setApplicationStatus(ApplicationStatus.RECEIVED);
        application.getDetails().setReceivedOn(receivedOn);
        application.getDetails().setRequestTerm(requestedTerm);
        application.getDetails().setRequestTermUnit(requestedTermUnit);
        application.getDetails().setRequestAmount(requestedAmount);
        application.getDetails().setRequestApr(requestedApr);

        return updateApplication(application);
    }

    public Application receiveApplication(@NonNull final String applicationId, @NonNull final Date receivedOn,
                                          final int requestedTerm, final String requestedTermUnit,
                                          @NonNull final BigDecimal requestedRepaymentRate, BigDecimal requestedAmount,
                                          @NonNull final BigDecimal requestedInterest) throws IOException, LPGRequestException {
        final Application application = getApplication(applicationId);
        application.setApplicationStatus(ApplicationStatus.RECEIVED);
        application.getDetails().setReceivedOn(receivedOn);
        application.getDetails().setRequestTerm(requestedTerm);
        application.getDetails().setRequestTermUnit(requestedTermUnit);
        application.getDetails().setRequestRepaymentRate(requestedRepaymentRate);
        application.getDetails().setRequestAmount(requestedAmount);
        application.getDetails().setRequestInterest(requestedInterest);

        return updateApplication(application);
    }

    @Override
    public Application approveApplicationWithPendingSellerAction(@NonNull final String applicationId,
                                                                  @NonNull final Date approvedWithPendingSellerActionOn,
                                                                  final int requestedTerm, @NonNull final String requestedTermUnit,
                                                                  @NonNull final BigDecimal requestedAmount,
                                                                  @NonNull final BigDecimal requestedApr) throws IOException, LPGRequestException {
        final Application application = getApplication(applicationId);
        application.setApplicationStatus(ApplicationStatus.APPROVED_WITH_PENDING_SELLER_ACTION);
        application.getDetails().setApprovedWithPendingSellerActionOn(approvedWithPendingSellerActionOn);
        application.getDetails().setRequestTerm(requestedTerm);
        application.getDetails().setRequestTermUnit(requestedTermUnit);
        application.getDetails().setRequestAmount(requestedAmount);
        application.getDetails().setRequestApr(requestedApr);

        return updateApplication(application);
    }

    @Override
    public Application approveApplicationWithPendingSellerAction(@NonNull final String applicationId,
                                                                  @NonNull final Date approvedWithPendingSellerActionOn,
                                                                  final int requestedTerm, final String requestedTermUnit,
                                                                  @NonNull final BigDecimal requestedRepaymentRate,
                                                                  BigDecimal requestedAmount,
                                                                  @NonNull final BigDecimal requestedInterest) throws IOException, LPGRequestException {
        final Application application = getApplication(applicationId);
        application.setApplicationStatus(ApplicationStatus.APPROVED_WITH_PENDING_SELLER_ACTION);
        application.getDetails().setApprovedWithPendingSellerActionOn(approvedWithPendingSellerActionOn);
        application.getDetails().setRequestTerm(requestedTerm);
        application.getDetails().setRequestTermUnit(requestedTermUnit);
        application.getDetails().setRequestRepaymentRate(requestedRepaymentRate);
        application.getDetails().setRequestAmount(requestedAmount);
        application.getDetails().setRequestInterest(requestedInterest);

        return updateApplication(application);
    }

    @Override
    public Application approveApplication(@NonNull final String applicationId, @NonNull final Date approvedOn, final int approvedTerm,
                                          @NonNull final String approvedTermUnit, @NonNull final BigDecimal approvedAmount,
                                          @NonNull final BigDecimal approvedApr, final Boolean accountLockRequired,
                                          final Boolean accountChangeRequired) throws IOException, LPGRequestException {
        final Application application = getApplication(applicationId);
        application.setApplicationStatus(ApplicationStatus.APPROVED);
        application.getDetails().setApprovedOn(approvedOn);
        application.getDetails().setApprovedTerm(approvedTerm);
        application.getDetails().setApprovedTermUnit(approvedTermUnit);
        application.getDetails().setApprovedAmount(approvedAmount);
        application.getDetails().setApprovedApr(approvedApr);
        application.getDetails().setAccountLockRequired(accountLockRequired);
        application.getDetails().setAccountChangeRequired(accountChangeRequired);

        return updateApplication(application);
    }

    @Override
    public Application approveApplication(@NonNull final String applicationId, @NonNull final Date approvedOn,
                                          final int approvedTerm, final String approvedTermUnit,
                                          @NonNull  final BigDecimal approvedRepaymentRate, @NonNull final BigDecimal approvedAmount,
                                          @NonNull final BigDecimal approvedInterest, final Boolean accountLockRequired,
                                          final Boolean accountChangeRequired) throws IOException, LPGRequestException {
        final Application application = getApplication(applicationId);
        application.setApplicationStatus(ApplicationStatus.APPROVED);
        application.getDetails().setApprovedOn(approvedOn);
        application.getDetails().setApprovedTerm(approvedTerm);
        application.getDetails().setApprovedTermUnit(approvedTermUnit);
        application.getDetails().setApprovedRepaymentRate(approvedRepaymentRate);
        application.getDetails().setApprovedAmount(approvedAmount);
        application.getDetails().setApprovedInterest(approvedInterest);
        application.getDetails().setAccountLockRequired(accountLockRequired);
        application.getDetails().setAccountChangeRequired(accountChangeRequired);

        return updateApplication(application);
    }

    @Override
    public Application approveApplication(@NonNull final String applicationId, @NonNull final Date approvedOn, final int approvedTerm,
                                          @NonNull final String approvedTermUnit, @NonNull final BigDecimal approvedAmount,
                                          @NonNull final BigDecimal approvedApr, final Boolean accountLockRequired,
                                          final Boolean accountChangeRequired, Boolean automatedRepaymentsEnabled) throws IOException, LPGRequestException {
        Application application = setCommonApplicationFields(applicationId, approvedOn, approvedTerm, approvedTermUnit, approvedAmount,
                accountLockRequired, accountChangeRequired, automatedRepaymentsEnabled);
        application.getDetails().setApprovedApr(approvedApr);
        return updateApplication(application);
    }

    @Override
    public Application approveApplication(@NonNull final String applicationId, @NonNull final Date approvedOn,
                                          final int approvedTerm, final String approvedTermUnit,
                                          @NonNull final BigDecimal approvedRepaymentRate, @NonNull final BigDecimal approvedAmount,
                                          @NonNull final BigDecimal approvedInterest, final Boolean accountLockRequired,
                                          final Boolean accountChangeRequired, Boolean automatedRepaymentsEnabled) throws IOException, LPGRequestException {
        Application application = setCommonApplicationFields(applicationId, approvedOn, approvedTerm, approvedTermUnit, approvedAmount,
                accountLockRequired, accountChangeRequired, automatedRepaymentsEnabled);
        application.getDetails().setApprovedRepaymentRate(approvedRepaymentRate);
        application.getDetails().setApprovedInterest(approvedInterest);
        return updateApplication(application);
    }

    private Application setCommonApplicationFields(@NonNull final String applicationId, @NonNull final Date approvedOn,
                                                final int approvedTerm, final String approvedTermUnit,
                                                @NonNull final BigDecimal approvedAmount, final Boolean accountLockRequired,
                                                final Boolean accountChangeRequired, Boolean automatedRepaymentsEnabled) throws IOException, LPGRequestException {
        final Application application = getApplication(applicationId);
        application.setApplicationStatus(ApplicationStatus.APPROVED);
        application.getDetails().setApprovedOn(approvedOn);
        application.getDetails().setApprovedTerm(approvedTerm);
        application.getDetails().setApprovedTermUnit(approvedTermUnit);
        application.getDetails().setApprovedAmount(approvedAmount);
        application.getDetails().setAccountLockRequired(accountLockRequired);
        application.getDetails().setAccountChangeRequired(accountChangeRequired);
        application.getDetails().setAutomatedRepaymentsEnabled(automatedRepaymentsEnabled);

        return application;
    }


    @Override
    public Application confirmApplication(@NonNull final String applicationId, @NonNull final Date confirmedOn) throws IOException, LPGRequestException {
        final URL url = generateTestUrl(Resource.APPLICATION, applicationId);
        return requestHandler.makeRequest(url, HttpMethod.PUT, null, Application.class, ApplicationDto.class);
    }

    @Override
    public Application rejectApplication(@NonNull final String applicationId, @NonNull final Date rejectedOn, @NonNull final ApplicationRejectionCategory rejectionCategory,
                                         @NonNull final ApplicationRejectionCode rejectionCode) throws IOException, LPGRequestException {
        final Application application = getApplication(applicationId);
        application.setApplicationStatus(ApplicationStatus.REJECTED);
        application.getDetails().setRejectedOn(rejectedOn);
        application.getDetails().setRejectionCategory(rejectionCategory);
        application.getDetails().setStatusReasonCode(rejectionCode);

        return updateApplication(application);
    }

    @Override
    public Application abandonApplication(@NonNull final String applicationId, @NonNull final Date abandonedOn, @NonNull final ApplicationAbandonCode abandonCode) throws IOException, LPGRequestException {
        final Application application = getApplication(applicationId);
        application.setApplicationStatus(ApplicationStatus.ABANDONED);
        application.getDetails().setAbandonedOn(abandonedOn);
        application.getDetails().setStatusReasonCode(abandonCode);

        return updateApplication(application);
    }

    @Override
    public Application lockAccountAndConfirmApplication(@NonNull final String applicationId) throws IOException, LPGRequestException {
        final URL url = generateTestUrl(Resource.ACCOUNT_LOCK, applicationId);
        return requestHandler.makeRequest(url, HttpMethod.PUT, null, Application.class, ApplicationDto.class);
    }

    @Override
    public AccountLock getAccountLock(@NonNull final String accountLockId) throws IOException, LPGRequestException {
        final URL url = generateUrl(Resource.ACCOUNT_LOCK, accountLockId);
        return requestHandler.makeRequest(url, HttpMethod.GET, null, AccountLock.class, AccountLockDto.class);
    }

    @Override
    public AccountLock updateAccountLock(AccountLock accountLock) throws IOException, LPGRequestException {
        final URL url = generateUrl(Resource.ACCOUNT_LOCK, accountLock.getAccountLockId());
        return requestHandler.makeRequest(url, HttpMethod.PUT, accountLock, AccountLock.class, AccountLockDto.class);
    }

    @Override
    public AccountLock confirmAccountLock(@NonNull final String accountLockId) throws IOException, LPGRequestException {
        final AccountLock accountLock = getAccountLock(accountLockId);
        accountLock.setAccountLockStatus(AccountLockStatus.PARTNER_CONFIRMED);

        return updateAccountLock(accountLock);
    }

    @Override
    public AccountLock removeAccountLock(@NonNull final String accountLockId, @NonNull final String unlockReasonCode) throws IOException, LPGRequestException {
        final AccountLock accountLock = getAccountLock(accountLockId);
        accountLock.setAccountLockStatus(AccountLockStatus.UNLOCKED);
        accountLock.setUnlockReasonCode(unlockReasonCode);

        return updateAccountLock(accountLock);
    }

    @Override
    public AccountLock unlockAccount(@NonNull final String accountLockId, String unlockReasonCode) throws IOException, LPGRequestException {
        final AccountLock accountLock = getAccountLock(accountLockId);
        accountLock.setAccountLockStatus(AccountLockStatus.UNLOCKED);
        accountLock.setUnlockReasonCode(unlockReasonCode);
        accountLock.setAllowedUpdates(ALLOWED_UPDATES_LIST_UNLOCK);
        return updateAccountLock(accountLock);
    }

    @Override
    public AccountLock relockAccountLock(@NonNull final String accountLockId) throws IOException, LPGRequestException {
        final AccountLock accountLock = getAccountLock(accountLockId);
        accountLock.setAccountLockStatus(AccountLockStatus.LOCKED);
        accountLock.setAllowedUpdates(ALLOWED_UPDATES_LIST_RELOCK);
        return updateAccountLock(accountLock);
    }

    @Override
    public AccountLock relockAccount(@NonNull final String accountLockId, @NonNull final String accountTail, @NonNull final String bankIdCode, @NonNull final BankIdCodeType bankIdCodeType)  throws IOException, LPGRequestException {
        final AccountLock accountLock = getAccountLock(accountLockId);
        accountLock.setAccountLockStatus(AccountLockStatus.LOCKED);
        accountLock.setAllowedUpdates(ALLOWED_UPDATES_LIST_RELOCK);
        accountLock.setAccountTail(accountTail);
        accountLock.setBankIdCode(bankIdCode);
        accountLock.setBankIdCodeType(bankIdCodeType);
        return updateAccountLock(accountLock);
    }

    @Override
    public CreditApproval getCreditApproval(@NonNull final String creditApprovalId) throws IOException, LPGRequestException {
        final URL url = generateUrl(Resource.CREDIT_APPROVAL, creditApprovalId);
        return requestHandler.makeRequest(url, HttpMethod.GET, null, CreditApproval.class, CreditApprovalDto.class);
    }

    @Override
    public CreditApproval createCreditApproval(@NonNull final CreditApproval creditApproval) throws IOException, LPGRequestException {
        final URL url = generateUrl(Resource.CREDIT_APPROVAL);
        return requestHandler.makeRequest(url, HttpMethod.POST, creditApproval, CreditApproval.class, CreditApprovalDto.class);
    }

    @Override
    public CreditApproval createCreditApproval(@NonNull final String applicationId, int term, @NonNull final String termUnit, @NonNull final BigDecimal apr, @NonNull final BigDecimal amount, @NonNull final String currency, final BigDecimal processingFee) throws IOException, LPGRequestException {
        final Application application = getApplication(applicationId);
        final CreditApproval creditApproval = CreditApproval.builder()
                .partnerCode(application.getPartnerCode())
                .productCode(application.getProductCode())
                .marketplaceCountry(application.getMarketplaceCountry())
                .applicationId(applicationId)
                .creditApprovalStatus(CreditApprovalStatus.ACTIVE)
                .details(
                        CreditApprovalDetails.builder()
                                .term(term)
                                .termUnit(termUnit)
                                .processingFee(processingFee)
                                .currency(currency)
                                .apr(apr)
                                .amount(amount)
                                .build()
                )
                .build();

        return createCreditApproval(creditApproval);
    }

    @Override
    public CreditApproval createCreditApproval(@NonNull final String applicationId, int term, final String termUnit, @NonNull final BigDecimal repaymentRate, @NonNull final BigDecimal interest, @NonNull final BigDecimal amount, @NonNull final String currency, final BigDecimal processingFee) throws IOException, LPGRequestException {
        final Application application = getApplication(applicationId);
        final CreditApproval creditApproval = CreditApproval.builder()
                .partnerCode(application.getPartnerCode())
                .productCode(application.getProductCode())
                .marketplaceCountry(application.getMarketplaceCountry())
                .applicationId(applicationId)
                .creditApprovalStatus(CreditApprovalStatus.ACTIVE)
                .details(
                        CreditApprovalDetails.builder()
                                .term(term)
                                .termUnit(termUnit)
                                .repaymentRate(repaymentRate)
                                .processingFee(processingFee)
                                .currency(currency)
                                .interest(interest)
                                .amount(amount)
                                .build()
                )
                .build();

        return createCreditApproval(creditApproval);
    }

    @Override
    public CreditApproval updateCreditApproval(@NonNull final CreditApproval creditApproval) throws IOException, LPGRequestException {
        final URL url = generateUrl(Resource.CREDIT_APPROVAL, creditApproval.getCreditApprovalId());
        return requestHandler.makeRequest(url, HttpMethod.PUT, creditApproval, CreditApproval.class, CreditApprovalDto.class);
    }

    @Override
    public CreditApproval closeCreditApproval(@NonNull final String creditApprovalId, @NonNull final Date closedOn) throws IOException, LPGRequestException {
        final CreditApproval creditApproval = getCreditApproval(creditApprovalId);
        creditApproval.setCreditApprovalStatus(CreditApprovalStatus.CLOSED);
        // TODO: closedOn

        return updateCreditApproval(creditApproval);
    }

    @Override
    public Loan getLoan(@NonNull final String loanId) throws IOException, LPGRequestException {
        final URL url = generateUrl(Resource.LOAN, loanId);
        return requestHandler.makeRequest(url, HttpMethod.GET, null, Loan.class, LoanDto.class);
    }

    @Override
    public Loan createLoan(@NonNull final Loan loan) throws IOException, LPGRequestException {
        final URL url = generateUrl(Resource.LOAN);
        return requestHandler.makeRequest(url, HttpMethod.POST, loan, Loan.class, LoanDto.class);
    }

    @Override
    public Loan createLoan(@NonNull final String creditApprovalId, @NonNull final BigDecimal processingFee, @NonNull final Date createdOn, @NonNull final Date matureOn, @NonNull final String idempotencyKey) throws IOException, LPGRequestException {
        final CreditApproval creditApproval = getCreditApproval(creditApprovalId);
        final Loan loan = Loan.builder()
                .partnerCode(creditApproval.getPartnerCode())
                .productCode(creditApproval.getProductCode())
                .marketplaceCountry(creditApproval.getMarketplaceCountry())
                .creditApprovalId(creditApprovalId)
                .loanStatus(LoanStatus.ACTIVE)
                .idempotencyKey(idempotencyKey)
                .details(
                        LoanDetails.builder()
                                .term(creditApproval.getDetails().getTerm())
                                .termUnit(creditApproval.getDetails().getTermUnit())
                                .repaymentRate(creditApproval.getDetails().getRepaymentRate())
                                .processingFee(processingFee)
                                .createdOn(createdOn)
                                .matureOn(matureOn)
                                .totalPrincipalRepaid(BigDecimal.ZERO)
                                .totalInterestRepaid(BigDecimal.ZERO)
                                .totalFeesRepaid(BigDecimal.ZERO)
                                .pastDueAmount(BigDecimal.ZERO)
                                .outstandingPrincipalBalance(creditApproval.getDetails().getAmount())
                                .amount(creditApproval.getDetails().getAmount())
                                .apr(creditApproval.getDetails().getApr())
                                .interest(creditApproval.getDetails().getInterest())
                                .currency(creditApproval.getDetails().getCurrency())
                                .build()
                )
                .build();

        return createLoan(loan);
    }

    @Override
    public Loan updateLoan(@NonNull final Loan loan) throws IOException, LPGRequestException {
        final URL url = generateUrl(Resource.LOAN, loan.getLoanId());
        return requestHandler.makeRequest(url, HttpMethod.PUT, loan, Loan.class, LoanDto.class);
    }

    @Override
    public Loan closeLoan(@NonNull final String loanId, @NonNull final Date closedOn) throws IOException, LPGRequestException {
        final Loan loan = getLoan(loanId);
        loan.setLoanStatus(LoanStatus.CLOSED);
        loan.getDetails().setClosedOn(closedOn);

        return updateLoan(loan);
    }

    @Override
    public VerificationKey getVerificationKey(@NonNull final String applicationId) throws IOException, LPGRequestException {
        final URL url = generateUrl(Resource.APPLICATION, applicationId, Operation.VERIFY);
        final VerificationKey verificationKey = requestHandler.makeRequest(url, HttpMethod.GET, null, VerificationKey.class, VerificationKeyDto.class);

        // Decrypt secret key
        verificationKey.setSecretKey(fieldEncryptionHandler.decryptAndVerify(verificationKey.getSecretKey()));

        return verificationKey;
    }

    @Override
    public boolean verifyBankAccount(@NonNull final VerificationKey verificationKey, @NonNull final String applicationId, @NonNull final String accountNumber) throws IOException, LPGRequestException {
        return verifyBankAccount(verificationKey, applicationId, accountNumber, VerificationType.BANK_ACCOUNT_NUMBER);
    }

    @Override
    public boolean verifyBankAccount(@NonNull final String applicationId, @NonNull final String accountNumber) throws IOException, LPGRequestException {
        return verifyBankAccount(getVerificationKey(applicationId), applicationId, accountNumber);
    }

    @Override
    public boolean verifyBankAccount(@NonNull final VerificationKey verificationKey, @NonNull final String applicationId, @NonNull final String routingNumber, @NonNull final String accountNumber) throws IOException, LPGRequestException {
        return verifyBankAccount(verificationKey, applicationId, String.format("%s::%s", routingNumber, accountNumber), VerificationType.BANK_ACCOUNT_NUMBER_WITH_ROUTING_NUMBER);
    }

    @Override
    public boolean verifyBankAccount(@NonNull final String applicationId, @NonNull final String routingNumber, @NonNull final String accountNumber) throws IOException, LPGRequestException {
        return verifyBankAccount(getVerificationKey(applicationId), applicationId, routingNumber, accountNumber);
    }

    @Override
    public RepaymentSchedule createRepaymentSchedule(@NonNull final RepaymentSchedule repaymentSchedule) throws IOException, LPGRequestException {
        final URL url = generateUrl(Resource.REPAYMENT_SCHEDULE);
        return requestHandler.makeRequest(url, HttpMethod.POST, repaymentSchedule, RepaymentSchedule.class, RepaymentScheduleDto.class);
    }

    @Override
    public RepaymentSchedule createRepaymentSchedule(@NonNull final String loanId, @NonNull final Date createdOn, @NonNull final List<Repayment> repayments) throws IOException, LPGRequestException {
        final Loan loan = getLoan(loanId);
        final RepaymentSchedule repaymentSchedule = RepaymentSchedule.builder()
                .partnerCode(loan.getPartnerCode())
                .productCode(loan.getProductCode())
                .marketplaceCountry(loan.getMarketplaceCountry())
                .loanId(loan.getLoanId())
                .details(
                        RepaymentScheduleDetails.builder()
                                .createdOn(createdOn)
                                .repayments(repayments)
                                .build()
                )
                .build();

        return createRepaymentSchedule(repaymentSchedule);
    }

    @Override
    public RepaymentSchedule updateRepaymentSchedule(@NonNull final RepaymentSchedule repaymentSchedule) throws IOException, LPGRequestException {
        final URL url = generateUrl(Resource.REPAYMENT_SCHEDULE, repaymentSchedule.getRepaymentScheduleId());
        return requestHandler.makeRequest(url, HttpMethod.PUT, repaymentSchedule, RepaymentSchedule.class, RepaymentScheduleDto.class);
    }

    @Override
    public RepaymentSchedule getRepaymentSchedule(@NonNull final String repaymentScheduleId) throws IOException, LPGRequestException {
        final URL url = generateUrl(Resource.REPAYMENT_SCHEDULE, repaymentScheduleId);
        return requestHandler.makeRequest(url, HttpMethod.GET, null, RepaymentSchedule.class, RepaymentScheduleDto.class);
    }

    @Override
    public LenderPortfolioData createLenderPortfolioData(@NonNull final CreateLenderPortfolioDataRequest createLenderPortfolioDataRequest) throws IOException, LPGRequestException {
        final URL url = generateUrl(Resource.LENDER_PORTFOLIO_DATA);
        return requestHandler.makeRequest(url, HttpMethod.POST, createLenderPortfolioDataRequest, CreateLenderPortfolioDataRequest.class, CreateLenderPortfolioDataRequestDto.class, LenderPortfolioDataDto.class, LenderPortfolioData.class);
    }

    @Override
    public LenderPortfolioData getLenderPortfolioData(@NonNull final String lenderPortfolioDataId) throws IOException, LPGRequestException {
        final URL url = generateUrl(Resource.LENDER_PORTFOLIO_DATA, lenderPortfolioDataId);
        return requestHandler.makeRequest(url, HttpMethod.GET, null, LenderPortfolioData.class, LenderPortfolioDataDto.class);
    }

    @Override
    public LenderCreditApprovalData createLenderCreditApprovalData(@NonNull final CreateLenderCreditApprovalDataRequest createLenderCreditApprovalDataRequest) throws IOException, LPGRequestException {
        final URL url = generateUrl(Resource.LENDER_CREDIT_APPROVAL_DATA);
        return requestHandler.makeRequest(url, HttpMethod.POST, createLenderCreditApprovalDataRequest, CreateLenderCreditApprovalDataRequest.class, CreateLenderCreditApprovalDataRequestDto.class, LenderCreditApprovalDataDto.class, LenderCreditApprovalData.class);
    }

    @Override
    public LenderCreditApprovalData getLenderCreditApprovalData(@NonNull final String lenderCreditApprovalDataId) throws IOException, LPGRequestException {
        final URL url = generateUrl(Resource.LENDER_CREDIT_APPROVAL_DATA, lenderCreditApprovalDataId);
        return requestHandler.makeRequest(url, HttpMethod.GET, null, LenderCreditApprovalData.class, LenderCreditApprovalDataDto.class);
    }

    @Override
    public LenderLoanData createLenderLoanData(@NonNull final CreateLenderLoanDataRequest createLenderLoanDataRequest) throws IOException, LPGRequestException {
        final URL url = generateUrl(Resource.LENDER_LOAN_DATA);
        return requestHandler.makeRequest(url, HttpMethod.POST, createLenderLoanDataRequest, CreateLenderLoanDataRequest.class, CreateLenderLoanDataRequestDto.class, LenderLoanDataDto.class, LenderLoanData.class);
    }

    @Override
    public LenderLoanData getLenderLoanData(@NonNull final String lenderLoanDataId) throws IOException, LPGRequestException {
        final URL url = generateUrl(Resource.LENDER_LOAN_DATA, lenderLoanDataId);
        return requestHandler.makeRequest(url, HttpMethod.GET, null, LenderLoanData.class, LenderLoanDataDto.class);
    }

    @Override
    public LoanRepaymentDetail getLoanRepaymentDetail(@NonNull final String loanId) throws IOException, LPGRequestException {
        final URL url = generateUrl(Resource.LOAN_REPAYMENT_DETAIL, loanId);
        return requestHandler.makeRequest(url, HttpMethod.GET, null, LoanRepaymentDetail.class, LoanRepaymentDetailDto.class);
    }

    @Override
    public RepaymentCalculationMetadata getRepaymentCalculationMetadata(@NonNull final String creditApprovalId) throws IOException, LPGRequestException {
        final URL url = generateUrl(Resource.CREDIT_APPROVAL, creditApprovalId, Operation.REPAYMENT_CALCULATION_METADATA);
        return requestHandler.makeRequest(url, HttpMethod.GET, null, RepaymentCalculationMetadata.class, RepaymentCalculationMetadataDto.class);
    }

    private boolean verifyBankAccount(@NonNull final VerificationKey verificationKey,
    @NonNull final String applicationId, @NonNull final String hmacContent,
    @NonNull final VerificationType verificationType) throws IOException, LPGRequestException {
        final VerificationRequest verificationRequest;
        try {
            verificationRequest = VerificationRequest.builder()
                    .secureHash(fieldEncryptionHandler.signAndEncrypt(HashUtils.hmacSha256Base64(verificationKey.getSecretKeyBytes(), hmacContent)))
                    .verificationPayload(verificationKey.getVerificationPayload())
                    .verificationType(verificationType)
                    .build();
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new LPGInvalidRequestException(e);
        }

        final URL url = generateUrl(Resource.APPLICATION, applicationId, Operation.VERIFY);
        final Boolean result = requestHandler.makeRequest(url, HttpMethod.POST, verificationRequest, VerificationRequest.class, VerificationRequestDto.class, VerificationResultDto.class, Boolean.class);

        return Boolean.TRUE.equals(result);
    }

    @Override
    public boolean verifyWithLockAndConfirm(@NonNull final VerificationKey verificationKey, @NonNull final String applicationId, @NonNull final String accountNumber) throws IOException, LPGRequestException {
        return verifyWithLockAndConfirm(verificationKey, applicationId, accountNumber, VerificationType.BANK_ACCOUNT_NUMBER);
    }

    @Override
    public boolean verifyWithLockAndConfirm(@NonNull final String applicationId, @NonNull final String accountNumber) throws IOException, LPGRequestException {
        return verifyWithLockAndConfirm(getVerificationKey(applicationId), applicationId, accountNumber);
    }

    @Override
    public boolean verifyWithLockAndConfirm(@NonNull final VerificationKey verificationKey, @NonNull final String applicationId, @NonNull final String routingNumber, @NonNull final String accountNumber) throws IOException, LPGRequestException {
        return verifyWithLockAndConfirm(verificationKey, applicationId, String.format("%s::%s", routingNumber, accountNumber), VerificationType.BANK_ACCOUNT_NUMBER_WITH_ROUTING_NUMBER);
    }

    @Override
    public boolean verifyWithLockAndConfirm(@NonNull final String applicationId, @NonNull final String routingNumber, @NonNull final String accountNumber) throws IOException, LPGRequestException {
        return verifyWithLockAndConfirm(getVerificationKey(applicationId), applicationId, routingNumber, accountNumber);
    }

    private boolean verifyWithLockAndConfirm(@NonNull final VerificationKey verificationKey, @NonNull final String applicationId, @NonNull final String hmacContent, @NonNull final VerificationType verificationType) throws IOException, LPGRequestException {
        final VerificationRequest verificationRequest;
        try {
            verificationRequest = VerificationRequest.builder()
                    .secureHash(fieldEncryptionHandler.signAndEncrypt(HashUtils.hmacSha256Base64(verificationKey.getSecretKeyBytes(), hmacContent)))
                    .verificationPayload(verificationKey.getVerificationPayload())
                    .verificationType(verificationType)
                    .build();
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new LPGInvalidRequestException(e);
        }

        final URL url = generateUrl(Resource.APPLICATION, applicationId, Operation.VERIFY_WITH_LOCK_AND_CONFIRM);
        final Boolean result = requestHandler.makeRequest(url, HttpMethod.POST, verificationRequest, VerificationRequest.class, VerificationRequestDto.class, VerificationResultDto.class, Boolean.class);

        return Boolean.TRUE.equals(result);
    }

    private URL generateUrl(@NonNull final Resource resource) throws MalformedURLException {
        return new URL(collectionUrl(resource));
    }

    private URL generateUrl(@NonNull final Resource resource, final String resourceId) throws MalformedURLException {
        return generateUrl(resource, resourceId, null);
    }

    private URL generateUrl(@NonNull final Resource resource, final String resourceId, final Operation operation) throws MalformedURLException {
        return resourceUrl(collectionUrl(resource), resourceId, operation);
    }

    private URL generateTestUrl(@NonNull final Resource resource) throws MalformedURLException {
        return new URL(testCollectionUrl(resource));
    }

    private URL generateTestUrl(@NonNull final Resource resource, final String resourceId) throws MalformedURLException {
        return generateTestUrl(resource, resourceId, null);
    }

    private URL generateTestUrl(@NonNull final Resource resource, final String resourceId, final Operation operation) throws MalformedURLException {
        return resourceUrl(testCollectionUrl(resource), resourceId, operation);
    }

    private String collectionUrl(final Resource resource) {
        return String.format("%s/%s/%s", lpgEndpoint.getEndpoint(), apiVersion.getValue(), resource.getValue());
    }

    private String testCollectionUrl(final Resource resource) {
        final Set<LPGEndpoint> prodEndpoints = ImmutableSet.of(
                LPGEndpoint.NA_PROD,
                LPGEndpoint.EU_PROD,
                LPGEndpoint.AP_PROD
        );
        if (prodEndpoints.contains(lpgEndpoint)) {
            throw new LPGInvalidRequestException("Invalid endpoint; only non-production endpoints are supported for this API.");
        }
        return String.format("%s/test/%s", lpgEndpoint.getEndpoint(), resource.getValue());
    }

    private static URL resourceUrl(final String collectionUrl, final String resourceId, final Operation operation) throws MalformedURLException {
        String url = String.format("%s/%s", collectionUrl, requireValidResourceId(resourceId));
        if (operation != null) {
            url = String.format("%s/%s", url, operation.getValue());
        }
        return new URL(url);
    }

    // An ID is pasted into the path as is, so it must be one path segment that needs no encoding:
    // no '/', '?', '#', '%' or space, and not "." or "..". Every ID LPG issues has this form.
    // The value is not echoed: it may be caller input that ends up in logs.
    private static String requireValidResourceId(final String resourceId) {
        if (resourceId == null || !RESOURCE_ID.matcher(resourceId).matches()) {
            throw new LPGInvalidRequestException("Invalid resource ID");
        }
        return resourceId;
    }
}

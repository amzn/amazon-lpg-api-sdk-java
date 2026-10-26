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

package com.amazon.lending.client;

import com.amazon.lending.config.AWSConfig;
import com.amazon.lending.config.LPGConfig;
import com.amazon.lending.config.SecurityConfig;
import com.amazon.lending.http.exception.LPGRequestException;
import com.amazon.lending.model.AccountLock;
import com.amazon.lending.model.Application;
import com.amazon.lending.model.ApplicationAbandonCode;
import com.amazon.lending.model.ApplicationRejectionCategory;
import com.amazon.lending.model.ApplicationRejectionCode;
import com.amazon.lending.model.BankIdCodeType;
import com.amazon.lending.model.CreateOfferAsyncRequest;
import com.amazon.lending.model.CreateOfferAsyncResponse;
import com.amazon.lending.model.CreditApproval;
import com.amazon.lending.model.LenderCreditApprovalData;
import com.amazon.lending.model.LenderLoanData;
import com.amazon.lending.model.LenderPortfolioData;
import com.amazon.lending.model.Loan;
import com.amazon.lending.model.LoanRepaymentDetail;
import com.amazon.lending.model.RepaymentCalculationMetadata;
import com.amazon.lending.model.Offer;
import com.amazon.lending.model.Repayment;
import com.amazon.lending.model.RepaymentSchedule;
import com.amazon.lending.model.VerificationKey;
import com.amazon.lending.model.CreateLenderPortfolioDataRequest;
import com.amazon.lending.model.CreateLenderCreditApprovalDataRequest;
import com.amazon.lending.model.CreateLenderLoanDataRequest;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Date;
import java.util.List;

public interface LendingPartnerGatewayClient {

    /**
     * Initialize the client; required prior to making any other calls.
     * @param securityConfig {@link SecurityConfig} containing security configuration
     * @param awsConfig {@link AWSConfig} containing AWS configuration
     * @param lpgConfig {@link LPGConfig} containing LPG configuration
     * @throws Exception in the event that some aspect of configuration cannot be loaded
     */
    void initialize(SecurityConfig securityConfig, AWSConfig awsConfig, LPGConfig lpgConfig) throws Exception;

    /**
     * Get offer by ID.
     * @param offerId OfferDto ID
     * @return {@link Offer} instance with the specified offer ID
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    Offer getOffer(String offerId) throws IOException, LPGRequestException;

    /**
     * Generate the authToken for contacting the Amazon Selling Partner API (SP-API) to retrieve customer performance data.
     * @param offerId OfferDto ID
     * @return authToken
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    String generatePerformanceDataAuthToken(String offerId) throws IOException, LPGRequestException;

    /**
     * <b>This API is only available in the test environment.</b><br>
     * Create an offer.
     * @param offer {@link Offer} instance containing the desired data
     * @param merchantId Merchant ID for which the offer should be created
     * @param idempotencyKey Unique ID for the request
     * @return Created offer
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    Offer createOffer(Offer offer, String merchantId, String idempotencyKey) throws IOException, LPGRequestException;

    /**
     * Create an offer in async manner.
     * @param createOfferRequest {@link CreateOfferAsyncRequest} create offer request object
     * @return CreateOfferResponse {@link CreateOfferAsyncResponse} response of the API
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    CreateOfferAsyncResponse createOfferAsync(CreateOfferAsyncRequest createOfferRequest) throws IOException, LPGRequestException;

    /**
     * <b>This API is only available in the test environment.</b><br>
     * Transition an offer to the INQUIRED state.
     * @param offerId OfferDto ID
     * @return Updated offer
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    Offer inquireOffer(String offerId) throws IOException, LPGRequestException;

    /**
     * <b>This API is only available in the test environment.</b><br>
     * Get a redirect URL for a given offer ID.
     * @param offerId Offer ID
     * @return Redirect URL
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    URL getRedirectUrl(String offerId) throws IOException, LPGRequestException;

    /**
     * Get application by ID.
     * @param applicationId ApplicationDto ID
     * @return {@link Application} instance with the specified application ID
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    Application getApplication(String applicationId) throws IOException, LPGRequestException;

    /**
     * Create application.
     * @param application {@link Application} instance containing the desired data
     * @return {@link Application} instance that was created
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    Application createApplication(Application application) throws IOException, LPGRequestException;

    /**
     * Create application.
     * @param offerId OfferDto ID for the application
     * @param createdOn date on which the application was created
     * @return {@link Application} instance that was created
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    Application createApplication(String offerId, Date createdOn) throws IOException, LPGRequestException;

    /**
     * Update an application.
     * @param application {@link Application} instance containing the desired data
     * @return Updated application
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    Application updateApplication(Application application) throws IOException, LPGRequestException;

    /**
     * Transition an application to the RECEIVED state.
     * @param applicationId ApplicationDto ID
     * @param receivedOn Date on which the application was received
     * @param requestedTerm Term requested in application
     * @param requestedTermUnit TermUnit requested in application
     * @param requestedAmount Amount requested in application
     * @param requestedApr APR requested in application
     * @return {@link Application} instance that was updated
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    Application receiveApplication(String applicationId, Date receivedOn, int requestedTerm, String requestedTermUnit, BigDecimal requestedAmount, BigDecimal requestedApr) throws IOException, LPGRequestException;

    /**
     * Transition an application to the RECEIVED state (overloaded method with additional fields relevant to new financing features)
     * @param applicationId ApplicationDto ID
     * @param receivedOn Date on which the application was received
     * @param requestedTerm Term requested in application
     * @param requestedTermUnit TermUnit requested in application
     * @param requestedRepaymentRate RepaymentRate requested in application
     * @param requestedAmount Amount requested in application
     * @param requestedInterest Interest requested in application
     * @return {@link Application} instance that was updated
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    Application receiveApplication(String applicationId, Date receivedOn, int requestedTerm, String requestedTermUnit, BigDecimal requestedRepaymentRate, BigDecimal requestedAmount, BigDecimal requestedInterest) throws IOException, LPGRequestException;

    /**
     * Transition an application to the APPROVED_WITH_PENDING_SELLER_ACTION state.
     * @param applicationId ApplicationDto ID
     * @param approvedWithPendingSellerActionOn Date on which the application was pre-approved
     * @param requestedTerm Term requested in application
     * @param requestedTermUnit TermUnit requested in application
     * @param requestedAmount Amount requested in application
     * @param requestedApr APR requested in application
     * @return {@link Application} instance that was updated
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    Application approveApplicationWithPendingSellerAction(String applicationId, Date approvedWithPendingSellerActionOn, int requestedTerm, String requestedTermUnit, BigDecimal requestedAmount, BigDecimal requestedApr) throws IOException, LPGRequestException;

    /**
     * Transition an application to the APPROVED_WITH_PENDING_SELLER_ACTION state (overloaded with repaymentRate and interest).
     * @param applicationId ApplicationDto ID
     * @param approvedWithPendingSellerActionOn Date on which the application was pre-approved
     * @param requestedTerm Term requested in application
     * @param requestedTermUnit TermUnit requested in application
     * @param requestedRepaymentRate RepaymentRate requested in application
     * @param requestedAmount Amount requested in application
     * @param requestedInterest Interest requested in application
     * @return {@link Application} instance that was updated
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    Application approveApplicationWithPendingSellerAction(String applicationId, Date approvedWithPendingSellerActionOn, int requestedTerm, String requestedTermUnit, BigDecimal requestedRepaymentRate, BigDecimal requestedAmount, BigDecimal requestedInterest) throws IOException, LPGRequestException;

    /**
     * Transition an application to the APPROVED state.
     * @param applicationId ApplicationDto ID
     * @param approvedOn Date on which the application was approved
     * @param approvedTerm Term approved
     * @param approvedTermUnit TermUnit approved
     * @param approvedAmount Amount approved
     * @param approvedApr APR approved
     * @param accountLockRequired account lock required
     * @param accountChangeRequired account change required
     * @return {@link Application} instance that was updated
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    Application approveApplication(String applicationId, Date approvedOn, int approvedTerm, String approvedTermUnit, BigDecimal approvedAmount, BigDecimal approvedApr, Boolean accountLockRequired, Boolean accountChangeRequired) throws IOException, LPGRequestException;

    /**
     * Transition an application to the APPROVED state (overloaded method with additional fields relevant to new financing features)
     * @param applicationId ApplicationDto ID
     * @param approvedOn Date on which the application was approved
     * @param approvedTerm Term approved
     * @param approvedTermUnit TermUnit approved
     * @param approvedRepaymentRate repaymentRate approved
     * @param approvedAmount Amount approved
     * @param approvedInterest Interest approved
     * @param accountLockRequired account lock required
     * @param accountChangeRequired account change required
     * @return {@link Application} instance that was updated
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    Application approveApplication(String applicationId, Date approvedOn, int approvedTerm, String approvedTermUnit, BigDecimal approvedRepaymentRate, BigDecimal approvedAmount, BigDecimal approvedInterest, Boolean accountLockRequired, Boolean accountChangeRequired) throws IOException, LPGRequestException;

    /**
     * Transition an application to the APPROVED state.
     * @param applicationId ApplicationDto ID
     * @param approvedOn Date on which the application was approved
     * @param approvedTerm Term approved
     * @param approvedTermUnit TermUnit approved
     * @param approvedAmount Amount approved
     * @param approvedApr APR approved
     * @param accountLockRequired account lock required
     * @param accountChangeRequired account change required
     * @param automatedRepaymentsEnabled automated repayments enabled
     * @return {@link Application} instance that was updated
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    Application approveApplication(String applicationId, Date approvedOn, int approvedTerm, String approvedTermUnit, BigDecimal approvedAmount, BigDecimal approvedApr, Boolean accountLockRequired, Boolean accountChangeRequired, Boolean automatedRepaymentsEnabled) throws IOException, LPGRequestException;

    /**
     * Transition an application to the APPROVED state (overloaded method with additional fields relevant to new financing features)
     * @param applicationId ApplicationDto ID
     * @param approvedOn Date on which the application was approved
     * @param approvedTerm Term approved
     * @param approvedTermUnit TermUnit approved
     * @param approvedRepaymentRate repaymentRate approved
     * @param approvedAmount Amount approved
     * @param approvedInterest Interest approved
     * @param accountLockRequired account lock required
     * @param accountChangeRequired account change required
     * @param automatedRepaymentsEnabled automated repayments enabled
     * @return {@link Application} instance that was updated
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    Application approveApplication(String applicationId, Date approvedOn, int approvedTerm, String approvedTermUnit, BigDecimal approvedRepaymentRate, BigDecimal approvedAmount, BigDecimal approvedInterest, Boolean accountLockRequired, Boolean accountChangeRequired, Boolean automatedRepaymentsEnabled) throws IOException, LPGRequestException;


    /**
     * <b>This API is only available in the test environment.</b><br>
     * Transition an application to the CONFIRMED state.
     * @param applicationId ApplicationDto ID
     * @param confirmedOn Date on which the application was confirmed
     * @return {@link Application} instance that was updated
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of request error
     */
    Application confirmApplication(String applicationId, Date confirmedOn) throws IOException, LPGRequestException;

    /**
     * Transition an application to the REJECTED state.
     * @param applicationId ApplicationDto ID
     * @param rejectedOn Date on which the application was rejected
     * @param rejectionCategory Category for which the application was rejected
     * @param rejectionCode Code for which the application was rejected
     * @return {@link Application} instance that was updated
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    Application rejectApplication(String applicationId, Date rejectedOn, ApplicationRejectionCategory rejectionCategory, ApplicationRejectionCode rejectionCode) throws IOException, LPGRequestException;

    /**
     * Transition an application to the ABANDONED state.
     * @param applicationId ApplicationDto ID
     * @param abandonedOn Date on which the application was abandoned
     * @param abandonCode Code for which the application was abandoned
     * @return {@link Application} instance that was updated
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    Application abandonApplication(String applicationId, Date abandonedOn, ApplicationAbandonCode abandonCode) throws IOException, LPGRequestException;

    /**
     * <b>This API is only available in the test environment.</b><br>
     * @param applicationId Application ID
     * @return Updated application
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    Application lockAccountAndConfirmApplication(String applicationId) throws IOException, LPGRequestException;

    /**
     * Get account lock by ID.
     * @param accountLockId Account lock ID
     * @return {@link AccountLock} instance with the specified ID
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    AccountLock getAccountLock(String accountLockId) throws IOException, LPGRequestException;

    /**
     * Update an account lock.
     * @param accountLock {@link AccountLock} instance containing the desired data
     * @return Updated account lock
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    AccountLock updateAccountLock(AccountLock accountLock) throws IOException, LPGRequestException;

    /**
     * Transition an account lock to the PARTNER_CONFIRMED state.
     * @param accountLockId Account lock ID
     * @return {@link AccountLock} instance that was updated
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    AccountLock confirmAccountLock(String accountLockId) throws IOException, LPGRequestException;

    /**
     * Transition an account lock to the UNLOCKED state. Only supports transition from LOCKED to UNLOCKED state.
     * @param accountLockId Account lock ID
     * @param unlockReasonCode ReasonCode for which the account was unlocked
     * @return {@link AccountLock} instance that was updated
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    AccountLock removeAccountLock(String accountLockId, String unlockReasonCode) throws IOException, LPGRequestException;

    /**
     * Transition an account lock to the UNLOCKED state. Supports transition from LOCKED to UNLOCKED and PARTNER_CONFIRMED to UNLOCKED state.
     * @param accountLockId Account lock ID
     * @param unlockReasonCode ReasonCode for which the account was unlocked
     * @return {@link AccountLock} instance that was updated
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    AccountLock unlockAccount(String accountLockId, String unlockReasonCode) throws IOException, LPGRequestException;

    /**
     * Transition an account lock to LOCKED state.
     * @param accountLockId Account lock ID
     * @return {@link AccountLock} instance that was updated
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    AccountLock relockAccountLock(String accountLockId) throws IOException, LPGRequestException;

    /**
     * Transition an account lock to LOCKED state.
     * @param accountLockId Account lock ID
     * @param accountTail Four digit tail of the account
     * @param bankIdCode Bank ID code
     * @param bankIdCodeType Bank ID code type
     * @return {@link AccountLock} instance that was updated
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    AccountLock relockAccount(String accountLockId, String accountTail, String bankIdCode, BankIdCodeType bankIdCodeType) throws IOException, LPGRequestException;

    /**
     * Get credit approval by ID.
     * @param creditApprovalId Credit approval ID
     * @return {@link CreditApproval} instance with the specified ID
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    CreditApproval getCreditApproval(String creditApprovalId) throws IOException, LPGRequestException;

    /**
     * Create a credit approval.
     * @param creditApproval {@link CreditApproval} instance containing the desired data
     * @return {@link CreditApproval} instance that was created
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    CreditApproval createCreditApproval(CreditApproval creditApproval) throws IOException, LPGRequestException;

    /**
     * Create a credit approval.
     * @param applicationId Application ID
     * @param term Term of credit approval
     * @param termUnit Term unit of credit approval
     * @param apr APR of credit approval
     * @param amount Amount of credit approval
     * @param currency Currency of credit approval
     * @param processingFee Processing fee of credit approval
     * @return Created credit approval
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    CreditApproval createCreditApproval(String applicationId, int term, String termUnit, BigDecimal apr, BigDecimal amount, String currency, BigDecimal processingFee) throws IOException, LPGRequestException;

    /**
     * Create a credit approval (overloaded method with additional fields relevant to new financing features)
     * @param applicationId Application ID
     * @param term Term of credit approval
     * @param termUnit Term unit of credit approval
     * @param repaymentRate RepaymentRate of credit approval
     * @param interest Interest of credit approval
     * @param amount Amount of credit approval
     * @param currency Currency of credit approval
     * @param processingFee Processing fee of credit approval
     * @return Created credit approval
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    CreditApproval createCreditApproval(String applicationId, int term, String termUnit, BigDecimal repaymentRate, BigDecimal interest, BigDecimal amount, String currency, BigDecimal processingFee) throws IOException, LPGRequestException;

    /**
     * Create a credit approval.
     * @param creditApproval {@link CreditApproval} instance containing the desired data
     * @return Created credit approval
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    CreditApproval updateCreditApproval(CreditApproval creditApproval) throws IOException, LPGRequestException;

    /**
     * Transition a credit approval to the CLOSED state.
     * @param creditApprovalId Credit approval ID
     * @param closedOn Date on which the credit approval was closed
     * @return {@link CreditApproval} instance that was updated
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    CreditApproval closeCreditApproval(String creditApprovalId, Date closedOn) throws IOException, LPGRequestException;

    /**
     * Get loan by ID.
     * @param loanId LoanDto ID
     * @return {@link Loan} instance with the specified ID
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    Loan getLoan(String loanId) throws IOException, LPGRequestException;

    /**
     * Create a loan.
     * @param loan {@link Loan} instance containing the desired data
     * @return {@link Loan} instance that was created
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    Loan createLoan(Loan loan) throws IOException, LPGRequestException;

    /**
     * Create a loan.
     * @param creditApprovalId Credit approval ID
     * @param processingFee Processing fee for the loan
     * @param createdOn Date of loan creation
     * @param matureOn Date of loan maturity
     * @param idempotencyKey Unique ID for the request
     * @return Created loan
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    Loan createLoan(String creditApprovalId, BigDecimal processingFee, Date createdOn, Date matureOn, String idempotencyKey) throws IOException, LPGRequestException;

    /**
     * Update a loan.
     * @param loan {@link Loan} instance containing the desired data
     * @return {@link Loan} instance that was updated
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    Loan updateLoan(Loan loan) throws IOException, LPGRequestException;

    /**
     * Transition a loan to the CLOSED state.
     * @param loanId LoanDto ID
     * @param closedOn Date on which the loan was closed
     * @return {@link Loan} instance that was updated
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    Loan closeLoan(String loanId, Date closedOn) throws IOException, LPGRequestException;

    /**
     * Get a verification key.
     * @param applicationId Application ID
     * @return {@link VerificationKey} instance
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    VerificationKey getVerificationKey(String applicationId) throws IOException, LPGRequestException;

    /**
     * Verify a bank account number.
     * @param verificationKey {@link VerificationKey} instance retrieved from {@link #getVerificationKey(String)}
     * @param applicationId Application ID
     * @param accountNumber Account number to compare against
     * @return True if the provided account number matches what the seller has on file, otherwise false
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    boolean verifyBankAccount(VerificationKey verificationKey, String applicationId, String accountNumber) throws IOException, LPGRequestException;

    /**
     * Verify a bank account number.
     * @param applicationId Application ID
     * @param accountNumber Account number to compare against
     * @return True if the provided account number matches what the seller has on file, otherwise false
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    boolean verifyBankAccount(String applicationId, String accountNumber) throws IOException, LPGRequestException;

    /**
     * Verify a bank account number with routing number.
     * @param verificationKey {@link VerificationKey} instance retrieved from {@link #getVerificationKey(String)}
     * @param applicationId Application ID
     * @param routingNumber Routing number
     * @param accountNumber Account number to compare against
     * @return True if the provided routing number and account number match what the seller has on file, otherwise false
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    boolean verifyBankAccount(VerificationKey verificationKey, String applicationId, String routingNumber, String accountNumber) throws IOException, LPGRequestException;

    /**
     * Verify a bank account number with routing number.
     * @param applicationId Application ID
     * @param routingNumber Routing number
     * @param accountNumber Account number to compare against
     * @return True if the provided routing number and account number match what the seller has on file, otherwise false
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    boolean verifyBankAccount(String applicationId, String routingNumber, String accountNumber) throws IOException, LPGRequestException;

    /**
     * Create a repayment schedule.
     * @param repaymentSchedule {@link RepaymentSchedule} instance containing the desired data
     * @return {@link RepaymentSchedule} instance that was created
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    RepaymentSchedule createRepaymentSchedule(RepaymentSchedule repaymentSchedule) throws IOException, LPGRequestException;

    /**
     * Create a repayment schedule.
     * @param loanId Loan ID
     * @param createdOn Date on which the repayment schedule was created
     * @param repayments List of {@link Repayment} objects
     * @return {@link RepaymentSchedule} instance that was created
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    RepaymentSchedule createRepaymentSchedule(String loanId, Date createdOn, List<Repayment> repayments) throws IOException, LPGRequestException;

    /**
     * Update a repayment schedule.
     * @param repaymentSchedule {@link RepaymentSchedule} instance containing the desired data
     * @return {@link RepaymentSchedule} instance that was updated
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    RepaymentSchedule updateRepaymentSchedule(RepaymentSchedule repaymentSchedule) throws IOException, LPGRequestException;

    /**
     * Get a repayment schedule.
     * @param repaymentScheduleId Repayment schedule ID
     * @return {@link RepaymentSchedule} instance
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    RepaymentSchedule getRepaymentSchedule(String repaymentScheduleId) throws IOException, LPGRequestException;

    /**
     * Get loan repayment detail by loan ID.
     * @param loanId Loan ID
     * @return {@link LoanRepaymentDetail} instance containing the loan repayment details
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    LoanRepaymentDetail getLoanRepaymentDetail(String loanId) throws IOException, LPGRequestException;

    /**
     * Get repayment calculation metadata for an active MCA (Merchant Cash Advance) credit approval.
     * Returns seller sales data (baseAmount, netPayable) and the expected payout date,
     * enabling partners to calculate repayment amounts.
     * @param creditApprovalId Credit approval ID (partner-scoped token, as returned by other LPG APIs)
     * @return {@link RepaymentCalculationMetadata} instance containing seller sales data and expected payout date
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    RepaymentCalculationMetadata getRepaymentCalculationMetadata(String creditApprovalId) throws IOException, LPGRequestException;

    /**
     * Create a lender portfolio data instance.
     * @param createLenderPortfolioDataRequest {@link CreateLenderPortfolioDataRequest} instance includes the necessary
     *                                    information for creating a LenderPortfolioData instance
     * @return {@link LenderPortfolioData} instance that was created
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    LenderPortfolioData createLenderPortfolioData(CreateLenderPortfolioDataRequest createLenderPortfolioDataRequest) throws IOException, LPGRequestException;

    /**
     * Get a lender portfolio data instance.
     * @param lenderPortfolioDataId Lender Portfolio Data ID
     * @return {@link LenderPortfolioData} instance
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    LenderPortfolioData getLenderPortfolioData(String lenderPortfolioDataId) throws IOException, LPGRequestException;

    /**
     * Create a lender credit approval data instance.
     * @param createLenderCreditApprovalDataRequest {@link CreateLenderCreditApprovalDataRequest} instance includes the necessary
     *                                    information for creating a LenderCreditApprovalData instance
     * @return {@link LenderCreditApprovalData} instance that was created
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    LenderCreditApprovalData createLenderCreditApprovalData(CreateLenderCreditApprovalDataRequest createLenderCreditApprovalDataRequest) throws IOException, LPGRequestException;

    /**
     * Get a lender credit approval data instance.
     * @param lenderCreditApprovalDataId Lender Credit Approval Data ID
     * @return {@link LenderCreditApprovalData} instance
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    LenderCreditApprovalData getLenderCreditApprovalData(String lenderCreditApprovalDataId) throws IOException, LPGRequestException;

    /**
     * Create a lender loan data instance.
     * @param createLenderLoanDataRequest {@link CreateLenderLoanDataRequest} instance includes the necessary
     *                                    information for creating a LenderLoanData instance
     * @return {@link LenderLoanData} instance that was created
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    LenderLoanData createLenderLoanData(CreateLenderLoanDataRequest createLenderLoanDataRequest) throws IOException, LPGRequestException;

    /**
     * Get a lender loan data instance.
     * @param lenderLoanDataId Lender Loan Data ID
     * @return {@link LenderLoanData} instance
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    LenderLoanData getLenderLoanData(String lenderLoanDataId) throws IOException, LPGRequestException;

    /**
     * Verify the bank account details, if matches lock the account and confirm the application.
     * @param verificationKey {@link VerificationKey} instance retrieved from {@link #getVerificationKey(String)}
     * @param applicationId Application ID
     * @param accountNumber Account number to compare against
     * @return True if the provided account number matches what the seller has on file, otherwise false
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    boolean verifyWithLockAndConfirm(VerificationKey verificationKey, String applicationId, String accountNumber) throws IOException, LPGRequestException;

    /**
     * Verify the bank account details, if matches lock the account and confirm the application.
     * @param applicationId Application ID
     * @param accountNumber Account number to compare against
     * @return True if the provided account number matches what the seller has on file, otherwise false
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    boolean verifyWithLockAndConfirm(String applicationId, String accountNumber) throws IOException, LPGRequestException;

    /**
     * Verify the bank account details with routing number, if matches lock the account and confirm the application.
     * @param verificationKey {@link VerificationKey} instance retrieved from {@link #getVerificationKey(String)}
     * @param applicationId Application ID
     * @param routingNumber Routing number
     * @param accountNumber Account number to compare against
     * @return True if the provided routing number and account number match what the seller has on file, otherwise false
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    boolean verifyWithLockAndConfirm(VerificationKey verificationKey, String applicationId, String routingNumber, String accountNumber) throws IOException, LPGRequestException;

    /**
     * Verify the bank account details with routing number, if matches lock the account and confirm the application.
     * @param applicationId Application ID
     * @param routingNumber Routing number
     * @param accountNumber Account number to compare against
     * @return True if the provided routing number and account number match what the seller has on file, otherwise false
     * @throws IOException in the event of network failure
     * @throws LPGRequestException in the event of a request error
     */
    boolean verifyWithLockAndConfirm(String applicationId, String routingNumber, String accountNumber) throws IOException, LPGRequestException;
}

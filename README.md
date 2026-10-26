# Amazon Lending Partner Gateway Java SDK

Java client for the Amazon Lending Partner Gateway (LPG). It handles request
signing with SigV4 and an API key, JWS-in-JWE payload security, and typed models
for offers, applications, credit approvals, loans and reporting.

## Installation

Maven:

```xml
<dependency>
    <groupId>software.amazon.lending</groupId>
    <artifactId>amazon-lpg-api-sdk-java</artifactId>
    <version>1.0.0</version>
</dependency>
```

Gradle:

```groovy
implementation 'software.amazon.lending:amazon-lpg-api-sdk-java:1.0.0'
```

Available on Maven Central from 1.0.0.

## Requirements

Java 8 or later; the SDK is built and tested on Java 8 and Java 17.

You also need the credentials and keys issued during Amazon Lending partner
onboarding: an allowlisted IAM role, an API key, the RSA key pairs you register
with Amazon Lending, and Amazon's public keys.

## Security model

Every request is protected at two independent layers.

**Transport.** The SDK calls AWS STS `AssumeRole` to exchange the access key in
your `AWSConfig` for temporary credentials on the IAM role you registered with
Amazon Lending, caches them until shortly before they expire, then signs each
request with SigV4 for the `execute-api` service and sends your API key in the
`x-api-key` header.

**Payload.** Request and response bodies are signed as a JWS and that JWS is
nested inside a JWE (RSA-OAEP-256 key wrapping with A256GCM content encryption).
Signing uses PS256; the bank account verification flow uses RS256, which is the
only algorithm it supports. Incoming messages are accepted with either.

**Request signing claims.** The PS256 JWS inside each request body has the header
`typ: "lpg-req+jwt"` and, next to `payload`, these claims (numbers are whole seconds
since the epoch):

| Claim | Value |
|-------|-------|
| `iat` | the signing time |
| `nbf` | the same as `iat` |
| `exp` | `iat` + 300: a request is valid for 300 seconds |
| `jti` | a random UUID, new for every request |

LPG may reject a request whose claims are missing, expired, not yet valid, or reused.
Keep your clock synchronised (NTP); LPG allows a small clock skew. If you sign requests
with your own code, send the same claims (`iat`, `nbf`, `exp` = `iat` + 300, a fresh
`jti` per request, header `typ: lpg-req+jwt`) and sign each request afresh, including
retries. `GET` and bodiless requests carry no JWS. The bank account
verification `secureHash` (RS256) is unchanged.

**Resource IDs.** Every ID the client puts in a URL path (an offer, application,
account lock, credit approval, loan, repayment schedule or lender-data ID) must be
one path segment that needs no encoding: 1 to 128 characters from `A-Z`, `a-z`,
`0-9`, `.`, `_` and `-`, starting with a letter or digit. Every ID Amazon Lending
issues has this form. Any other value, including `null` from a model passed to an
`update*` method, is rejected with `LPGInvalidRequestException("Invalid resource ID")`
before a request is made; the message does not include the value.

**Keys.** Signing and encryption keys are looked up by key id through
`SecurityKeyProvider`. Use `FileSecurityKeyProvider` for PEM files on disk,
`StringSecurityKeyProvider` for keys already in memory, and
`CompositeSecurityKeyProvider` to search active and standby keys together during
rotation. Pass the key id to the provider constructor so that lookups are
matched against it; the single-argument constructors return their key whatever
id is requested, and exist only for backwards compatibility.

## Endpoints

`LPGEndpoint` selects the gateway your client talks to.

| Constant  | Endpoint                                      | Region         |
|-----------|-----------------------------------------------|----------------|
| `NA_PROD` | `https://lending-na.amazonservices.com`       | us-east-1      |
| `EU_PROD` | `https://lending-eu.amazonservices.com`       | eu-west-1      |
| `AP_PROD` | `https://lending-fe.amazonservices.com`       | ap-northeast-1 |
| `NA_TEST` | `https://lending-na-gamma.amazonservices.com` | us-east-1      |
| `EU_TEST` | `https://lending-eu-gamma.amazonservices.com` | eu-west-1      |
| `AP_TEST` | `https://lending-fe-gamma.amazonservices.com` | ap-northeast-1 |
| `NA_BETA` | `https://lending-na-beta.amazonservices.com`  | us-west-2      |

## Usage

### 1. Implement three interfaces

**AWSConfig**
```java
import com.amazon.lending.config.AWSConfig;

public class ExampleAWSConfig implements AWSConfig {
    @Override
    public String getApiKey() {
        return "some-api-key";
    }

    @Override
    public String getAwsAccessKey() {
        return "some-access-key";
    }

    @Override
    public String getAwsSecretKey() {
        return "some-secret-key";
    }

    @Override
    public String getRoleArn() {
        return "arn:aws:iam::some-account:role/some-role";
    }

    @Override
    public String getSessionName() {
        return "amazon";
    }
}
```

**LPGConfig**
```java
import com.amazon.lending.config.ApiVersion;
import com.amazon.lending.config.LPGConfig;
import com.amazon.lending.config.LPGEndpoint;

public class ExampleLPGConfig implements LPGConfig {
    @Override
    public LPGEndpoint getEndpoint() {
        return LPGEndpoint.NA_TEST;
    }

    @Override
    public ApiVersion getApiVersion() {
        return ApiVersion.V1_1;
    }
}
```

**SecurityConfig**
```java
import com.amazon.lending.config.SecurityConfig;
import com.amazon.lending.security.SecurityKeyProvider;
import com.amazon.lending.security.impl.FileSecurityKeyProvider;

public class ExampleSecurityConfig implements SecurityConfig {

    // Amazon public keys (provided by Amazon)
    @Override
    public SecurityKeyProvider getAmazonSigningPublicKeyProvider() {
        return new FileSecurityKeyProvider("/path/to/amazon/signing/public/key.pem");
    }

    @Override
    public String getDefaultAmazonSigningKeyId() {
        return "amazon-signing-key-id";
    }

    @Override
    public SecurityKeyProvider getAmazonEncryptionPublicKeyProvider() {
        return new FileSecurityKeyProvider("/path/to/amazon/encryption/public/key.pem");
    }

    @Override
    public String getDefaultAmazonEncryptionKeyId() {
        return "amazon-encryption-key-id";
    }

    // Partner private keys
    @Override
    public SecurityKeyProvider getSigningPrivateKeyProvider() {
        return new FileSecurityKeyProvider("/path/to/signing/private/key.pem");
    }

    @Override
    public String getDefaultSigningKeyId() {
        return "signing-key-id";
    }

    @Override
    public SecurityKeyProvider getEncryptionPrivateKeyProvider() {
        return new FileSecurityKeyProvider("/path/to/encryption/private/key.pem");
    }

    @Override
    public String getDefaultEncryptionKeyId() {
        return "encryption-key-id";
    }

    // Verification keys (only needed if using bank account verification API)
    @Override
    public SecurityKeyProvider getVerificationPublicKeyProvider() {
        return new FileSecurityKeyProvider("/path/to/verification/public/key.pem");
    }

    @Override
    public String getVerificationPublicKeyId() {
        return "9SGVuhp0+kfHZ6pCWor5tA==";
    }

    // Note: Initially, signing and encryption keys may point to the same key pair.
    // Standby key methods (getAmazonStandbySigningPublicKeyProvider, etc.) are optional
    // and only needed during key rotation. They return null by default.
}
```

### 2. Create a client

```java
import com.amazon.lending.client.LendingPartnerGatewayClient;
import com.amazon.lending.client.impl.DefaultLendingPartnerGatewayClient;

final LendingPartnerGatewayClient client = new DefaultLendingPartnerGatewayClient();
client.initialize(new ExampleSecurityConfig(), new ExampleAWSConfig(), new ExampleLPGConfig());
```

An initialised client is safe to share across threads; create one and reuse it.
Call `initialize` once, before sharing the client between threads.

### 3. Call the methods

```java
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
```

#### Test-environment-only APIs

Five of the methods above are marked *only available in the test environment*:
`createOffer`, `inquireOffer`, `getRedirectUrl`, `confirmApplication` and
`lockAccountAndConfirmApplication`. They are routed to `/test/...` paths, and the
SDK refuses to call them against the production endpoints (`NA_PROD`, `EU_PROD`,
`AP_PROD`), throwing `LPGInvalidRequestException` instead. Use them to drive a
test merchant through the offer and application states while you integrate.

`createOffer` is the synchronous, test-only form: it returns the created `Offer`
including its offer id, so a test can carry straight on. Outside the test
environment use `createOfferAsync`, which POSTs to `/{apiVersion}/offers`
(`/v1.1/offers` with `ApiVersion.V1_1`) and returns a `CreateOfferAsyncResponse`
carrying only a response code, a response message and a request id. It does not
return an offer id: LPG sends that asynchronously to the notification topic
registered for your account during onboarding.

---

### 4. Encrypt a Bank Account Number (Bank Account Onboarding)

When onboarding a lender's bank account for disbursements, you must encrypt the bank account number using the `LPGMessageEncrypter` helper class. This is **not** a method on `LendingPartnerGatewayClient` — it is a standalone helper in the `com.amazon.lending.security.impl` namespace.

Amazon Lending will provide the RSA public key required to initialize the encrypter during the bank account onboarding process.

```java
import com.amazon.lending.security.impl.LPGEncryptionKey;
import com.amazon.lending.security.impl.LPGMessageEncrypter;
import com.amazon.lending.security.exception.LPGEncryptionException;

// amazonPublicKey: RSAPublicKey loaded from the public key provided by Amazon Lending
// keyId: key identifier string provided by Amazon Lending
LPGEncryptionKey encryptionKey = LPGEncryptionKey.builder()
        .rsaPublicKey(amazonPublicKey)
        .keyId(keyId)
        .build();
LPGMessageEncrypter encrypter = new LPGMessageEncrypter(encryptionKey);

try {
    String encryptedBankAccountNumber = encrypter.encryptBankAccountNumber("123456789");
    // Write to the S3 upload file as:
    // EncryptedBankAccountNumber:<encryptedBankAccountNumber>
} catch (LPGEncryptionException e) {
    // Handle encryption failure
}
```

Note: If you already hold a MessageHandler instance (e.g. LPGMessageHandler), you can also call `messageHandler.encryptBankAccountNumber(bankAccountNumber)` directly, as MessageHandler exposes the same method via the `com.amazon.lending.security.MessageHandler` interface.

## Building from source

```
mvn verify
```

Requires JDK 8 or later and runs the unit tests. A JaCoCo coverage report is
written to `target/site/jacoco/index.html`; pass `-Djacoco.skip=true` to
build without it. The `release` profile, which
attaches sources and Javadoc and signs the artifacts, is for maintainers
publishing to Maven Central.

## Dependencies

This is the full runtime dependency closure that Maven resolves for a project depending
on `software.amazon.lending:amazon-lpg-api-sdk-java:1.0.0`: the SDK's direct dependencies
and everything they bring in. Each licence is the one declared in that artifact's pom, or
in the parent pom it inherits from. To check it against your own build, run
`mvn dependency:tree` in your project.

| Dependency                                           | Version                                   | Licence      | Brought in by |
|------------------------------------------------------|-------------------------------------------|--------------|---------------|
| `com.google.http-client:google-http-client`          | 1.47.1                                    | Apache-2.0   | direct |
| `org.apache.httpcomponents:httpclient`               | 4.5.14                                    | Apache-2.0   | `google-http-client` |
| `org.apache.httpcomponents:httpcore`                 | 4.4.16                                    | Apache-2.0   | `google-http-client` |
| `com.google.code.findbugs:jsr305`                    | 3.0.2                                     | Apache-2.0   | `google-http-client` |
| `com.google.errorprone:error_prone_annotations`      | 2.36.0                                    | Apache-2.0   | `google-http-client` |
| `com.google.j2objc:j2objc-annotations`               | 3.0.0                                     | Apache-2.0   | `google-http-client` |
| `io.grpc:grpc-context`                               | 1.70.0                                    | Apache-2.0   | `google-http-client` |
| `io.grpc:grpc-api`                                   | 1.70.0                                    | Apache-2.0   | `google-http-client` → `grpc-context` (runtime scope) |
| `io.opencensus:opencensus-api`                       | 0.31.1                                    | Apache-2.0   | `google-http-client` |
| `io.opencensus:opencensus-contrib-http-util`         | 0.31.1                                    | Apache-2.0   | `google-http-client` |
| `com.google.http-client:google-http-client-jackson2` | 1.47.1                                    | Apache-2.0   | direct |
| `com.fasterxml.jackson.core:jackson-core`            | 2.18.12                                   | Apache-2.0   | direct |
| `com.google.guava:guava`                             | 33.7.2-jre                                | Apache-2.0   | direct |
| `com.google.guava:failureaccess`                     | 1.0.3                                     | Apache-2.0   | `guava` |
| `com.google.guava:listenablefuture`                  | 9999.0-empty-to-avoid-conflict-with-guava | Apache-2.0   | `guava` |
| `org.jspecify:jspecify`                              | 1.0.1                                     | Apache-2.0   | `guava` |
| `commons-codec:commons-codec`                        | 1.22.1                                    | Apache-2.0   | direct |
| `com.nimbusds:nimbus-jose-jwt`                       | 9.48                                      | Apache-2.0   | direct |
| `net.minidev:json-smart`                             | 2.6.0                                     | Apache-2.0   | direct |
| `net.minidev:accessors-smart`                        | 2.6.0                                     | Apache-2.0   | `json-smart` |
| `org.ow2.asm:asm`                                    | 9.7.1                                     | BSD-3-Clause | `json-smart` → `accessors-smart` |

The only dependency that is not Apache-2.0 is `org.ow2.asm:asm` 9.7.1 (BSD-3-Clause),
via `json-smart`. Lombok is compile-time only: it is declared in the `provided` scope
and is not a transitive dependency of the published artifact.

## Versioning

Releases follow semantic versioning and are published to Maven Central. In
addition, every request carries the SDK build identifier from
`com.amazon.lending.config.SdkVersion` in the `x-amz-lpg-sdk-ver` header, which
Amazon Lending uses to identify the SDK build a partner is running.

## Changelog

See [CHANGES.md](CHANGES.md).

## Licence

Apache-2.0. See [LICENSE](LICENSE).

## Security

Report vulnerabilities via https://aws.amazon.com/security/vulnerability-reporting/

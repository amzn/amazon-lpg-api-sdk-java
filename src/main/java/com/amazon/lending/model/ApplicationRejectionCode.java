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

package com.amazon.lending.model;

public interface ApplicationRejectionCode extends ApplicationStatusCode {
    enum KYC implements ApplicationRejectionCode {
        IDENTITY_PROOF_NOT_VERIFIABLE,
        ADDRESS_PROOF_NOT_VERIFIABLE,
        CRIME_LITIGATION_GOVERNMENT_POLICY_VIOLATION_HISTORY,
        COMPANY_IDENTITY_PROOF_NOT_VERIFIABLE,
        SANCTION_AND_POLITICAL_PERSON_CHECK_FAILED,
        NEGATIVE_NEWS_CHECK_FAILED,
        FINANCIAL_CRIME_RISK_CHECK_FAILED,
        LENDER_BLACKLISTED,
        FRAUD
    }

    enum COMPLIANCE implements ApplicationRejectionCode {
        GST_NOT_FILED,
        VAT_RETURNS_NOT_FILED
    }

    enum POLICY implements ApplicationRejectionCode {
        NO_OWNERSHIP_PROOF,
        DECLINE_IN_PAYMENTS,
        SELLER_OVERLEVERAGED,
        SELLER_HAS_LOANS_WITH_OTHER_BANKS,
        OUTSIDE_SERVICE_OR_LOCATION,
        NO_ELIGIBLE_PERSONAL_GUARANTOR,
        IRREGULAR_OPERATING_METRICS,
        APPLICANT_AGE,
        EXISTING_CUSTOMER,
        LOAN_LIKELY_USED_FOR_NON_COMMERCIAL,
        INTELLECTUAL_PROPERTY_VIOLATION,
        FAILED_PREQUALIFICATION,
        FAILED_APPLICATION_CRITERIA,
        OTHERS
    }

    enum SELLER_REJECT implements ApplicationRejectionCode {
        PROCESSING_FEE,
        LOCKING_ACCOUNT,
        INTEREST_RATE,
        LOAN_PRODUCT_TYPE,
        LOAN_TENURE,
        NEW_ACCOUNT_OPENING,
        NO_RESPONSE_FROM_SELLER
    }

    enum BUREAU implements ApplicationRejectionCode {
        CREDIT_SCORE_BELOW_LENDER_THRESHOLD,
        TOO_MANY_RECENT_INQUIRIES,
        DEFAULT_OR_OVERDUE_HISTORY
    }
}

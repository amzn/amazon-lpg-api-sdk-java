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

package com.amazon.lending.http.model;

import com.google.api.client.util.DateTime;
import com.google.api.client.util.Key;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanDto {
    @Key
    private String marketplaceCountry;
    @Key
    private String productCode;
    @Key
    private String partnerCode;
    @Key
    private String lastUpdatedBy;
    @Key
    private DateTime lastUpdatedOn;
    @Key
    private long version;
    @Key
    private String apiVersion;

    @Key
    private String loanId;
    @Key
    private String creditApprovalId;
    @Key
    private String loanStatus;
    @Key
    private String idempotencyKey;
    @Key
    private LoanDetails details;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoanDetails {
        @Key
        private int term;
        @Key
        private String termUnit;
        @Key
        private BigDecimal apr;
        @Key
        private BigDecimal interest;
        @Key
        private BigDecimal repaymentRate;
        @Key
        private BigDecimal amount;
        @Key
        private String currency;
        @Key
        private BigDecimal processingFee;
        @Key
        private BigDecimal totalPrincipalRepaid;
        @Key
        private BigDecimal totalInterestRepaid;
        @Key(value = "totalFeeRepaid")
        private BigDecimal totalFeesRepaid;
        @Key
        private BigDecimal outstandingPrincipalBalance;
        @Key
        private BigDecimal pastDueAmount;
        @Key
        private Integer pastDuePeriod;
        @Key
        private String pastDuePeriodUnit;
        @Key
        private DateTime matureOn;
        @Key
        private DateTime createdOn;
        @Key
        private DateTime closedOn;
        @Key
        private Boolean automatedRepaymentsEnabled;
    }
}

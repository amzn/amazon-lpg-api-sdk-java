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
public class ApplicationDto {
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
    private String applicationId;
    @Key
    private String offerId;
    @Key
    private String applicationStatus;
    @Key
    private ApplicationDetails details;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicationDetails {
        @Key
        private String accountLockId;
        @Key
        private int offerTerm;
        @Key
        private String offerTermUnit;
        @Key
        private BigDecimal offerMinApr;
        @Key
        private BigDecimal offerMaxApr;
        @Key
        private BigDecimal offerMinInterest;
        @Key
        private BigDecimal offerMaxInterest;
        @Key
        private BigDecimal offerMinRepaymentRate;
        @Key
        private BigDecimal offerMaxRepaymentRate;
        @Key
        private BigDecimal offerMinAmount;
        @Key
        private BigDecimal offerMaxAmount;
        @Key
        private int requestTerm;
        @Key
        private String requestTermUnit;
        @Key
        private BigDecimal requestApr;
        @Key
        private BigDecimal requestInterest;
        @Key
        private BigDecimal requestRepaymentRate;
        @Key
        private BigDecimal requestAmount;
        @Key
        private int approvedTerm;
        @Key
        private String approvedTermUnit;
        @Key
        private BigDecimal approvedApr;
        @Key
        private BigDecimal approvedInterest;
        @Key
        private BigDecimal approvedRepaymentRate;
        @Key
        private BigDecimal approvedAmount;
        @Key
        private Boolean accountLockRequired;
        @Key
        private Boolean accountChangeRequired;
        @Key
        private String currency;
        @Key
        private String rejectionCategory;
        @Key
        private String statusReasonCode;
        @Key
        private DateTime createdOn;
        @Key
        private DateTime receivedOn;
        @Key
        private DateTime approvedOn;
        @Key
        private DateTime approvedWithPendingSellerActionOn;
        @Key
        private DateTime rejectedOn;
        @Key
        private DateTime abandonedOn;
        @Key
        private DateTime confirmedOn;
        @Key
        private DateTime closedOn;
        @Key
        private Boolean automatedRepaymentsEnabled;
    }
}

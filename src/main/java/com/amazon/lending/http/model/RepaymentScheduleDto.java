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
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepaymentScheduleDto {
    @Key
    private String repaymentScheduleId;
    @Key
    private String loanId;
    @Key
    private String marketplaceCountry;
    @Key
    private String productCode;
    @Key
    private String partnerCode;
    @Key
    private RepaymentScheduleDetails details;
    @Key
    private long version;
    @Key
    private String lastUpdatedBy;
    @Key
    private DateTime lastUpdatedOn;
    @Key
    private String apiVersion;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RepaymentScheduleDetails {
        @Key
        private DateTime createdOn;
        @Key
        private List<Repayment> repayments;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Repayment {
            @Key
            private DateTime dueOn;
            @Key
            private BigDecimal totalDueAmount;
            @Key
            private BigDecimal principalDueAmount;
            @Key
            private BigDecimal interestDueAmount;
            @Key
            private BigDecimal feeDueAmount;
            @Key
            private String repaymentStatus;
            @Key
            private DateTime paidOn;
            @Key
            private BigDecimal totalPaidAmount;
            @Key
            private BigDecimal principalPaidAmount;
            @Key
            private BigDecimal interestPaidAmount;
            @Key
            private BigDecimal feePaidAmount;
            @Key
            private DateTime lastUpdatedOn;
            @Key
            private String lastUpdatedBy;
        }
    }
}

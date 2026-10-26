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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationDetails {
    private String accountLockId;
    private int offerTerm;
    private String offerTermUnit;
    private BigDecimal offerMinApr;
    private BigDecimal offerMaxApr;
    private BigDecimal offerMinInterest;
    private BigDecimal offerMaxInterest;
    private BigDecimal offerMinRepaymentRate;
    private BigDecimal offerMaxRepaymentRate;
    private BigDecimal offerMinAmount;
    private BigDecimal offerMaxAmount;
    private int requestTerm;
    private String requestTermUnit;
    private BigDecimal requestApr;
    private BigDecimal requestInterest;
    private BigDecimal requestRepaymentRate;
    private BigDecimal requestAmount;
    private int approvedTerm;
    private String approvedTermUnit;
    private BigDecimal approvedApr;
    private BigDecimal approvedInterest;
    private BigDecimal approvedRepaymentRate;
    private BigDecimal approvedAmount;
    private Boolean accountLockRequired;
    private Boolean accountChangeRequired;
    private String currency;
    private ApplicationRejectionCategory rejectionCategory;
    private ApplicationStatusCode statusReasonCode;
    private Date createdOn;
    private Date receivedOn;
    private Date approvedOn;
    private Date approvedWithPendingSellerActionOn;
    private Date rejectedOn;
    private Date abandonedOn;
    private Date confirmedOn;
    private Date closedOn;
    private Boolean automatedRepaymentsEnabled;
}

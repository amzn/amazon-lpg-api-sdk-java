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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LenderPortfolioSnapshotData {
    private BigDecimal outstandingPrincipalBalance;
    private BigDecimal outstandingPrincipalBalanceOfCurrentLoans;
    private BigDecimal outstandingPrincipalBalanceOf1to30DaysDelinquentLoans;
    private BigDecimal outstandingPrincipalBalanceOf31to59DaysDelinquentLoans;
    private BigDecimal outstandingPrincipalBalanceOf60to89DaysDelinquentLoans;
    private BigDecimal outstandingPrincipalBalanceOfGT90DaysDelinquentLoans;

    private int createdApplicationCount;
    private BigDecimal createdApplicationAmount;
    private int receivedApplicationCount;
    private BigDecimal receivedApplicationAmount;
    private int approvedApplicationCount;
    private BigDecimal approvedApplicationAmount;
    private int confirmedApplicationCount;
    private BigDecimal confirmedApplicationAmount;
    private int activeCreditApprovalsWithNoDrawDownsCount;
    private BigDecimal activeCreditApprovalsWithNoDrawDownsAmount;
    private BigDecimal avgAprOfOutstandingPrincipalBalance;
    private BigDecimal avgAprOfOutstandingPrincipalBalanceOfCurrentLoans;
    private BigDecimal avgAprOfOutstandingPrincipalBalanceOf1to30DaysDelinquentLoans;
    private BigDecimal avgAprOfOutstandingPrincipalBalanceOf31to59DaysDelinquentLoans;
    private BigDecimal avgAprOfOutstandingPrincipalBalanceOf60to89DaysDelinquentLoans;
    private BigDecimal avgAprOfOutstandingPrincipalBalanceOfGT90DaysDelinquentLoans;
}

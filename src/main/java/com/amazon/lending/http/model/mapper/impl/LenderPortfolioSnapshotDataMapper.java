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

package com.amazon.lending.http.model.mapper.impl;

import com.amazon.lending.model.LenderPortfolioSnapshotData;
import com.amazon.lending.http.model.LenderPortfolioSnapshotDataDto;

public class LenderPortfolioSnapshotDataMapper {
    public LenderPortfolioSnapshotData networkToClient(LenderPortfolioSnapshotDataDto source) {
        if (source == null) {
            return null;
        }

        return LenderPortfolioSnapshotData.builder()
                .outstandingPrincipalBalance(source.getOutstandingPrincipalBalance())
                .outstandingPrincipalBalanceOfCurrentLoans(source.getOutstandingPrincipalBalanceOfCurrentLoans())
                .outstandingPrincipalBalanceOf1to30DaysDelinquentLoans(source.getOutstandingPrincipalBalanceOf1to30DaysDelinquentLoans())
                .outstandingPrincipalBalanceOf31to59DaysDelinquentLoans(source.getOutstandingPrincipalBalanceOf31to59DaysDelinquentLoans())
                .outstandingPrincipalBalanceOf60to89DaysDelinquentLoans(source.getOutstandingPrincipalBalanceOf60to89DaysDelinquentLoans())
                .outstandingPrincipalBalanceOfGT90DaysDelinquentLoans(source.getOutstandingPrincipalBalanceOfGT90DaysDelinquentLoans())
                .createdApplicationCount(source.getCreatedApplicationCount())
                .createdApplicationAmount(source.getCreatedApplicationAmount())
                .receivedApplicationCount(source.getReceivedApplicationCount())
                .receivedApplicationAmount(source.getReceivedApplicationAmount())
                .approvedApplicationCount(source.getApprovedApplicationCount())
                .approvedApplicationAmount(source.getApprovedApplicationAmount())
                .confirmedApplicationCount(source.getConfirmedApplicationCount())
                .confirmedApplicationAmount(source.getConfirmedApplicationAmount())
                .activeCreditApprovalsWithNoDrawDownsCount(source.getActiveCreditApprovalsWithNoDrawDownsCount())
                .activeCreditApprovalsWithNoDrawDownsAmount(source.getActiveCreditApprovalsWithNoDrawDownsAmount())
                .avgAprOfOutstandingPrincipalBalance(source.getAvgAprOfOutstandingPrincipalBalance())
                .avgAprOfOutstandingPrincipalBalanceOfCurrentLoans(source.getAvgAprOfOutstandingPrincipalBalanceOfCurrentLoans())
                .avgAprOfOutstandingPrincipalBalanceOf1to30DaysDelinquentLoans(
                        source.getAvgAprOfOutstandingPrincipalBalanceOf1to30DaysDelinquentLoans())
                .avgAprOfOutstandingPrincipalBalanceOf31to59DaysDelinquentLoans(
                        source.getAvgAprOfOutstandingPrincipalBalanceOf31to59DaysDelinquentLoans())
                .avgAprOfOutstandingPrincipalBalanceOf60to89DaysDelinquentLoans(
                        source.getAvgAprOfOutstandingPrincipalBalanceOf60to89DaysDelinquentLoans())
                .avgAprOfOutstandingPrincipalBalanceOfGT90DaysDelinquentLoans(
                        source.getAvgAprOfOutstandingPrincipalBalanceOfGT90DaysDelinquentLoans())
                .build();
    }

    public LenderPortfolioSnapshotDataDto clientToNetwork(LenderPortfolioSnapshotData source) {
        if (source == null) {
            return null;
        }

        return LenderPortfolioSnapshotDataDto.builder()
                .outstandingPrincipalBalance(source.getOutstandingPrincipalBalance())
                .outstandingPrincipalBalanceOfCurrentLoans(source.getOutstandingPrincipalBalanceOfCurrentLoans())
                .outstandingPrincipalBalanceOf1to30DaysDelinquentLoans(source.getOutstandingPrincipalBalanceOf1to30DaysDelinquentLoans())
                .outstandingPrincipalBalanceOf31to59DaysDelinquentLoans(source.getOutstandingPrincipalBalanceOf31to59DaysDelinquentLoans())
                .outstandingPrincipalBalanceOf60to89DaysDelinquentLoans(source.getOutstandingPrincipalBalanceOf60to89DaysDelinquentLoans())
                .outstandingPrincipalBalanceOfGT90DaysDelinquentLoans(source.getOutstandingPrincipalBalanceOfGT90DaysDelinquentLoans())
                .createdApplicationCount(source.getCreatedApplicationCount())
                .createdApplicationAmount(source.getCreatedApplicationAmount())
                .receivedApplicationCount(source.getReceivedApplicationCount())
                .receivedApplicationAmount(source.getReceivedApplicationAmount())
                .approvedApplicationCount(source.getApprovedApplicationCount())
                .approvedApplicationAmount(source.getApprovedApplicationAmount())
                .confirmedApplicationCount(source.getConfirmedApplicationCount())
                .confirmedApplicationAmount(source.getConfirmedApplicationAmount())
                .activeCreditApprovalsWithNoDrawDownsCount(source.getActiveCreditApprovalsWithNoDrawDownsCount())
                .activeCreditApprovalsWithNoDrawDownsAmount(source.getActiveCreditApprovalsWithNoDrawDownsAmount())
                .avgAprOfOutstandingPrincipalBalance(source.getAvgAprOfOutstandingPrincipalBalance())
                .avgAprOfOutstandingPrincipalBalanceOfCurrentLoans(source.getAvgAprOfOutstandingPrincipalBalanceOfCurrentLoans())
                .avgAprOfOutstandingPrincipalBalanceOf1to30DaysDelinquentLoans(
                        source.getAvgAprOfOutstandingPrincipalBalanceOf1to30DaysDelinquentLoans())
                .avgAprOfOutstandingPrincipalBalanceOf31to59DaysDelinquentLoans(
                        source.getAvgAprOfOutstandingPrincipalBalanceOf31to59DaysDelinquentLoans())
                .avgAprOfOutstandingPrincipalBalanceOf60to89DaysDelinquentLoans(
                        source.getAvgAprOfOutstandingPrincipalBalanceOf60to89DaysDelinquentLoans())
                .avgAprOfOutstandingPrincipalBalanceOfGT90DaysDelinquentLoans(
                        source.getAvgAprOfOutstandingPrincipalBalanceOfGT90DaysDelinquentLoans())
                .build();
    }
}

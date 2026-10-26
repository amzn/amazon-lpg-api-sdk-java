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

import com.amazon.lending.model.LenderLoanAggregatedData;
import com.amazon.lending.http.model.LenderLoanAggregatedDataDto;

public class LenderLoanAggregateDataMapper {

    private static final DateMapper DATE_MAPPER = new DateMapper();

    public LenderLoanAggregatedData networkToClient(LenderLoanAggregatedDataDto source) {
        if (source == null) {
            return null;
        }

        return LenderLoanAggregatedData.builder()
                .aggregationIntervalStartDate(DATE_MAPPER.dateTimeToDate(source.getAggregationIntervalStartDate()))
                .aggregationIntervalEndDate(DATE_MAPPER.dateTimeToDate(source.getAggregationIntervalEndDate()))
                .averageOutstandingPrincipalBalance(source.getAverageOutstandingPrincipalBalance())
                .aggregateRepaidPrincipalAmount(source.getAggregateRepaidPrincipalAmount())
                .aggregateRepaidInterestAmount(source.getAggregateRepaidInterestAmount())
                .aggregateProcessingFeePaidBySeller(source.getAggregateProcessingFeePaidBySeller())
                .aggregateOverdueAmount(source.getAggregateOverdueAmount())
                .aggregateNetInterestRevenue(source.getAggregateNetInterestRevenue())
                .aggregateCostOfDebt(source.getAggregateCostOfDebt())
                .aggregateExpectedLoss(source.getAggregateExpectedLoss())
                .aggregateExpectedLossTrueUp(source.getAggregateExpectedLossTrueUp())
                .build();
    }

    public LenderLoanAggregatedDataDto clientToNetwork(LenderLoanAggregatedData source) {
        if (source == null) {
            return null;
        }

        return LenderLoanAggregatedDataDto.builder()
                .aggregationIntervalStartDate(DATE_MAPPER.dateToDateTime(source.getAggregationIntervalStartDate()))
                .aggregationIntervalEndDate(DATE_MAPPER.dateToDateTime(source.getAggregationIntervalEndDate()))
                .averageOutstandingPrincipalBalance(source.getAverageOutstandingPrincipalBalance())
                .aggregateRepaidPrincipalAmount(source.getAggregateRepaidPrincipalAmount())
                .aggregateRepaidInterestAmount(source.getAggregateRepaidInterestAmount())
                .aggregateProcessingFeePaidBySeller(source.getAggregateProcessingFeePaidBySeller())
                .aggregateOverdueAmount(source.getAggregateOverdueAmount())
                .aggregateNetInterestRevenue(source.getAggregateNetInterestRevenue())
                .aggregateCostOfDebt(source.getAggregateCostOfDebt())
                .aggregateExpectedLoss(source.getAggregateExpectedLoss())
                .aggregateExpectedLossTrueUp(source.getAggregateExpectedLossTrueUp())
                .build();
    }
}

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

import com.amazon.lending.model.LenderCreditApprovalAggregatedData;
import com.amazon.lending.http.model.LenderCreditApprovalAggregatedDataDto;

public class LenderCreditApprovalAggregatedDataMapper {

    private static final DateMapper DATE_MAPPER = new DateMapper();

    public LenderCreditApprovalAggregatedData networkToClient(LenderCreditApprovalAggregatedDataDto source) {
        if (source == null) {
            return null;
        }

        return LenderCreditApprovalAggregatedData.builder()
                .aggregationIntervalStartDate(DATE_MAPPER.dateTimeToDate(source.getAggregationIntervalStartDate()))
                .aggregationIntervalEndDate(DATE_MAPPER.dateTimeToDate(source.getAggregationIntervalEndDate()))
                .averageOutstandingPrincipalBalance(source.getAverageOutstandingPrincipalBalance())
                .averageNumberOfDrawDowns(source.getAverageNumberOfDrawDowns())
                .averageUtilizationPercentage(source.getAverageUtilizationPercentage())
                .aggregateRepaidPrincipalAmount(source.getAggregateRepaidPrincipalAmount())
                .aggregateRepaidInterestAmount(source.getAggregateRepaidInterestAmount())
                .aggregateProcessingFeePaidBySeller(source.getAggregateProcessingFeePaidBySeller())
                .aggregateOverdueAmount(source.getAggregateOverdueAmount())
                .build();
    }

    public LenderCreditApprovalAggregatedDataDto clientToNetwork(LenderCreditApprovalAggregatedData source) {
        if (source == null) {
            return null;
        }

        return LenderCreditApprovalAggregatedDataDto.builder()
                .aggregationIntervalStartDate(DATE_MAPPER.dateToDateTime(source.getAggregationIntervalStartDate()))
                .aggregationIntervalEndDate(DATE_MAPPER.dateToDateTime(source.getAggregationIntervalEndDate()))
                .averageOutstandingPrincipalBalance(source.getAverageOutstandingPrincipalBalance())
                .averageNumberOfDrawDowns(source.getAverageNumberOfDrawDowns())
                .averageUtilizationPercentage(source.getAverageUtilizationPercentage())
                .aggregateRepaidPrincipalAmount(source.getAggregateRepaidPrincipalAmount())
                .aggregateRepaidInterestAmount(source.getAggregateRepaidInterestAmount())
                .aggregateProcessingFeePaidBySeller(source.getAggregateProcessingFeePaidBySeller())
                .aggregateOverdueAmount(source.getAggregateOverdueAmount())
                .build();
    }
}

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

import com.amazon.lending.http.model.mapper.CreateLenderLoanDataRequestMapper;
import com.amazon.lending.http.model.CreateLenderLoanDataRequestDto;
import com.amazon.lending.model.CreateLenderLoanDataRequest;

public class CreateLenderLoanDataRequestMapperImpl implements CreateLenderLoanDataRequestMapper {

    private static final DateMapper DATE_MAPPER = new DateMapper();
    private static final LenderLoanSnapshotDataMapper LENDER_LOAN_SNAPSHOT_DATA_MAPPER = new LenderLoanSnapshotDataMapper();
    private static final LenderLoanAggregateDataMapper LENDER_LOAN_AGGREGATE_DATA_MAPPER = new LenderLoanAggregateDataMapper();

    @Override
    public CreateLenderLoanDataRequest networkToClient(CreateLenderLoanDataRequestDto source) {
        if (source == null) {
            return null;
        }

        return CreateLenderLoanDataRequest.builder()
                .idempotencyKey(source.getIdempotencyKey())
                .marketplaceCountry(source.getMarketplaceCountry())
                .partnerCode(source.getPartnerCode())
                .productCode(source.getProductCode())
                .snapshotDate(DATE_MAPPER.dateTimeToDate(source.getSnapshotDate()))
                .currencyCode(source.getCurrencyCode())
                .loanId(source.getLoanId())
                .lenderLoanSnapshotData(LENDER_LOAN_SNAPSHOT_DATA_MAPPER.networkToClient(source.getLenderLoanSnapshotData()))
                .lenderLoanAggregatedData(LENDER_LOAN_AGGREGATE_DATA_MAPPER.networkToClient(source.getLenderLoanAggregatedData()))
                .createdBy(source.getCreatedBy())
                .build();
    }

    @Override
    public CreateLenderLoanDataRequestDto clientToNetwork(CreateLenderLoanDataRequest source) {
        if (source == null) {
            return null;
        }

        return CreateLenderLoanDataRequestDto.builder()
                .idempotencyKey(source.getIdempotencyKey())
                .marketplaceCountry(source.getMarketplaceCountry())
                .partnerCode(source.getPartnerCode())
                .productCode(source.getProductCode())
                .snapshotDate(DATE_MAPPER.dateToDateTime(source.getSnapshotDate()))
                .currencyCode(source.getCurrencyCode())
                .loanId(source.getLoanId())
                .lenderLoanSnapshotData(LENDER_LOAN_SNAPSHOT_DATA_MAPPER.clientToNetwork(source.getLenderLoanSnapshotData()))
                .lenderLoanAggregatedData(LENDER_LOAN_AGGREGATE_DATA_MAPPER.clientToNetwork(source.getLenderLoanAggregatedData()))
                .createdBy(source.getCreatedBy())
                .build();
    }
}

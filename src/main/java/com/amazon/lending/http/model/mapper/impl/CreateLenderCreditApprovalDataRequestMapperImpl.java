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

import com.amazon.lending.http.model.mapper.CreateLenderCreditApprovalDataRequestMapper;
import com.amazon.lending.model.CreateLenderCreditApprovalDataRequest;
import com.amazon.lending.http.model.CreateLenderCreditApprovalDataRequestDto;

public class CreateLenderCreditApprovalDataRequestMapperImpl implements CreateLenderCreditApprovalDataRequestMapper {

    private static final DateMapper DATE_MAPPER = new DateMapper();
    private static final LenderCreditApprovalSnapshotDataMapper LENDER_CREDIT_APPROVAL_SNAPSHOT_DATA_MAPPER = new LenderCreditApprovalSnapshotDataMapper();
    private static final LenderCreditApprovalAggregatedDataMapper LENDER_CREDIT_APPROVAL_AGGREGATED_DATA_MAPPER = new LenderCreditApprovalAggregatedDataMapper();

    @Override
    public CreateLenderCreditApprovalDataRequest networkToClient(CreateLenderCreditApprovalDataRequestDto source) {
        if (source == null) {
            return null;
        }

        return CreateLenderCreditApprovalDataRequest.builder()
                .idempotencyKey(source.getIdempotencyKey())
                .marketplaceCountry(source.getMarketplaceCountry())
                .partnerCode(source.getPartnerCode())
                .productCode(source.getProductCode())
                .snapshotDate(DATE_MAPPER.dateTimeToDate(source.getSnapshotDate()))
                .currencyCode(source.getCurrencyCode())
                .creditApprovalId(source.getCreditApprovalId())
                .createdBy(source.getCreatedBy())
                .lenderCreditApprovalSnapshotData(LENDER_CREDIT_APPROVAL_SNAPSHOT_DATA_MAPPER.networkToClient(source.getLenderCreditApprovalSnapshotData()))
                .lenderCreditApprovalAggregatedData(LENDER_CREDIT_APPROVAL_AGGREGATED_DATA_MAPPER.networkToClient(source.getLenderCreditApprovalAggregatedData()))
                .build();
    }

    @Override
    public CreateLenderCreditApprovalDataRequestDto clientToNetwork(CreateLenderCreditApprovalDataRequest source) {
        if (source == null) {
            return null;
        }

        return CreateLenderCreditApprovalDataRequestDto.builder()
                .idempotencyKey(source.getIdempotencyKey())
                .marketplaceCountry(source.getMarketplaceCountry())
                .partnerCode(source.getPartnerCode())
                .productCode(source.getProductCode())
                .snapshotDate(DATE_MAPPER.dateToDateTime(source.getSnapshotDate()))
                .currencyCode(source.getCurrencyCode())
                .creditApprovalId(source.getCreditApprovalId())
                .createdBy(source.getCreatedBy())
                .lenderCreditApprovalSnapshotData(LENDER_CREDIT_APPROVAL_SNAPSHOT_DATA_MAPPER.clientToNetwork(source.getLenderCreditApprovalSnapshotData()))
                .lenderCreditApprovalAggregatedData(LENDER_CREDIT_APPROVAL_AGGREGATED_DATA_MAPPER.clientToNetwork(source.getLenderCreditApprovalAggregatedData()))
                .build();
    }
}

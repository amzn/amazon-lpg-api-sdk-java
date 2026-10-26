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

import com.amazon.lending.http.model.mapper.CreateLenderPortfolioDataRequestMapper;
import com.amazon.lending.model.CreateLenderPortfolioDataRequest;
import com.amazon.lending.http.model.CreateLenderPortfolioDataRequestDto;

public class CreateLenderPortfolioDataRequestMapperImpl implements CreateLenderPortfolioDataRequestMapper {

    private static final DateMapper DATE_MAPPER = new DateMapper();
    private static final LenderPortfolioSnapshotDataMapper LENDER_PORTFOLIO_SNAPSHOT_DATA_MAPPER = new LenderPortfolioSnapshotDataMapper();
    private static final LenderPortfolioAggregatedDataMapper LENDER_PORTFOLIO_AGGREGATED_DATA_MAPPER = new LenderPortfolioAggregatedDataMapper();

    @Override
    public CreateLenderPortfolioDataRequest networkToClient(CreateLenderPortfolioDataRequestDto source) {
        if (source == null) {
            return null;
        }

        return CreateLenderPortfolioDataRequest.builder()
                .idempotencyKey(source.getIdempotencyKey())
                .marketplaceCountry(source.getMarketplaceCountry())
                .partnerCode(source.getPartnerCode())
                .productCode(source.getProductCode())
                .snapshotDate(DATE_MAPPER.dateTimeToDate(source.getSnapshotDate()))
                .currencyCode(source.getCurrencyCode())
                .createdBy(source.getCreatedBy())
                .lenderPortfolioSnapshotData(LENDER_PORTFOLIO_SNAPSHOT_DATA_MAPPER.networkToClient(source.getLenderPortfolioSnapshotData()))
                .lenderPortfolioAggregatedData(LENDER_PORTFOLIO_AGGREGATED_DATA_MAPPER.networkToClient(source.getLenderPortfolioAggregatedData()))
                .build();
    }

    @Override
    public CreateLenderPortfolioDataRequestDto clientToNetwork(CreateLenderPortfolioDataRequest source) {
        if (source == null) {
            return null;
        }

        return CreateLenderPortfolioDataRequestDto.builder()
                .idempotencyKey(source.getIdempotencyKey())
                .marketplaceCountry(source.getMarketplaceCountry())
                .partnerCode(source.getPartnerCode())
                .productCode(source.getProductCode())
                .snapshotDate(DATE_MAPPER.dateToDateTime(source.getSnapshotDate()))
                .currencyCode(source.getCurrencyCode())
                .createdBy(source.getCreatedBy())
                .lenderPortfolioSnapshotData(LENDER_PORTFOLIO_SNAPSHOT_DATA_MAPPER.clientToNetwork(source.getLenderPortfolioSnapshotData()))
                .lenderPortfolioAggregatedData(LENDER_PORTFOLIO_AGGREGATED_DATA_MAPPER.clientToNetwork(source.getLenderPortfolioAggregatedData()))
                .build();
    }
}

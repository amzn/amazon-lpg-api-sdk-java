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

import com.amazon.lending.model.LenderPortfolioData;
import com.amazon.lending.http.model.LenderPortfolioDataDto;
import com.amazon.lending.http.model.mapper.LenderPortfolioDataMapper;
import com.amazon.lending.model.DataReportingFrequency;

public class LenderPortfolioDataMapperImpl implements LenderPortfolioDataMapper {

    private static final DateMapper DATE_MAPPER = new DateMapper();
    private static final LenderPortfolioSnapshotDataMapper LENDER_PORTFOLIO_SNAPSHOT_DATA_MAPPER = new LenderPortfolioSnapshotDataMapper();
    private static final LenderPortfolioAggregatedDataMapper LENDER_PORTFOLIO_AGGREGATED_DATA_MAPPER = new LenderPortfolioAggregatedDataMapper();

    @Override
    public LenderPortfolioData networkToClient(LenderPortfolioDataDto source) {
        if (source == null) {
            return null;
        }

        return LenderPortfolioData.builder()
                .lenderPortfolioDataId(source.getLenderPortfolioDataId())
                .marketplaceCountry(source.getMarketplaceCountry())
                .partnerCode(source.getPartnerCode())
                .productCode(source.getProductCode())
                .snapshotDate(DATE_MAPPER.dateTimeToDate(source.getSnapshotDate()))
                .dataReportingFrequency(EnumMapper.stringToEnum(source.getDataReportingFrequency(), DataReportingFrequency.class))
                .currencyCode(source.getCurrencyCode())
                .lenderPortfolioSnapshotData(LENDER_PORTFOLIO_SNAPSHOT_DATA_MAPPER.networkToClient(source.getLenderPortfolioSnapshotData()))
                .lenderPortfolioAggregatedData(LENDER_PORTFOLIO_AGGREGATED_DATA_MAPPER.networkToClient(source.getLenderPortfolioAggregatedData()))
                .createdOn(DATE_MAPPER.dateTimeToDate(source.getCreatedOn()))
                .createdBy(source.getCreatedBy())
                .lastUpdatedOn(DATE_MAPPER.dateTimeToDate(source.getLastUpdatedOn()))
                .lastUpdatedBy(source.getLastUpdatedBy())
                .version(source.getVersion())
                .apiVersion(source.getApiVersion())
                .build();
    }

    @Override
    public LenderPortfolioDataDto clientToNetwork(LenderPortfolioData source) {
        if (source == null) {
            return null;
        }

        return LenderPortfolioDataDto.builder()
                .lenderPortfolioDataId(source.getLenderPortfolioDataId())
                .marketplaceCountry(source.getMarketplaceCountry())
                .partnerCode(source.getPartnerCode())
                .productCode(source.getProductCode())
                .snapshotDate(DATE_MAPPER.dateToDateTime(source.getSnapshotDate()))
                .dataReportingFrequency(EnumMapper.enumToString(source.getDataReportingFrequency()))
                .currencyCode(source.getCurrencyCode())
                .lenderPortfolioSnapshotData(LENDER_PORTFOLIO_SNAPSHOT_DATA_MAPPER.clientToNetwork(source.getLenderPortfolioSnapshotData()))
                .lenderPortfolioAggregatedData(LENDER_PORTFOLIO_AGGREGATED_DATA_MAPPER.clientToNetwork(source.getLenderPortfolioAggregatedData()))
                .createdOn(DATE_MAPPER.dateToDateTime(source.getCreatedOn()))
                .createdBy(source.getCreatedBy())
                .lastUpdatedOn(DATE_MAPPER.dateToDateTime(source.getLastUpdatedOn()))
                .lastUpdatedBy(source.getLastUpdatedBy())
                .version(source.getVersion())
                .apiVersion(source.getApiVersion())
                .build();
    }
}

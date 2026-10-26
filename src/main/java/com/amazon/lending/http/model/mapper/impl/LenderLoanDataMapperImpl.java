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

import com.amazon.lending.http.model.LenderLoanDataDto;
import com.amazon.lending.http.model.mapper.LenderLoanDataMapper;
import com.amazon.lending.model.LenderLoanData;
import com.amazon.lending.model.DataReportingFrequency;

public class LenderLoanDataMapperImpl implements LenderLoanDataMapper {

    private static final DateMapper DATE_MAPPER = new DateMapper();
    private static final LenderLoanSnapshotDataMapper LENDER_LOAN_SNAPSHOT_DATA_MAPPER = new LenderLoanSnapshotDataMapper();
    private static final LenderLoanAggregateDataMapper LENDER_LOAN_AGGREGATE_DATA_MAPPER = new LenderLoanAggregateDataMapper();

    @Override
    public LenderLoanData networkToClient(LenderLoanDataDto source) {
        if (source == null) {
            return null;
        }

        return LenderLoanData.builder()
                .lenderLoanDataId(source.getLenderLoanDataId())
                .marketplaceCountry(source.getMarketplaceCountry())
                .partnerCode(source.getPartnerCode())
                .productCode(source.getProductCode())
                .loanId(source.getLoanId())
                .snapshotDate(DATE_MAPPER.dateTimeToDate(source.getSnapshotDate()))
                .dataReportingFrequency(EnumMapper.stringToEnum(source.getDataReportingFrequency(), DataReportingFrequency.class))
                .currencyCode(source.getCurrencyCode())
                .lenderLoanSnapshotData(LENDER_LOAN_SNAPSHOT_DATA_MAPPER.networkToClient(source.getLenderLoanSnapshotData()))
                .lenderLoanAggregatedData(LENDER_LOAN_AGGREGATE_DATA_MAPPER.networkToClient(source.getLenderLoanAggregatedData()))
                .createdOn(DATE_MAPPER.dateTimeToDate(source.getCreatedOn()))
                .createdBy(source.getCreatedBy())
                .lastUpdatedOn(DATE_MAPPER.dateTimeToDate(source.getLastUpdatedOn()))
                .lastUpdatedBy(source.getLastUpdatedBy())
                .version(source.getVersion())
                .apiVersion(source.getApiVersion())
                .build();
    }

    @Override
    public LenderLoanDataDto clientToNetwork(LenderLoanData source) {
        if (source == null) {
            return null;
        }

        return LenderLoanDataDto.builder()
                .lenderLoanDataId(source.getLenderLoanDataId())
                .marketplaceCountry(source.getMarketplaceCountry())
                .partnerCode(source.getPartnerCode())
                .productCode(source.getProductCode())
                .loanId(source.getLoanId())
                .snapshotDate(DATE_MAPPER.dateToDateTime(source.getSnapshotDate()))
                .dataReportingFrequency(EnumMapper.enumToString(source.getDataReportingFrequency()))
                .currencyCode(source.getCurrencyCode())
                .lenderLoanSnapshotData(LENDER_LOAN_SNAPSHOT_DATA_MAPPER.clientToNetwork(source.getLenderLoanSnapshotData()))
                .lenderLoanAggregatedData(LENDER_LOAN_AGGREGATE_DATA_MAPPER.clientToNetwork(source.getLenderLoanAggregatedData()))
                .createdOn(DATE_MAPPER.dateToDateTime(source.getCreatedOn()))
                .createdBy(source.getCreatedBy())
                .lastUpdatedOn(DATE_MAPPER.dateToDateTime(source.getLastUpdatedOn()))
                .lastUpdatedBy(source.getLastUpdatedBy())
                .version(source.getVersion())
                .apiVersion(source.getApiVersion())
                .build();
    }
}

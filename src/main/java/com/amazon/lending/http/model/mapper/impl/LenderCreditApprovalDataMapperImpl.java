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

import com.amazon.lending.http.model.LenderCreditApprovalDataDto;
import com.amazon.lending.http.model.mapper.LenderCreditApprovalDataMapper;
import com.amazon.lending.model.LenderCreditApprovalData;
import com.amazon.lending.model.DataReportingFrequency;

public class LenderCreditApprovalDataMapperImpl implements LenderCreditApprovalDataMapper {

    private static final DateMapper DATE_MAPPER = new DateMapper();
    private static final LenderCreditApprovalSnapshotDataMapper LENDER_CREDIT_APPROVAL_SNAPSHOT_DATA_MAPPER = new LenderCreditApprovalSnapshotDataMapper();
    private static final LenderCreditApprovalAggregatedDataMapper LENDER_CREDIT_APPROVAL_AGGREGATED_DATA_MAPPER = new LenderCreditApprovalAggregatedDataMapper();

    @Override
    public LenderCreditApprovalData networkToClient(LenderCreditApprovalDataDto source) {
        if (source == null) {
            return null;
        }

        return LenderCreditApprovalData.builder()
                .lenderCreditApprovalDataId(source.getLenderCreditApprovalDataId())
                .marketplaceCountry(source.getMarketplaceCountry())
                .partnerCode(source.getPartnerCode())
                .productCode(source.getProductCode())
                .snapshotDate(DATE_MAPPER.dateTimeToDate(source.getSnapshotDate()))
                .dataReportingFrequency(EnumMapper.stringToEnum(source.getDataReportingFrequency(), DataReportingFrequency.class))
                .currencyCode(source.getCurrencyCode())
                .creditApprovalId(source.getCreditApprovalId())
                .lenderCreditApprovalSnapshotData(LENDER_CREDIT_APPROVAL_SNAPSHOT_DATA_MAPPER.networkToClient(source.getLenderCreditApprovalSnapshotData()))
                .lenderCreditApprovalAggregatedData(LENDER_CREDIT_APPROVAL_AGGREGATED_DATA_MAPPER.networkToClient(source.getLenderCreditApprovalAggregatedData()))
                .createdOn(DATE_MAPPER.dateTimeToDate(source.getCreatedOn()))
                .createdBy(source.getCreatedBy())
                .lastUpdatedOn(DATE_MAPPER.dateTimeToDate(source.getLastUpdatedOn()))
                .lastUpdatedBy(source.getLastUpdatedBy())
                .version(source.getVersion())
                .apiVersion(source.getApiVersion())
                .build();
    }

    @Override
    public LenderCreditApprovalDataDto clientToNetwork(LenderCreditApprovalData source) {
        if (source == null) {
            return null;
        }

        return LenderCreditApprovalDataDto.builder()
                .lenderCreditApprovalDataId(source.getLenderCreditApprovalDataId())
                .marketplaceCountry(source.getMarketplaceCountry())
                .partnerCode(source.getPartnerCode())
                .productCode(source.getProductCode())
                .snapshotDate(DATE_MAPPER.dateToDateTime(source.getSnapshotDate()))
                .dataReportingFrequency(EnumMapper.enumToString(source.getDataReportingFrequency()))
                .currencyCode(source.getCurrencyCode())
                .creditApprovalId(source.getCreditApprovalId())
                .lenderCreditApprovalSnapshotData(LENDER_CREDIT_APPROVAL_SNAPSHOT_DATA_MAPPER.clientToNetwork(source.getLenderCreditApprovalSnapshotData()))
                .lenderCreditApprovalAggregatedData(LENDER_CREDIT_APPROVAL_AGGREGATED_DATA_MAPPER.clientToNetwork(source.getLenderCreditApprovalAggregatedData()))
                .createdOn(DATE_MAPPER.dateToDateTime(source.getCreatedOn()))
                .createdBy(source.getCreatedBy())
                .lastUpdatedOn(DATE_MAPPER.dateToDateTime(source.getLastUpdatedOn()))
                .lastUpdatedBy(source.getLastUpdatedBy())
                .version(source.getVersion())
                .apiVersion(source.getApiVersion())
                .build();
    }
}

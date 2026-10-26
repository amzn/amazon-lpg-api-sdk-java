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

import com.amazon.lending.http.model.CreditApprovalDto;
import com.amazon.lending.http.model.mapper.CreditApprovalMapper;
import com.amazon.lending.model.CreditApproval;
import com.amazon.lending.model.CreditApprovalDetails;
import com.amazon.lending.model.CreditApprovalStatus;

import java.util.Optional;

public class CreditApprovalMapperImpl implements CreditApprovalMapper {

    private static final DateMapper DATE_MAPPER = new DateMapper();

    @Override
    public CreditApproval networkToClient(CreditApprovalDto source) {
        if (source == null) {
            return null;
        }

        return CreditApproval.builder()
                .partnerCode(source.getPartnerCode())
                .productCode(source.getProductCode())
                .marketplaceCountry(source.getMarketplaceCountry())
                .lastUpdatedBy(source.getLastUpdatedBy())
                .lastUpdatedOn(DATE_MAPPER.dateTimeToDate(source.getLastUpdatedOn()))
                .apiVersion(source.getApiVersion())
                .version(source.getVersion())
                .creditApprovalId(source.getCreditApprovalId())
                .applicationId(source.getApplicationId())
                .creditApprovalStatus(EnumMapper.stringToEnum(source.getCreditApprovalStatus(), CreditApprovalStatus.class))
                .details(networkToClient(source.getDetails()))
                .build();
    }

    @Override
    public CreditApprovalDetails networkToClient(CreditApprovalDto.CreditApprovalDetails source) {
        if (source == null) {
            return null;
        }

        return CreditApprovalDetails.builder()
                .amount(source.getAmount())
                .apr(source.getApr())
                .interest(source.getInterest())
                .repaymentRate(source.getRepaymentRate())
                .currency(source.getCurrency())
                .processingFee(source.getProcessingFee())
                .term(source.getTerm())
                .termUnit(source.getTermUnit())
                .build();
    }

    @Override
    public CreditApprovalDto clientToNetwork(CreditApproval source) {
        if (source == null) {
            return null;
        }

        return CreditApprovalDto.builder()
                .partnerCode(source.getPartnerCode())
                .productCode(source.getProductCode())
                .marketplaceCountry(source.getMarketplaceCountry())
                .lastUpdatedBy(source.getLastUpdatedBy())
                .lastUpdatedOn(DATE_MAPPER.dateToDateTime(source.getLastUpdatedOn()))
                .apiVersion(source.getApiVersion())
                .version(source.getVersion())
                .creditApprovalId(source.getCreditApprovalId())
                .applicationId(source.getApplicationId())
                .creditApprovalStatus(Optional.ofNullable(source.getCreditApprovalStatus()).map(Enum::name).orElse(null))
                .details(clientToNetwork(source.getDetails()))
                .build();
    }

    @Override
    public CreditApprovalDto.CreditApprovalDetails clientToNetwork(CreditApprovalDetails source) {
        if (source == null) {
            return null;
        }

        return CreditApprovalDto.CreditApprovalDetails.builder()
                .amount(source.getAmount())
                .apr(source.getApr())
                .interest(source.getInterest())
                .repaymentRate(source.getRepaymentRate())
                .currency(source.getCurrency())
                .processingFee(source.getProcessingFee())
                .term(source.getTerm())
                .termUnit(source.getTermUnit())
                .build();
    }
}

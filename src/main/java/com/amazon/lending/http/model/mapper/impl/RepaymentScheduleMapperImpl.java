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

import com.amazon.lending.http.model.RepaymentScheduleDto;
import com.amazon.lending.http.model.mapper.RepaymentScheduleMapper;
import com.amazon.lending.model.Repayment;
import com.amazon.lending.model.RepaymentSchedule;
import com.amazon.lending.model.RepaymentScheduleDetails;
import com.amazon.lending.model.RepaymentStatus;

import java.util.Optional;
import java.util.stream.Collectors;

public class RepaymentScheduleMapperImpl implements RepaymentScheduleMapper {

    private static final DateMapper DATE_MAPPER = new DateMapper();

    @Override
    public RepaymentSchedule networkToClient(final RepaymentScheduleDto source) {
        if (source == null) {
            return null;
        }

        return RepaymentSchedule.builder()
                .repaymentScheduleId(source.getRepaymentScheduleId())
                .loanId(source.getLoanId())
                .productCode(source.getProductCode())
                .partnerCode(source.getPartnerCode())
                .marketplaceCountry(source.getMarketplaceCountry())
                .apiVersion(source.getApiVersion())
                .lastUpdatedBy(source.getLastUpdatedBy())
                .lastUpdatedOn(DATE_MAPPER.dateTimeToDate(source.getLastUpdatedOn()))
                .version(source.getVersion())
                .details(networkToClient(source.getDetails()))
                .build();
    }

    @Override
    public RepaymentScheduleDto clientToNetwork(final RepaymentSchedule source) {
        if (source == null) {
            return null;
        }

        return RepaymentScheduleDto.builder()
                .repaymentScheduleId(source.getRepaymentScheduleId())
                .loanId(source.getLoanId())
                .productCode(source.getProductCode())
                .partnerCode(source.getPartnerCode())
                .marketplaceCountry(source.getMarketplaceCountry())
                .apiVersion(source.getApiVersion())
                .lastUpdatedBy(source.getLastUpdatedBy())
                .lastUpdatedOn(DATE_MAPPER.dateToDateTime(source.getLastUpdatedOn()))
                .version(source.getVersion())
                .details(clientToNetwork(source.getDetails()))
                .build();
    }

    private RepaymentScheduleDetails networkToClient(final RepaymentScheduleDto.RepaymentScheduleDetails source) {
        if (source == null) {
            return null;
        }

        return RepaymentScheduleDetails.builder()
                .createdOn(DATE_MAPPER.dateTimeToDate(source.getCreatedOn()))
                .repayments(
                        Optional.ofNullable(
                                source.getRepayments())
                                .map(list -> list.stream()
                                        .map(this::networkToClient)
                                        .collect(Collectors.toList())
                                )
                                .orElse(null)
                )
                .build();
    }

    private RepaymentScheduleDto.RepaymentScheduleDetails clientToNetwork(final RepaymentScheduleDetails source) {
        if (source == null) {
            return null;
        }

        return RepaymentScheduleDto.RepaymentScheduleDetails.builder()
                .createdOn(DATE_MAPPER.dateToDateTime(source.getCreatedOn()))
                .repayments(
                        Optional.ofNullable(
                                source.getRepayments())
                                .map(list -> list.stream()
                                        .map(this::clientToNetwork)
                                        .collect(Collectors.toList())
                                )
                                .orElse(null)
                )
                .build();
    }

    private Repayment networkToClient(final RepaymentScheduleDto.RepaymentScheduleDetails.Repayment source) {
        if (source == null) {
            return null;
        }

        return Repayment.builder()
                .dueOn(DATE_MAPPER.dateTimeToDate(source.getDueOn()))
                .paidOn(DATE_MAPPER.dateTimeToDate(source.getPaidOn()))
                .feeDueAmount(source.getFeeDueAmount())
                .feePaidAmount(source.getFeePaidAmount())
                .interestDueAmount(source.getInterestDueAmount())
                .interestPaidAmount(source.getInterestPaidAmount())
                .principalDueAmount(source.getPrincipalDueAmount())
                .principalPaidAmount(source.getPrincipalPaidAmount())
                .totalDueAmount(source.getTotalDueAmount())
                .totalPaidAmount(source.getTotalPaidAmount())
                .repaymentStatus(EnumMapper.stringToEnum(source.getRepaymentStatus(), RepaymentStatus.class))
                .lastUpdatedBy(source.getLastUpdatedBy())
                .lastUpdatedBy(source.getLastUpdatedBy())
                .build();
    }

    private RepaymentScheduleDto.RepaymentScheduleDetails.Repayment clientToNetwork(final Repayment source) {
        if (source == null) {
            return null;
        }

        return RepaymentScheduleDto.RepaymentScheduleDetails.Repayment.builder()
                .dueOn(DATE_MAPPER.dateToDateTime(source.getDueOn()))
                .paidOn(DATE_MAPPER.dateToDateTime(source.getPaidOn()))
                .feeDueAmount(source.getFeeDueAmount())
                .feePaidAmount(source.getFeePaidAmount())
                .interestDueAmount(source.getInterestDueAmount())
                .interestPaidAmount(source.getInterestPaidAmount())
                .principalDueAmount(source.getPrincipalDueAmount())
                .principalPaidAmount(source.getPrincipalPaidAmount())
                .totalDueAmount(source.getTotalDueAmount())
                .totalPaidAmount(source.getTotalPaidAmount())
                .repaymentStatus(EnumMapper.enumToString(source.getRepaymentStatus()))
                .lastUpdatedBy(source.getLastUpdatedBy())
                .lastUpdatedBy(source.getLastUpdatedBy())
                .build();
    }
}

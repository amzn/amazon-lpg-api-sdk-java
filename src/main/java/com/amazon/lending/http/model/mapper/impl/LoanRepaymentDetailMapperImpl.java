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

import com.amazon.lending.http.model.LoanRepaymentDetailDto;
import com.amazon.lending.http.model.mapper.LoanRepaymentDetailMapper;
import com.amazon.lending.model.LoanRepayment;
import com.amazon.lending.model.LoanRepaymentDetail;
import com.amazon.lending.model.RepaymentScheduleContext;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class LoanRepaymentDetailMapperImpl implements LoanRepaymentDetailMapper {

    private static final DateMapper DATE_MAPPER = new DateMapper();

    @Override
    public LoanRepaymentDetail networkToClient(LoanRepaymentDetailDto source) {
        if (source == null) {
            return null;
        }

        return LoanRepaymentDetail.builder()
                .loanId(source.getLoanId())
                .repayments(mapRepayments(source.getRepayments()))
                .build();
    }

    @Override
    public LoanRepaymentDetailDto clientToNetwork(LoanRepaymentDetail source) {
        if (source == null) {
            return null;
        }

        return LoanRepaymentDetailDto.builder()
                .loanId(source.getLoanId())
                .repayments(mapRepaymentsToDto(source.getRepayments()))
                .build();
    }

    private List<LoanRepayment> mapRepayments(List<LoanRepaymentDetailDto.LoanRepayment> dtoRepayments) {
        if (dtoRepayments == null) {
            return Collections.emptyList();
        }

        return dtoRepayments.stream()
                .map(dto -> {
                    LoanRepayment.LoanRepaymentBuilder builder = LoanRepayment.builder()
                            .repaymentId(dto.getRepaymentId())
                            .reconciliationId(dto.getReconciliationId())
                            .amount(dto.getAmount())
                            .timeOfInitiation(DATE_MAPPER.dateTimeToDate(dto.getTimeOfInitiation()))
                            .status(dto.getStatus())
                            .currency(dto.getCurrency());
                    mapScheduleContext(dto.getRepaymentScheduleContext())
                            .ifPresent(builder::repaymentScheduleContext);
                    return builder.build();
                })
                .collect(Collectors.toList());
    }

    private List<LoanRepaymentDetailDto.LoanRepayment> mapRepaymentsToDto(List<LoanRepayment> repayments) {
        if (repayments == null) {
            return Collections.emptyList();
        }

        return repayments.stream()
                .map(model -> {
                    LoanRepaymentDetailDto.LoanRepayment.LoanRepaymentBuilder builder =
                            LoanRepaymentDetailDto.LoanRepayment.builder()
                                    .repaymentId(model.getRepaymentId())
                                    .reconciliationId(model.getReconciliationId())
                                    .amount(model.getAmount())
                                    .timeOfInitiation(DATE_MAPPER.dateToDateTime(model.getTimeOfInitiation()))
                                    .status(model.getStatus())
                                    .currency(model.getCurrency());
                    mapScheduleContextToDto(model.getRepaymentScheduleContext())
                            .ifPresent(builder::repaymentScheduleContext);
                    return builder.build();
                })
                .collect(Collectors.toList());
    }

    private Optional<RepaymentScheduleContext> mapScheduleContext(
            LoanRepaymentDetailDto.RepaymentScheduleContext dto) {
        if (dto == null) {
            return Optional.empty();
        }
        return Optional.of(RepaymentScheduleContext.builder()
                .lastUpdatedOn(DATE_MAPPER.dateTimeToDate(dto.getLastUpdatedOn()))
                .build());
    }

    private Optional<LoanRepaymentDetailDto.RepaymentScheduleContext> mapScheduleContextToDto(
            RepaymentScheduleContext model) {
        if (model == null) {
            return Optional.empty();
        }
        return Optional.of(LoanRepaymentDetailDto.RepaymentScheduleContext.builder()
                .lastUpdatedOn(DATE_MAPPER.dateToDateTime(model.getLastUpdatedOn()))
                .build());
    }
}

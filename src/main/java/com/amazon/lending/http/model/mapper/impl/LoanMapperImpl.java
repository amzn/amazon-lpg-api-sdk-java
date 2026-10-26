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

import com.amazon.lending.http.model.LoanDto;
import com.amazon.lending.http.model.mapper.LoanMapper;
import com.amazon.lending.model.Loan;
import com.amazon.lending.model.LoanDetails;
import com.amazon.lending.model.LoanStatus;

import java.util.Optional;

public class LoanMapperImpl implements LoanMapper {

    private static final DateMapper DATE_MAPPER = new DateMapper();

    @Override
    public Loan networkToClient(LoanDto source) {
        if (source == null) {
            return null;
        }

        return Loan.builder()
                .partnerCode(source.getPartnerCode())
                .productCode(source.getProductCode())
                .marketplaceCountry(source.getMarketplaceCountry())
                .lastUpdatedBy(source.getLastUpdatedBy())
                .lastUpdatedOn(DATE_MAPPER.dateTimeToDate(source.getLastUpdatedOn()))
                .apiVersion(source.getApiVersion())
                .version(source.getVersion())
                .loanId(source.getLoanId())
                .creditApprovalId(source.getCreditApprovalId())
                .loanStatus(EnumMapper.stringToEnum(source.getLoanStatus(), LoanStatus.class))
                .idempotencyKey(source.getIdempotencyKey())
                .details(networkToClient(source.getDetails()))
                .build();
    }

    @Override
    public LoanDetails networkToClient(LoanDto.LoanDetails source) {
        if (source == null) {
            return null;
        }

        return LoanDetails.builder()
                .amount(source.getAmount())
                .apr(source.getApr())
                .interest(source.getInterest())
                .repaymentRate(source.getRepaymentRate())
                .closedOn(DATE_MAPPER.dateTimeToDate(source.getClosedOn()))
                .createdOn(DATE_MAPPER.dateTimeToDate(source.getCreatedOn()))
                .currency(source.getCurrency())
                .matureOn(DATE_MAPPER.dateTimeToDate(source.getMatureOn()))
                .outstandingPrincipalBalance(source.getOutstandingPrincipalBalance())
                .pastDueAmount(source.getPastDueAmount())
                .pastDuePeriod(source.getPastDuePeriod())
                .pastDuePeriodUnit(source.getPastDuePeriodUnit())
                .processingFee(source.getProcessingFee())
                .term(source.getTerm())
                .termUnit(source.getTermUnit())
                .totalFeesRepaid(source.getTotalFeesRepaid())
                .totalInterestRepaid(source.getTotalInterestRepaid())
                .totalPrincipalRepaid(source.getTotalPrincipalRepaid())
                .automatedRepaymentsEnabled(source.getAutomatedRepaymentsEnabled())
                .build();
    }

    @Override
    public LoanDto clientToNetwork(Loan source) {
        if (source == null) {
            return null;
        }

        return LoanDto.builder()
                .partnerCode(source.getPartnerCode())
                .productCode(source.getProductCode())
                .marketplaceCountry(source.getMarketplaceCountry())
                .lastUpdatedBy(source.getLastUpdatedBy())
                .lastUpdatedOn(DATE_MAPPER.dateToDateTime(source.getLastUpdatedOn()))
                .apiVersion(source.getApiVersion())
                .version(source.getVersion())
                .loanId(source.getLoanId())
                .creditApprovalId(source.getCreditApprovalId())
                .loanStatus(Optional.ofNullable(source.getLoanStatus()).map(Enum::name).orElse(null))
                .idempotencyKey(source.getIdempotencyKey())
                .details(clientToNetwork(source.getDetails()))
                .build();
    }

    @Override
    public LoanDto.LoanDetails clientToNetwork(LoanDetails source) {
        if (source == null) {
            return null;
        }

        return LoanDto.LoanDetails.builder()
                .amount(source.getAmount())
                .apr(source.getApr())
                .interest(source.getInterest())
                .repaymentRate(source.getRepaymentRate())
                .closedOn(DATE_MAPPER.dateToDateTime(source.getClosedOn()))
                .createdOn(DATE_MAPPER.dateToDateTime(source.getCreatedOn()))
                .currency(source.getCurrency())
                .matureOn(DATE_MAPPER.dateToDateTime(source.getMatureOn()))
                .outstandingPrincipalBalance(source.getOutstandingPrincipalBalance())
                .pastDueAmount(source.getPastDueAmount())
                .pastDuePeriod(source.getPastDuePeriod())
                .pastDuePeriodUnit(source.getPastDuePeriodUnit())
                .processingFee(source.getProcessingFee())
                .term(source.getTerm())
                .termUnit(source.getTermUnit())
                .totalFeesRepaid(source.getTotalFeesRepaid())
                .totalInterestRepaid(source.getTotalInterestRepaid())
                .totalPrincipalRepaid(source.getTotalPrincipalRepaid())
                .automatedRepaymentsEnabled(source.getAutomatedRepaymentsEnabled())
                .build();
    }
}

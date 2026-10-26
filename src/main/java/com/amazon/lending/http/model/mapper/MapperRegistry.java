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

package com.amazon.lending.http.model.mapper;

import com.amazon.lending.http.model.AccountLockDto;
import com.amazon.lending.http.model.ApplicationDto;
import com.amazon.lending.http.model.CreateOfferRequest;
import com.amazon.lending.http.model.CreateOfferAsyncRequestDto;
import com.amazon.lending.http.model.PerformanceDataAuthTokenDto;
import com.amazon.lending.http.model.mapper.impl.CreateOfferAsyncRequestDtoMapperImpl;
import com.amazon.lending.http.model.mapper.impl.CreateOfferAsyncResponseMapperImpl;
import com.amazon.lending.http.model.mapper.impl.PerformanceDataAuthTokenResultMapperImpl;
import com.amazon.lending.model.CreateLenderPortfolioDataRequest;
import com.amazon.lending.http.model.CreateLenderPortfolioDataRequestDto;
import com.amazon.lending.model.CreateLenderCreditApprovalDataRequest;
import com.amazon.lending.http.model.CreateLenderCreditApprovalDataRequestDto;
import com.amazon.lending.model.CreateLenderLoanDataRequest;
import com.amazon.lending.http.model.CreateLenderLoanDataRequestDto;
import com.amazon.lending.http.model.CreditApprovalDto;
import com.amazon.lending.http.model.LoanDto;
import com.amazon.lending.http.model.LoanRepaymentDetailDto;
import com.amazon.lending.http.model.OfferDto;
import com.amazon.lending.http.model.RepaymentCalculationMetadataDto;
import com.amazon.lending.http.model.RedirectUrlDto;
import com.amazon.lending.http.model.RepaymentScheduleDto;
import com.amazon.lending.http.model.VerificationKeyDto;
import com.amazon.lending.http.model.VerificationRequestDto;
import com.amazon.lending.http.model.VerificationResultDto;
import com.amazon.lending.http.model.LenderPortfolioDataDto;
import com.amazon.lending.http.model.LenderCreditApprovalDataDto;
import com.amazon.lending.http.model.LenderLoanDataDto;
import com.amazon.lending.http.model.mapper.impl.AccountLockMapperImpl;
import com.amazon.lending.http.model.mapper.impl.RedirectUrlMapperImpl;
import com.amazon.lending.http.model.mapper.impl.RepaymentScheduleMapperImpl;
import com.amazon.lending.http.model.mapper.impl.VerificationKeyMapperImpl;
import com.amazon.lending.http.model.mapper.impl.VerificationRequestMapperImpl;
import com.amazon.lending.http.model.mapper.impl.VerificationResultMapperImpl;
import com.amazon.lending.http.model.mapper.impl.CreateLenderPortfolioDataRequestMapperImpl;
import com.amazon.lending.http.model.mapper.impl.CreateLenderCreditApprovalDataRequestMapperImpl;
import com.amazon.lending.http.model.mapper.impl.CreateLenderLoanDataRequestMapperImpl;
import com.amazon.lending.http.model.mapper.impl.LoanRepaymentDetailMapperImpl;
import com.amazon.lending.http.model.mapper.impl.RepaymentCalculationMetadataMapperImpl;
import com.amazon.lending.model.AccountLock;
import com.amazon.lending.model.Application;
import com.amazon.lending.model.CreateOfferAsyncResponse;
import com.amazon.lending.model.CreditApproval;
import com.amazon.lending.model.Loan;
import com.amazon.lending.model.Offer;
import com.amazon.lending.model.LenderPortfolioData;
import com.amazon.lending.model.LenderCreditApprovalData;
import com.amazon.lending.model.LenderLoanData;
import com.amazon.lending.model.LoanRepaymentDetail;
import com.amazon.lending.model.RepaymentCalculationMetadata;
import com.amazon.lending.http.model.mapper.impl.ApplicationMapperImpl;
import com.amazon.lending.http.model.mapper.impl.CreditApprovalMapperImpl;
import com.amazon.lending.http.model.mapper.impl.LoanMapperImpl;
import com.amazon.lending.http.model.mapper.impl.OfferMapperImpl;
import com.amazon.lending.model.RepaymentSchedule;
import com.amazon.lending.model.VerificationKey;
import com.amazon.lending.model.VerificationRequest;
import com.amazon.lending.http.model.mapper.impl.LenderPortfolioDataMapperImpl;
import com.amazon.lending.http.model.mapper.impl.LenderCreditApprovalDataMapperImpl;
import com.amazon.lending.http.model.mapper.impl.LenderLoanDataMapperImpl;

import com.google.common.collect.ImmutableMap;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

import java.net.URL;
import java.util.Map;
import java.util.Optional;

public final class MapperRegistry {

    private static final Map<Pair<Class, Class>, DtoMapper> MAPPER_MAP = ImmutableMap.<Pair<Class, Class>, DtoMapper>builder()
            .put(Pair.of(AccountLockDto.class, AccountLock.class), new AccountLockMapperImpl())
            .put(Pair.of(ApplicationDto.class, Application.class), new ApplicationMapperImpl())
            .put(Pair.of(CreditApprovalDto.class, CreditApproval.class), new CreditApprovalMapperImpl())
            .put(Pair.of(LoanDto.class, Loan.class), new LoanMapperImpl())
            .put(Pair.of(OfferDto.class, Offer.class), new OfferMapperImpl())
            .put(Pair.of(CreateOfferRequest.class, CreateOfferRequest.class), new CreateOfferRequestMapper())
            .put(Pair.of(VerificationKeyDto.class, VerificationKey.class), new VerificationKeyMapperImpl())
            .put(Pair.of(VerificationRequestDto.class, VerificationRequest.class), new VerificationRequestMapperImpl())
            .put(Pair.of(VerificationResultDto.class, Boolean.class), new VerificationResultMapperImpl())
            .put(Pair.of(RedirectUrlDto.class, URL.class), new RedirectUrlMapperImpl())
            .put(Pair.of(RepaymentScheduleDto.class, RepaymentSchedule.class), new RepaymentScheduleMapperImpl())
            .put(Pair.of(LenderPortfolioDataDto.class, LenderPortfolioData.class), new LenderPortfolioDataMapperImpl())
            .put(Pair.of(CreateLenderPortfolioDataRequestDto.class, CreateLenderPortfolioDataRequest.class), new CreateLenderPortfolioDataRequestMapperImpl())
            .put(Pair.of(LenderCreditApprovalDataDto.class, LenderCreditApprovalData.class), new LenderCreditApprovalDataMapperImpl())
            .put(Pair.of(CreateLenderCreditApprovalDataRequestDto.class, CreateLenderCreditApprovalDataRequest.class), new CreateLenderCreditApprovalDataRequestMapperImpl())
            .put(Pair.of(LenderLoanDataDto.class, LenderLoanData.class), new LenderLoanDataMapperImpl())
            .put(Pair.of(CreateLenderLoanDataRequestDto.class, CreateLenderLoanDataRequest.class), new CreateLenderLoanDataRequestMapperImpl())
            .put(Pair.of(PerformanceDataAuthTokenDto.class, String.class), new PerformanceDataAuthTokenResultMapperImpl())
            .put(Pair.of(CreateOfferAsyncRequestDto.class, CreateOfferAsyncRequestDto.class), new CreateOfferAsyncRequestDtoMapperImpl())
            .put(Pair.of(CreateOfferAsyncResponse.class, CreateOfferAsyncResponse.class), new CreateOfferAsyncResponseMapperImpl())
            .put(Pair.of(LoanRepaymentDetailDto.class, LoanRepaymentDetail.class), new LoanRepaymentDetailMapperImpl())
            .put(Pair.of(RepaymentCalculationMetadataDto.class, RepaymentCalculationMetadata.class), new RepaymentCalculationMetadataMapperImpl())
            .build();

    private MapperRegistry() {}

    public static <NetworkType, ClientType> Optional<DtoMapper<NetworkType, ClientType>> getMapper(@NonNull final Class<NetworkType> networkType, @NonNull final Class<ClientType> clientType) {
        return Optional.ofNullable(MAPPER_MAP.get(Pair.of(networkType, clientType)));
    }

    @Value
    @AllArgsConstructor
    @EqualsAndHashCode
    private static class Pair<T, S> {
        private final T x;
        private final S y;

        static <T, S> Pair<T, S> of(final T x, final S y) {
            return new Pair<>(x, y);
        }
    }
}

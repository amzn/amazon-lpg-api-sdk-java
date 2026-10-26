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

import java.util.Optional;

import com.amazon.lending.http.exception.LPGNonTransientServiceException;
import com.amazon.lending.http.model.ApplicationDto;
import com.amazon.lending.http.model.mapper.ApplicationDetailsMapper;
import com.amazon.lending.model.Application;
import com.amazon.lending.model.ApplicationAbandonCode;
import com.amazon.lending.model.ApplicationDetails;
import com.amazon.lending.model.ApplicationRejectionCategory;
import com.amazon.lending.model.ApplicationRejectionCode;
import com.amazon.lending.model.ApplicationStatus;
import com.amazon.lending.model.ApplicationStatusCode;

public class ApplicationDetailsMapperImpl implements ApplicationDetailsMapper {

    private static final DateMapper DATE_MAPPER = new DateMapper();

    @Override
    public ApplicationDetails networkToClient(ApplicationDto source) {
        if (source == null) {
            return null;
        }

        return ApplicationDetails.builder()
                .accountLockId(source.getDetails().getAccountLockId())
                .currency(source.getDetails().getCurrency())
                .offerMaxApr(source.getDetails().getOfferMaxApr())
                .offerMinApr(source.getDetails().getOfferMinApr())
                .offerMaxInterest(source.getDetails().getOfferMaxInterest())
                .offerMinInterest(source.getDetails().getOfferMinInterest())
                .offerMaxAmount(source.getDetails().getOfferMaxAmount())
                .offerMinAmount(source.getDetails().getOfferMinAmount())
                .offerTermUnit(source.getDetails().getOfferTermUnit())
                .offerTerm(source.getDetails().getOfferTerm())
                .offerMinRepaymentRate(source.getDetails().getOfferMinRepaymentRate())
                .offerMaxRepaymentRate(source.getDetails().getOfferMaxRepaymentRate())
                .createdOn(DATE_MAPPER.dateTimeToDate(source.getDetails().getCreatedOn()))
                .abandonedOn(DATE_MAPPER.dateTimeToDate(source.getDetails().getAbandonedOn()))
                .approvedAmount(source.getDetails().getApprovedAmount())
                .approvedApr(source.getDetails().getApprovedApr())
                .approvedInterest(source.getDetails().getApprovedInterest())
                .approvedRepaymentRate(source.getDetails().getApprovedRepaymentRate())
                .approvedOn(DATE_MAPPER.dateTimeToDate(source.getDetails().getApprovedOn()))
                .approvedWithPendingSellerActionOn(DATE_MAPPER.dateTimeToDate(source.getDetails().getApprovedWithPendingSellerActionOn()))
                .approvedTerm(source.getDetails().getApprovedTerm())
                .approvedTermUnit(source.getDetails().getApprovedTermUnit())
                .closedOn(DATE_MAPPER.dateTimeToDate(source.getDetails().getClosedOn()))
                .confirmedOn(DATE_MAPPER.dateTimeToDate(source.getDetails().getConfirmedOn()))
                .accountLockRequired(source.getDetails().getAccountLockRequired())
                .accountChangeRequired(source.getDetails().getAccountChangeRequired())
                .receivedOn(DATE_MAPPER.dateTimeToDate(source.getDetails().getReceivedOn()))
                .rejectedOn(DATE_MAPPER.dateTimeToDate(source.getDetails().getRejectedOn()))
                .rejectionCategory(EnumMapper.stringToEnum(source.getDetails().getRejectionCategory(), ApplicationRejectionCategory.class))
                .requestAmount(source.getDetails().getRequestAmount())
                .requestApr(source.getDetails().getRequestApr())
                .requestInterest(source.getDetails().getRequestInterest())
                .requestRepaymentRate(source.getDetails().getRequestRepaymentRate())
                .requestTerm(source.getDetails().getRequestTerm())
                .requestTermUnit(source.getDetails().getRequestTermUnit())
                .statusReasonCode(getStatusReasonCode(source))
                .automatedRepaymentsEnabled(source.getDetails().getAutomatedRepaymentsEnabled())
                .build();
    }

    @Override
    public ApplicationDto.ApplicationDetails clientToNetwork(Application source) {
        if (source == null) {
            return null;
        }

        return ApplicationDto.ApplicationDetails.builder()
                .accountLockId(source.getDetails().getAccountLockId())
                .currency(source.getDetails().getCurrency())
                .offerMaxApr(source.getDetails().getOfferMaxApr())
                .offerMinApr(source.getDetails().getOfferMinApr())
                .offerMaxInterest(source.getDetails().getOfferMaxInterest())
                .offerMinInterest(source.getDetails().getOfferMinInterest())
                .offerMaxAmount(source.getDetails().getOfferMaxAmount())
                .offerMinAmount(source.getDetails().getOfferMinAmount())
                .offerTermUnit(source.getDetails().getOfferTermUnit())
                .offerTerm(source.getDetails().getOfferTerm())
                .offerMinRepaymentRate(source.getDetails().getOfferMinRepaymentRate())
                .offerMaxRepaymentRate(source.getDetails().getOfferMaxRepaymentRate())
                .createdOn(DATE_MAPPER.dateToDateTime(source.getDetails().getCreatedOn()))
                .abandonedOn(DATE_MAPPER.dateToDateTime(source.getDetails().getAbandonedOn()))
                .approvedAmount(source.getDetails().getApprovedAmount())
                .approvedApr(source.getDetails().getApprovedApr())
                .approvedInterest(source.getDetails().getApprovedInterest())
                .approvedRepaymentRate(source.getDetails().getApprovedRepaymentRate())
                .approvedOn(DATE_MAPPER.dateToDateTime(source.getDetails().getApprovedOn()))
                .approvedWithPendingSellerActionOn(DATE_MAPPER.dateToDateTime(source.getDetails().getApprovedWithPendingSellerActionOn()))
                .approvedTerm(source.getDetails().getApprovedTerm())
                .approvedTermUnit(source.getDetails().getApprovedTermUnit())
                .closedOn(DATE_MAPPER.dateToDateTime(source.getDetails().getClosedOn()))
                .confirmedOn(DATE_MAPPER.dateToDateTime(source.getDetails().getConfirmedOn()))
                .accountLockRequired(source.getDetails().getAccountLockRequired())
                .accountChangeRequired(source.getDetails().getAccountChangeRequired())
                .receivedOn(DATE_MAPPER.dateToDateTime(source.getDetails().getReceivedOn()))
                .rejectedOn(DATE_MAPPER.dateToDateTime(source.getDetails().getRejectedOn()))
                .rejectionCategory(Optional.ofNullable(source.getDetails().getRejectionCategory()).map(Enum::name).orElse(null))
                .requestAmount(source.getDetails().getRequestAmount())
                .requestApr(source.getDetails().getRequestApr())
                .requestRepaymentRate(source.getDetails().getRequestRepaymentRate())
                .requestInterest(source.getDetails().getRequestInterest())
                .requestTerm(source.getDetails().getRequestTerm())
                .requestTermUnit(source.getDetails().getRequestTermUnit())
                .statusReasonCode(statusCodeToString(source.getDetails().getStatusReasonCode()))
                .automatedRepaymentsEnabled(source.getDetails().getAutomatedRepaymentsEnabled())
                .build();
    }

    private ApplicationStatusCode getStatusReasonCode(final ApplicationDto applicationDto) {
        final String applicationStatus = applicationDto.getApplicationStatus();
        final String applicationStatusReasonCode = applicationDto.getDetails().getStatusReasonCode();

        if (applicationStatus == null || applicationStatusReasonCode == null) {
            return null;
        }

        if (ApplicationStatus.ABANDONED.name().equals(applicationStatus)) {
            return ApplicationAbandonCode.valueOf(applicationStatusReasonCode);
        } else if (ApplicationStatus.REJECTED.name().equals(applicationStatus)) {
            if (applicationDto.getDetails().getRejectionCategory() == null) {
                return null;
            }
            ApplicationRejectionCategory rejectionCategory = ApplicationRejectionCategory.valueOf(applicationDto
                    .getDetails().getRejectionCategory());

            switch (rejectionCategory) {
                case BUREAU:
                    return ApplicationRejectionCode.BUREAU.valueOf(applicationStatusReasonCode);
                case COMPLIANCE:
                    return ApplicationRejectionCode.COMPLIANCE.valueOf(applicationStatusReasonCode);
                case KYC:
                    return ApplicationRejectionCode.KYC.valueOf(applicationStatusReasonCode);
                case POLICY:
                    return ApplicationRejectionCode.POLICY.valueOf(applicationStatusReasonCode);
                case SELLER_REJECT:
                    return ApplicationRejectionCode.SELLER_REJECT.valueOf(applicationStatusReasonCode);
                default:
                    throw new LPGNonTransientServiceException("Unrecognized rejection category found: " + rejectionCategory.name());
            }
        }

        return null;
    }

    private String statusCodeToString(final ApplicationStatusCode source) {
        if (source == null) {
            return null;
        }

        return source.name();
    }
}

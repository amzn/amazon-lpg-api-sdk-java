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

import com.amazon.lending.http.model.OfferDto;
import com.amazon.lending.http.model.mapper.OfferMapper;
import com.amazon.lending.model.Offer;
import com.amazon.lending.model.OfferDetails;
import com.amazon.lending.model.OfferStatus;

import java.util.Optional;

public class OfferMapperImpl implements OfferMapper {

    private static final DateMapper DATE_MAPPER = new DateMapper();

    @Override
    public Offer networkToClient(OfferDto source) {
        if (source == null) {
            return null;
        }

        return Offer.builder()
                .partnerCode(source.getPartnerCode())
                .productCode(source.getProductCode())
                .marketplaceCountry(source.getMarketplaceCountry())
                .apiVersion(source.getApiVersion())
                .lastUpdatedBy(source.getLastUpdatedBy())
                .lastUpdatedOn(DATE_MAPPER.dateTimeToDate(source.getLastUpdatedOn()))
                .offerId(source.getOfferId())
                .offerStatus(EnumMapper.stringToEnum(source.getOfferStatus(), OfferStatus.class))
                .version(source.getVersion())
                .offerDataset(source.getOfferDataset())
                .latestDataset(source.getLatestDataset())
                .details(networkToClient(source.getDetails()))
                .offerCategory(source.getOfferCategory())
                .partnerOfferId(source.getPartnerOfferId())
                .build();
    }

    @Override
    public OfferDetails networkToClient(OfferDto.OfferDetails source) {
        if (source == null) {
            return null;
        }

        return OfferDetails.builder()
                .term(source.getTerm())
                .termUnit(source.getTermUnit())
                .currency(source.getCurrency())
                .minApr(source.getMinApr())
                .maxApr(source.getMaxApr())
                .minInterest(source.getMinInterest())
                .maxInterest(source.getMaxInterest())
                .minRepaymentRate(source.getMinRepaymentRate())
                .maxRepaymentRate(source.getMaxRepaymentRate())
                .minAmount(source.getMinAmount())
                .maxAmount(source.getMaxAmount())
                .expireOn(DATE_MAPPER.dateTimeToDate(source.getExpireOn()))
                .heuristicVersion(source.getHeuristicVersion())
                .automatedRepaymentsRequired(source.getAutomatedRepaymentsRequired())
                .build();
    }

    @Override
    public OfferDto clientToNetwork(Offer source) {
        if (source == null) {
            return null;
        }

        return OfferDto.builder()
                .partnerCode(source.getPartnerCode())
                .productCode(source.getProductCode())
                .marketplaceCountry(source.getMarketplaceCountry())
                .apiVersion(source.getApiVersion())
                .lastUpdatedBy(source.getLastUpdatedBy())
                .lastUpdatedOn(DATE_MAPPER.dateToDateTime(source.getLastUpdatedOn()))
                .offerId(source.getOfferId())
                .offerStatus(Optional.ofNullable(source.getOfferStatus()).map(Enum::name).orElse(null))
                .version(source.getVersion())
                .latestDataset(source.getLatestDataset())
                .offerDataset(source.getOfferDataset())
                .details(clientToNetwork(source.getDetails()))
                .offerCategory(source.getOfferCategory())
                .partnerOfferId(source.getPartnerOfferId())
                .build();
    }

    @Override
    public OfferDto.OfferDetails clientToNetwork(OfferDetails source) {
        if (source == null) {
            return null;
        }

        return OfferDto.OfferDetails.builder()
                .term(source.getTerm())
                .termUnit(source.getTermUnit())
                .currency(source.getCurrency())
                .minApr(source.getMinApr())
                .maxApr(source.getMaxApr())
                .minInterest(source.getMinInterest())
                .maxInterest(source.getMaxInterest())
                .minRepaymentRate(source.getMinRepaymentRate())
                .maxRepaymentRate(source.getMaxRepaymentRate())
                .minAmount(source.getMinAmount())
                .maxAmount(source.getMaxAmount())
                .expireOn(DATE_MAPPER.dateToDateTime(source.getExpireOn()))
                .heuristicVersion(source.getHeuristicVersion())
                .automatedRepaymentsRequired(source.getAutomatedRepaymentsRequired())
                .build();
    }
}

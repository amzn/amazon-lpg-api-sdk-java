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

import com.amazon.lending.http.model.ApplicationDto;
import com.amazon.lending.http.model.mapper.ApplicationDetailsMapper;
import com.amazon.lending.http.model.mapper.ApplicationMapper;
import com.amazon.lending.model.Application;
import com.amazon.lending.model.ApplicationStatus;

import java.util.Optional;

public class ApplicationMapperImpl implements ApplicationMapper {

    private static final DateMapper DATE_MAPPER = new DateMapper();
    private static final ApplicationDetailsMapper APPLICATION_DETAILS_MAPPER = new ApplicationDetailsMapperImpl();

    @Override
    public Application networkToClient(ApplicationDto source) {
        if (source == null) {
            return null;
        }

        return Application.builder()
                .partnerCode(source.getPartnerCode())
                .productCode(source.getProductCode())
                .marketplaceCountry(source.getMarketplaceCountry())
                .applicationId(source.getApplicationId())
                .offerId(source.getOfferId())
                .applicationStatus(EnumMapper.stringToEnum(source.getApplicationStatus(), ApplicationStatus.class))
                .lastUpdatedBy(source.getLastUpdatedBy())
                .lastUpdatedOn(DATE_MAPPER.dateTimeToDate(source.getLastUpdatedOn()))
                .apiVersion(source.getApiVersion())
                .version(source.getVersion())
                .details(APPLICATION_DETAILS_MAPPER.networkToClient(source))
                .build();
    }

    @Override
    public ApplicationDto clientToNetwork(Application source) {
        if (source == null) {
            return null;
        }

        return ApplicationDto.builder()
                .partnerCode(source.getPartnerCode())
                .productCode(source.getProductCode())
                .marketplaceCountry(source.getMarketplaceCountry())
                .applicationId(source.getApplicationId())
                .offerId(source.getOfferId())
                .applicationStatus(Optional.ofNullable(source.getApplicationStatus()).map(Enum::name).orElse(null))
                .lastUpdatedBy(source.getLastUpdatedBy())
                .lastUpdatedOn(DATE_MAPPER.dateToDateTime(source.getLastUpdatedOn()))
                .apiVersion(source.getApiVersion())
                .version(source.getVersion())
                .details(APPLICATION_DETAILS_MAPPER.clientToNetwork(source))
                .build();
    }
}

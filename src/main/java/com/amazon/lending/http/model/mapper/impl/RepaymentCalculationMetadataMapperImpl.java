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

import com.amazon.lending.http.model.RepaymentCalculationMetadataDto;
import com.amazon.lending.http.model.mapper.RepaymentCalculationMetadataMapper;
import com.amazon.lending.model.MoneyAmount;
import com.amazon.lending.model.PointInTime;
import com.amazon.lending.model.RepaymentCalculationMetadata;

public class RepaymentCalculationMetadataMapperImpl implements RepaymentCalculationMetadataMapper {

    private static final DateMapper DATE_MAPPER = new DateMapper();

    @Override
    public RepaymentCalculationMetadata networkToClient(RepaymentCalculationMetadataDto source) {
        if (source == null) {
            return null;
        }

        return RepaymentCalculationMetadata.builder()
                .creditApprovalId(source.getCreditApprovalId())
                .pointInTime(mapPointInTime(source.getPointInTime()))
                .expectedPayoutDate(DATE_MAPPER.dateTimeToDate(source.getExpectedPayoutDate()))
                .referenceTime(source.getReferenceTime())
                .build();
    }

    @Override
    public RepaymentCalculationMetadataDto clientToNetwork(RepaymentCalculationMetadata source) {
        if (source == null) {
            return null;
        }

        throw new UnsupportedOperationException(
                "Conversion from RepaymentCalculationMetadata to RepaymentCalculationMetadataDto is not supported");
    }

    private PointInTime mapPointInTime(RepaymentCalculationMetadataDto.PointInTime dto) {
        if (dto == null) {
            return null;
        }

        return PointInTime.builder()
                .baseAmount(mapMoneyAmount(dto.getBaseAmount()))
                .netPayable(mapMoneyAmount(dto.getNetPayable()))
                .build();
    }

    private MoneyAmount mapMoneyAmount(RepaymentCalculationMetadataDto.MoneyAmount dto) {
        if (dto == null) {
            return null;
        }

        return MoneyAmount.builder()
                .amount(dto.getAmount())
                .currencyCode(dto.getCurrencyCode())
                .build();
    }

}

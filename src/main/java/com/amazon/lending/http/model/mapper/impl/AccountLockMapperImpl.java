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

import com.amazon.lending.http.model.AccountLockDto;
import com.amazon.lending.model.AccountLock;
import com.amazon.lending.model.AccountLockAllowedUpdates;
import com.amazon.lending.model.AccountLockStatus;
import com.amazon.lending.model.BankIdCodeType;
import com.amazon.lending.http.model.mapper.AccountLockMapper;
import com.google.common.collect.ImmutableList;

import java.util.Optional;
import java.util.stream.Collectors;

public class AccountLockMapperImpl implements AccountLockMapper {

    private static final DateMapper DATE_MAPPER = new DateMapper();

    @Override
    public AccountLock networkToClient(AccountLockDto source) {
        if (source == null) {
            return null;
        }

        return AccountLock.builder()
                .partnerCode(source.getPartnerCode())
                .productCode(source.getProductCode())
                .marketplaceCountry(source.getMarketplaceCountry())
                .accountLockId(source.getAccountLockId())
                .accountLockStatus(EnumMapper.stringToEnum(source.getAccountLockStatus(), AccountLockStatus.class))
                .accountTail(source.getAccountTail())
                .bankIdCode(source.getBankIdCode())
                .bankIdCodeType(EnumMapper.stringToEnum(source.getBankIdCodeType(), BankIdCodeType.class))
                .unlockReasonCode(source.getUnlockReasonCode())
                .lastUpdatedBy(source.getLastUpdatedBy())
                .lastUpdatedOn(DATE_MAPPER.dateTimeToDate(source.getLastUpdatedOn()))
                .apiVersion(source.getApiVersion())
                .version(source.getVersion())
                .allowedUpdates(Optional.ofNullable(source.getAllowedUpdates()).orElseGet(ImmutableList::of).stream()
                        .map(s -> EnumMapper.stringToEnum(s, AccountLockAllowedUpdates.class)).collect(Collectors.toList()))
                .build();
    }

    @Override
    public AccountLockDto clientToNetwork(AccountLock source) {
        if (source == null) {
            return null;
        }

        return AccountLockDto.builder()
                .partnerCode(source.getPartnerCode())
                .productCode(source.getProductCode())
                .marketplaceCountry(source.getMarketplaceCountry())
                .accountLockId(source.getAccountLockId())
                .accountLockStatus(Optional.ofNullable(source.getAccountLockStatus()).map(Enum::name).orElse(null))
                .accountTail(source.getAccountTail())
                .bankIdCode(source.getBankIdCode())
                .bankIdCodeType(Optional.ofNullable(source.getBankIdCodeType()).map(Enum::name).orElse(null))
                .unlockReasonCode(source.getUnlockReasonCode())
                .lastUpdatedBy(source.getLastUpdatedBy())
                .lastUpdatedOn(DATE_MAPPER.dateToDateTime(source.getLastUpdatedOn()))
                .apiVersion(source.getApiVersion())
                .version(source.getVersion())
                .allowedUpdates(Optional.ofNullable(source.getAllowedUpdates()).orElseGet(ImmutableList::of).stream()
                        .map(Enum::name).collect(Collectors.toList()))
                .build();
    }
}

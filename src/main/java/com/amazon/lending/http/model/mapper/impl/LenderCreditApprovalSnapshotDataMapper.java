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

import com.amazon.lending.model.LenderCreditApprovalSnapshotData;
import com.amazon.lending.http.model.LenderCreditApprovalSnapshotDataDto;
import com.amazon.lending.model.CreditApprovalStatus;

public class LenderCreditApprovalSnapshotDataMapper {
    public LenderCreditApprovalSnapshotData networkToClient(LenderCreditApprovalSnapshotDataDto source) {
        if (source == null) {
            return null;
        }

        return LenderCreditApprovalSnapshotData.builder()
                .creditApprovalStatus(EnumMapper.stringToEnum(source.getCreditApprovalStatus(), CreditApprovalStatus.class))
                .build();
    }

    public LenderCreditApprovalSnapshotDataDto clientToNetwork(LenderCreditApprovalSnapshotData source) {
        if (source == null) {
            return null;
        }

        return LenderCreditApprovalSnapshotDataDto.builder()
                .creditApprovalStatus(EnumMapper.enumToString(source.getCreditApprovalStatus()))
                .build();
    }
}

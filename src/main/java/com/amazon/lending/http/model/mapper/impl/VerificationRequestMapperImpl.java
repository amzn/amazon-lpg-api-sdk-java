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

import com.amazon.lending.http.model.VerificationRequestDto;
import com.amazon.lending.http.model.mapper.VerificationRequestMapper;
import com.amazon.lending.model.VerificationRequest;

public class VerificationRequestMapperImpl implements VerificationRequestMapper {
    @Override
    public VerificationRequest networkToClient(final VerificationRequestDto source) {
        if (source == null) {
            return null;
        }

        throw new UnsupportedOperationException("Conversion from VerificationRequestDto to VerificationRequest is not supported");
    }

    @Override
    public VerificationRequestDto clientToNetwork(final VerificationRequest source) {
        if (source == null) {
            return null;
        }

        return VerificationRequestDto.builder()
                .secureHash(source.getSecureHash())
                .verificationPayload(source.getVerificationPayload())
                .verificationType(source.getVerificationType().getValue())
                .build();
    }
}

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

import com.amazon.lending.http.model.VerificationKeyDto;
import com.amazon.lending.http.model.mapper.VerificationKeyMapper;
import com.amazon.lending.model.VerificationKey;

public class VerificationKeyMapperImpl implements VerificationKeyMapper {

    @Override
    public VerificationKey networkToClient(final VerificationKeyDto source) {
        if (source == null) {
            return null;
        }

        return VerificationKey.builder()
                .secretKey(source.getEncryptedSecretKey())
                .verificationPayload(source.getVerificationPayload())
                .build();
    }

    @Override
    public VerificationKeyDto clientToNetwork(final VerificationKey source) {
        if (source == null) {
            return null;
        }

        throw new UnsupportedOperationException("Conversion from VerificationKey to VerificationKeyDto is not supported");
    }
}

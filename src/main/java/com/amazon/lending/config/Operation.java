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

package com.amazon.lending.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Operation {

    VERIFY("verify"),
    REDIRECT("redirect"),
    GENERATE_PERFORMANCE_DATA_AUTH_TOKEN("generatePerformanceDataAuthToken"),
    VERIFY_WITH_LOCK_AND_CONFIRM("verifyWithLockandConfirm"),
    REPAYMENT_CALCULATION_METADATA("repaymentCalculationMetadata");

    private final String value;
}

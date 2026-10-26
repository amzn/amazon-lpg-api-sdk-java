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

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum LPGEndpoint {

    NA_PROD("https://lending-na.amazonservices.com", "us-east-1"),
    EU_PROD("https://lending-eu.amazonservices.com", "eu-west-1"),
    AP_PROD("https://lending-fe.amazonservices.com", "ap-northeast-1"),

    NA_TEST("https://lending-na-gamma.amazonservices.com", "us-east-1"),
    EU_TEST("https://lending-eu-gamma.amazonservices.com", "eu-west-1"),
    AP_TEST("https://lending-fe-gamma.amazonservices.com", "ap-northeast-1"),

    NA_BETA("https://lending-na-beta.amazonservices.com", "us-west-2");

    private final String endpoint;
    private final String region;
}

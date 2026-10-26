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

package com.amazon.lending.http.model;

import com.google.api.client.util.DateTime;
import com.google.api.client.util.Key;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LenderPortfolioDataDto {
    @Key
    private String lenderPortfolioDataId;
    @Key
    private String marketplaceCountry;
    @Key
    private String partnerCode;
    @Key
    private String productCode;
    @Key
    private String currencyCode;
    @Key
    private DateTime snapshotDate;
    @Key
    private String dataReportingFrequency;
    @Key
    private DateTime createdOn;
    @Key
    private String createdBy;
    @Key
    private DateTime lastUpdatedOn;
    @Key
    private String lastUpdatedBy;
    @Key
    private long version;
    @Key
    private String apiVersion;
    @Key
    private LenderPortfolioSnapshotDataDto lenderPortfolioSnapshotData;
    @Key
    private LenderPortfolioAggregatedDataDto lenderPortfolioAggregatedData;
}

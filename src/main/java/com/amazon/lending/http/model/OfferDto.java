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

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferDto {
    @Key
    private String marketplaceCountry;
    @Key
    private String productCode;
    @Key
    private String partnerCode;
    @Key
    private String lastUpdatedBy;
    @Key
    private DateTime lastUpdatedOn;
    @Key
    private long version;
    @Key
    private String apiVersion;
    @Key
    private String offerId;
    @Key
    private String offerStatus;
    @Key
    private OfferDetails details;
    @Key
    private Map<String, String> offerDataset;
    @Key
    private Map<String, String> latestDataset;
    @Key
    private String offerCategory;
    @Key
    private String partnerOfferId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OfferDetails {
        @Key
        private int term;
        @Key
        private String termUnit;
        @Key
        private BigDecimal minApr;
        @Key
        private BigDecimal maxApr;
        @Key
        private BigDecimal minInterest;
        @Key
        private BigDecimal maxInterest;
        @Key
        private BigDecimal minRepaymentRate;
        @Key
        private BigDecimal maxRepaymentRate;
        @Key
        private BigDecimal minAmount;
        @Key
        private BigDecimal maxAmount;
        @Key
        private String currency;
        @Key
        private String heuristicVersion;
        @Key
        private DateTime expireOn;
        @Key
        private Boolean automatedRepaymentsRequired;
    }
}

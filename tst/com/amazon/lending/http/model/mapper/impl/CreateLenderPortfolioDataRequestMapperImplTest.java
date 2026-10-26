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

import com.amazon.lending.http.model.CreateLenderPortfolioDataRequestDto;
import com.amazon.lending.model.CreateLenderPortfolioDataRequest;
import com.amazon.lending.model.LenderPortfolioAggregatedData;
import com.amazon.lending.model.LenderPortfolioSnapshotData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CreateLenderPortfolioDataRequestMapperImplTest {
    private final CreateLenderPortfolioDataRequestMapperImpl mapper = new CreateLenderPortfolioDataRequestMapperImpl();

    @Test
    void everyFieldSurvivesTheRoundTripToTheWireModel() {
        final CreateLenderPortfolioDataRequest request = CreateLenderPortfolioDataRequest.builder()
                .idempotencyKey("idem-1")
                .marketplaceCountry("US")
                .partnerCode("PARTNER")
                .productCode("PRODUCT")
                .snapshotDate(Date.from(Instant.parse("2026-09-30T00:00:00Z")))
                .currencyCode("USD")
                .createdBy("portfolio-job")
                .lenderPortfolioSnapshotData(LenderPortfolioSnapshotData.builder()
                        .outstandingPrincipalBalance(new BigDecimal("1250.50"))
                        .createdApplicationCount(3)
                        .build())
                .lenderPortfolioAggregatedData(LenderPortfolioAggregatedData.builder()
                        .aggregationIntervalStartDate(Date.from(Instant.parse("2026-09-01T00:00:00Z")))
                        .aggregateRepaidPrincipalAmount(new BigDecimal("99.99"))
                        .build())
                .build();

        final CreateLenderPortfolioDataRequestDto dto = mapper.clientToNetwork(request);

        assertEquals("portfolio-job", dto.getCreatedBy());
        assertEquals(request, mapper.networkToClient(dto));
    }
}

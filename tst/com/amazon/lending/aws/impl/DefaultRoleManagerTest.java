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

package com.amazon.lending.aws.impl;

import com.google.api.client.http.HttpRequest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultRoleManagerTest {

    @Test
    void stsRequestCarriesTheSigV4DateOfTheDateItIsSignedWith() {
        final AtomicReference<String> amzDate = new AtomicReference<>();
        final AtomicReference<Date> signedDate = new AtomicReference<>();
        // Records what the role manager hands the signer, then stops the call before it reaches STS.
        final DefaultRoleManager roleManager = new DefaultRoleManager((request, date, service) -> {
            amzDate.set(request.getHeaders().getFirstHeaderStringValue("x-amz-date"));
            signedDate.set(date);
            throw new IOException("stop before the network");
        });

        assertThrows(IOException.class, () -> roleManager.assumeRole("arn:aws:iam::123456789012:role/LPGApiInvoke", "amazon"));

        assertEquals(AWSSigV4Signer.FULL_DATE_FORMAT.format(Instant.ofEpochMilli(signedDate.get().getTime())), amzDate.get());
    }

    @Test
    void stsRequestHasExplicitConnectAndReadTimeouts() {
        final AtomicReference<HttpRequest> stsRequest = new AtomicReference<>();
        final DefaultRoleManager roleManager = new DefaultRoleManager((request, date, service) -> {
            stsRequest.set(request);
            throw new IOException("stop before the network");
        });

        assertThrows(IOException.class, () -> roleManager.assumeRole("arn:aws:iam::123456789012:role/LPGApiInvoke", "amazon"));

        assertEquals(5_000, stsRequest.get().getConnectTimeout());
        assertEquals(10_000, stsRequest.get().getReadTimeout());
    }
}

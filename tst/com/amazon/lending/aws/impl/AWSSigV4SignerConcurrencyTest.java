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

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.javanet.NetHttpTransport;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One signer shared by many threads, each signing with its own fixed date, must give every
 * thread the same Authorization header it gets alone. The date goes into the credential scope
 * and the string to sign, so a date formatter that mixes two threads' fields produces a wrong
 * signature (or no signature at all); this test counts both.
 */
class AWSSigV4SignerConcurrencyTest {
    // The example credentials from the AWS documentation and test suite; not a real key pair.
    private static final String ACCESS_KEY = "AKIDEXAMPLE";
    private static final String EXAMPLE_SECRET = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";
    private static final String SESSION_TOKEN = "session-token-for-this-test";
    private static final int THREADS = 16;
    private static final int SIGNATURES_PER_THREAD = 2_000;
    // Spread the dates so that every field (year, month, day, hour, minute, second) differs between threads.
    private static final Instant FIRST_DATE = Instant.parse("2015-08-30T12:36:00Z");
    private static final long DATE_STEP_SECONDS = 37L * 24 * 3600 + 5 * 3600 + 7 * 60 + 11;

    private final AWSSigV4Signer signer = new AWSSigV4Signer(
            () -> ACCESS_KEY, () -> EXAMPLE_SECRET, () -> Optional.of(SESSION_TOKEN), () -> "us-east-1");

    @Test
    void sharedSignerGivesEveryThreadItsOwnSingleThreadedSignature() throws Exception {
        final List<Date> dates = new ArrayList<>();
        final List<String> expected = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            final Date date = Date.from(FIRST_DATE.plusSeconds(i * DATE_STEP_SECONDS));
            dates.add(date);
            expected.add(signer.generateSignature(request(), date, "execute-api"));
        }
        assertEquals(THREADS, expected.stream().distinct().count(), "the dates must produce distinct signatures");

        final CyclicBarrier start = new CyclicBarrier(THREADS);
        final ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        final List<Future<Integer>> mismatches = new ArrayList<>();
        try {
            for (int i = 0; i < THREADS; i++) {
                final Date date = dates.get(i);
                final String want = expected.get(i);
                mismatches.add(pool.submit(signRepeatedly(start, date, want)));
            }
            int total = 0;
            for (final Future<Integer> f : mismatches) {
                total += f.get(2, TimeUnit.MINUTES);
            }
            assertEquals(0, total, "signatures that differed from the single-threaded result, or failed outright");
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    /** Waits for every thread to be ready, then signs the same request repeatedly; returns how many results were wrong. */
    private Callable<Integer> signRepeatedly(final CyclicBarrier start, final Date date, final String want) {
        return () -> {
            final HttpRequest request = request();
            start.await();
            int wrong = 0;
            for (int n = 0; n < SIGNATURES_PER_THREAD; n++) {
                try {
                    if (!want.equals(signer.generateSignature(request, date, "execute-api"))) {
                        wrong++;
                    }
                } catch (RuntimeException e) {
                    // A torn date can make the formatter itself throw (e.g. ArrayIndexOutOfBounds); that is a wrong result too.
                    wrong++;
                }
            }
            return wrong;
        };
    }

    /**
     * A new request per thread, reused for every signature on that thread: HttpRequest is not
     * thread-safe and is not what is under test, and signing mutates it only by setting
     * x-amz-security-token to the same value each time, so repeated signing is idempotent.
     */
    private static HttpRequest request() throws IOException {
        final HttpRequest request = new NetHttpTransport().createRequestFactory()
                .buildGetRequest(new GenericUrl("https://lending-na-beta.amazonservices.com/v1.1/offers/offer-1"));
        request.getHeaders().set("host", "lending-na-beta.amazonservices.com");
        request.getHeaders().set("x-amz-date", "20150830T123600Z");
        request.getHeaders().set("x-api-key", "partner-api-key");
        return request;
    }
}

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

package com.amazon.lending.http.exception.impl;

import com.amazon.lending.http.HttpHeader;
import com.amazon.lending.http.exception.LPGClientException;
import com.amazon.lending.http.exception.LPGForbiddenException;
import com.amazon.lending.http.exception.LPGRequestException;
import com.amazon.lending.http.exception.LPGServiceException;
import com.amazon.lending.http.exception.LPGUnknownErrorException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** HTTP status to typed exception, which is what partners catch on. */
class LPGExceptionConverterTest {
    private final LPGExceptionConverter converter = new LPGExceptionConverter();

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "400, com.amazon.lending.http.exception.LPGInvalidRequestException,      400, true,  false",
            "403, com.amazon.lending.http.exception.LPGForbiddenException,           403, true,  false",
            "404, com.amazon.lending.http.exception.LPGInvalidRequestException,      400, true,  false",
            "429, com.amazon.lending.http.exception.LPGThrottledRequestException,    429, true,  false",
            "500, com.amazon.lending.http.exception.LPGNonTransientServiceException, 500, false, true",
            "502, com.amazon.lending.http.exception.LPGTransientServiceException,    502, false, true",
            "418, com.amazon.lending.http.exception.LPGUnknownErrorException,        500, false, false",
            "503, com.amazon.lending.http.exception.LPGUnknownErrorException,        500, false, false",
    })
    void mapsHttpStatusToTypedException(final int httpStatus, final Class<?> expectedType, final int reportedStatus,
                                        final boolean client, final boolean service) {
        final HttpResponseException cause = httpError(httpStatus);

        final LPGRequestException mapped = converter.convertException(cause, null);

        assertSame(expectedType, mapped.getClass());
        assertEquals(reportedStatus, mapped.getStatusCode());
        assertEquals(cause.getMessage(), mapped.getMessage());
        assertSame(cause, mapped.getCause());
        assertEquals(client, mapped instanceof LPGClientException);
        assertEquals(service, mapped instanceof LPGServiceException);
    }

    @Test
    void responseHeadersDoNotChangeTheMapping() {
        final HttpResponseException cause = httpError(403);

        final LPGRequestException withErrorHeader = converter.convertException(cause,
                Collections.singletonList(HttpHeader.builder().name("X-Amz-Error").value("AccessDenied").build()));
        final LPGRequestException withOtherHeader = converter.convertException(cause,
                Collections.singletonList(HttpHeader.builder().name("x-amzn-requestid").value("abc").build()));

        assertSame(LPGForbiddenException.class, withErrorHeader.getClass());
        assertSame(LPGForbiddenException.class, withOtherHeader.getClass());
    }

    @Test
    void nonHttpFailuresBecomeUnknownErrors() {
        final IOException cause = new IOException("connection reset");

        final LPGRequestException mapped = converter.convertException(cause, Collections.emptyList());

        assertSame(LPGUnknownErrorException.class, mapped.getClass());
        assertEquals("connection reset", mapped.getMessage());
        assertSame(cause, mapped.getCause());
    }

    @Test
    void nullExceptionIsRejected() {
        assertThrows(NullPointerException.class, () -> converter.convertException(null, null));
    }

    private static HttpResponseException httpError(final int status) {
        return new HttpResponseException.Builder(status, "status " + status, new HttpHeaders()).build();
    }
}

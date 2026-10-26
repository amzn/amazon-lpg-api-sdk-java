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
import com.amazon.lending.http.exception.HttpExceptionConverter;
import com.amazon.lending.http.exception.LPGForbiddenException;
import com.amazon.lending.http.exception.LPGInvalidRequestException;
import com.amazon.lending.http.exception.LPGNonTransientServiceException;
import com.amazon.lending.http.exception.LPGRequestException;
import com.amazon.lending.http.exception.LPGThrottledRequestException;
import com.amazon.lending.http.exception.LPGTransientServiceException;
import com.amazon.lending.http.exception.LPGUnknownErrorException;
import com.google.api.client.http.HttpResponseException;

import lombok.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class LPGExceptionConverter implements HttpExceptionConverter {

    private static final String HEADER_NAME_AMZN_ERROR = "x-amz-error";

    @Override
    public LPGRequestException convertException(@NonNull final Exception exception, final List<HttpHeader> responseHeaders) {
        if (exception instanceof HttpResponseException) {
            final HttpResponseException httpException = (HttpResponseException) exception;
            final Optional<HttpHeader> maybeErrorHeader = Optional.ofNullable(responseHeaders)
                    .orElse(Collections.emptyList())
                    .stream()
                    .filter(header -> HEADER_NAME_AMZN_ERROR.equalsIgnoreCase(header.getName()))
                    .findFirst();
            if (maybeErrorHeader.isPresent()) {

            }
            switch (httpException.getStatusCode()) {
                case 500:
                    return new LPGNonTransientServiceException(httpException.getMessage(), httpException);

                case 502:
                    return new LPGTransientServiceException(httpException.getMessage(), httpException);

                case 404:
                    return new LPGInvalidRequestException(httpException.getMessage(), httpException);

                case 429:
                    return new LPGThrottledRequestException(httpException.getMessage(), httpException);

                case 403:
                    return new LPGForbiddenException(httpException.getMessage(), httpException);

                case 400:
                    return new LPGInvalidRequestException(httpException.getMessage(), httpException);

                default:
                    return new LPGUnknownErrorException(httpException.getMessage(), httpException);
            }
        }
        return new LPGUnknownErrorException(exception.getMessage(), exception);
    }
}

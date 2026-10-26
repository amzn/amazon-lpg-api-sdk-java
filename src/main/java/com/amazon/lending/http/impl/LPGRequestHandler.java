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

package com.amazon.lending.http.impl;

import com.amazon.lending.aws.AWSSigner;
import com.amazon.lending.config.SdkVersion;
import com.amazon.lending.http.HttpMethod;
import com.amazon.lending.http.RequestHandler;
import com.amazon.lending.http.exception.HttpExceptionConverter;
import com.amazon.lending.http.exception.LPGInvalidRequestException;
import com.amazon.lending.http.exception.LPGRequestException;
import com.amazon.lending.http.model.mapper.DtoMapper;
import com.amazon.lending.http.model.mapper.MapperRegistry;
import com.amazon.lending.security.MessageHandler;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

@RequiredArgsConstructor
public class LPGRequestHandler implements RequestHandler {

    private static final String HTTP_HEADER_HOST = "host";
    private static final String HTTP_HEADER_API_KEY = "x-api-key";
    private static final String HTTP_HEADER_X_AMZ_DATE = "x-amz-date";
    private static final String HTTP_HEADER_SDK_VERSION = "x-amz-lpg-sdk-ver";

    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();

    // The SigV4 x-amz-date format, matching the signer's: immutable and thread-safe, and independent
    // of the JVM's default locale and calendar.
    private static final DateTimeFormatter HEADER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmss'Z'", Locale.ROOT).withZone(ZoneOffset.UTC);

    private final MessageHandler messageHandler;
    private final AWSSigner awsSigner;
    private final String apiKey;
    private final HttpExceptionConverter exceptionConverter;

    @Override
    public <ClientRequestType, NetworkRequestType, NetworkResponseType, ClientResponseType>
    ClientResponseType makeRequest(@NonNull final URL url, @NonNull final HttpMethod httpMethod, final ClientRequestType payload, @NonNull final Class<ClientRequestType> clientRequestType, @NonNull final Class<NetworkRequestType> networkRequestType, @NonNull final Class<NetworkResponseType> networkResponseType, @NonNull final Class<ClientResponseType> clientResponseType) throws IOException, LPGRequestException {
        final DtoMapper<NetworkRequestType, ClientRequestType> requestMapper = MapperRegistry.getMapper(networkRequestType, clientRequestType).orElseThrow(() -> new LPGInvalidRequestException(String.format("Unable to find mapper for %s <-> %s conversion", networkRequestType.getName(), clientRequestType.getName())));
        final DtoMapper<NetworkResponseType, ClientResponseType> responseMapper = MapperRegistry.getMapper(networkResponseType, clientResponseType).orElseThrow(() -> new LPGInvalidRequestException(String.format("Unable to find mapper for %s <-> %s conversion", networkResponseType.getName(), clientResponseType.getName())));;
        final HttpResponse response;
        final NetworkRequestType networkPayload = requestMapper.clientToNetwork(payload);

        try {
            response = executeRequest(url, httpMethod, networkPayload);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new LPGInvalidRequestException(e);
        } catch (HttpResponseException e) {
            throw exceptionConverter.convertException(e, null);
        }

        final NetworkResponseType networkObj = parseResponse(response, networkResponseType);
        return responseMapper.networkToClient(networkObj);
    }

    private HttpResponse executeRequest(@NonNull final URL url, @NonNull final HttpMethod httpMethod, final Object payload) throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        final HttpRequest request = createRequest(url, httpMethod, payload);
        setHeaders(request);
        return request.execute();
    }

    private <NetworkResponseType> NetworkResponseType parseResponse(@NonNull final HttpResponse httpResponse, @NonNull Class<NetworkResponseType> networkType) throws IOException {
        final String jsonString = messageHandler.decryptAndVerify(httpResponse.parseAsString());
        final JsonObjectParser jsonObjectParser = new JsonObjectParser(JSON_FACTORY);
        return jsonObjectParser.parseAndClose(new StringReader(jsonString), networkType);
    }

    private HttpRequest createRequest(@NonNull final URL url, @NonNull final HttpMethod httpMethod, final Object payload) throws IOException {
        final HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory();
        final GenericUrl genericUrl = new GenericUrl(url);
        switch (httpMethod) {
            case GET:
                return requestFactory.buildGetRequest(genericUrl);

            case POST:
                return requestFactory.buildPostRequest(genericUrl, new LPGHttpContent(messageHandler, payload, JSON_FACTORY));

            case PUT:
                return requestFactory.buildPutRequest(genericUrl, new LPGHttpContent(messageHandler, payload, JSON_FACTORY));

            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + httpMethod.toString());
        }
    }

    private void setHeaders(@NonNull final HttpRequest httpRequest) throws IOException, InvalidKeyException, NoSuchAlgorithmException {
        final HttpHeaders headers = httpRequest.getHeaders();
        final Date requestDate = new Date();
        headers.set(HTTP_HEADER_HOST, httpRequest.getUrl().getHost());
        headers.set(HTTP_HEADER_X_AMZ_DATE, HEADER_DATE_FORMAT.format(Instant.ofEpochMilli(requestDate.getTime())));
        headers.set(HTTP_HEADER_API_KEY, apiKey);
        headers.set(HTTP_HEADER_SDK_VERSION, SdkVersion.SDK_VERSION);
        headers.setAuthorization(awsSigner.generateSignature(httpRequest, requestDate, "execute-api"));
    }
}

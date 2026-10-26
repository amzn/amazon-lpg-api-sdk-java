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

package com.amazon.lending.http;

import com.amazon.lending.http.exception.LPGRequestException;

import java.io.IOException;
import java.net.URL;

public interface RequestHandler {
    /**
     * Make a request, parsing and handling the response.
     * @param url Request URL
     * @param httpMethod HTTP method for request
     * @param payload Payload to use as the request body; set to null if no body is required
     * @param clientRequestType Data type of the payload
     * @param networkRequestType Data type to convert the payload to prior to making the request
     * @param networkResponseType Data type of the response body
     * @param clientResponseType Data type to convert the response body to
     * @param <ClientRequestType> Data type of the payload
     * @param <NetworkRequestType> Data type of the request body; the payload will be converted to this data type
     * @param <NetworkResponseType> Data type of the response body
     * @param <ClientResponseType> Data type to return; the response body will be converted to this data type
     * @return Response body converted to ClientResponseType
     * @throws IOException in the event of a network error
     * @throws LPGRequestException in the event of a request error
     */
    <ClientRequestType, NetworkRequestType, NetworkResponseType, ClientResponseType>
    ClientResponseType makeRequest(URL url, HttpMethod httpMethod, ClientRequestType payload, Class<ClientRequestType> clientRequestType, Class<NetworkRequestType> networkRequestType, Class<NetworkResponseType> networkResponseType, Class<ClientResponseType> clientResponseType) throws IOException, LPGRequestException;

    /**
     * Make a request, parsing and handling the response.
     * @param url Request URL
     * @param httpMethod HTTP method for request
     * @param payload Payload to use as the request body; set to null if no body is required
     * @param clientType Data type of the payload
     * @param networkType Data type to convert the payload to prior to making the request
     * @param <ClientType> Data type of the payload
     * @param <NetworkType> Data type of the request body; the payload will be converted to this data type
     * @return Response body converted to ClientType
     * @throws IOException in the event of a network error
     * @throws LPGRequestException in the event of a request error
     */
    default <ClientType, NetworkType>
    ClientType makeRequest(URL url, HttpMethod httpMethod, ClientType payload, Class<ClientType> clientType, Class<NetworkType> networkType) throws IOException, LPGRequestException {
        return makeRequest(url, httpMethod, payload, clientType, networkType, networkType, clientType);
    }
}

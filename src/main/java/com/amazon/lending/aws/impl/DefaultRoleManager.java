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

import com.amazon.lending.aws.AWSSigner;
import com.amazon.lending.aws.Credentials;
import com.amazon.lending.aws.RoleManager;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class DefaultRoleManager implements RoleManager {

    private static final String STS_ENDPOINT = "https://sts.amazonaws.com";
    private static final int REFRESH_THRESHOLD_SECS = 60;  // Refresh credentials if they will expire within the next 60 seconds
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    // Explicit, because assumeRole holds its lock across the STS call: these bound how long a
    // slow STS can make concurrent callers wait (the library defaults are 20 s each).
    private static final int STS_CONNECT_TIMEOUT_MS = 5_000;
    private static final int STS_READ_TIMEOUT_MS = 10_000;

    private final AWSSigner awsSigner;

    private Credentials cachedCredentials;
    private Date credentialExpiryDate;

    // Synchronized so that concurrent callers with an expired cache share one refresh and read one
    // consistent pair of credentials and expiry.
    @Override
    public synchronized Credentials assumeRole(@NonNull final String roleArn, @NonNull final String sessionName) throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        if (!isCacheValid()) {
            refreshCache(roleArn, sessionName);
        }
        return cachedCredentials;
    }

    private void refreshCache(@NonNull final String roleArn, @NonNull final String sessionName) throws IOException, InvalidKeyException, NoSuchAlgorithmException {
        final HttpResponse httpResponse = buildRequest(roleArn, sessionName).execute();
        final String responseContent = httpResponse.parseAsString();

        final String accessKeyId = getContentForTag("AccessKeyId", responseContent);
        final String secretAccessKey = getContentForTag("SecretAccessKey", responseContent);
        final String sessionToken = getContentForTag("SessionToken", responseContent);
        final String expiration = getContentForTag("Expiration", responseContent);

        cachedCredentials = new Credentials(accessKeyId, secretAccessKey, sessionToken);
        credentialExpiryDate = Date.from(Instant.parse(expiration));
    }

    private HttpRequest buildRequest(@NonNull final String roleArn, @NonNull final String sessionName) throws IOException, InvalidKeyException, NoSuchAlgorithmException {
        final GenericUrl url = new GenericUrl(generateRequestUrl(roleArn, sessionName));
        final HttpRequest httpRequest = HTTP_TRANSPORT.createRequestFactory().buildGetRequest(url);
        httpRequest.setConnectTimeout(STS_CONNECT_TIMEOUT_MS).setReadTimeout(STS_READ_TIMEOUT_MS);
        final Date requestDate = new Date();
        httpRequest.getHeaders().set("x-amz-date", AWSSigV4Signer.FULL_DATE_FORMAT.format(Instant.ofEpochMilli(requestDate.getTime())));
        httpRequest.getHeaders().set("host", httpRequest.getUrl().getHost());
        httpRequest.getHeaders().setAuthorization(awsSigner.generateSignature(httpRequest, requestDate, "sts"));
        return httpRequest;
    }

    private String generateRequestUrl(@NonNull final String roleArn, @NonNull final String sessionName) {
        return STS_ENDPOINT + "?" + new StringJoiner("&")
                .add("Action=AssumeRole")
                .add("Version=2011-06-15")
                .add(String.format("RoleArn=%s", roleArn))
                .add(String.format("RoleSessionName=%s", sessionName))
                .toString();
    }

    private String getContentForTag(@NonNull final String tag, @NonNull final String xml) {
        final Pattern pattern = Pattern.compile(String.format(".*<%s>(.+)</%s>.*", tag, tag), Pattern.DOTALL);
        final Matcher matcher = pattern.matcher(xml);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return "";
    }

    private boolean isCacheValid() {
        return cachedCredentials != null
                && credentialExpiryDate != null
                && (credentialExpiryDate.toInstant().getEpochSecond() - new Date().toInstant().getEpochSecond()) > REFRESH_THRESHOLD_SECS;
    }
}

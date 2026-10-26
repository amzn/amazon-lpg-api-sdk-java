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
import com.amazon.lending.security.util.HashUtils;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.common.collect.ImmutableSet;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class AWSSigV4Signer implements AWSSigner {

    // Immutable and thread-safe, unlike SimpleDateFormat. Locale.ROOT and the ISO year (uuuu) keep
    // the output independent of the JVM's default locale and calendar. Dates are converted with
    // Instant.ofEpochMilli(getTime()), because java.sql.Date and java.sql.Time throw from toInstant().
    // Package-private so DefaultRoleManager formats x-amz-date with the same definition.
    static final DateTimeFormatter FULL_DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmss'Z'", Locale.ROOT).withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter YMD_DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuuMMdd", Locale.ROOT).withZone(ZoneOffset.UTC);

    private static final String SIGNATURE_DELIMITER = "\n";
    private static final String SIGNATURE_ALGORITHM = "AWS4-HMAC-SHA256";
    private static final Set<String> SIGNED_HEADER_LIST = ImmutableSet.of(
            "host",
            "content-type",
            "x-amz-date",
            "x-amz-security-token",
            "x-api-key"
    );
    private static final String AWS_REQUEST_IDENTIFIER = "aws4_request";
    private static final String SIGNING_KEY_PREFIX = "AWS4";

    private final Supplier<String> accessKeyIdSupplier;
    private final Supplier<String> secretKeySupplier;
    private final Supplier<Optional<String>> sessionKeySupplier;
    private final Supplier<String> awsRegionSupplier;

    @Override
    public String generateSignature(@NonNull final HttpRequest httpRequest, @NonNull final Date requestDate, @NonNull final String serviceName) throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        setSessionKey(httpRequest);
        final String canonicalRequest = generateCanonicalRequest(httpRequest);
        final String signingString = generateSigningString(requestDate, canonicalRequest, serviceName);
        final byte[] signingKey = generateSigningKey(requestDate, serviceName);
        return String.format("%s Credential=%s/%s, SignedHeaders=%s, Signature=%s",
                SIGNATURE_ALGORITHM,
                accessKeyIdSupplier.get(),
                getCredentialScope(requestDate, serviceName),
                getSignedHeaders(httpRequest.getHeaders()),
                HashUtils.hexEncode(HashUtils.hmacSha256(signingKey, signingString))
        );
    }

    private byte[] generateSigningKey(@NonNull final Date requestDate, @NonNull final String serviceName) throws InvalidKeyException, NoSuchAlgorithmException {
        final String hmacKey = SIGNING_KEY_PREFIX + secretKeySupplier.get();
        final byte[] dateHmac = HashUtils.hmacSha256(hmacKey.getBytes(StandardCharsets.UTF_8), YMD_DATE_FORMAT.format(Instant.ofEpochMilli(requestDate.getTime())));
        final byte[] regionHmac = HashUtils.hmacSha256(dateHmac, awsRegionSupplier.get());
        final byte[] serviceHmac = HashUtils.hmacSha256(regionHmac, serviceName);
        return HashUtils.hmacSha256(serviceHmac, AWS_REQUEST_IDENTIFIER);
    }

    private String generateSigningString(@NonNull final Date date, @NonNull final String canonicalRequest, @NonNull final String serviceName) {
        final StringJoiner stringJoiner = new StringJoiner(SIGNATURE_DELIMITER);
        stringJoiner.add(SIGNATURE_ALGORITHM);
        stringJoiner.add(FULL_DATE_FORMAT.format(Instant.ofEpochMilli(date.getTime())));
        stringJoiner.add(getCredentialScope(date, serviceName));
        stringJoiner.add(canonicalRequest);
        return stringJoiner.toString();
    }

    private String getCredentialScope(@NonNull final Date date, @NonNull final String serviceName) {
        final StringJoiner stringJoiner = new StringJoiner("/");
        stringJoiner.add(YMD_DATE_FORMAT.format(Instant.ofEpochMilli(date.getTime())));
        stringJoiner.add(awsRegionSupplier.get());
        stringJoiner.add(serviceName);
        stringJoiner.add(AWS_REQUEST_IDENTIFIER);
        return stringJoiner.toString();
    }

    private String generateCanonicalRequest(@NonNull final HttpRequest httpRequest) throws IOException {
        final StringJoiner stringJoiner = new StringJoiner(SIGNATURE_DELIMITER);
        stringJoiner.add(httpRequest.getRequestMethod().toUpperCase());
        stringJoiner.add(Optional.ofNullable(httpRequest.getUrl().getRawPath()).orElse("/"));
        stringJoiner.add(getCanonicalQueryString(httpRequest));
        stringJoiner.add(getCanonicalHeaders(httpRequest.getHeaders()));
        stringJoiner.add(""); // Empty line required after canonical headers
        stringJoiner.add(getSignedHeaders(httpRequest.getHeaders()));
        stringJoiner.add(getContentHash(httpRequest.getContent()));
        return HashUtils.sha256Hex(stringJoiner.toString());
    }

    private String getCanonicalQueryString(@NonNull final HttpRequest httpRequest) {
        final String urlString = httpRequest.getUrl().toString();
        if (!urlString.contains("?")) {
            return "";
        }
        final String queryString = urlString.substring(urlString.indexOf("?") + 1);
        final List<String> queryParameters = Arrays.stream(queryString.split("&"))
                .sorted()
                .map(str -> {
                    try {
                        final int index = str.indexOf("=") + 1;
                        return str.substring(0, index) + URLEncoder.encode(str.substring(index), "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        return str;
                    }
                })
                .collect(Collectors.toList());
        return String.join("&", queryParameters);
    }

    private String getCanonicalHeaders(@NonNull final HttpHeaders httpHeaders) {
        final List<String> headers = new ArrayList<>(httpHeaders.size());
        httpHeaders.forEach((name, content) -> {
            if (SIGNED_HEADER_LIST.contains(name.toLowerCase())) {
                headers.add(String.format("%s:%s", name.toLowerCase(), content.toString().trim()));
            }
        });
        headers.sort(String::compareTo);
        return String.join(SIGNATURE_DELIMITER, headers);
    }

    private String getSignedHeaders(@NonNull final HttpHeaders httpHeaders) {
        final List<String> headerNames = new ArrayList<>(httpHeaders.size());
        httpHeaders.forEach((name, content) -> headerNames.add(name.toLowerCase()));
        return String.join(";", headerNames
                .stream()
                .filter(SIGNED_HEADER_LIST::contains)
                .sorted()
                .collect(Collectors.toList())
        );
    }

    private String getContentHash(final HttpContent httpContent) throws IOException {
        byte[] contentBytes;
        if (httpContent != null) {
            try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                httpContent.writeTo(outputStream);
                contentBytes = outputStream.toByteArray();
            }
        } else {
            contentBytes = new byte[0];
        }
        return HashUtils.sha256Hex(contentBytes);
    }

    private void setSessionKey(@NonNull final HttpRequest httpRequest) {
        final Optional<String> maybeSessionKey = sessionKeySupplier.get();
        maybeSessionKey.ifPresent(sessionKey -> httpRequest.getHeaders().set("x-amz-security-token", sessionKey));
    }
}

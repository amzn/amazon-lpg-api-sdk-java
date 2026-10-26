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

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.javanet.NetHttpTransport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The exact Authorization header the SDK produces for each request shape it sends, with fixed
 * keys and a fixed date, so that any change to the signer is shown to leave every live request
 * byte-for-byte identical. The AWS test-suite vectors in {@link AWSSigV4SignerTest}
 * guard the same property for the generic shapes; these are the SDK's own: the three LPG
 * request shapes (GET, bodiless POST, PUT with a body) with the session token and api key the
 * client always sends, and the STS AssumeRole query that DefaultRoleManager builds.
 *
 * The values were captured from the SimpleDateFormat-based signer that preceded the
 * DateTimeFormatter one, and must never be updated to match new output: a difference here is a
 * difference every partner's gateway sees.
 */
class SigningGoldenValuesTest {
    // The example credentials from the AWS documentation and test suite; not a real key pair.
    private static final String ACCESS_KEY = "AKIDEXAMPLE";
    private static final String EXAMPLE_SECRET = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";
    private static final String SESSION_TOKEN = "session-token-for-this-test";
    private static final String API_KEY = "partner-api-key";
    private static final String REGION = "us-east-1";
    private static final Date REQUEST_DATE = Date.from(Instant.parse("2026-09-04T10:15:30Z"));
    private static final String AMZ_DATE = "20260904T101530Z";
    private static final String LPG_HOST = "lending-na-beta.amazonservices.com";
    private static final String LPG_SCOPE = "AKIDEXAMPLE/20260904/us-east-1/execute-api/aws4_request";
    private static final String LPG_SIGNED_HEADERS = "host;x-amz-date;x-amz-security-token;x-api-key";

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource(delimiter = '|', value = {
            "GET  | /v1.1/offers/offer-1/redirect                          |                                                                | 7df357e6d6951312874393cb999eab7de84f7d84caddd68004a9c6bf1fabbbc4",
            "POST | /v1.1/offers/offer-1/generatePerformanceDataAuthToken  | ''                                                             | 7323374c34f29435a20ca9e2d2e18a7103d3b63e611f27c700dd8a6dfb50aeed",
            "PUT  | /v1.1/accountlock/lock-42                              | eyJhbGciOiJSU0EtT0FFUC0yNTYiLCJlbmMiOiJBMjU2R0NNIn0.fixed-body | 25a678f9e68ede77e4f02e517c52b1c1a0eac9a7f5937526afb58744bd32becb",
    })
    void lpgRequestIsSignedExactlyAsBefore(final String method, final String path, final String body, final String signature) throws Exception {
        final AWSSigV4Signer signer = new AWSSigV4Signer(() -> ACCESS_KEY, () -> EXAMPLE_SECRET, () -> Optional.of(SESSION_TOKEN), () -> REGION);
        final HttpRequest request = request(method, "https://" + LPG_HOST + path, body == null ? null : envelope(body));
        request.getHeaders().set("host", LPG_HOST);
        request.getHeaders().set("x-amz-date", AMZ_DATE);
        request.getHeaders().set("x-api-key", API_KEY);
        request.getHeaders().set("x-amz-lpg-sdk-ver", "20260904");

        final String authorization = signer.generateSignature(request, REQUEST_DATE, "execute-api");

        assertEquals("AWS4-HMAC-SHA256 Credential=" + LPG_SCOPE + ", SignedHeaders=" + LPG_SIGNED_HEADERS + ", Signature=" + signature, authorization);
        // The header the client sends and the scope the signer derives come from two formatters; they must agree on the day.
        assertEquals(AMZ_DATE.substring(0, 8), authorization.replaceFirst(".*Credential=AKIDEXAMPLE/(\\d{8})/.*", "$1"));
    }

    @ParameterizedTest(name = "STS AssumeRole")
    @CsvSource({"ef59e29c714f0d5badf2b5e51f67ce22e19af37ec5967a257b9d465028b22e4b"})
    void stsAssumeRoleIsSignedExactlyAsBefore(final String signature) throws Exception {
        // The query DefaultRoleManager builds, for a plain session name, signed with the long-term key.
        final AWSSigV4Signer signer = new AWSSigV4Signer(() -> ACCESS_KEY, () -> EXAMPLE_SECRET, Optional::empty, () -> REGION);
        final HttpRequest request = request("GET",
                "https://sts.amazonaws.com?Action=AssumeRole&Version=2011-06-15&RoleArn=arn:aws:iam::123456789012:role/LPGApiInvoke&RoleSessionName=amazon", null);
        request.getHeaders().set("x-amz-date", AMZ_DATE);
        request.getHeaders().set("host", "sts.amazonaws.com");

        assertEquals(
                "AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20260904/us-east-1/sts/aws4_request, SignedHeaders=host;x-amz-date, Signature=" + signature,
                signer.generateSignature(request, REQUEST_DATE, "sts"));
    }

    @Test
    void dateSubclassesSignLikeTheirUtilDate() throws Exception {
        // AWSSigner.generateSignature takes java.util.Date, so callers may pass any subclass; the JDBC
        // ones throw UnsupportedOperationException from toInstant(), which the signer must not call.
        final AWSSigV4Signer signer = new AWSSigV4Signer(() -> ACCESS_KEY, () -> EXAMPLE_SECRET, Optional::empty, () -> REGION);
        final long millis = REQUEST_DATE.getTime();
        final String withUtilDate = signer.generateSignature(vectorRequest(), new Date(millis), "service");

        assertEquals(withUtilDate, signer.generateSignature(vectorRequest(), new java.sql.Date(millis), "service"));
        assertEquals(withUtilDate, signer.generateSignature(vectorRequest(), new java.sql.Timestamp(millis), "service"));
        assertEquals(withUtilDate, signer.generateSignature(vectorRequest(), new java.sql.Time(millis), "service"));
    }

    @Test
    @ResourceLock(Resources.LOCALE) // sets the JVM default locale, so no other test may run alongside
    void credentialScopeDateIsIsoUnderAThaiDefaultLocale() throws Exception {
        // th_TH defaults to the Buddhist calendar, where 2015 is the year 2558.
        final Locale previous = Locale.getDefault();
        Locale.setDefault(new Locale("th", "TH"));
        try {
            final AWSSigV4Signer signer = new AWSSigV4Signer(() -> ACCESS_KEY, () -> EXAMPLE_SECRET, Optional::empty, () -> REGION);
            final String authorization = signer.generateSignature(vectorRequest(), new Date(1440938160000L), "service"); // 2015-08-30T12:36:00Z

            assertEquals("20150830", authorization.replaceFirst(".*Credential=AKIDEXAMPLE/(\\d+)/.*", "$1"));
        } finally {
            Locale.setDefault(previous);
        }
    }

    private static HttpRequest vectorRequest() throws IOException {
        final HttpRequest request = request("GET", "https://example.amazonaws.com/", null);
        request.getHeaders().set("host", "example.amazonaws.com");
        request.getHeaders().set("x-amz-date", AMZ_DATE);
        return request;
    }

    /** Builds, but never executes, a request. */
    private static HttpRequest request(final String method, final String url, final HttpContent content) throws IOException {
        return new NetHttpTransport().createRequestFactory().buildRequest(method, new GenericUrl(url), content);
    }

    /** The media type LPGHttpContent declares; the body stands in for a signed and encrypted payload. */
    private static HttpContent envelope(final String body) {
        return new ByteArrayContent("text/plain; charset=UTF-8", body.getBytes(StandardCharsets.UTF_8));
    }
}

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

import com.amazon.lending.config.SdkVersion;
import com.amazon.lending.testsupport.ReferenceSigV4;
import com.amazon.lending.testsupport.TestKeys;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An in-process stand-in for the Lending Partner Gateway. It holds "Amazon's" keys, knows the
 * partner's public keys and API credentials, and on every request does what the real gateway
 * does before any business logic runs: checks the API key and SDK version, recomputes the SigV4
 * signature from the bytes it received, decrypts the JWE, verifies the JWS, and reads the payload.
 * It then answers with a payload signed by Amazon and encrypted to the partner.
 *
 * Every check failure is recorded in {@link #failures()} and answered the way API Gateway and the
 * service would: 403 for a bad API key or signature, 400 for an envelope that does not open. The
 * JSON body names the failure, so a test that expected success fails with the gateway's reason.
 */
final class FakeLendingPartnerGateway implements AutoCloseable {
    static final String AMAZON_SIGNING_KEY_ID = "amazon-signing";
    static final String AMAZON_ENCRYPTION_KEY_ID = "amazon-encryption";
    static final String PARTNER_SIGNING_KEY_ID = "partner-signing";
    static final String PARTNER_ENCRYPTION_KEY_ID = "partner-encryption";

    // The example credentials from the AWS documentation and test suite; not a real key pair.
    static final String ACCESS_KEY = "AKIDEXAMPLE";
    static final String EXAMPLE_SECRET = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";
    static final String SESSION_TOKEN = "session-token-for-this-test";
    static final String REGION = "us-east-1";
    static final String API_KEY = "partner-api-key";

    private static final String JSON = "application/json; charset=UTF-8";
    private static final String ENVELOPE = "text/plain; charset=UTF-8";

    /** Exactly what LPGRequestHandler signs; the gateway refuses anything less, whatever SignedHeaders claims. */
    private static final String REQUIRED_SIGNED_HEADERS = "host;x-amz-date;x-amz-security-token;x-api-key";
    private static final long MAX_CLOCK_SKEW_MILLIS = 5 * 60 * 1000;
    private static final SimpleDateFormat AMZ_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.ROOT);

    static {
        AMZ_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static final Pattern AUTHORIZATION = Pattern.compile(
            "AWS4-HMAC-SHA256 Credential=([^/]+)/(\\d{8})/([^/]+)/([^/]+)/aws4_request, SignedHeaders=([^,]+), Signature=([0-9a-f]{64})");

    // Generated once per JVM: key generation dominates the suite's run time, and the keys are
    // never written anywhere, so sharing them across tests costs nothing.
    static final KeyPair AMAZON_SIGNING = TestKeys.generateRsaKeyPair();
    static final KeyPair AMAZON_ENCRYPTION = TestKeys.generateRsaKeyPair();
    static final KeyPair PARTNER_SIGNING = TestKeys.generateRsaKeyPair();
    static final KeyPair PARTNER_ENCRYPTION = TestKeys.generateRsaKeyPair();

    private final ReferenceSigV4 referenceSigner = new ReferenceSigV4(EXAMPLE_SECRET, REGION, "execute-api");
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final HttpServer server;
    private final List<String> failures = Collections.synchronizedList(new ArrayList<>());
    private final List<ReceivedRequest> received = Collections.synchronizedList(new ArrayList<>());

    private volatile String expectedApiKey = API_KEY;
    private volatile int responseStatus = 200;
    private volatile String responseJson = "{}";
    private volatile KeyPair responseSigningKey = AMAZON_SIGNING;
    private volatile KeyPair responseEncryptionKey = PARTNER_ENCRYPTION;

    FakeLendingPartnerGateway() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", this::handle);
        server.setExecutor(executor);
        server.start();
    }

    URL url(final String path) throws MalformedURLException {
        return new URL("http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort() + path);
    }

    /** The next response: {@code status} with {@code json} as the signed and encrypted payload. */
    void respondWith(final int status, final String json) {
        responseStatus = status;
        responseJson = json;
    }

    void signResponsesWith(final KeyPair keyPair) {
        responseSigningKey = keyPair;
    }

    void encryptResponsesTo(final KeyPair keyPair) {
        responseEncryptionKey = keyPair;
    }

    void expectApiKey(final String apiKey) {
        expectedApiKey = apiKey;
    }

    List<String> failures() {
        return new ArrayList<>(failures);
    }

    List<ReceivedRequest> received() {
        return new ArrayList<>(received);
    }

    ReceivedRequest onlyRequest() {
        assertEquals(1, received.size(), "requests received by the gateway");
        return received.get(0);
    }

    @Override
    public void close() {
        // Every test has read its response before @AfterEach, so there is nothing in flight to wait for.
        server.stop(0);
        executor.shutdownNow();
    }

    private void handle(final HttpExchange exchange) throws IOException {
        final byte[] body = readAll(exchange.getRequestBody());
        final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        exchange.getRequestHeaders().forEach((name, values) -> headers.put(name, String.join(",", values)));
        final ReceivedRequest request = new ReceivedRequest(exchange.getRequestMethod(), exchange.getRequestURI().getRawPath(), headers);
        received.add(request);

        try {
            checkApiKeyAndVersion(headers);
            checkSignature(exchange, headers, request, body);
        } catch (Exception e) {
            // API Gateway answers a bad key or signature with 403.
            failures.add(e.getMessage());
            respond(exchange, 403, JSON, errorJson(e.toString()));
            return;
        }
        try {
            if (body.length > 0) {
                request.decryptedPayload = decryptAndVerify(new String(body, StandardCharsets.UTF_8), request);
            }
        } catch (Exception e) {
            // Nimbus and the JCA throw with a message; a bare NullPointerException would not, so record toString().
            failures.add(e.toString());
            respond(exchange, 400, JSON, errorJson(e.toString()));
            return;
        }

        if (responseStatus / 100 != 2) {
            respond(exchange, responseStatus, JSON, errorJson("simulated " + responseStatus));
            return;
        }
        try {
            respond(exchange, responseStatus, ENVELOPE, signAndEncrypt(responseJson));
        } catch (Exception e) {
            failures.add("could not build response: " + e);
            respond(exchange, 500, JSON, errorJson(e.toString()));
        }
    }

    private void checkApiKeyAndVersion(final Map<String, String> headers) {
        if (!expectedApiKey.equals(headers.get("x-api-key"))) {
            throw new IllegalStateException("x-api-key was " + headers.get("x-api-key"));
        }
        if (!SdkVersion.SDK_VERSION.equals(headers.get("x-amz-lpg-sdk-ver"))) {
            throw new IllegalStateException("x-amz-lpg-sdk-ver was " + headers.get("x-amz-lpg-sdk-ver"));
        }
    }

    private void checkSignature(final HttpExchange exchange, final Map<String, String> headers, final ReceivedRequest request, final byte[] body) {
        final String authorization = headers.get("Authorization");
        final Matcher matcher = AUTHORIZATION.matcher(String.valueOf(authorization));
        if (!matcher.matches()) {
            throw new IllegalStateException("Authorization header not in SigV4 form: " + authorization);
        }
        final String amzDate = headers.get("x-amz-date");
        if (!ACCESS_KEY.equals(matcher.group(1)) || amzDate == null || !amzDate.startsWith(matcher.group(2))
                || !REGION.equals(matcher.group(3)) || !"execute-api".equals(matcher.group(4))) {
            throw new IllegalStateException("credential scope mismatch: " + authorization + " with x-amz-date " + amzDate);
        }
        if (!SESSION_TOKEN.equals(headers.get("x-amz-security-token"))) {
            throw new IllegalStateException("x-amz-security-token was " + headers.get("x-amz-security-token"));
        }
        if (!REQUIRED_SIGNED_HEADERS.equals(matcher.group(5))) {
            throw new IllegalStateException("SignedHeaders was " + matcher.group(5) + ", need " + REQUIRED_SIGNED_HEADERS);
        }
        checkRequestTime(amzDate);

        final Map<String, String> signedHeaders = new LinkedHashMap<>();
        for (final String name : matcher.group(5).split(";")) {
            final String value = headers.get(name);
            if (value == null) {
                throw new IllegalStateException("signed header " + name + " was not sent");
            }
            // HttpURLConnection replaces the Host header with host:port, while the SDK signs the
            // bare host name. On the gateway's default port the two are identical; here they are
            // not, so compare against what the SDK could have signed and record that we had to.
            if ("host".equals(name) && value.matches(".*:\\d+$")) {
                request.hostPortStrippedToVerify = true;
                signedHeaders.put(name, value.replaceFirst(":\\d+$", ""));
            } else {
                signedHeaders.put(name, value);
            }
        }
        if (exchange.getRequestURI().getRawQuery() != null) {
            throw new IllegalStateException("this gateway does not canonicalise query strings; the SDK never sends one");
        }
        final String expected = referenceSigner.signature(exchange.getRequestMethod(), exchange.getRequestURI().getRawPath(), "", signedHeaders, body);
        if (!expected.equals(matcher.group(6))) {
            throw new IllegalStateException("signature mismatch: expected " + expected + " in " + authorization);
        }
    }

    private static void checkRequestTime(final String amzDate) {
        final long requestTime;
        try {
            synchronized (AMZ_DATE_FORMAT) {
                requestTime = AMZ_DATE_FORMAT.parse(amzDate).getTime();
            }
        } catch (ParseException e) {
            throw new IllegalStateException("x-amz-date is not yyyyMMdd'T'HHmmss'Z': " + amzDate, e);
        }
        if (Math.abs(System.currentTimeMillis() - requestTime) > MAX_CLOCK_SKEW_MILLIS) {
            throw new IllegalStateException("x-amz-date " + amzDate + " is more than five minutes from now");
        }
    }

    private String decryptAndVerify(final String envelope, final ReceivedRequest request) throws Exception {
        final JWEObject jwe = JWEObject.parse(envelope);
        request.jweKeyId = jwe.getHeader().getKeyID();
        request.jweAlgorithms = jwe.getHeader().getAlgorithm().getName() + "/" + jwe.getHeader().getEncryptionMethod().getName();
        if (!JWEAlgorithm.RSA_OAEP_256.equals(jwe.getHeader().getAlgorithm())
                || !EncryptionMethod.A256GCM.equals(jwe.getHeader().getEncryptionMethod())
                || !"JWT".equals(jwe.getHeader().getContentType())) {
            throw new IllegalStateException("JWE header was " + jwe.getHeader().toString());
        }
        if (!AMAZON_ENCRYPTION_KEY_ID.equals(request.jweKeyId)) {
            throw new IllegalStateException("JWE kid was " + request.jweKeyId);
        }
        jwe.decrypt(new RSADecrypter(TestKeys.privateKey(AMAZON_ENCRYPTION)));
        final SignedJWT jws = jwe.getPayload().toSignedJWT();
        request.jwsKeyId = jws.getHeader().getKeyID();
        request.jwsAlgorithm = jws.getHeader().getAlgorithm().getName();
        if (!JWSAlgorithm.PS256.equals(jws.getHeader().getAlgorithm())) {
            throw new IllegalStateException("JWS alg was " + request.jwsAlgorithm);
        }
        if (!PARTNER_SIGNING_KEY_ID.equals(request.jwsKeyId)) {
            throw new IllegalStateException("JWS kid was " + request.jwsKeyId);
        }
        if (!jws.verify(new RSASSAVerifier(TestKeys.publicKey(PARTNER_SIGNING)))) {
            throw new IllegalStateException("JWS signature did not verify against the partner's signing key");
        }
        request.jwsType = jws.getHeader().getType() == null ? null : jws.getHeader().getType().getType();
        request.jwsClaims = jws.getJWTClaimsSet().getClaims();
        return Optional.ofNullable(jws.getJWTClaimsSet().getStringClaim("payload"))
                .orElseThrow(() -> new IllegalStateException("no payload claim"));
    }

    private String signAndEncrypt(final String json) throws Exception {
        final SignedJWT jws = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.PS256).keyID(AMAZON_SIGNING_KEY_ID).build(),
                new JWTClaimsSet.Builder().claim("payload", json).build());
        jws.sign(new RSASSASigner(TestKeys.privateKey(responseSigningKey)));
        final JWEObject jwe = new JWEObject(
                new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM)
                        .contentType("JWT").keyID(PARTNER_ENCRYPTION_KEY_ID).build(),
                new Payload(jws));
        jwe.encrypt(new RSAEncrypter(TestKeys.publicKey(responseEncryptionKey)));
        return jwe.serialize();
    }

    private static String errorJson(final String message) {
        return "{\"message\":\"" + String.valueOf(message).replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
    }

    private static void respond(final HttpExchange exchange, final int status, final String contentType, final String body) throws IOException {
        final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static byte[] readAll(final InputStream in) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buffer = new byte[4096];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    /**
     * What one request looked like from the gateway's side. The mutable fields are written on the
     * server thread and read on the test thread after the response has arrived; volatile makes
     * that hand-off safe without a lock.
     */
    static final class ReceivedRequest {
        final String method;
        final String path;
        final Map<String, String> headers;
        volatile boolean hostPortStrippedToVerify;
        volatile String decryptedPayload;
        volatile String jweKeyId;
        volatile String jweAlgorithms;
        volatile String jwsKeyId;
        volatile String jwsAlgorithm;
        volatile String jwsType;
        volatile Map<String, Object> jwsClaims;

        ReceivedRequest(final String method, final String path, final Map<String, String> headers) {
            this.method = method;
            this.path = path;
            this.headers = headers;
        }
    }
}

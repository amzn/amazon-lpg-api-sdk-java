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

package com.amazon.lending.client.impl;

import com.amazon.lending.aws.Credentials;
import com.amazon.lending.aws.RoleManager;
import com.amazon.lending.aws.impl.AWSSigV4Signer;
import com.amazon.lending.config.AWSConfig;
import com.amazon.lending.config.ApiVersion;
import com.amazon.lending.config.LPGConfig;
import com.amazon.lending.config.LPGEndpoint;
import com.amazon.lending.config.SecurityConfig;
import com.amazon.lending.http.impl.LPGRequestHandler;
import com.amazon.lending.security.SecurityKeyProvider;
import com.amazon.lending.testsupport.TestKeys;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.javanet.NetHttpTransport;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.KeyPair;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssumedRoleSignerTest {
    private static final String ROLE_ARN = "arn:aws:iam::123456789012:role/LPGApiInvoke";
    private static final Date REQUEST_DATE = new Date(1440938160000L); // 2015-08-30T12:36:00Z

    /** Returns v1 credentials on the first call and v2 on every later one, as a refresh would. */
    private final AtomicInteger calls = new AtomicInteger();
    private final RoleManager rotatingRoleManager = (roleArn, sessionName) -> {
        final String v = calls.incrementAndGet() == 1 ? "v1" : "v2";
        return new Credentials("AKID-" + v, "SECRET-" + v, "TOKEN-" + v);
    };
    private final AssumedRoleSigner signer = new AssumedRoleSigner(rotatingRoleManager, ROLE_ARN, "amazon", () -> "us-east-1");

    @Test
    void oneSignatureUsesOneCredentialsSnapshot() throws Exception {
        final HttpRequest request = request();

        final String authorization = signer.generateSignature(request, REQUEST_DATE, "execute-api");

        assertEquals(1, calls.get());
        assertEquals("TOKEN-v1", request.getHeaders().getFirstHeaderStringValue("x-amz-security-token"));
        assertEquals(signedWith("v1"), authorization);
    }

    @Test
    void theNextSignatureUsesTheRefreshedSnapshotThroughout() throws Exception {
        signer.generateSignature(request(), REQUEST_DATE, "execute-api");
        final HttpRequest request = request();

        final String authorization = signer.generateSignature(request, REQUEST_DATE, "execute-api");

        assertEquals(2, calls.get());
        assertEquals("TOKEN-v2", request.getHeaders().getFirstHeaderStringValue("x-amz-security-token"));
        assertEquals(signedWith("v2"), authorization);
    }

    @Test
    void anStsFailureSurfacesAsTheDeclaredIOException() {
        final AssumedRoleSigner failing = new AssumedRoleSigner((roleArn, sessionName) -> {
            throw new IOException("STS unavailable");
        }, ROLE_ARN, "amazon", () -> "us-east-1");

        assertThrows(IOException.class, () -> failing.generateSignature(request(), REQUEST_DATE, "execute-api"));
    }

    @Test
    void initializeSignsLpgRequestsWithOneSnapshotPerSignature() throws Exception {
        final DefaultLendingPartnerGatewayClient client = new DefaultLendingPartnerGatewayClient();
        client.initialize(securityConfig(), awsConfig(), lpgConfig()); // builds the signers; STS is only called on a request

        final Object requestHandler = field(DefaultLendingPartnerGatewayClient.class, "requestHandler", client);
        final AssumedRoleSigner lpgSigner = assertInstanceOf(AssumedRoleSigner.class, field(LPGRequestHandler.class, "awsSigner", requestHandler));
        assertEquals(ROLE_ARN, field(AssumedRoleSigner.class, "roleArn", lpgSigner));
        assertEquals("amazon", field(AssumedRoleSigner.class, "sessionName", lpgSigner));
        assertEquals(LPGEndpoint.NA_BETA.getRegion(), ((Supplier<?>) field(AssumedRoleSigner.class, "regionSupplier", lpgSigner)).get());
    }

    /** The Authorization header an AWSSigV4Signer built from one credentials version produces for the same request. */
    private static String signedWith(final String v) throws Exception {
        return new AWSSigV4Signer(() -> "AKID-" + v, () -> "SECRET-" + v, () -> Optional.of("TOKEN-" + v), () -> "us-east-1")
                .generateSignature(request(), REQUEST_DATE, "execute-api");
    }

    private static Object field(final Class<?> owner, final String name, final Object target) throws Exception {
        final Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static SecurityConfig securityConfig() {
        final KeyPair amazon = TestKeys.generateRsaKeyPair();
        final KeyPair partner = TestKeys.generateRsaKeyPair();
        final SecurityKeyProvider amazonPublic = TestKeys.publicKeyProvider(amazon, "amazon");
        final SecurityKeyProvider partnerPrivate = TestKeys.privateKeyProvider(partner, "partner");
        return new SecurityConfig() {
            @Override public SecurityKeyProvider getAmazonSigningPublicKeyProvider() { return amazonPublic; }
            @Override public String getDefaultAmazonSigningKeyId() { return "amazon"; }
            @Override public SecurityKeyProvider getAmazonEncryptionPublicKeyProvider() { return amazonPublic; }
            @Override public String getDefaultAmazonEncryptionKeyId() { return "amazon"; }
            @Override public SecurityKeyProvider getSigningPrivateKeyProvider() { return partnerPrivate; }
            @Override public String getDefaultSigningKeyId() { return "partner"; }
            @Override public SecurityKeyProvider getEncryptionPrivateKeyProvider() { return partnerPrivate; }
            @Override public String getDefaultEncryptionKeyId() { return "partner"; }
        };
    }

    private static AWSConfig awsConfig() {
        return new AWSConfig() {
            @Override public String getApiKey() { return "partner-api-key"; }
            @Override public String getAwsAccessKey() { return "AKIDEXAMPLE"; }
            @Override public String getAwsSecretKey() { return "not-a-real-secret"; }
            @Override public String getRoleArn() { return ROLE_ARN; }
            @Override public String getSessionName() { return "amazon"; }
        };
    }

    private static LPGConfig lpgConfig() {
        return new LPGConfig() {
            @Override public LPGEndpoint getEndpoint() { return LPGEndpoint.NA_BETA; }
            @Override public ApiVersion getApiVersion() { return ApiVersion.V1_1; }
        };
    }

    private static HttpRequest request() throws IOException {
        final HttpRequest request = new NetHttpTransport().createRequestFactory()
                .buildGetRequest(new GenericUrl("https://lending-na-beta.amazonservices.com/v1.1/offers/offer-1"));
        request.getHeaders().set("host", "lending-na-beta.amazonservices.com");
        request.getHeaders().set("x-amz-date", "20150830T123600Z");
        request.getHeaders().set("x-api-key", "partner-api-key");
        return request;
    }
}

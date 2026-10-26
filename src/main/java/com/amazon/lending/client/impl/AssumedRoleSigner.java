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

import com.amazon.lending.aws.AWSSigner;
import com.amazon.lending.aws.Credentials;
import com.amazon.lending.aws.RoleManager;
import com.amazon.lending.aws.impl.AWSSigV4Signer;
import com.google.api.client.http.HttpRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Signs with the role's temporary credentials, taking one credentials snapshot per signature so
 * that the access key, secret key and session token always come from the same STS response, even
 * when the role manager refreshes them between or during requests.
 */
@RequiredArgsConstructor
final class AssumedRoleSigner implements AWSSigner {
    private final RoleManager roleManager;
    private final String roleArn;
    private final String sessionName;
    private final Supplier<String> regionSupplier;

    @Override
    public String generateSignature(@NonNull final HttpRequest httpRequest, @NonNull final Date requestDate, @NonNull final String serviceName) throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        final Credentials credentials = roleManager.assumeRole(roleArn, sessionName);
        return new AWSSigV4Signer(credentials::getAccessKey, credentials::getSecretKey,
                () -> Optional.ofNullable(credentials.getSessionKey()), regionSupplier)
                .generateSignature(httpRequest, requestDate, serviceName);
    }
}

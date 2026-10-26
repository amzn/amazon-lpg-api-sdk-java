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

package com.amazon.lending.security.impl;

import com.amazon.lending.security.SecurityKey;
import com.amazon.lending.security.SecurityKeyProvider;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps multiple SecurityKeyProviders and tries each in order for kid-based lookup.
 * Used during key rotation so decrypt/verify can find the correct key across active + standby.
 */
public class CompositeSecurityKeyProvider implements SecurityKeyProvider {

    private final List<SecurityKeyProvider> providers;

    public CompositeSecurityKeyProvider(@NonNull List<SecurityKeyProvider> providers) {
        this.providers = new ArrayList<>(providers);
    }

    @Override
    public <T extends SecurityKey> T getKeyById(String keyId, @NonNull Class<T> type) throws Exception {
        List<Exception> failures = new ArrayList<>();
        for (SecurityKeyProvider provider : providers) {
            if (provider == null) {
                continue;
            }
            try {
                T key = provider.getKeyById(keyId, type);
                if (key != null) {
                    return key;
                }
            } catch (Exception e) {
                failures.add(e);
            }
        }
        Exception notFound = new Exception("Key not found for keyId: " + keyId);
        failures.forEach(notFound::addSuppressed);
        throw notFound;
    }
}

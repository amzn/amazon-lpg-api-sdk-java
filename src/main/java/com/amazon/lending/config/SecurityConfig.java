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

package com.amazon.lending.config;

import com.amazon.lending.security.SecurityKeyProvider;

/**
 * Security configuration for the LPG SDK.
 *
 * Partners must provide separate key pairs for signing and encryption operations.
 * Initially, both signing and encryption keys may point to the same key pair
 * as per the existing setup. Standby key providers are optional and used during
 * key rotation to avoid downtime.
 */
public interface SecurityConfig {

    // =========================================================================
    // Amazon public keys — provided by Amazon to the partner
    // =========================================================================

    /** Amazon's signing public key provider. Used to verify Amazon's signatures on responses. */
    SecurityKeyProvider getAmazonSigningPublicKeyProvider();

    /** Key ID for the Amazon signing public key. */
    String getDefaultAmazonSigningKeyId();

    /** Amazon's encryption public key provider. Used to encrypt outbound requests to Amazon. */
    SecurityKeyProvider getAmazonEncryptionPublicKeyProvider();

    /** Key ID for the Amazon encryption public key. */
    String getDefaultAmazonEncryptionKeyId();

    // =========================================================================
    // Private keys — owned by the partner
    // =========================================================================

    /** Signing private key provider. Used to sign outbound requests. */
    SecurityKeyProvider getSigningPrivateKeyProvider();

    /** Key ID for the signing private key. */
    String getDefaultSigningKeyId();

    /** Encryption private key provider. Used to decrypt responses from Amazon. */
    SecurityKeyProvider getEncryptionPrivateKeyProvider();

    /** Key ID for the encryption private key. */
    String getDefaultEncryptionKeyId();

    // =========================================================================
    // Standby keys — optional, for zero-downtime key rotation
    // During rotation, Amazon may sign/encrypt with either active or standby keys.
    // The SDK uses kid from JWS/JWE headers to find the correct key.
    // =========================================================================

    /** Standby Amazon signing public key provider. */
    default SecurityKeyProvider getAmazonStandbySigningPublicKeyProvider() {
        return null;
    }

    /** Key ID for the standby Amazon signing public key. */
    default String getDefaultAmazonStandbySigningKeyId() {
        return null;
    }

    /** Standby Amazon encryption public key provider. */
    default SecurityKeyProvider getAmazonStandbyEncryptionPublicKeyProvider() {
        return null;
    }

    /** Key ID for the standby Amazon encryption public key. */
    default String getDefaultAmazonStandbyEncryptionKeyId() {
        return null;
    }

    /** Standby encryption private key provider. */
    default SecurityKeyProvider getStandbyEncryptionPrivateKeyProvider() {
        return null;
    }

    /** Key ID for the standby encryption private key. */
    default String getDefaultStandbyEncryptionKeyId() {
        return null;
    }

    // =========================================================================
    // Verification keys — only needed if using bank account verification API
    // =========================================================================

    /**
     * Get the verification public key provider (Amazon-owned).
     * Used to verify Amazon's signature on the encrypted secret key and
     * to encrypt the secureHash sent back to Amazon.
     */
    default SecurityKeyProvider getVerificationPublicKeyProvider() {
        return null;
    }

    /** Key ID for the verification public key. */
    default String getVerificationPublicKeyId() {
        return null;
    }
}

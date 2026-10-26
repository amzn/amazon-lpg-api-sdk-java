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

package com.amazon.lending.security;

import com.amazon.lending.security.exception.LPGSignatureException;
import com.nimbusds.jwt.SignedJWT;

public interface MessageSignerAndVerifier {
    /**
     * Sign the payload with new PSA signature.
     * @param payload String to sign
     * @return Signed payload
     * @throws LPGSignatureException in the event the payload cannot be signed
     */
    SignedJWT signWithPsa(String payload) throws LPGSignatureException;

    /**
     * Sign the payload with legacy RSA signature.
     * @param payload String to sign
     * @return Signed payload
     * @throws LPGSignatureException in the event the payload cannot be signed
     */
    SignedJWT sign(String payload) throws LPGSignatureException;

    /**
     * Verify the signature on a payload
     * @param signedJWT Payload to verify
     * @throws LPGSignatureException in the event the signature is invalid or cannot be verified
     */
    void verify(SignedJWT signedJWT) throws LPGSignatureException;
}

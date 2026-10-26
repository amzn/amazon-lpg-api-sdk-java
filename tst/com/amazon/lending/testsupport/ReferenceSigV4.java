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

package com.amazon.lending.testsupport;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * An independent, spec-following AWS Signature Version 4 verifier's view of signing: one
 * instance per credential scope (secret, region, service), fed the already-canonical pieces of a
 * request as they appear on the wire, so the fake gateway can recompute what a real gateway would.
 *
 * Header values are trimmed and runs of spaces collapsed, and each path segment is URI-encoded
 * once more on top of what is on the wire (the "encode twice" rule for every service but S3), as
 * the spec requires. Dot segments are not removed: nothing in this SDK sends them. The query
 * string is used exactly as given; callers pass it already canonical (percent-encoded,
 * parameters sorted by name), which is how a gateway sees it on the wire.
 *
 * Spec: https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_sigv-create-signed-request.html
 */
public final class ReferenceSigV4 {
    private static final String AMZ_DATE_HEADER = "x-amz-date";

    private final String secretKey;
    private final String region;
    private final String service;

    public ReferenceSigV4(final String secretKey, final String region, final String service) {
        this.secretKey = secretKey;
        this.region = region;
        this.service = service;
    }

    /**
     * @param wirePath      the request path as it appears on the wire, before canonicalisation
     * @param signedHeaders header name to value for exactly the headers listed in {@code SignedHeaders};
     *                      must include {@code x-amz-date}, which also dates the credential scope
     * @param payload       request body bytes; empty for GET
     * @return the lowercase hex signature
     */
    public String signature(final String method, final String wirePath, final String canonicalQuery,
                            final Map<String, String> signedHeaders, final byte[] payload) {
        final TreeMap<String, String> sorted = new TreeMap<>();
        signedHeaders.forEach((name, value) -> {
            // Two spellings of one header would need their values joined in wire order (RFC 9110);
            // nothing here sends that, so refuse rather than silently keep one of them.
            if (sorted.put(name.toLowerCase(Locale.ROOT), value.trim().replaceAll(" +", " ")) != null) {
                throw new IllegalArgumentException("header given twice with different case: " + name);
            }
        });
        final String amzDate = sorted.get(AMZ_DATE_HEADER);
        if (amzDate == null) {
            throw new IllegalArgumentException(AMZ_DATE_HEADER + " must be among the signed headers");
        }
        final StringBuilder canonicalHeaders = new StringBuilder();
        sorted.forEach((name, value) -> canonicalHeaders.append(name).append(':').append(value).append('\n'));
        final String signedHeaderNames = String.join(";", sorted.keySet());

        final String canonicalRequest = method + '\n'
                + canonicalPath(wirePath) + '\n'
                + canonicalQuery + '\n'
                + canonicalHeaders + '\n'
                + signedHeaderNames + '\n'
                + HexStrings.toHex(sha256(payload));

        final String date = amzDate.substring(0, 8);
        final String scope = date + '/' + region + '/' + service + "/aws4_request";
        final String stringToSign = "AWS4-HMAC-SHA256\n" + amzDate + '\n' + scope + '\n'
                + HexStrings.toHex(sha256(canonicalRequest.getBytes(StandardCharsets.UTF_8)));

        byte[] key = hmac(("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8), date);
        key = hmac(key, region);
        key = hmac(key, service);
        key = hmac(key, "aws4_request");
        return HexStrings.toHex(hmac(key, stringToSign));
    }

    /** URI-encodes each segment of the wire path, so an already percent-encoded byte becomes %25XX. */
    public static String canonicalPath(final String wirePath) {
        if (wirePath == null || wirePath.isEmpty()) {
            return "/";
        }
        final StringBuilder out = new StringBuilder();
        for (final byte b : wirePath.getBytes(StandardCharsets.UTF_8)) {
            final int c = b & 0xFF;
            final boolean unreserved = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.' || c == '~';
            if (unreserved || c == '/') {
                out.append((char) c);
            } else {
                out.append('%').append(Character.toUpperCase(Character.forDigit(c >> 4, 16)))
                        .append(Character.toUpperCase(Character.forDigit(c & 0xF, 16)));
            }
        }
        return out.toString();
    }

    private static byte[] hmac(final byte[] key, final String data) {
        try {
            final Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] sha256(final byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}

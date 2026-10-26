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

package com.amazon.lending.security.util;

import com.amazon.lending.testsupport.HexStrings;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Known-answer tests for the primitives SigV4 is built from. */
class HashUtilsTest {
    // RFC 4231, test case 2.
    private static final byte[] HMAC_KEY = "Jefe".getBytes(StandardCharsets.UTF_8);
    private static final String HMAC_DATA = "what do ya want for nothing?";
    private static final String HMAC_SHA256_HEX = "5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843";

    // FIPS 180-4 example: SHA-256("abc").
    private static final String SHA256_ABC_HEX = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

    @Test
    void hmacSha256MatchesRfc4231() throws Exception {
        assertEquals(HMAC_SHA256_HEX, HashUtils.hexEncode(HashUtils.hmacSha256(HMAC_KEY, HMAC_DATA)));
        assertEquals(
                Base64.getEncoder().encodeToString(HexStrings.fromHex(HMAC_SHA256_HEX)),
                HashUtils.hmacSha256Base64(HMAC_KEY, HMAC_DATA));
    }

    @Test
    void sha256HexMatchesFips180AndIsLowercase() {
        assertEquals(SHA256_ABC_HEX, HashUtils.sha256Hex("abc"));
        assertEquals(SHA256_ABC_HEX, HashUtils.sha256Hex("abc".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void hexEncodeIsLowercaseAndRoundTrips() {
        assertEquals("48656c6c6f", HashUtils.hexEncode("Hello"));
        assertArrayEquals(new byte[] {0, (byte) 0xff, 0x10}, HexStrings.fromHex(HashUtils.hexEncode(new byte[] {0, (byte) 0xff, 0x10})));
    }

    @Test
    void nullArgumentsAreRejected() {
        assertThrows(NullPointerException.class, () -> HashUtils.hmacSha256(null, HMAC_DATA));
        assertThrows(NullPointerException.class, () -> HashUtils.hmacSha256(HMAC_KEY, null));
        assertThrows(NullPointerException.class, () -> HashUtils.hmacSha256Base64(null, HMAC_DATA));
        assertThrows(NullPointerException.class, () -> HashUtils.sha256Hex((String) null));
        assertThrows(NullPointerException.class, () -> HashUtils.sha256Hex((byte[]) null));
        assertThrows(NullPointerException.class, () -> HashUtils.hexEncode((String) null));
        assertThrows(NullPointerException.class, () -> HashUtils.hexEncode((byte[]) null));
    }
}

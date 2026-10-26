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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The fake gateway's verifier is only as good as its own agreement with the AWS test suite
 * (https://github.com/awslabs/aws-c-auth/tree/main/tests/aws-signing-test-suite/v4), so it is
 * pinned to ten of its vectors: the vanilla GET and POST, the three query-string cases
 * (key-case ordering, encoded-name ordering, unreserved characters), the three path cases
 * (UTF-8, space, unreserved), header-value trimming and the session token. The query cases
 * feed the already-canonical query string, since that is what callers pass; path encoding and
 * header handling are what this class does itself and are checked on their own.
 */
class ReferenceSigV4Test {
    private final ReferenceSigV4 reference = new ReferenceSigV4("wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY", "us-east-1", "service");

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "get-vanilla                     | GET  | /  |                                              | 5fa00fa31553b73ebf1942676e86291e8372ff2a2260956d9b8aae1d763fbf31",
            "post-vanilla                    | POST | /  |                                              | 5da7c1a2acd57cee7505fc6676e4e544621c30862966e37dddb68e92efbe5d6b",
            "get-vanilla-query-order-key-case| GET  | /  | Param1=value1&Param2=value2                  | b97d918cfa904a5beff61c982a1b6f458b799221646efd99d3219ec94cdf2500",
            "get-vanilla-query-order-encoded | GET  | /  | %E1%88%B4=Value1&Param=Value2&Param-3=Value3 | 371d3713e185cc334048618a97f809c9ffe339c62934c032af5a0e595648fcac",
            "get-vanilla-query-unreserved    | GET  | /  | -._~0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz=-._~0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz | 9c3e54bfcdf0b19771a7f523ee5669cdf59bc7cc0884027167c21bb143a40197",
            "get-utf8                        | GET  | /\u1234      |                                      | 8318018e0b0f223aa2bbf98705b62bb787dc9c0e678f255a891fd03141be5d85",
            "get-space-normalized            | GET  | /example space/ |                                 | 652487583200325589f1fba4c7e578f72c47cb61beeca81406b39ddec1366741",
            "get-unreserved                  | GET  | /-._~0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz | | 07ef7494c76fa4850883e2b006601f940f8a34d404d0cfa977f52a65bbf5f24f",
    })
    void matchesTheAwsTestSuiteVector(final String vector, final String method, final String path, final String query, final String expected) {
        final Map<String, String> headers = new HashMap<>();
        headers.put("Host", "example.amazonaws.com");
        headers.put("X-Amz-Date", "20150830T123600Z");

        final String signature = reference.signature(method, path, query == null ? "" : query, headers, new byte[0]);

        assertEquals(expected, signature, vector);
    }

    @Test
    void pathSegmentsAreEncodedOnTopOfTheWireForm() {
        assertEquals("/", ReferenceSigV4.canonicalPath(null));
        assertEquals("/", ReferenceSigV4.canonicalPath(""));
        assertEquals("/-._~/Az09", ReferenceSigV4.canonicalPath("/-._~/Az09"));
        assertEquals("/example%20space/", ReferenceSigV4.canonicalPath("/example space/"));
        assertEquals("/%E1%88%B4", ReferenceSigV4.canonicalPath("/\u1234"));
        assertEquals("/example%2520space/", ReferenceSigV4.canonicalPath("/example%20space/"));
        assertEquals("/a%252Fb", ReferenceSigV4.canonicalPath("/a%2Fb"));
    }

    @Test
    void collapsesRunsOfSpacesInsideHeaderValues() {
        // Vector get-header-value-trim: My-Header2 is sent as "a   b   c" and canonicalised as "a b c".
        final Map<String, String> headers = new HashMap<>();
        headers.put("Host", "example.amazonaws.com");
        headers.put("X-Amz-Date", "20150830T123600Z");
        headers.put("My-Header1", " value1");
        headers.put("My-Header2", " \"a   b   c\"");

        assertEquals("acc3ed3afb60bb290fc8d2dd0098b9911fcaa05412b367055dee359757a9c736",
                reference.signature("GET", "/", "", headers, new byte[0]));
    }

    @Test
    void refusesTheSameHeaderInTwoSpellings() {
        final Map<String, String> headers = new HashMap<>();
        headers.put("host", "example.amazonaws.com");
        headers.put("Host", "other.example");
        headers.put("x-amz-date", "20150830T123600Z");

        assertThrows(IllegalArgumentException.class, () -> reference.signature("GET", "/", "", headers, new byte[0]));
    }

    @Test
    void signsTheSessionTokenHeader() {
        // Vector get-vanilla-with-session-token.
        final Map<String, String> headers = new HashMap<>();
        headers.put("host", "example.amazonaws.com");
        headers.put("x-amz-date", "20150830T123600Z");
        headers.put("x-amz-security-token", "6e86291e8372ff2a2260956d9b8aae1d763fbf315fa00fa31553b73ebf194267");

        assertEquals("07ec1639c89043aa0e3e2de82b96708f198cceab042d4a97044c66dd9f74e7f8",
                reference.signature("GET", "/", "", headers, new byte[0]));
    }
}

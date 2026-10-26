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

package com.amazon.lending.http.exception;

public class LPGThrottledRequestException extends LPGClientException {
    @Override
    public int getStatusCode() {
        return 429;
    }

    public LPGThrottledRequestException() {
        super();
    }

    public LPGThrottledRequestException(String s) {
        super(s);
    }

    public LPGThrottledRequestException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public LPGThrottledRequestException(Throwable throwable) {
        super(throwable);
    }

    protected LPGThrottledRequestException(String s, Throwable throwable, boolean b, boolean b1) {
        super(s, throwable, b, b1);
    }
}

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

package com.amazon.lending.model;

public enum ApplicationAbandonCode implements ApplicationStatusCode {
    NOT_REACHABLE,
    DOCUMENTS_NOT_PROVIDED,
    UNFINISHED_APPLICATION,
    OTHER_APPLICATION_CONFIRMED,
    OTHER_APPLICATION_REJECTED,
    SELLER_WITHDREW_APPLICATION,
    SELLER_FAILED_TO_RESUBMIT,
    SELLER_FAILED_TO_ACCEPT,
    APPLICATION_INCOMPATIBLE,
    CREDIT_APPROVAL_CLOSED_BEFORE_REFINANCE_APPLICATION_NOT_CONFIRMED,
    OTHERS
}

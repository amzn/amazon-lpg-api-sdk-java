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

package com.amazon.lending.http.impl;

import com.amazon.lending.security.MessageHandler;
import com.google.api.client.http.AbstractHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonGenerator;
import lombok.NonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

public class LPGHttpContent extends AbstractHttpContent {

    private static final String MEDIA_TYPE = "text/plain; charset=UTF-8";

    private final MessageHandler messageHandler;
    private final Object data;
    private final JsonFactory jsonFactory;

    private byte[] signedEncryptedPayload;

    public LPGHttpContent(@NonNull final MessageHandler messageHandler, final Object data, @NonNull final JsonFactory jsonFactory) {
        super(MEDIA_TYPE);
        this.messageHandler = messageHandler;
        this.data = data;
        this.jsonFactory = jsonFactory;
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        if (data != null) {
            // Signing + encryption is non-deterministic, so cache the result the first time it's performed.
            // Otherwise, the AWS signature validation will fail due to the payload resolving to a different hash value.
            if (signedEncryptedPayload == null) {
                final StringWriter writer = new StringWriter();
                final JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(writer);
                jsonGenerator.serialize(data);
                jsonGenerator.flush();
                final String serializedData = writer.getBuffer().toString();
                final String payload = messageHandler.signAndEncrypt(serializedData);
                signedEncryptedPayload = payload.getBytes(StandardCharsets.UTF_8);
            }
            out.write(signedEncryptedPayload);
        }
        out.flush();
    }
}

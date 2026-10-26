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

package com.amazon.lending.http.model.mapper.impl;

import com.amazon.lending.http.exception.LPGUnknownErrorException;
import com.amazon.lending.http.model.RedirectUrlDto;
import com.amazon.lending.http.model.mapper.RedirectUrlMapper;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

public class RedirectUrlMapperImpl implements RedirectUrlMapper {
    @Override
    public URL networkToClient(final RedirectUrlDto source) {
        return Optional.ofNullable(source)
                .map(src -> {
                    try {
                        return new URL(src.getRedirectUrl());
                    } catch (MalformedURLException e) {
                        throw new LPGUnknownErrorException(e);
                    }
                })
                .orElse(null);
    }

    @Override
    public RedirectUrlDto clientToNetwork(final URL source) {
        return Optional.ofNullable(source)
                .map(url -> RedirectUrlDto.builder().redirectUrl(url.toString()).build())
                .orElse(null);
    }
}

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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The SDK version is stamped in three places: {@link SdkVersion#SDK_VERSION}, which goes on the
 * wire as {@code x-amz-lpg-sdk-ver}; {@code pom.xml}'s {@code lpg.sdk.version}, which goes into
 * the published jar's manifest; and {@code build.gradle}'s {@code sdkVersion}, which names the
 * partner zip. A release that bumps one without the others ships a jar that misreports itself,
 * so each build file is checked against the constant.
 */
class SdkVersionTest {
    private static final Pattern POM_VERSION = Pattern.compile("<lpg\\.sdk\\.version>\\s*([^<\\s]+)\\s*</lpg\\.sdk\\.version>");
    private static final Pattern GRADLE_VERSION = Pattern.compile("^\\s*def\\s+sdkVersion\\s*=\\s*'([^']+)'", Pattern.MULTILINE);
    // Surefire and Gradle both fork with the project directory as the working directory, so the
    // walk normally stops at once; a few levels cover a build run from a module or build subdirectory.
    private static final int MAX_PARENTS_TO_SEARCH = 3;

    @Test
    void sdkVersionIsAnEightDigitDate() {
        assertTrue(SdkVersion.SDK_VERSION.matches("^20\\d{6}$"), SdkVersion.SDK_VERSION);
    }

    @Test
    void pomVersionMatchesTheConstant() throws IOException {
        assertEquals(SdkVersion.SDK_VERSION, versionIn(projectRoot().resolve("pom.xml"), POM_VERSION));
    }

    @Test
    void gradleVersionMatchesTheConstantWhenTheInternalBuildFileIsPresent() throws IOException {
        final Path buildGradle = projectRoot().resolve("build.gradle");
        // build.gradle is internal-only and is not part of the exported GitHub tree.
        assumeTrue(Files.exists(buildGradle), "build.gradle is not in this tree; nothing to compare");

        assertEquals(SdkVersion.SDK_VERSION, versionIn(buildGradle, GRADLE_VERSION));
    }

    private static String versionIn(final Path file, final Pattern pattern) throws IOException {
        final String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        final Matcher matcher = pattern.matcher(content);
        assertTrue(matcher.find(), "no version stamp matching " + pattern + " in " + file);
        final String version = matcher.group(1);
        // A second match would mean a stale copy (for example a commented-out tag) that could be the
        // one a reader edits; insist on exactly one.
        assertFalse(matcher.find(), "more than one version stamp matching " + pattern + " in " + file);
        return version;
    }

    private static Path projectRoot() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i <= MAX_PARENTS_TO_SEARCH && dir != null; i++, dir = dir.getParent()) {
            if (Files.isRegularFile(dir.resolve("pom.xml")) && Files.isDirectory(dir.resolve("tst"))) {
                return dir;
            }
        }
        throw new IllegalStateException("Could not find the project root (pom.xml next to tst/) above " + Paths.get("").toAbsolutePath());
    }
}

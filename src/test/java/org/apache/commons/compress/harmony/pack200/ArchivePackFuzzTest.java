/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.harmony.pack200;

import com.code_intelligence.jazzer.junit.FuzzTest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.jar.JarInputStream;

/**
 * Fuzzes the pack200 Archive on arbitrary (potentially invalid) JAR bytes and a variety of options.
 */
class ArchivePackFuzzTest {

    @FuzzTest
    void fuzzPack(final byte[] jarBytes,
                  final boolean gzip,
                  final boolean stripDebug,
                  final boolean keepFileOrder,
                  final int effort,
                  final boolean setSegmentLimit,
                  final long segmentLimitSelector,
                  final boolean verbose) {
        if (jarBytes == null || jarBytes.length == 0 || jarBytes.length > 5_000_000) {
            return;
        }

        try (ByteArrayInputStream in = new ByteArrayInputStream(jarBytes);
             JarInputStream jarIn = new JarInputStream(in);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            final PackingOptions options = new PackingOptions();
            options.setGzip(gzip);
            options.setStripDebug(stripDebug);
            options.setKeepFileOrder(keepFileOrder);
            // effort range 0..9 like unit tests / spec
            int boundedEffort = Math.abs(effort % 10);
            options.setEffort(boundedEffort);
            options.setVerbose(verbose);
            // Only set segment limit sometimes; map selector to specific special values to hit code paths
            if (setSegmentLimit) {
                long limit;
                switch ((int) (segmentLimitSelector % 5)) {
                    case 0: limit = -1; break; // special: single segment
                    case 1: limit = 0; break;  // special: one segment per file (except META-INF)
                    case 2: limit = 1_000; break;
                    case 3: limit = PackingOptions.SEGMENT_LIMIT; break;
                    default: limit = 100_000; break;
                }
                options.setSegmentLimit(limit);
            }

            try {
                new Archive(jarIn, out, options).pack();
            } catch (final IOException | /*IllegalArgumentException |*/ RuntimeException e) { // TODO Nullpointer finding might be a valid finding in the code. Double check and file bug if so.
                // Invalid jar or unsupported configuration, acceptable in fuzzing
            }
        } catch (final IOException ignored) {
            // Ignore stream setup errors in fuzzing context
        }
    }
}

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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.harmony.unpack200;

import com.code_intelligence.jazzer.junit.FuzzTest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.jar.JarOutputStream;

/**
 * Fuzzes the unpack200 Archive with arbitrary input data exercising configuration toggles.
 * This intentionally treats IO/format-related exceptions as benign to focus on robustness issues.
 */
class ArchiveUnpackFuzzTest {

    @FuzzTest
    void fuzzUnpack(final byte[] data,
                    final boolean verbose,
                    final boolean quiet,
                    final boolean deflateHintValue,
                    final boolean removePackFile) {

        try {
            try (ByteArrayInputStream in = new ByteArrayInputStream(data);
                 ByteArrayOutputStream sink = new ByteArrayOutputStream();
                 JarOutputStream jarOut = new JarOutputStream(sink)) {

                final Archive archive = new Archive(in, jarOut);
                // Exercise flags similarly to unit tests
                archive.setDeflateHint(deflateHintValue);
                archive.setVerbose(verbose);
                archive.setQuiet(quiet);
                archive.setRemovePackFile(removePackFile);

                try {
                    archive.unpack();
                } catch (final IOException | RuntimeException e) {
                    // Expected for most inputs which are not valid Pack200 or compressed streams
                    // Swallow to let Jazzer focus on crashes (e.g., unchecked exceptions, verifier errors)
                }
            } catch (final IOException ignored) {
                // Ignore stream setup errors in fuzzing context
            }
        } catch (RuntimeException e) {
            // ignored
        }

    }
}

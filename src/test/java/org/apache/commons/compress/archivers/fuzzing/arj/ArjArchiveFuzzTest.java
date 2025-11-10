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

package org.apache.commons.compress.archivers.fuzzing.arj;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.List;

import com.code_intelligence.jazzer.junit.FuzzTest;
import com.code_intelligence.jazzer.mutation.annotation.NotNull;
import com.code_intelligence.jazzer.mutation.utils.PropertyConstraint;
import org.apache.commons.compress.archivers.arj.ArjArchiveEntry;
import org.apache.commons.compress.archivers.arj.ArjArchiveInputStream;

/**
 * Records providing structured fuzzing input using Jazzer's mutator framework
 */
record ArjMainHeaderValues(String fileName,
                           String comment,
                           String charsetName) {}

record ArjLocalHeaderValues(String fileName,
                            String comment,
                            byte[] content,
                            String charsetName) {}

public class ArjArchiveFuzzTest {

    @FuzzTest
    public void arjInFuzzTest(
            @NotNull(constraint = PropertyConstraint.RECURSIVE) ArjMainHeaderValues mainHeaderValues,
            @NotNull(constraint = PropertyConstraint.RECURSIVE) List<ArjLocalHeaderValues> localHeaders) {
        // Build a synthetic ARJ archive in-memory consisting of one main header and N local headers.
        // We currently rely on the helper header writers in this package; local headers always declare
        // zero sizes (so no file data is consumed) – this still exercises header parsing logic heavily.

        ArjMainHeader mainHeader = new ArjMainHeader(getCharsetOrDefault(mainHeaderValues.charsetName()),
                safeString(mainHeaderValues.fileName()),
                safeString(mainHeaderValues.comment()));

        // Pre-compute required capacity (headerLength + 5 for CRC+ext length already accounted internally? writeTo needs headerLength+5) + terminator + locals
        int capacity = mainHeader.getHeaderLength(); // extra slack
        for (ArjLocalHeaderValues v : localHeaders) {
            ArjLocalHeader local = new ArjLocalHeader(getCharsetOrDefault(v.charsetName()), safeString(v.fileName()), safeString(v.comment()));
            capacity += local.getHeaderLength() +  v.content().length;
        }
        capacity += 8; // space for end-of-archive marker

        ByteBuffer buffer = ByteBuffer.allocate(capacity);
        try {
            // Write main header
            mainHeader.writeTo(buffer);
            // Write local headers sequentially
            for (ArjLocalHeaderValues v : localHeaders) {
                try {
                    ArjLocalHeader lh = new ArjLocalHeader(getCharsetOrDefault(v.charsetName()), safeString(v.fileName()), safeString(v.comment()));
                    lh.writeTo(buffer);
                    buffer.put(v.content());
                } catch (RuntimeException | IOException e) {
                    // ignore malformed header construction so fuzzing continues
                }
            }
            // End-of-archive marker: magic bytes + zero basic header length indicating termination
            if (buffer.remaining() >= 4) {
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.put((byte) 0x60);
                buffer.put((byte) 0xEA);
                buffer.putShort((short) 0); // basic header size = 0 terminator
            }
        } catch (IOException | RuntimeException ignored) {
            // ignore and still attempt parsing whatever has been written so far
        }

        int size = buffer.position();
        try (ArjArchiveInputStream in = new ArjArchiveInputStream(new ByteArrayInputStream(buffer.array(), 0, size))) {
            for (ArjArchiveEntry e = in.getNextEntry(); e != null; e = in.getNextEntry()) {
                // Access various getters to exercise logic
                e.getName();
                e.getMode();
                e.getLastModifiedDate();
                e.isDirectory();
                if (in.canReadEntryData(e) && e.getSize() > 0) {
                    long remaining = e.getSize();
                    byte[] tmp = new byte[256];
                    while (remaining > 0) {
                        int toRead = (int) Math.min(tmp.length, remaining);
                        int r = in.read(tmp, 0, toRead);
                        if (r < 0) {
                            break;
                        }
                        remaining -= r;
                    }
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // Parsing errors expected under fuzzing
        }
    }

    private static String safeString(String s) {
        if (s == null) {
            return "";
        }
        int idx = s.indexOf('\0');
        return idx >= 0 ? s.substring(0, idx) : s;
    }

    private Charset getCharsetOrDefault(String charsetName) {

        try {
            return Charset.forName(charsetName);
        } catch (RuntimeException | Error e) {
            return Charset.defaultCharset();
        }
    }
}

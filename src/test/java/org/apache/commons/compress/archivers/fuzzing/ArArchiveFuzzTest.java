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

package org.apache.commons.compress.archivers.fuzzing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import com.code_intelligence.jazzer.junit.FuzzTest;
import com.code_intelligence.jazzer.mutation.annotation.InRange;
import com.code_intelligence.jazzer.mutation.annotation.NotNull;
import com.code_intelligence.jazzer.mutation.annotation.WithLength;
import com.code_intelligence.jazzer.mutation.annotation.WithUtf8Length;
import com.code_intelligence.jazzer.mutation.utils.PropertyConstraint;
import org.apache.commons.compress.archivers.ar.*;
import org.apache.commons.lang3.tuple.MutablePair;


public class ArArchiveFuzzTest extends AbstractWritable {
    public void writeArchiveHeader(final ByteBuffer buffer, byte[] archiveHeader) {
        if (buffer.remaining() == buffer.capacity()) {
            writeBytes(buffer, archiveHeader, 8);
        } else {
            throw  new IllegalArgumentException("Invalid archiveHeader size");
        }
    }


    @FuzzTest
    public void arOutAndInFuzzTest(
            final @NotNull(constraint = PropertyConstraint.RECURSIVE) @WithUtf8Length(min = 1, max = 64) String fileName,
            final @WithLength(min = 0, max = 8192) byte[] data) throws IOException {
        final File file = new File("target/ArOutAndInTest.ar");
        Files.deleteIfExists(file.toPath());

        // Write an ar archive with two entries using fuzzed data
        try (ArArchiveOutputStream arOut = new ArArchiveOutputStream(new FileOutputStream(file))) {
            arOut.setLongFileMode(ArArchiveOutputStream.LONGFILE_BSD);
            // entry 1
            arOut.putArchiveEntry(new ArArchiveEntry(fileName, data.length));
            arOut.write(data);
            arOut.closeArchiveEntry();
            // entry 2
            arOut.putArchiveEntry(new ArArchiveEntry("a", data.length));
            arOut.write(data);
            arOut.closeArchiveEntry();
        }
        // Read back and verify round-trip
        try (ArArchiveInputStream arIn = ArArchiveInputStream.builder().setFile(file).get()) {
            final ArArchiveEntry first = arIn.getNextEntry();
            assertNotNull(first);
            assertEquals(fileName, first.getName());
            assertEquals(data.length, (int) first.getLength());
            byte[] firstContent = readNBytes(arIn, (int) first.getLength());
            assertEquals(data.length, firstContent.length);
            for (int i = 0; i < data.length; i++) {
                if (data[i] != firstContent[i]) {
                    throw new AssertionError("First entry content mismatch at index " + i);
                }
            }

            final ArArchiveEntry second = arIn.getNextEntry();
            assertNotNull(second);
            assertEquals("a", second.getName());
            assertEquals(data.length, (int) second.getLength());
            byte[] secondContent = readNBytes(arIn, (int) second.getLength());
            assertEquals(data.length, secondContent.length);
            for (int i = 0; i < data.length; i++) {
                if (data[i] != secondContent[i]) {
                    throw new AssertionError("Second entry content mismatch at index " + i);
                }
            }
        } finally {
            Files.deleteIfExists(file.toPath());
        }
    }

    private static byte[] readNBytes(InputStream in, int len) throws IOException {
        byte[] out = new byte[len];
        int off = 0;
        while (off < len) {
            int r = in.read(out, off, len - off);
            if (r < 0) break;
            off += r;
        }
        if (off == len) return out;
        byte[] truncated = new byte[off];
        System.arraycopy(out, 0, truncated, 0, off);
        return truncated;
    }

    @FuzzTest
    public void test(MutablePair<
            Boolean,
            @WithLength(min = 0, max = 8) byte[]> test) {}

    @FuzzTest
    public void arInTest(@NotNull(constraint = PropertyConstraint.RECURSIVE) List<MutablePair<
                                     ArHeader,
                                     byte[]>> arEntriesList,
                         @NotNull(constraint = PropertyConstraint.RECURSIVE) MutablePair<
                                 Boolean,
                                 @WithLength(min = 0, max = 8) byte[]> archiveStarter) {
        ByteBuffer buffer = ByteBuffer.allocate(getNecessaryByteArraySize(arEntriesList));
        try {
            if(!archiveStarter.getLeft()) {
                this.writeArchiveHeader(buffer, ArArchiveEntry.HEADER.getBytes(StandardCharsets.US_ASCII));
            } else {
                this.writeArchiveHeader(buffer, archiveStarter.getRight());
            }


            for (MutablePair<ArHeader, byte[]> arEntry : arEntriesList ) {
                try {
                    arEntry.getLeft().writeTo(buffer);
                    byte[] payload = arEntry.getRight();
                    int payloadLen = payload == null ? 0 : payload.length;
                    this.writeBytes(buffer, payload == null ? new byte[0] : payload, payloadLen);
                } catch (RuntimeException e) {
                    // ignored
                }
            }
        } catch (RuntimeException e) {
            // ignored
        }
        try (ArArchiveInputStream arIn = new ArArchiveInputStream(new ByteArrayInputStream(buffer.array()))) {
            while ( arIn.getNextEntry() != null) {}
        } catch (IOException | RuntimeException e) {
            // ignored
        }
    }

    private int getNecessaryByteArraySize(List<MutablePair<ArHeader, byte[]>> arEntriesList) {
        int size = 8;
        for (MutablePair<ArHeader, byte[]> arEntry : arEntriesList) {
            int rightLen = arEntry.getRight() == null ? 0 : arEntry.getRight().length;
            size += 60 + rightLen;
        }

        return size;
    }

    @Override
    public int getRecordSize() {
        return 0;
    }

    @Override
    public void writeTo(ByteBuffer buffer) {

    }
}

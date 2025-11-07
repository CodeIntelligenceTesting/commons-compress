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

package org.apache.commons.compress.archivers.fuzzing.ar;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.code_intelligence.jazzer.junit.FuzzTest;
import com.code_intelligence.jazzer.mutation.annotation.NotNull;
import com.code_intelligence.jazzer.mutation.annotation.WithLength;
import com.code_intelligence.jazzer.mutation.annotation.WithUtf8Length;
import com.code_intelligence.jazzer.mutation.utils.PropertyConstraint;
import org.apache.commons.compress.archivers.ar.*;
import org.apache.commons.compress.archivers.fuzzing.AbstractWritable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

record ArchiveValues(@WithUtf8Length(min = 1, max = 64) String fileName,
                            @WithLength(min = 0, max = 8192) byte[] data,
                            long length,
                            int userId,
                            int groupId,
                            int mode,
                            long lastModified,
                            boolean useRealDataLength,
                            int alternativeDataLength) {}
record ArEntriesRecord(ArHeader header, byte[] content) {}
record ArchiveStarter(Boolean ifRealHeader, @WithLength(min = 0, max = 8) byte[] alternativeArchiveHeader) {}

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
            final @NotNull(constraint = PropertyConstraint.RECURSIVE) List<ArchiveValues> archiveValuesList,
            final int longFileMode) {

        System.out.println("Starting arOutAndInFuzzTest.");

        // Write an ar archive with two entries using fuzzed data
        try ( ByteArrayOutputStream baos = new ByteArrayOutputStream() ;
              ArArchiveOutputStream arOut = new ArArchiveOutputStream(baos)) {
            arOut.setLongFileMode(longFileMode);
            for (ArchiveValues archiveValues : archiveValuesList) {
                try {
                    long dataLength = archiveValues.useRealDataLength() ? archiveValues.data().length : archiveValues.alternativeDataLength();
                    ArArchiveEntry entry = new ArArchiveEntry(archiveValues.fileName(),
                            dataLength,
                            archiveValues.userId(),
                            archiveValues.groupId(),
                            archiveValues.mode(),
                            archiveValues.lastModified()
                    );

                    arOut.putArchiveEntry(entry);
                    arOut.write(archiveValues.data());
                    arOut.closeArchiveEntry();
                } catch (RuntimeException e) {
                    // ignored
                }
            }

            try (ArArchiveInputStream arIn = new ArArchiveInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
                try {
                    while ( arIn.getNextEntry() != null) {}
                } catch (RuntimeException e) {
                    //ignored
                }
            }
        } catch (IOException | RuntimeException e) {
            // ignored
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
    public void arInTest(@NotNull(constraint = PropertyConstraint.RECURSIVE) List<ArEntriesRecord> arEntriesList,
                         @NotNull(constraint = PropertyConstraint.RECURSIVE) ArchiveStarter archiveStarter) {
        ByteBuffer buffer = ByteBuffer.allocate(getNecessaryByteArraySize(arEntriesList));
        try {
            if(!archiveStarter.ifRealHeader()) {
                this.writeArchiveHeader(buffer, ArArchiveEntry.HEADER.getBytes(StandardCharsets.US_ASCII));
            } else {
                this.writeArchiveHeader(buffer, archiveStarter.alternativeArchiveHeader());
            }


            for (ArEntriesRecord arEntry : arEntriesList ) {
                try {
                    arEntry.header().writeTo(buffer);
                    byte[] payload = arEntry.content();
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

    private int getNecessaryByteArraySize(List<ArEntriesRecord> arEntriesList) {
        int size = 8;
        for (ArEntriesRecord arEntry : arEntriesList) {
            int rightLen = arEntry.header() == null ? 0 : arEntry.content().length;
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

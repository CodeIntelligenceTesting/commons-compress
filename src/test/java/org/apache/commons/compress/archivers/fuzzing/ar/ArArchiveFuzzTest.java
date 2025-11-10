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

record ArchiveValues(@WithUtf8Length(min = 1, max = 64) String fileName,
                            byte /*@WithLength(min = 0, max = 8192)*/[] data,
                            long length,
                            int userId,
                            int groupId,
                            int mode,
                            long lastModified,
                            boolean useRealDataLength,
                            int alternativeDataLength) {}
record ArEntriesRecord(ArHeader header, byte[] content) {}
record ArchiveStarter(Boolean ifHeader, Boolean ifRealHeader, byte /*@WithLength(min = 0, max = 8)*/[] alternativeArchiveHeader) {}

public class ArArchiveFuzzTest extends AbstractWritable {

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
                    int i = 0;

                    for ( ArArchiveEntry entry = arIn.getNextEntry(); entry != null; entry = arIn.getNextEntry() ) {
                        i++;
                        // touch all getters to see if something breaks
                        checkEntryData(arIn, entry);
                    }
                    //System.out.println("Number of entries in arOutAndInFuzzTest: " + i);
                } catch (RuntimeException e) {
                    //ignored
                }
            }
        } catch (IOException | RuntimeException e) {
            // ignored
        }
    }

    @FuzzTest
    public void arInFuzzTest(@NotNull(constraint = PropertyConstraint.RECURSIVE) List<ArEntriesRecord> arEntriesList,
                         @NotNull(constraint = PropertyConstraint.RECURSIVE) ArchiveStarter archiveStarter) {
        ByteBuffer buffer = ByteBuffer.allocate(getNecessaryByteArraySize(arEntriesList)*2); // Not sure how much space we need, so allocate double the expected size
        try {
            if (archiveStarter.ifHeader()) {
                if(archiveStarter.ifRealHeader()) {
                    this.writeBytes(buffer, ArArchiveEntry.HEADER.getBytes(StandardCharsets.US_ASCII), 8);
                } else {
                    this.writeBytes(buffer, archiveStarter.alternativeArchiveHeader(), 8);
                }
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
            int i = 0;
            for ( ArArchiveEntry entry = arIn.getNextEntry(); entry != null; entry = arIn.getNextEntry() ) {
                i++;
                // touch all getters to see if something breaks
                checkEntryData(arIn, entry);
            }
            System.out.println("Number of entries in arInFuzzTest: " + i);
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

    private void checkEntryData(ArArchiveInputStream arIn, ArArchiveEntry entry) throws IOException {
        entry.getName();
        entry.getUserId();
        entry.getGroupId();
        entry.getMode();
        entry.getLastModifiedDate();

        long bytesRead = 0;
        while (bytesRead < entry.getLength()) {
            if (entry.getLength() - bytesRead > Integer.MAX_VALUE) {
                arIn.readNBytes(Integer.MAX_VALUE);
                bytesRead += Integer.MAX_VALUE;
            } else {
                long toRead = entry.getLength() - bytesRead;
                arIn.readNBytes((int) toRead);
                bytesRead += toRead;
            }
        }
    }


    @Override
    public int getRecordSize() {
        return 0;
    }

    @Override
    public void writeTo(ByteBuffer buffer) {}
}

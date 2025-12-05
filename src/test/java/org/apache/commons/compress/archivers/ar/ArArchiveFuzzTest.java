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

package org.apache.commons.compress.archivers.ar;

import com.code_intelligence.jazzer.junit.FuzzTest;
import com.code_intelligence.jazzer.mutation.annotation.NotNull;
import com.code_intelligence.jazzer.mutation.annotation.WithUtf8Length;
import com.code_intelligence.jazzer.mutation.utils.PropertyConstraint;
import org.apache.commons.compress.AbstractWritable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

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
                    while (arIn.getNextEntry() != null) {
                        // ignored
                    }
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
            while (arIn.getNextEntry() != null) {
                // ignored
            }
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
    public void writeTo(ByteBuffer buffer) {}

    final class ArchiveValues {
        @WithUtf8Length(min = 1, max = 64)
        private final String fileName;
        private final byte /*@WithLength(min = 0, max = 8192)*/[] data;
        private final long length;
        private final int userId;
        private final int groupId;
        private final int mode;
        private final long lastModified;
        private final boolean useRealDataLength;
        private final int alternativeDataLength;

        ArchiveValues(@WithUtf8Length(min = 1, max = 64) String fileName,
                      byte /*@WithLength(min = 0, max = 8192)*/[] data,
                      long length,
                      int userId,
                      int groupId,
                      int mode,
                      long lastModified,
                      boolean useRealDataLength,
                      int alternativeDataLength) {
            this.fileName = fileName;
            this.data = data;
            this.length = length;
            this.userId = userId;
            this.groupId = groupId;
            this.mode = mode;
            this.lastModified = lastModified;
            this.useRealDataLength = useRealDataLength;
            this.alternativeDataLength = alternativeDataLength;
        }

        @WithUtf8Length(min = 1, max = 64)
        public String fileName() {
            return fileName;
        }

        public byte /*@WithLength(min = 0, max = 8192)*/[] data() {
            return data;
        }

        public long length() {
            return length;
        }

        public int userId() {
            return userId;
        }

        public int groupId() {
            return groupId;
        }

        public int mode() {
            return mode;
        }

        public long lastModified() {
            return lastModified;
        }

        public boolean useRealDataLength() {
            return useRealDataLength;
        }

        public int alternativeDataLength() {
            return alternativeDataLength;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            ArchiveValues that = (ArchiveValues) obj;
            return Objects.equals(this.fileName, that.fileName) &&
                    Objects.equals(this.data, that.data) &&
                    this.length == that.length &&
                    this.userId == that.userId &&
                    this.groupId == that.groupId &&
                    this.mode == that.mode &&
                    this.lastModified == that.lastModified &&
                    this.useRealDataLength == that.useRealDataLength &&
                    this.alternativeDataLength == that.alternativeDataLength;
        }

        @Override
        public int hashCode() {
            return Objects.hash(fileName, data, length, userId, groupId, mode, lastModified, useRealDataLength, alternativeDataLength);
        }

        @Override
        public String toString() {
            return "ArchiveValues[" +
                    "fileName=" + fileName + ", " +
                    "data=" + data + ", " +
                    "length=" + length + ", " +
                    "userId=" + userId + ", " +
                    "groupId=" + groupId + ", " +
                    "mode=" + mode + ", " +
                    "lastModified=" + lastModified + ", " +
                    "useRealDataLength=" + useRealDataLength + ", " +
                    "alternativeDataLength=" + alternativeDataLength + ']';
        }
    }

    final class ArEntriesRecord {
        private final ArHeader header;
        private final byte[] content;

        ArEntriesRecord(ArHeader header, byte[] content) {
            this.header = header;
            this.content = content;
        }

        public ArHeader header() {
            return header;
        }

        public byte[] content() {
            return content;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            ArEntriesRecord that = (ArEntriesRecord) obj;
            return Objects.equals(this.header, that.header) &&
                    Objects.equals(this.content, that.content);
        }

        @Override
        public int hashCode() {
            return Objects.hash(header, content);
        }

        @Override
        public String toString() {
            return "ArEntriesRecord[" +
                    "header=" + header + ", " +
                    "content=" + content + ']';
        }
    }

    final class ArchiveStarter {
        private final Boolean ifHeader;
        private final Boolean ifRealHeader;
        private final byte /*@WithLength(min = 0, max = 8)*/[] alternativeArchiveHeader;

        ArchiveStarter(Boolean ifHeader, Boolean ifRealHeader, byte /*@WithLength(min = 0, max = 8)*/[] alternativeArchiveHeader) {
            this.ifHeader = ifHeader;
            this.ifRealHeader = ifRealHeader;
            this.alternativeArchiveHeader = alternativeArchiveHeader;
        }

        public Boolean ifHeader() {
            return ifHeader;
        }

        public Boolean ifRealHeader() {
            return ifRealHeader;
        }

        public byte /*@WithLength(min = 0, max = 8)*/[] alternativeArchiveHeader() {
            return alternativeArchiveHeader;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            ArchiveStarter that = (ArchiveStarter) obj;
            return Objects.equals(this.ifHeader, that.ifHeader) &&
                    Objects.equals(this.ifRealHeader, that.ifRealHeader) &&
                    Objects.equals(this.alternativeArchiveHeader, that.alternativeArchiveHeader);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ifHeader, ifRealHeader, alternativeArchiveHeader);
        }

        @Override
        public String toString() {
            return "ArchiveStarter[" +
                    "ifHeader=" + ifHeader + ", " +
                    "ifRealHeader=" + ifRealHeader + ", " +
                    "alternativeArchiveHeader=" + alternativeArchiveHeader + ']';
        }
    }
}

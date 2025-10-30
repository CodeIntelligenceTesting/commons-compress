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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import com.code_intelligence.jazzer.junit.FuzzTest;
import com.code_intelligence.jazzer.mutation.annotation.InRange;
import com.code_intelligence.jazzer.mutation.annotation.NotNull;
import com.code_intelligence.jazzer.mutation.annotation.WithLength;
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
    public void arOutAndInTest(final String fileName,final long length, final int userId, final int groupId, final int mode, final long lastModified, byte[] data) throws IOException, FileNotFoundException {
        final File file = new File("target/ArOutAndInTest.ar");
        Files.deleteIfExists(file.toPath());


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
        try (
                ArArchiveInputStream arIn = ArArchiveInputStream.builder().setFile(file).get()) {
            final ArArchiveEntry entry = arIn.getNextEntry();
            assertEquals(fileName, entry.getName());
            // Fix
            // ar -tv Compress678Test-b.ar
            // rw-r--r-- 0/0 1 Apr 27 16:10 2024 01234567891234567
            // ar: Compress678Test-b.ar: Inappropriate file type or format
            assertNotNull(arIn.getNextEntry());
        }
        Files.deleteIfExists(file.toPath());
    }

    @FuzzTest
    public void arInTest(@NotNull(constraint = PropertyConstraint.RECURSIVE) List<MutablePair<
                                     ArHeader,
                                     @WithLength(min = 0, max = 8) byte[]>> arEntriesList,
                         @NotNull(constraint = PropertyConstraint.RECURSIVE) MutablePair<
                                 Boolean,
                                 @InRange(min = 0, max = 8) byte[]> archiveStarter) {
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
                    this.writeBytes(buffer, arEntry.getRight(), arEntry.getRight().length);
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
            size += 60 + arEntry.getRight().length;
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

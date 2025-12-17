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
package org.apache.commons.compress.archivers.tar;

import org.apache.commons.compress.archivers.*;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TarLostFileReproducer {

    @Test
    public void tarLostFileReproducer() {
        byte[] data = {48, 55, 48, 55, 48, 48, 48, 48, 48, 50, 48, 54, 53, 52, 51, 56, 56, 50, 51, 52, 55, 56, 55, 52, 56, 56, 50, 55, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -44, -44, -1, -44, -44, -44, -44, -44, 0, 0, 0, 0, 113, -57, -1, 9, -1, 35, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 55, 48, 55, 48, 48, 55, 48, 48, 55, 48, 55, 48, 55, 48, 48, 55, 48, 50, 48, 48, 48, 55, 48, 55, 50, 48, 48, 55, 48, 48, 55, 16, 48, 55, 48, 55, 48, 55, 48, 48, 55, 48, 50, 50, 50, 50, 50, 50, 50, 92, 69, 93, 92, 69, 93, 93, 93, 93, 93, 93, 50, 50, 48, 48, 48, 48, 48, 48, 48, 48, 55, 48, 55, 48, 55, 48, 48, 55, 48, 48, 55, 48, 55, 48, 55, 48, 48, 55, 48, 50, 48, 48, 48, 55, 48, 55, 50, 48, 48, -40, -40, -40, -40, -40, -40, -40, -40, -40, 0, 30, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 94, 0, 0, 0, 0, 1, 60, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 60, 60, 60, 60, 60, 0, 0, 0, 0, 0, 0, 1, 0, 1, 9, -40, 40, -40, -40, -40, -40, 96, 48, 50, 48, 48, 48, 55, 48, 55, 50, 48, 48, 55, 48, 48, 54, 16, 48, 55, 48, 55, 48, 55, 48, 55, 48, 0, 0, 0, 0, 0, 0, 48, 55, 48, 55, 48, 50, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, 92, 69, 93, 92, 69, 93, 93, 93, 93, 93, 6, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 48, 48, 55, 48, 48, 50, 50, 50, 50, 48, 50, 6, 6, 6, 6, 6, 6, 6, 6, 6, 48, 48, 48, 51, 53, 48, 51, 53, 51, 53, 51, 53, 48, 51, 53, 48, 51, 53, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 55, 53, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 69, 69, 69, 69, 69, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, -40, 92, 69, 93, 92, 69, 93, 93, 93, 93, 93, 6, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 4, 6, 6, 6, 6, 6, 6, 6, 48, 48, 55, 48, 48, 50, 50, 50, 50, 48, 50, 6, 6, 6, 6, 122, 6, 6, 6, 6, 48, 48, 48, 51, 53, 48, 51, 53, 51, 53, 51, 53, 48, 51, 53, 48, 51, 53, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 55, 53, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 49, 49, 52, 53, 50, 55, 48, 0, 0, 0, 0, 0, 0, 0, 0, 0, 113, -57, -57, 0, 0, 30, 103, 0, 0, 0, 76, 0, 0, 0, 0, 0, 0, 0, 3, 85, -15, 0, 0, 0, 0, 0, -21, 0, 0, 0, 0, 0, 113, -57, -57, -57, -57, 10, -57, -57, 113, -57, -57, -57, -57, 10, -57, -57, -57, -57, -57, -57, -57, 44, 44, 31, 44, 44, 60, 60, 0, 59, 60, 60, 60, 68, 60, 60, 60, 60, 60, 0, 59, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, -20, -20, 60, 60, 60, 60, 60, 0, 0, 0, -21, 0, 0, 0, 0, 0, 0, 0, 0, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 92, 69, 93, 92, 69, 93, 93, 93, 93, 93, 93, 60, 60, 0, 59, 60, 60, 1, 11, 14, 60, 91, 56, 50, 51, 52, 55, 56, 55, 52, 56, 56, 50, 55, 64, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 48, 55, 48, 55, 48, 48, 48, 48, 48, 50, 48, 54, 53, 52, 51, 56, 56, 50, 51, 52, 55, 56, 55, 52, 56, 56, 50, 55, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -44, -44, -1, -44, -44, -44, -44, -44, 0, 0, 0, 0, 113, -57, -1, 9, -1, 35, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 48, 48, 48, 48, 48, 55, 55, 48, 48, 55, 48, 48, 55, 48, 55, 48, 55, 48, 48, 55, 48, 50, 48, 48, 48, 55, 48, 55, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -44, -44, 0, 0, 0, 0, 113, -57, -1, 9, -1, 35, 0, 0, 0, 0, 0, 48, 55, 48, 55, 48, 49, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 55, 48, 55, 48, 48, 55, 48, 55, 48, 48, 55, 48, 55, 48, 48, 55, 48, 50, 48, 48, 48, 55, 48, 55, 50, 48, 48, -49, 113, 0, 0, 0, 0, 0, 0, 55, 48, 55, 48, 48, 55, 48, 50, 50, 50, 50, 50, 50, 50, 92, 69, 93, 92, 69, 93, 93, 16, 48, 55, 48, 55, 48, 48, 48, 48, 48, 48, 48, 48, 49, 49, 52, 53, 50, 55, 48, 0, 0, 0, 0, 0, 0, 0, 0, 0, 113, -57, -57, 0, 0, -122, 0, 0, 0, 30, 103, 0, 0, 0, 76, 0, 0, 0, 0, 0, 0, 0, 3, 85, -15, 0, 0, 0, 0, 0, -21, 0, 0, 0, 0, 0, 113, -57, -57, -57, -57, 10, -57, -57, 113, -57, -57, -57, -57, 10, -57, -57, -57, -57, -57, -57, -57, 44, 44, 31, 44, 44, 60, 60, 0, 59, 60, 60, 60, 68, 60, 60, 60, 60, 60, 0, 59, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, -20, -20, 60, 60, 60, 60, 60, 0, 0, 0, -21, 0, 0, 0, 0, 0, 0, 0, 0, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 92, 69, 93, 92, 69, 93, 93, 93, 93, 93, 93, 60, 60, 0, 59, 60, 60, 1, 11, 14, 60, 91, 56, 50, 51, 52, 55, 56, 55, 52, 56, 56, 50, 55, 64, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 48, 55, 48, 55, 48, 48, 48, 48, 48, 50, 48, 54, 53, 52, 51, 56, 56, 50, 51, 52, 55, 56, 55, 52, 56, 56, 50, 55, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -44, -44, -1, -44, -44, -44, -44, -44, 0, 0, 0, 0, 113, -57, -1, 9, -1, 35, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 48, 48, 48, 48, 48, 55, 55, 48, 48, 55, 48, 48, 55, 48, 55, 48, 55, 48, 48, 55, 48, 50, 48, 48, 48, 55, 48, 55, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -44, -44, 0, 0, 0, 0, 113, -57, -1, 9, -1, 35, 0, 0, 0, 0, 0, 48, 55, 48, 55, 48, 49, 0, 0, 1, 0, 0, 0, 0, 0, 78, 23, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 69, 69, 69, 69, 69, 69, -1, -1, -1, -1, -1, -1, -1, -1, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, 69, -40, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 48, 48, 55, 48, 48, 6, 6, 6, 6, 48, 48, 48, 51, 53, 48, 51, 53, 51, 53, 51, 53, 48, 51, 53, 48, 51, 53, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 97, 114, 0, 48, 55, 50, 48, 48, 48, 48, 48, 48, 48, 55, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 84, 82, 65, 73, 76, 69, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 50, 48, 48, 48, 49, 49, 52, 53, 50, 55, 48, 0, 0, 0, 0, 0, 3, 0, 0, 113, -57, -58, -1, -1, -1, -2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 48, 48, 48, 48, 48, 48, 48, 48, 48, 0, 0, 0, 0, 0, 50, 48, 0, 0, 0, 0, 48, 0, 55, 48};
        ArchiveStreamFactory factory = new ArchiveStreamFactory();
        List<ArchiveEntryAndDataWrapper> decompList1 = new ArrayList<>();
        List<ArchiveEntryAndDataWrapper> decompList2 = new ArrayList<>();
        byte[] comp1;
        String archiveType;

        try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
            // Detecting what we are actually trying to read here. Necessary for recompression.
            archiveType = ArchiveStreamFactory.detect(bais);

            try (ArchiveInputStream<? extends ArchiveEntry> in = factory.createArchiveInputStream(archiveType, bais)) {
                for (ArchiveEntry entry = in.getNextEntry(); entry != null; entry = in.getNextEntry()) {
                    decompList1.add(new ArchiveEntryAndDataWrapper(entry, IOUtils.toByteArray(in)));
                }
            } catch (IOException | IllegalArgumentException | IllegalStateException ignored) {
                return;
            }
        } catch (IOException ignored ) {
            return;
        }


        // Writing the extracted data back to an archive.
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ArchiveOutputStream<ArchiveEntry> out = factory.createArchiveOutputStream(archiveType, baos)) {
            for (ArchiveEntryAndDataWrapper decomp : decompList1) {
                out.putArchiveEntry(decomp.entry);
                out.write(decomp.data);
                out.closeArchiveEntry();
            }
            out.finish();
            baos.flush();
            comp1 = baos.toByteArray();

        } catch (IOException | IllegalArgumentException | IllegalStateException ignored) {
            return;
        }

        // Extracting the archive again.
        try (ArchiveInputStream<? extends ArchiveEntry> in = factory.createArchiveInputStream(new ByteArrayInputStream(comp1))) {
            for (ArchiveEntry entry = in.getNextEntry(); entry != null; entry = in.getNextEntry()) {
                decompList2.add(new ArchiveEntryAndDataWrapper(entry, IOUtils.toByteArray(in)));
            }
        } catch (IOException | IllegalArgumentException | IllegalStateException ignored) {
            return;
        }

        // Check for hidden or lost files.
        Assertions.assertTrue(decompList1.size() == decompList2.size());
        Assertions.assertEquals(decompList1, decompList2);
    }



    private static class ArchiveEntryAndDataWrapper {
        private  final ArchiveEntry entry;
        private final byte[] data;

        private ArchiveEntryAndDataWrapper(ArchiveEntry entry, byte[] data) {
            this.entry = entry;
            this.data = data;
        }

        @Override
        public boolean equals(Object o) {
            try {
                if (o instanceof ArchiveEntryAndDataWrapper) {
                    ArchiveEntryAndDataWrapper other = (ArchiveEntryAndDataWrapper) o;
                    return entry.equals(other.entry) && Arrays.equals(data, other.data);
                }
            } catch (ClassCastException e) {
                return false;
            }
            return false;
        }
    }
}

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
package org.apache.commons.compress.archivers.zip;

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

public class ZipFileCountMissmatchReproducer {

    @Test
    public void zipOneLostFileReproducer() {
        byte[] data = {80, 75, 3, 4, -13, 5, -6, -1, 8, 0, -87, 0, 122, 29, 105, -127, -9, -1, -1, 127, 0, 0, 0, 0, 85, 0, 4, 0, 0, 0, 0, 34, 0, 0, 51, 0, 0, 1, 0, 65, 82, 74, 75, 0, 0, 0, 3, 4, 0, -87, -8, -1, -1, -17, 0, 0, 0, 80, 75, 3, 4, -58, 113, 0, 0, 0, 0, 0, 97, 1, 12, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 99, 0, 0, 20, 0, 0, 28, 0, 0, 0, 1, 0, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 85, 84, 5, 0, 1, -96, 31, -90, 18, 10, 0, 32, 0, 0, 0, 58, 0, 1, 0, 24, 45, 0, 16, -42, 69, 120, -49, -88, 1, 0, 0, 3, 0, 0, 95, -1, -9, 55, 48, 55, 48, -78, 50, -4, -1, -2, -54, 0, 0, 4, -1, -1, 23, 48, -1, -1, -1, 13, 0, 0, 0, 0, 0, 62, 48, -49, 75, 55, 75, 0, 0, 80, 75, 3, 4, -58, 113, 0, 0, 93, 0, 97, 1, 0, 12, 0, 122, 105, 112, 0, 0, 0, 0, 42, 2, 0, 0, 0, 0, 99, 0, 1, 0, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 85, 84, 5, 0, 1, -96, 31, -90, 18, 10, 0, 32, 0, 0, 0, 58, 0, 1, 0, 24, 45, 0, 16, -42, 69, 120, -49, -88, 1, 0, 0, 3, 0, 0, 95, -1, -9, 55, 48, 55, 48, -78, 50, -4, -1, -2, -54, 0, 0, 4, -1, -1, 23, 48, -1, -1, -1, 13, 0, 0, 0, 0, 0, 62, 62, 62, 62, -76, -76, -76, 44, 48, 51, -53, -40, -49, -106, 75, 75, -42, 2};
        roundtripCheck(data);
    }

    private void roundtripCheck(byte[] data) {
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

        Assertions.assertEquals(decompList1.size(), decompList2.size());
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

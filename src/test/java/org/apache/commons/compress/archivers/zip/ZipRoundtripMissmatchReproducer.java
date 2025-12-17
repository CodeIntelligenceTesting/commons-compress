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

public class ZipRoundtripMissmatchReproducer {

    @Test
    public void zipRoundtripMissmatchReproducer() {
        byte[] data = {80, 75, 3, 4, 21, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 66, 66, 66, 66, 66, 66, 66, 0, 0, 0, 75};

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

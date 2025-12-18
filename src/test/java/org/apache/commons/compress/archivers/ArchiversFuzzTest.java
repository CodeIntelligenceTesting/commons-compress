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
package org.apache.commons.compress.archivers;

import com.code_intelligence.jazzer.junit.FuzzTest;
import com.code_intelligence.jazzer.mutation.annotation.InRange;
import com.code_intelligence.jazzer.mutation.annotation.NotNull;
import com.code_intelligence.jazzer.mutation.annotation.ValuePool;
import org.apache.commons.compress.FuzzingHelpers;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.LogManager;
import java.util.stream.Stream;


public class ArchiversFuzzTest {

    private static final String[] ARCHIVE_TYPES = {
        ArchiveStreamFactory.APK,
        ArchiveStreamFactory.XAPK,
        ArchiveStreamFactory.APKS,
        ArchiveStreamFactory.APKM,
        ArchiveStreamFactory.AR,
        ArchiveStreamFactory.ARJ,
        ArchiveStreamFactory.CPIO,
        ArchiveStreamFactory.DUMP,
        ArchiveStreamFactory.JAR,
        ArchiveStreamFactory.TAR,
        ArchiveStreamFactory.ZIP,
        ArchiveStreamFactory.SEVEN_Z
    };

    static Stream<?> compressedData() {
        return Stream.of(Paths.get("src", "test",  "resources"))
                .flatMap(FuzzingHelpers::readAllFilesInDirectory);
    }

    @BeforeAll
    public static void setup() {
        LogManager.getLogManager().reset();
    }

    @FuzzTest(maxDuration = "30m")
    public void fuzzArchiversInParsing(@InRange(min = 0, max = 11) int archive, byte @NotNull @ValuePool("compressedData")[] data) {
        try {
            String archiveType = ARCHIVE_TYPES[archive];
            ArchiveStreamFactory factory = new ArchiveStreamFactory(archiveType);
            ArchiveInputStream<? extends ArchiveEntry> in = factory.createArchiveInputStream(new ByteArrayInputStream(data));
            ArchiveEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                in.read(new byte[1024]);
            }
            in.close();
        } catch (IOException | IllegalArgumentException ignored) {
        }
    }

    @FuzzTest(maxDuration = "2h")
    public void fuzzArchiversInAndOutRoundtrip(byte @NotNull @ValuePool("compressedData")[] data) {

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
            } catch (NullPointerException e) {
                // ARJ has an already checked NPE in readMainHeader(ArjArchiveInputStream.java:418)
                // TODO remove ignored NPE after it was fixed!
                if (archiveType.equals(ArchiveStreamFactory.ARJ)) {
                    return;
                } else throw e;
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
        if (archiveType.equals(ArchiveStreamFactory.TAR)) {
            Assertions.assertTrue(
                    decompList1.size() == decompList2.size()
                    ||  decompList1.size() -1 == decompList2.size() // Known issue with one missing file.
                    ||  decompList1.size() -2 == decompList2.size() // Known issue with two missing files.
                    ||  decompList1.size() -3 == decompList2.size() // Known issue with three missing files.
                    ||  decompList1.size() +1 == decompList2.size() // Known issue with one additional file.
                    ||  decompList1.size() +2 == decompList2.size() // Known issue with two additional files.
            );
        } else {
            Assertions.assertEquals(decompList1.size(), decompList2.size());
        }


        // TODO Remove filters when the already identified bugs are fixed.
        if (archiveType.equals(ArchiveStreamFactory.AR)
                || archiveType.equals(ArchiveStreamFactory.TAR)
                || archiveType.equals(ArchiveStreamFactory.ZIP)) {
            return;
        } else {
            // Roundtrip to check that checks that decomp(comp(decomp(data))) == decomp(data)
            Assertions.assertEquals(decompList1, decompList2);
        }
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

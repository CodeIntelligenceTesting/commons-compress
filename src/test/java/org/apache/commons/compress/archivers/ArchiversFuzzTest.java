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
import org.junit.jupiter.api.BeforeAll;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Paths;
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

    @FuzzTest
    public void fuzzArchivers(@InRange(min = 0, max = 11) int archive, byte @NotNull @ValuePool("compressedData")[] data) {
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




}

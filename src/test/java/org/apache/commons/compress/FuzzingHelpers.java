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
package org.apache.commons.compress;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;



public class FuzzingHelpers {
    // Factor to detect compression bombs
    public static final int COMPRESSION_BOMB_FACTOR = 5;
    // Minsize to detect compression bombs
    public static final int COMPRESSION_BOMB_MIN_SIZE_IN_BYTES = 50;
    // Max size allowed to not automatically trigger a compression bomb warning
    public static final int COMPRESSION_BOMB_MAX_INCREASED_SIZE_IN_BYTES = 1024;

    public static boolean isCompressionBomb(int uncompressedSize, int compressedSize) {

        if (compressedSize < COMPRESSION_BOMB_MIN_SIZE_IN_BYTES) {
            return false;
        } else if (compressedSize > COMPRESSION_BOMB_MAX_INCREASED_SIZE_IN_BYTES + uncompressedSize) {
            return true;
        } else return uncompressedSize * COMPRESSION_BOMB_FACTOR < compressedSize ;
    }

    public static String getArchiveTypeFromArchiveEntryInstanceClassName (Class<? extends ArchiveEntry> cls) {
        // Ending of the ArchiveEntry classes
        final String ARCHIVE_ENTRY_ENDING = "ArchiveEntry";
        if (cls.getSimpleName().endsWith(ARCHIVE_ENTRY_ENDING)) {
            String archiveType = cls.getSimpleName().substring(0, cls.getSimpleName().length() - ARCHIVE_ENTRY_ENDING.length()).toLowerCase();
            if (archiveType.equals("SevenZ".toLowerCase())) {
                archiveType = ArchiveStreamFactory.SEVEN_Z;
            }
            return archiveType;
        }
        throw new IllegalArgumentException("Unsupported archive entry type: " + cls.getSimpleName());
    }

    // traverse directory, and read all files and return a stream of Strings
    public static Stream<byte[]> readAllFilesInDirectory(Path path) {
        try (Stream<Path> paths = Files.walk(path)) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(p -> {
                        try {
                            return Files.readAllBytes(p);
                        } catch (IOException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    // copy the stream to prevent later errors with the
                    .collect(Collectors.toList())
                    .stream();
        } catch (IOException e) {
            System.out.println("EXCEPTION!");
        }
        return Stream.empty();
    }
}

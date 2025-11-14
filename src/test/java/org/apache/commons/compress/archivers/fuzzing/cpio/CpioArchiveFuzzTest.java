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
 * Unless required by applicable law or agreed in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.commons.compress.archivers.fuzzing.cpio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.code_intelligence.jazzer.junit.FuzzTest;
import com.code_intelligence.jazzer.mutation.annotation.NotNull;
import com.code_intelligence.jazzer.mutation.utils.PropertyConstraint;
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveInputStream;

/**
 * Fuzz test for {@link CpioArchiveInputStream} using Jazzer's mutator framework.
 *
 * <p>This test builds synthetic CPIO "newc" archives from structured input and then
 * parses them with {@link CpioArchiveInputStream}, exercising header and data reading
 * logic similar to {@code CpioArchiveInputStreamTest}.</p>
 */
public class CpioArchiveFuzzTest {

    /** Structured fuzz input for a single CPIO entry. */
    public record CpioEntryValues(@NotNull String name,
                                  byte @NotNull [] content,
                                  boolean useLongName,
                                  boolean corruptNameSize,
                                  @NotNull String charsetName,
                                  boolean useOldAsciiFormat,
                                  boolean useBinaryFormat,
                                  boolean useBigEndianBinary) {}

    @FuzzTest
    public void cpioInFuzzTest(
            @NotNull(constraint = PropertyConstraint.RECURSIVE) List<CpioEntryValues> entries) {

        byte[] archiveBytes = buildArchiveWithHeaders(entries);
        if (archiveBytes.length == 0) {
            return;
        }

        try (CpioArchiveInputStream in = CpioArchiveInputStream.builder()
                .setInputStream(new ByteArrayInputStream(archiveBytes))
                .setCharset(StandardCharsets.UTF_8)
                .get()) {

            for (CpioArchiveEntry e = in.getNextEntry(); e != null; e = in.getNextEntry()) {
                // Touch metadata getters as in the unit tests
                String name = e.getName();
                long size = e.getSize();
                long mode = e.getMode();
                long time = e.getTime();
                boolean isDirectory = e.isDirectory();
                // Use values in a benign way to avoid "ignored result" and "always false" warnings.
                if ((name == null ? 0 : name.length()) + size + mode + time + (isDirectory ? 1 : 0) == -1) {
                    // unreachable aggregation, only to keep analyzers satisfied
                    throw new AssertionError("unreachable");
                }

                if (in.canReadEntryData(e) && e.getSize() > 0) {
                    long remaining = e.getSize();
                    byte[] buf = new byte[256];
                    while (remaining > 0) {
                        int toRead = (int) Math.min(buf.length, remaining);
                        int r = in.read(buf, 0, toRead);
                        if (r < 0) {
                            break;
                        }
                        remaining -= r;
                    }

                    // Also exercise single-byte read EOF behaviour
                    int r1 = in.read();
                    int r2 = in.read();
                    // consume values in a no-op expression
                    if (r1 + r2 == Integer.MIN_VALUE) {
                        throw new AssertionError("unreachable");
                    }
                }
            }
        } catch (IOException | RuntimeException | Error ignored) {
            // Parsing errors (including MemoryLimitException/ArchiveException) are expected
            // under fuzzing with malformed inputs.
        }
    }

    private static byte[] buildArchiveWithHeaders(List<CpioEntryValues> entries) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (CpioEntryValues v : entries) {
            try {
                Charset cs = getCharsetOrDefault(v.charsetName);
                String name = v.name;

                int fileSize = v.content.length;

                if (v.useBinaryFormat) {
                    // Binary header (big or little endian) using helper in this package
                    CpioBinaryHeader header = new CpioBinaryHeader(cs, name, fileSize);
                    ByteBuffer headerBuf = ByteBuffer.allocate(512);
                    header.writeTo(headerBuf, v.useBigEndianBinary ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
                    int used = headerBuf.position();
                    baos.write(headerBuf.array(), 0, used);
                } else {
                    // ASCII headers via Writer/PrintWriter using helpers in this package
                    try (OutputStreamWriter osw = new OutputStreamWriter(baos, cs);
                         PrintWriter pw = new PrintWriter(osw)) {
                        if (v.useOldAsciiFormat) {
                            CpioOldAsciiHeader hdr;
                            if (v.useLongName) {
                                long nameSize = Math.min((long) name.length() + 1024L, 0x7FFF);
                                hdr = new CpioOldAsciiHeader(cs, name, (int) nameSize, fileSize);
                            } else if (v.corruptNameSize) {
                                hdr = new CpioOldAsciiHeader(cs, name, Integer.MAX_VALUE, fileSize);
                            } else {
                                hdr = new CpioOldAsciiHeader(cs, name, fileSize);
                            }
                            hdr.writeTo(pw);
                        } else {
                            CpioNewAsciiHeader hdr;
                            if (v.useLongName) {
                                long nameSize = Math.min((long) name.length() + 1024L, 0x7FFF);
                                hdr = new CpioNewAsciiHeader(cs, name, nameSize, fileSize);
                            } else if (v.corruptNameSize) {
                                hdr = new CpioNewAsciiHeader(cs, name, Integer.MAX_VALUE, fileSize);
                            } else {
                                hdr = new CpioNewAsciiHeader(cs, name, fileSize);
                            }
                            hdr.writeTo(pw, false);
                        }
                        pw.flush();
                    }
                }

                // write file data (raw bytes, not via Writer)
                baos.write(v.content, 0, fileSize);

                // For binary format, pad to even boundary; for newc, pad4 after data; for old ascii, pad2
                if (v.useBinaryFormat) {
                    long pad = AbstractCpioHeader.pad2(fileSize);
                    for (int i = 0; i < pad; i++) {
                        baos.write(0);
                    }
                } else if (v.useOldAsciiFormat) {
                    long pad = AbstractCpioHeader.pad2(fileSize);
                    for (int i = 0; i < pad; i++) {
                        baos.write(0);
                    }
                } else {
                    long pad = AbstractCpioHeader.pad4(fileSize);
                    for (int i = 0; i < pad; i++) {
                        baos.write(0);
                    }
                }


            } catch (IOException | RuntimeException ignored) {
                // continue with next entry
            }
        }

        return baos.toByteArray();
    }

    private static Charset getCharsetOrDefault(String charsetName) {
        try {
            return Charset.forName(charsetName);
        } catch (RuntimeException | Error e) {
            return Charset.defaultCharset();
        }
    }
}

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

import org.apache.commons.compress.archivers.dump.DumpArchiveConstants.SEGMENT_TYPE;
import org.apache.commons.compress.archivers.dump.DumpArchiveEntry.TYPE;
import org.apache.commons.compress.archivers.fuzzing.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.US_ASCII;


/**
 * Utility to generate test archives with specific properties.
 * <p>
 * Run from the command line, it takes one argument: the output directory.
 * </p>
 * <p>
 * The generated files are checked into the src/test/resources/invalid directory.
 * </p>
 */
public final class TestArchiveGenerator {

    private static final byte[] USTAR_TRAILER = new byte[1024];
    @SuppressWarnings("OctalInteger")
    private static final int FILE_MODE = 0100644;
    private static final int GROUP_ID = 0;
    private static final String GROUP_NAME = "group";
    // TAR
    private static final String OLD_GNU_MAGIC = "ustar  ";
    private static final int OWNER_ID = 0;
    private static final String OWNER_NAME = "owner";
    private static final String PAX_MAGIC = "ustar\u000000";
    private static final int TIMESTAMP = 0;

    // Maximum size for a Java array: AR, CPIO and TAR support longer names
    private static final int SOFT_ARRAY_MAX_SIZE = Integer.MAX_VALUE - 8;
    private static final int ARJ_MAX_SIZE = 2568; // ARJ header - fixed fields

    /**
     * Generates a truncated AR archive with a very long BSD name.
     * <p>
     * The name has a declared length of {@link #SOFT_ARRAY_MAX_SIZE}, which is the largest
     * name a Java array can hold.
     * </p>
     * <p>
     * The AR archive specification allows for even longer names.
     * </p>
     *
     * @param path The output directory
     */
    private static void arInvalidBsdLongName(final Path path) throws IOException {
        final Path file = path.resolve("bsd-fail.ar");
        try (final PrintWriter out = new PrintWriter(Files.newBufferedWriter(file))) {
            writeArHeader(out);
            final ArHeader header = new ArHeader(
                "#1/" + SOFT_ARRAY_MAX_SIZE, TIMESTAMP, OWNER_ID, GROUP_ID, FILE_MODE, SOFT_ARRAY_MAX_SIZE);
            header.writeTo(out);
        }
    }

    /**
     * Generates a truncated AR archive with a very long GNU name.
     * <p>
     * The name has a declared length of {@link #SOFT_ARRAY_MAX_SIZE}, which is the largest
     * name a Java array can hold.
     * </p>
     * <p>
     * The AR archive specification allows for even longer names.
     * </p>
     *
     * @param path The output directory
     */
    private static void arInvalidGnuLongName(final Path path) throws IOException {
        final Path file = path.resolve("gnu-fail.ar");
        try (final PrintWriter out = new PrintWriter(Files.newBufferedWriter(file))) {
            writeArHeader(out);
            final ArHeader header = new ArHeader("//", TIMESTAMP, OWNER_ID, GROUP_ID, FILE_MODE, SOFT_ARRAY_MAX_SIZE);
            header.writeTo(out);
        }
    }

    /**
     * Generates an ARJ archive with a very long file name.
     * <p>
     * The name in ARJ must be contained in 2600 bytes of the header, and 32 bytes are used by
     * compulsory fields and null terminator, so the maximum length is 2568 bytes.
     * </p>
     *
     * @param path The output directory
     */
    private static void arjLongName(final Path path) throws IOException {
        final Path file = path.resolve("long-name.arj");
        try (final OutputStream out = Files.newOutputStream(file)) {
            ByteBuffer buffer = ByteBuffer.allocate(IOUtils.DEFAULT_BUFFER_SIZE);
            final String longName = StringUtils.repeat('a', ARJ_MAX_SIZE);
            ArjMainHeader mainHeader = new ArjMainHeader(US_ASCII, "long-name.arj", "");
            mainHeader.writeTo(buffer);
            ArjLocalHeader localHeader = new ArjLocalHeader(US_ASCII, longName, "");
            localHeader.writeTo(buffer);
            buffer.flip();
            out.write(buffer.array(), 0, buffer.limit());
            byte[] trailer = {(byte) 0x60, (byte) 0xEA, 0x00, 0x00}; // ARJ file trailer
            out.write(trailer);
        }
    }

    /**
     * Generates a valid AR archive with a very long BSD name.
     * <p>
     * The name has a length of {@link Short#MAX_VALUE}.
     * </p>
     *
     * @param path The output directory
     */
    private static void arValidBsdLongName(final Path path) throws IOException {
        final Path file = path.resolve("bsd-short-max-value.ar");
        try (final PrintWriter out = new PrintWriter(Files.newBufferedWriter(file))) {
            writeArHeader(out);
            final ArHeader header =
                new ArHeader("#1/" + Short.MAX_VALUE, TIMESTAMP, OWNER_ID, GROUP_ID, FILE_MODE, Short.MAX_VALUE);
            header.writeTo(out);
            out.write(StringUtils.repeat('a', Short.MAX_VALUE));
        }
    }

    /**
     * Generates a valid AR archive with a very long GNU name.
     * <p>
     * The name has a length of {@link Short#MAX_VALUE}.
     * </p>
     *
     * @param path The output directory
     */
    private static void arValidGnuLongName(final Path path) throws IOException {
        final Path file = path.resolve("gnu-short-max-value.ar");
        try (final PrintWriter out = new PrintWriter(Files.newBufferedWriter(file))) {
            writeArHeader(out);
            // GNU long name table with one entry and a new line
            final ArHeader header1 = new ArHeader("//", TIMESTAMP, OWNER_ID, GROUP_ID, FILE_MODE, Short.MAX_VALUE + 1);
            header1.writeTo(out);
            out.write(StringUtils.repeat('a', Short.MAX_VALUE));
            // End with a new line
            out.write('\n');
            // Add a file to make the archive valid
            final ArHeader header = new ArHeader("/0", TIMESTAMP, OWNER_ID, GROUP_ID, FILE_MODE, 0);
            header.writeTo(out);
        }
    }

    /**
     * Generates a truncated CPIO new ASCII archive with a very long file name.
     * <p>
     * The name has a length of {@code SOFT_MAX_ARRAY_SIZE}, which is the largest
     * name that can be theoretically represented in Java.
     * </p>
     * <p>
     * The CPIO archive specification allows for even longer names.
     * </p>
     *
     * @param path The output directory
     */
    private static void cpioNewAsciiTruncatedLongNames(final Path path) throws IOException {
        CpioNewAsciiHeader header = new CpioNewAsciiHeader(US_ASCII, "", SOFT_ARRAY_MAX_SIZE, 0);
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(path.resolve("newc-fail.cpio")))) {
            header.writeTo(out, false);
        }
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(path.resolve("crc-fail.cpio")))) {
            header.writeTo(out, true);
        }
    }

    /**
     * Generates CPIO new ASCII and CRC archives with a very long file name.
     * <p>
     * The name has a length of {@link Short#MAX_VALUE}.
     * </p>
     *
     * @param path The output directory
     */
    private static void cpioNewAsciiValidLongNames(final Path path) throws IOException {
        final String longName = StringUtils.repeat('a', Short.MAX_VALUE);
        CpioNewAsciiHeader header = new CpioNewAsciiHeader(US_ASCII, longName, 0);
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(path.resolve("newc.cpio")))) {
            header.writeTo(out, false);
        }
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(path.resolve("crc.cpio")))) {
            header.writeTo(out, true);
        }
    }

    /**
     * Generates CPIO binary archives with a very long file name.
     * <p>
     * The name has a length of {@link Short#MAX_VALUE} - 1.
     * </p>
     *
     * @param path The output directory
     */
    private static void cpioBinaryValidLongNames(final Path path) throws IOException {
        final String longName = StringUtils.repeat('a', Short.MAX_VALUE - 1);
        CpioBinaryHeader header = new CpioBinaryHeader(US_ASCII, longName, 0);
        final ByteBuffer buffer = ByteBuffer.allocate(2 * Short.MAX_VALUE);
        try (OutputStream out = Files.newOutputStream(path.resolve("bin-big-endian.cpio"))) {
            header.writeTo(buffer, ByteOrder.BIG_ENDIAN);
            buffer.flip();
            out.write(buffer.array(), 0, buffer.limit());
        }
        try (OutputStream out = Files.newOutputStream(path.resolve("bin-little-endian.cpio"))) {
            header.writeTo(buffer, ByteOrder.LITTLE_ENDIAN);
            buffer.flip();
            out.write(buffer.array(), 0, buffer.limit());
        }
    }

    /**
     * Generates a truncated CPIO old ASCII archive with a very long file name.
     * <p>
     * The name has a length of {@code 0777776}, which is the largest
     * name that can be represented in the name size field of the header.
     * </p>
     *
     * @param path The output directory
     */
    private static void cpioOldAsciiTruncatedLongNames(final Path path) throws IOException {
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(path.resolve("odc-fail.cpio")))) {
            @SuppressWarnings("OctalInteger")
            CpioOldAsciiHeader header = new CpioOldAsciiHeader(US_ASCII, "", 0777776, 0);
            header.writeTo(out);
        }
    }

    /**
     * Generates CPIO old ASCII archives with a very long file name.
     * <p>
     * The name has a length of {@link Short#MAX_VALUE}.
     * </p>
     *
     * @param path The output directory
     */
    private static void cpioOldAsciiValidLongNames(final Path path) throws IOException {
        final String longName = StringUtils.repeat('a', Short.MAX_VALUE);
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(path.resolve("odc.cpio")))) {
            CpioOldAsciiHeader header = new CpioOldAsciiHeader(US_ASCII, longName, 0);
            header.writeTo(out);
        }
    }

    private static byte[] createData(final int size) {
        final byte[] data = new byte[size];
        for (int i = 0; i < size; i++) {
            data[i] = (byte) (i % 256);
        }
        return data;
    }

    // Very fragmented sparse file
    private static List<Pair<Integer, Integer>> createFragmentedSparseEntries(final int realSize) {
        final List<Pair<Integer, Integer>> sparseEntries = new ArrayList<>();
        for (int offset = 0; offset < realSize; offset++) {
            sparseEntries.add(Pair.of(offset, 1));
        }
        return sparseEntries;
    }

    private static byte[] createGnuSparse00PaxData(
        final Collection<? extends Pair<Integer, Integer>> sparseEntries, final int realSize) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, US_ASCII))) {
            writePaxKeyValue("GNU.sparse.size", realSize, writer);
            writePaxKeyValue("GNU.sparse.numblocks", sparseEntries.size(), writer);
            for (final Pair<Integer, Integer> entry : sparseEntries) {
                writePaxKeyValue("GNU.sparse.offset", entry.getLeft(), writer);
                writePaxKeyValue("GNU.sparse.numbytes", entry.getRight(), writer);
            }
        }
        return baos.toByteArray();
    }

    private static byte[] createGnuSparse01PaxData(
        final Collection<? extends Pair<Integer, Integer>> sparseEntries, final int realSize) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, US_ASCII))) {
            writePaxKeyValue("GNU.sparse.size", realSize, writer);
            writePaxKeyValue("GNU.sparse.numblocks", sparseEntries.size(), writer);
            final String map = sparseEntries.stream()
                .map(e -> e.getLeft() + "," + e.getRight())
                .collect(Collectors.joining(","));
            writePaxKeyValue("GNU.sparse.map", map, writer);
        }
        return baos.toByteArray();
    }

    private static byte[] createGnuSparse1EntriesData(final Collection<? extends Pair<Integer, Integer>> sparseEntries)
        throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, US_ASCII))) {
            writer.printf("%d\n", sparseEntries.size());
            for (final Pair<Integer, Integer> entry : sparseEntries) {
                writer.printf("%d\n", entry.getLeft());
                writer.printf("%d\n", entry.getRight());
            }
        }
        padTo512Bytes(baos.size(), baos);
        return baos.toByteArray();
    }

    private static byte[] createGnuSparse1PaxData(
        final Collection<Pair<Integer, Integer>> sparseEntries, final int realSize) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, US_ASCII))) {
            writePaxKeyValue("GNU.sparse.realsize", realSize, writer);
            writePaxKeyValue("GNU.sparse.numblocks", sparseEntries.size(), writer);
            writePaxKeyValue("GNU.sparse.major", 1, writer);
            writePaxKeyValue("GNU.sparse.minor", 0, writer);
        }
        return baos.toByteArray();
    }


    private static String createPaxKeyValue(final String key, final String value) {
        final String entry = ' ' + key + "=" + value + "\n";
        // Guess length: length of length + space + entry
        int length = String.valueOf(entry.length()).length() + entry.length();
        // Recompute if number of digits changes
        length = String.valueOf(length).length() + entry.length();
        // Return the value
        return length + entry;
    }


    public static void createSparseFileTestCases(final Path path) throws IOException {
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("Not a directory: " + path);
        }
        oldGnuSparse(path);
        gnuSparse00(path);
        gnuSparse01(path);
        gnuSparse1X(path);
    }

    /**
     * Generates a Dump archive with a very long name, but with the directories in reverse order.
     * <p>
     * The name has a length of {@link Short#MAX_VALUE} - 1, which is the longest
     * name that can be represented in a DumpDirectoryEntry.
     * </p>
     *
     * @param path The output directory
     */
    private static void dumpReversedLongName(final Path path) throws IOException {
        final String longName = StringUtils.repeat('a', 255);
        try (OutputStream out = Files.newOutputStream(path.resolve("long-name-reversed.dump"))) {
            ByteBuffer buffer = ByteBuffer.allocate(64 * 1024);
            // Archive summary
            DumpSummaryHeader summary = new DumpSummaryHeader(1);
            summary.writeTo(buffer);
            writeByteBuffer(buffer, out);
            // Ignored records
            DumpLocalHeader header = new DumpLocalHeader(SEGMENT_TYPE.CLRI, TYPE.FILE, 1, 0, 0);
            header.writeTo(buffer);
            writeByteBuffer(buffer, out);
            header = new DumpLocalHeader(SEGMENT_TYPE.BITS, TYPE.FILE, 1, 0, 0);
            header.writeTo(buffer);
            writeByteBuffer(buffer, out);
            // Empty file
            final int rootInode = 2;
            header = new DumpLocalHeader(SEGMENT_TYPE.INODE, TYPE.FILE, 1, 128 + rootInode, 0);
            header.writeTo(buffer);
            writeByteBuffer(buffer, out);
            // 128 directory entries with a single file of very long name
            //
            // The first directory is the root directory with an empty name.
            // The total path length for the file will be 127 * 256 + 255 = Short.MAX_VALUE
            for (int i = 127 + rootInode; i >= rootInode; i--) {
                writeSingleFileDumpDirectory(i, longName, out);
            }
            // End of dump
            header = new DumpLocalHeader(SEGMENT_TYPE.END, TYPE.FILE, 1, 0, 0);
            header.writeTo(buffer);
            writeByteBuffer(buffer, out);
        }
    }

    /**
     * Generates a Dump archive with a very long name.
     * <p>
     * The name has a length of {@link Short#MAX_VALUE} - 1, which is the longest
     * name that can be represented in a DumpDirectoryEntry.
     * </p>
     *
     * @param path The output directory
     */
    private static void dumpValidLongName(final Path path) throws IOException {
        final String longName = StringUtils.repeat('a', 255);
        try (OutputStream out = Files.newOutputStream(path.resolve("long-name.dump"))) {
            ByteBuffer buffer = ByteBuffer.allocate(64 * 1024);
            // Archive summary
            DumpSummaryHeader summary = new DumpSummaryHeader(1);
            summary.writeTo(buffer);
            writeByteBuffer(buffer, out);
            // Ignored records
            DumpLocalHeader header = new DumpLocalHeader(SEGMENT_TYPE.CLRI, TYPE.FILE, 1, 0, 0);
            header.writeTo(buffer);
            writeByteBuffer(buffer, out);
            header = new DumpLocalHeader(SEGMENT_TYPE.BITS, TYPE.FILE, 1, 0, 0);
            header.writeTo(buffer);
            writeByteBuffer(buffer, out);
            // 128 directory entries with a single file of very long name
            //
            // The first directory is the root directory with an empty name.
            // The total path length for the file will be 127 * 256 + 255 = Short.MAX_VALUE
            final int rootInode = 2;
            for (int i = rootInode; i < 128 + rootInode; i++) {
                writeSingleFileDumpDirectory(i, longName, out);
            }
            // Empty file
            header = new DumpLocalHeader(SEGMENT_TYPE.INODE, TYPE.FILE, 1, 128 + rootInode, 0);
            header.writeTo(buffer);
            writeByteBuffer(buffer, out);
            // End of dump
            header = new DumpLocalHeader(SEGMENT_TYPE.END, TYPE.FILE, 1, 0, 0);
            header.writeTo(buffer);
            writeByteBuffer(buffer, out);
        }
    }


    public static void generateLongFileNames(final Path path) throws IOException {
        if (!Files.isDirectory(path)) {
            throw new IOException("Not a directory: " + path);
        }
        Files.createDirectories(path);
        // AR
        arInvalidBsdLongName(path);
        arInvalidGnuLongName(path);
        arValidBsdLongName(path);
        arValidGnuLongName(path);
        // ARJ
        arjLongName(path);
        // CPIO
        cpioOldAsciiTruncatedLongNames(path);
        cpioNewAsciiTruncatedLongNames(path);
        cpioBinaryValidLongNames(path);
        cpioOldAsciiValidLongNames(path);
        cpioNewAsciiValidLongNames(path);
        // DUMP
        dumpValidLongName(path);
        dumpReversedLongName(path);
        // TAR
        tarPaxInvalidLongNames(path);
        tarGnuInvalidLongNames(path);
        tarPaxValidLongNames(path);
        tarGnuValidLongNames(path);
        // ZIP
        zipValidLongName(path);
    }

    private static void gnuSparse00(final Path path) throws IOException {
        final Path file = path.resolve("gnu-sparse-00.tar");
        try (OutputStream out = Files.newOutputStream(file)) {
            final byte[] data = createData(8 * 1024);
            final List<Pair<Integer, Integer>> sparseEntries = createFragmentedSparseEntries(data.length);
            final byte[] paxData = createGnuSparse00PaxData(sparseEntries, data.length);
            writeGnuSparse0File(data, paxData, out);
            writeUstarTrailer(out);
        }
    }

    private static void gnuSparse01(final Path path) throws IOException {
        final Path file = path.resolve("gnu-sparse-01.tar");
        try (OutputStream out = Files.newOutputStream(file)) {
            final byte[] data = createData(8 * 1024);
            final List<Pair<Integer, Integer>> sparseEntries = createFragmentedSparseEntries(data.length);
            final byte[] paxData = createGnuSparse01PaxData(sparseEntries, data.length);
            writeGnuSparse0File(data, paxData, out);
            writeUstarTrailer(out);
        }
    }

    private static void gnuSparse1X(final Path path) throws IOException {
        final Path file = path.resolve("gnu-sparse-1.tar");
        try (OutputStream out = Files.newOutputStream(file)) {
            final byte[] data = createData(8 * 1024);
            final List<Pair<Integer, Integer>> sparseEntries = createFragmentedSparseEntries(data.length);
            writeGnuSparse1File(sparseEntries, data, out);
            writeUstarTrailer(out);
        }
    }

    public static void main(final String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Expected one argument: output directory");
            System.exit(1);
        }
        final Path path = Paths.get(args[0]);
        if (!Files.isDirectory(path)) {
            System.err.println("Not a directory: " + path);
            System.exit(1);
        }
        // Sparse file examples
        final Path sparsePath = path.resolve("sparse");
        Files.createDirectories(sparsePath);
        createSparseFileTestCases(sparsePath);
    }

    private static void oldGnuSparse(final Path path) throws IOException {
        final Path file = path.resolve("old-gnu-sparse.tar");
        try (OutputStream out = Files.newOutputStream(file)) {
            final byte[] data = createData(8 * 1024);
            final List<Pair<Integer, Integer>> sparseEntries = createFragmentedSparseEntries(data.length);
            writeOldGnuSparseFile(sparseEntries, data, data.length, out);
            writeUstarTrailer(out);
        }
    }

    private static int padTo512Bytes(final int offset, final OutputStream out) throws IOException {
        int count = offset;
        while (count % 512 != 0) {
            out.write(0);
            count++;
        }
        return count;
    }

    /**
     * Generates a truncated TAR archive with a very long name using the old GNU format.
     * <p>
     * The name has a declared length of {@link #SOFT_ARRAY_MAX_SIZE}, which is the largest
     * name a Java array can hold.
     * </p>
     * <p>
     * The TAR archive specification allows for even longer names.
     * </p>
     *
     * @param path The output directory
     */
    private static void tarGnuInvalidLongNames(final Path path) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(512);
        PosixTarHeader gnuHeader = new PosixTarHeader("././@LongLink", SOFT_ARRAY_MAX_SIZE, 0, (byte) 'L', "");
        try (WritableByteChannel out = Files.newByteChannel(
            path.resolve("gnu-fail.tar"),
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING)) {
            gnuHeader.writeTo(buffer);
            buffer.flip();
            out.write(buffer);
        }
    }

    /**
     * Generates a TAR archive with a very long name using the old GNU format.
     * <p>
     * The name has a length of {@link Short#MAX_VALUE}.
     * </p>
     *
     * @param path The output directory
     */
    private static void tarGnuValidLongNames(final Path path) throws IOException {
        final byte[] gnuEntryContent = StringUtils.repeat('a', Short.MAX_VALUE).getBytes(US_ASCII);
        ByteBuffer buffer = ByteBuffer.allocate(512);
        PosixTarHeader gnuHeader = new PosixTarHeader("././@LongLink", gnuEntryContent.length, 0, (byte) 'L', "");
        PosixTarHeader fileHeader = new PosixTarHeader("a", 0, 0, (byte) '0', "");
        try (OutputStream out = Files.newOutputStream(path.resolve("gnu.tar"))) {
            gnuHeader.writeTo(buffer);
            buffer.flip();
            out.write(buffer.array(), 0, buffer.limit());
            out.write(gnuEntryContent);
            padTo512Bytes(gnuEntryContent.length, out);
            buffer.clear();
            fileHeader.writeTo(buffer);
            buffer.flip();
            out.write(buffer.array(), 0, buffer.limit());
            writeUstarTrailer(out);
        }
    }


    /**
     * Generates a truncated TAR archive with a very long name using the PAX format.
     * <p>
     * The name has a declared length of {@link #SOFT_ARRAY_MAX_SIZE}, which is the largest
     * name a Java array can hold.
     * </p>
     * <p>
     * The TAR archive specification allows for even longer names.
     * </p>
     *
     * @param path The output directory
     */
    private static void tarPaxInvalidLongNames(final Path path) throws IOException {
        // The size of a pax entry for a file with a name of SOFT_ARRAY_MAX_SIZE
        final long paxEntrySize =
            String.valueOf(SOFT_ARRAY_MAX_SIZE).length() + " path=".length() + SOFT_ARRAY_MAX_SIZE + "\n".length();
        ByteBuffer buffer = ByteBuffer.allocate(512);
        PosixTarHeader paxHeader = new PosixTarHeader("PaxHeader/long", paxEntrySize, 0, (byte) 'x', "");
        try (WritableByteChannel out = Files.newByteChannel(
            path.resolve("pax-fail.tar"),
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING)) {
            paxHeader.writeTo(buffer);
            buffer.flip();
            out.write(buffer);
        }
    }

    /**
     * Generates a TAR archive with a very long name using the PAX format.
     * <p>
     * The name has a length of {@link Short#MAX_VALUE}.
     * </p>
     *
     * @param path The output directory
     */
    private static void tarPaxValidLongNames(final Path path) throws IOException {
        final byte[] paxEntryContent = createPaxKeyValue("path", StringUtils.repeat('a', Short.MAX_VALUE))
            .getBytes(US_ASCII);
        ByteBuffer buffer = ByteBuffer.allocate(512);
        PosixTarHeader paxHeader = new PosixTarHeader("PaxHeader/long", paxEntryContent.length, 0, (byte) 'x', "");
        PosixTarHeader fileHeader = new PosixTarHeader("a", 0, 0, (byte) '0', "");
        try (OutputStream out = Files.newOutputStream(path.resolve("pax.tar"))) {
            paxHeader.writeTo(buffer);
            buffer.flip();
            out.write(buffer.array(), 0, buffer.limit());
            out.write(paxEntryContent);
            padTo512Bytes(paxEntryContent.length, out);
            buffer.clear();
            fileHeader.writeTo(buffer);
            buffer.flip();
            out.write(buffer.array(), 0, buffer.limit());
            writeUstarTrailer(out);
        }
    }

    private static void writeArHeader(final PrintWriter out) {
        out.print("!<arch>\n");
    }


    private static void writeByteBuffer(final ByteBuffer buffer, final OutputStream out) throws IOException {
        buffer.flip();
        out.write(buffer.array(), 0, buffer.limit());
        buffer.clear();
    }

    private static void writeGnuSparse0File(final byte[] data, final byte[] paxData, final OutputStream out)
        throws IOException {
        // PAX entry
        int offset = writeTarUstarHeader("./GNUSparseFile.1/" + "sparse-file.txt", paxData.length, PAX_MAGIC, 'x', out);
        offset = padTo512Bytes(offset, out);
        // PAX data
        out.write(paxData);
        offset += paxData.length;
        offset = padTo512Bytes(offset, out);
        // File entry
        offset += writeTarUstarHeader("sparse-file.txt", data.length, PAX_MAGIC, '0', out);
        offset = padTo512Bytes(offset, out);
        // File data
        out.write(data);
        offset += data.length;
        padTo512Bytes(offset, out);
    }

    private static void writeGnuSparse1File(
        final Collection<Pair<Integer, Integer>> sparseEntries, final byte[] data, final OutputStream out)
        throws IOException {
        // PAX entry
        final byte[] paxData = createGnuSparse1PaxData(sparseEntries, data.length);
        int offset = writeTarUstarHeader("./GNUSparseFile.1/sparse-file.txt", paxData.length, PAX_MAGIC, 'x', out);
        offset = padTo512Bytes(offset, out);
        // PAX data
        out.write(paxData);
        offset += paxData.length;
        offset = padTo512Bytes(offset, out);
        // File entry
        final byte[] sparseEntriesData = createGnuSparse1EntriesData(sparseEntries);
        offset += writeTarUstarHeader("sparse-file.txt", sparseEntriesData.length + data.length, PAX_MAGIC, '0', out);
        offset = padTo512Bytes(offset, out);
        // File data
        out.write(sparseEntriesData);
        offset += sparseEntriesData.length;
        out.write(data);
        offset += data.length;
        padTo512Bytes(offset, out);
    }

    private static int writeOctalString(final long value, final int length, final OutputStream out) throws IOException {
        int count = 0;
        final String s = Long.toOctalString(value);
        count += writeString(s, length - 1, out);
        out.write('\0');
        return ++count;
    }

    private static int writeOldGnuSparseEntries(
        final Iterable<Pair<Integer, Integer>> sparseEntries, final int limit, final OutputStream out)
        throws IOException {
        int offset = 0;
        int count = 0;
        final Iterator<Pair<Integer, Integer>> it = sparseEntries.iterator();
        while (it.hasNext()) {
            if (count >= limit) {
                out.write(1); // more entries follow
                return ++offset;
            }
            final Pair<Integer, Integer> entry = it.next();
            it.remove();
            count++;
            offset += writeOldGnuSparseEntry(entry.getLeft(), entry.getRight(), out);
        }
        while (count < limit) {
            // pad with empty entries
            offset += writeOldGnuSparseEntry(0, 0, out);
            count++;
        }
        out.write(0); // no more entries
        return ++offset;
    }

    private static int writeOldGnuSparseEntry(final int offset, final int length, final OutputStream out)
        throws IOException {
        int count = 0;
        count += writeOctalString(offset, 12, out);
        count += writeOctalString(length, 12, out);
        return count;
    }

    private static int writeOldGnuSparseExtendedHeader(
        final Iterable<Pair<Integer, Integer>> sparseEntries, final OutputStream out) throws IOException {
        int offset = 0;
        offset += writeOldGnuSparseEntries(sparseEntries, 21, out);
        offset = padTo512Bytes(offset, out);
        return offset;
    }

    private static void writeOldGnuSparseFile(
        final Collection<Pair<Integer, Integer>> sparseEntries,
        final byte[] data,
        final int realSize,
        final OutputStream out)
        throws IOException {
        int offset = writeTarUstarHeader("sparse-file.txt", data.length, OLD_GNU_MAGIC, 'S', out);
        while (offset < 386) {
            out.write(0);
            offset++;
        }
        // Sparse entries (24 bytes each)
        offset += writeOldGnuSparseEntries(sparseEntries, 4, out);
        // Real size (12 bytes)
        offset += writeOctalString(realSize, 12, out);
        offset = padTo512Bytes(offset, out);
        // Write extended headers
        while (!sparseEntries.isEmpty()) {
            offset += writeOldGnuSparseExtendedHeader(sparseEntries, out);
        }
        // Write file data
        out.write(data);
        offset += data.length;
        padTo512Bytes(offset, out);
    }

    private static void writePaxKeyValue(final String key, final int value, final PrintWriter out) {
        writePaxKeyValue(key, Integer.toString(value), out);
    }

    private static void writePaxKeyValue(final String key, final String value, final PrintWriter out) {
        final String entry = ' ' + key + "=" + value + "\n";
        // Guess length: length of length + space + entry
        final int length = String.valueOf(entry.length()).length() + entry.length();
        // Recompute if number of digits changes
        out.print(String.valueOf(length).length() + entry.length());
        out.print(entry);
    }


    private static void writeSingleFileDumpDirectory(int inode, String fileName, OutputStream out) throws IOException {
        final DumpDirectoryEntry dotEntry = new DumpDirectoryEntry(inode, ".");
        final DumpDirectoryEntry dotDotEntry = new DumpDirectoryEntry(inode > 2 ? inode - 1 : inode, "..");
        final DumpDirectoryEntry entry = new DumpDirectoryEntry(inode + 1, fileName);
        int totalLength = dotEntry.recordLength() + dotDotEntry.recordLength() + entry.recordLength();
        final DumpLocalHeader header = new DumpLocalHeader(SEGMENT_TYPE.INODE, TYPE.DIRECTORY, 1, inode, totalLength);
        final ByteBuffer buffer = ByteBuffer.allocate(64 * 1024);
        header.writeTo(buffer);
        writeByteBuffer(buffer, out);
        dotEntry.writeTo(buffer);
        writeByteBuffer(buffer, out);
        dotDotEntry.writeTo(buffer);
        writeByteBuffer(buffer, out);
        entry.writeTo(buffer);
        writeByteBuffer(buffer, out);
        while (totalLength % 1024 != 0) {
            out.write(0);
            totalLength++;
        }
    }

    private static int writeString(final String s, final int length, final OutputStream out) throws IOException {
        final byte[] bytes = s.getBytes(US_ASCII);
        out.write(bytes);
        for (int i = bytes.length; i < length; i++) {
            out.write('\0');
        }
        return length;
    }

    private static int writeTarUstarHeader(
        final String fileName,
        final long fileSize,
        final String magicAndVersion,
        final char typeFlag,
        final OutputStream out)
        throws IOException {
        int count = 0;
        // File name (100 bytes)
        count += writeString(fileName, 100, out);
        // File mode (8 bytes)
        count += writeOctalString(FILE_MODE, 8, out);
        // Owner ID (8 bytes)
        count += writeOctalString(OWNER_ID, 8, out);
        // Group ID (8 bytes)
        count += writeOctalString(GROUP_ID, 8, out);
        // File size (12 bytes)
        count += writeOctalString(fileSize, 12, out);
        // Modification timestamp (12 bytes)
        count += writeOctalString(TIMESTAMP, 12, out);
        // Checksum (8 bytes), filled with spaces for now
        count += writeString(StringUtils.repeat(' ', 7), 8, out);
        // Link indicator (1 byte)
        out.write(typeFlag);
        count++;
        // Name of linked file (100 bytes)
        count += writeString("", 100, out);
        // Magic (6 bytes) + Version (2 bytes)
        count += writeString(magicAndVersion, 8, out);
        // Owner user name (32 bytes)
        count += writeString(OWNER_NAME, 32, out);
        // Owner group name (32 bytes)
        count += writeString(GROUP_NAME, 32, out);
        // Device major number (8 bytes)
        count += writeString("", 8, out);
        // Device minor number (8 bytes)
        count += writeString("", 8, out);
        return count;
    }

    private static void writeUstarTrailer(final OutputStream out) throws IOException {
        out.write(USTAR_TRAILER);
    }

    /**
     * Generates a ZIP archive with a very long name.
     * <p>
     * The name has a length of {@link Short#MAX_VALUE}, which is the longest
     * name that can be represented in a ZIP local file header.
     * </p>
     *
     * @param path The output directory
     */
    private static void zipValidLongName(final Path path) throws IOException {
        try (OutputStream out = Files.newOutputStream(path.resolve("long-name.zip"))) {
            ByteBuffer buffer = ByteBuffer.allocate(64 * 1024);
            // File entry
            String fileName = StringUtils.repeat('a', Short.MAX_VALUE);
            ZipLocalHeader header = new ZipLocalHeader(US_ASCII, fileName, 0, 0);
            header.writeTo(buffer);
            final int offsetCentralDirectory = buffer.position();
            writeByteBuffer(buffer, out);
            // Central directory entry
            ZipCentralDirectoryHeader centralHeader = new ZipCentralDirectoryHeader(US_ASCII, fileName, 0);
            centralHeader.writeTo(buffer);
            final int sizeCentralDirectory = buffer.position();
            writeByteBuffer(buffer, out);
            // End of central directory
            ZipEndOfCentralDirectory end =
                new ZipEndOfCentralDirectory(1, sizeCentralDirectory, offsetCentralDirectory);
            end.writeTo(buffer);
            writeByteBuffer(buffer, out);
        }
    }

    private TestArchiveGenerator() {
        // hide constructor
    }
}

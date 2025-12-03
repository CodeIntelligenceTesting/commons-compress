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

import com.code_intelligence.jazzer.junit.FuzzTest;
import com.code_intelligence.jazzer.mutation.annotation.NotNull;
import com.code_intelligence.jazzer.mutation.annotation.ValuePool;
import com.code_intelligence.jazzer.mutation.annotation.WithLength;
import com.code_intelligence.jazzer.mutation.annotation.WithSize;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.apache.commons.compress.FuzzingHelpers.readAllFilesInDirectory;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class FuzzTests {

  static final int MAX_BYTES_TO_UNPACK = 10 * 1024 * 1024; // 10 MB
  static final int MAX_BYTES_TO_PACK = 1000000;
  static final int MAX_NUM_ARCHIVE_ENTRIES = 100;
  private final String TMP_DIR_PREFIX = "fuzz-zip-";

  static Stream<?> compressionFormats() {
    return Stream.of(
          CompressorStreamFactory.GZIP,
          CompressorStreamFactory.BZIP2,
          CompressorStreamFactory.XZ,
          CompressorStreamFactory.LZMA,
          CompressorStreamFactory.SNAPPY_FRAMED,
          CompressorStreamFactory.LZ4_BLOCK,
          CompressorStreamFactory.LZ4_FRAMED,
          CompressorStreamFactory.DEFLATE,
          CompressorStreamFactory.PACK200,
          CompressorStreamFactory.SNAPPY_RAW,
          CompressorStreamFactory.BROTLI,
          CompressorStreamFactory.Z,
          CompressorStreamFactory.ZSTANDARD,
          CompressorStreamFactory.DEFLATE64
    );
  }

  static Stream<?> compressedData() {
    String resourcesDir = Objects.requireNonNull(FuzzTests.class.getResource("/")).getPath();
    return readAllFilesInDirectory(Paths.get(resourcesDir));
  }

  static Stream<?> passwords() {
    return Stream.of(
          "".getBytes(),
          "password".getBytes(),
          "123456".getBytes(),
          "letmein".getBytes(),
          "qwerty".getBytes(),
          "abc123".getBytes(),
          "trustno1".getBytes(),
          "iloveyou".getBytes(),
          "admin".getBytes(),
          "welcome".getBytes()
    );
  }

  private ZipArchiveOutputStream.UnicodeExtraFieldPolicy mapPolicy(CopyUnicodeExtraFieldPolicy policy) {
    switch (policy) {
      case ALWAYS:
        return ZipArchiveOutputStream.UnicodeExtraFieldPolicy.ALWAYS;
      case NEVER:
        return ZipArchiveOutputStream.UnicodeExtraFieldPolicy.NEVER;
      case NOT_ENCODEABLE:
        return ZipArchiveOutputStream.UnicodeExtraFieldPolicy.NOT_ENCODEABLE;
      default:
        throw new IllegalArgumentException("Unknown policy: " + policy);
    }
  }

  @FuzzTest(maxDuration = "0m", maxExecutions = 0)
  public void compressDecompressRoundtrip(byte @NotNull @WithLength(max = MAX_BYTES_TO_PACK) [] data,
                                          @ValuePool({"compressionFormats"}) @NotNull String format) {
    byte[] compressed = compress(data, format);
    if (compressed != null && compressed.length > 0 && data.length > 0) {
      byte[] decompressed = decompress(compressed, format);
      if (decompressed == null) {
        return;
      }
      if (!format.equalsIgnoreCase(CompressorStreamFactory.PACK200)) {
        assertArrayEquals(data, decompressed, "Decompressed data does not match original data for format: " + format);
      }
    }
  }

  @FuzzTest(maxDuration = "0m", maxExecutions = 0)
  @ValuePool({"compressionFormats", "compressedData"})
  public void decompressRoundtrip(byte @NotNull @WithLength(max = MAX_BYTES_TO_UNPACK) [] data,
                                  @NotNull String format) {
    byte[] decompressed = decompress(data, format);
    if (decompressed == null) {
      return;
    }

    byte[] recompressed = compress(decompressed, format);
    if (recompressed == null) {
      return;
    }

    byte[] redecompressed = decompress(recompressed, format);
    if (redecompressed == null) {
      return;
    }

    if (!format.equalsIgnoreCase(CompressorStreamFactory.PACK200)) {
      assertArrayEquals(decompressed, redecompressed, "Re-decompressed data does not match original decompressed data for format: " + format);
    }
  }

  @FuzzTest(maxDuration = "0m", maxExecutions = 0)
  @ValuePool({"compressionFormats"})
  public void archiveOnly(byte @NotNull @WithLength(max = MAX_BYTES_TO_PACK) [] data,
                          @ValuePool("compressionFormats") @NotNull String format,
                          @NotNull @WithSize(max = MAX_NUM_ARCHIVE_ENTRIES) List<@NotNull String> archiveEntries) {
    byte[] archived = archiveData(data, archiveEntries, format);
    if (archived != null && data.length > 0) {
      // TODO : make sure we get the same data back after unarchive
      // byte[] extracted = extractArchive(archived, format);
    }
  }

  @FuzzTest(maxDuration = "0m", maxExecutions = 0)
  @ValuePool({"compressionFormats", "compressedData"})
  public void dearchiveOnly(byte @NotNull @WithLength(max = MAX_BYTES_TO_UNPACK) [] data,
                            @NotNull String format) {
    extractArchive(data, format);
  }

  @FuzzTest(maxDuration = "0m", maxExecutions = 0)
  public void compressSevenzRoundtrip(byte @NotNull @WithLength(max = MAX_BYTES_TO_PACK) [] data,
                                      @NotNull @WithSize(max = MAX_NUM_ARCHIVE_ENTRIES) List<@NotNull String> archiveEntries,
                                      byte @WithLength(max = 20) @ValuePool("passwords") [] password
  ) {
    byte[] sevenzCompressed = SevenZipCompress(data, password, archiveEntries);
    if (sevenzCompressed != null && sevenzCompressed.length > 0 && data.length > 0) {
      // TODO : properly decompress and verify
      byte[] sevenzDecompressed = SevenZipDecompress(sevenzCompressed, password);
      // assertArrayEquals(data, sevenzDecompressed, "7z Decompressed data does not match original data.");
    }
  }

  @FuzzTest(maxDuration = "0m", maxExecutions = 0)
  public void decompressSevenz(byte @NotNull @WithLength(max = MAX_BYTES_TO_UNPACK) @ValuePool("compressedData") [] data,
                               byte @WithLength(max = 20) @ValuePool("passwords") [] password) {
    SevenZipDecompress(data, password);
  }

  @FuzzTest(maxDuration = "0m", maxExecutions = 0)
  @ValuePool("compressedData")
  public void zipFile(byte @NotNull @WithLength(max = MAX_BYTES_TO_UNPACK) [] data,
                      @NotNull String zipFileEncoding,
                      boolean useUnicodeExtraFields,
                      boolean ignoreLocalFileHeader) {
    unarchiveZipFile(data, zipFileEncoding, useUnicodeExtraFields, ignoreLocalFileHeader);
  }

  @FuzzTest(maxDuration = "0m", maxExecutions = 0)
  @ValuePool("compressedData")
  public void zipUnarchiveInputStream(byte @NotNull @WithLength(max = MAX_BYTES_TO_UNPACK) [] data,
                                      // force use of mutation framework by adding an extra parameter
                                      // we want WithLength annotation to be respected (it's not when using libFuzzer directly)
                                      boolean ignore
  ) {
    unarchiveZipInputStream(data);
  }

  @FuzzTest(maxDuration = "0m", maxExecutions = 0)
  public void zipArchiveOutputStream(byte @NotNull @WithLength(max = MAX_BYTES_TO_PACK) [] data,
                                     boolean splitZip,
                                     int zipSplitSize,
                                     @NotNull @WithSize(max = MAX_NUM_ARCHIVE_ENTRIES) List<@NotNull String> archiveEntries,
                                     long zipCrc,
                                     @NotNull CopyUnicodeExtraFieldPolicy unicodeExtraFieldPolicy) {
    archiveZipOutputStream(data, splitZip, zipSplitSize, archiveEntries, zipCrc, mapPolicy(unicodeExtraFieldPolicy));
  }

  @FuzzTest(maxDuration = "0m", maxExecutions = 0)
  @ValuePool("compressedData")
  public void tarFileUnarchive(byte @NotNull @WithLength(max = MAX_BYTES_TO_UNPACK) [] data,
                               boolean force_mutation_framework) {
    unarchiveTarFile(data);
  }

  @FuzzTest(maxDuration = "0m", maxExecutions = 0)
  @ValuePool("compressedData")
  public void tarInputStreamUnarchive(byte @NotNull @WithLength(max = MAX_BYTES_TO_UNPACK) [] data,
                                      boolean force_mutation_framework) {
    unarchiveTarInputStream(data);
  }

  @FuzzTest(maxDuration = "0m", maxExecutions = 0)
  public void tarOutputStreamArchive(byte @NotNull @WithLength(max = MAX_BYTES_TO_PACK) [] data,
                                     @NotNull @WithSize(max = MAX_NUM_ARCHIVE_ENTRIES) List<@NotNull String> archiveEntries,
                                     boolean addPaxHeadersForNonAsciiNames,
                                     int bigNumberMode,
                                     int longFileMode) {
    archiveTarOutputStream(data, archiveEntries, addPaxHeadersForNonAsciiNames, bigNumberMode, longFileMode);
  }

  public byte[] compress(byte[] data, String format) {
    // Compress
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (CompressorOutputStream<? extends OutputStream> cos = new CompressorStreamFactory()
          .createCompressorOutputStream(format, baos)) {
      cos.write(data);
      cos.finish();
      cos.flush();
      return baos.toByteArray();
    } catch (CompressorException e) {
      // ignore
    } catch (IOException e) {
      // ignore
    } catch (IllegalArgumentException e) {
      // ignore
    }
    return null;
  }


  public byte[] decompress(byte[] data, String format) {
    if (data == null) {
      return null;
    }
    if (data.length == 0) {
      return new byte[0];
    }

    try (CompressorInputStream cis = new CompressorStreamFactory()
          .createCompressorInputStream(format, new BufferedInputStream(new ByteArrayInputStream(data)))) {
      ByteArrayOutputStream result = new ByteArrayOutputStream();
      byte[] buffer = new byte[8192];
      int n;
      while ((n = cis.read(buffer)) != -1) {
        result.write(buffer, 0, n);
      }
      return result.toByteArray();
    } catch (CompressorException e) {
      // ignore
    } catch (IOException e) {
      // ignore 1
    } catch (IllegalArgumentException t) {
      // ignore 2
    }
    return null;
  }

  public byte[] extractArchive(byte[] archived, String format) {
    if (archived == null) {
      return null;
    }
    if (archived.length == 0) {
      return new byte[0];
    }
    try (ArchiveInputStream<? extends ArchiveEntry> ais = new ArchiveStreamFactory()
          .createArchiveInputStream(format, new BufferedInputStream(new ByteArrayInputStream(archived)))) {
      ArchiveEntry entry;
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      while ((entry = ais.getNextEntry()) != null) {
        if (!ais.canReadEntryData(entry)) {
          continue;
        }
        // try to stat the entry
        File f = new File(entry.getName());
        long size = entry.getSize();
        Files.getLastModifiedTime(f.toPath());

        byte[] buffer = new byte[8192];
        int bytesRead;

        while ((bytesRead = ais.read(buffer)) != -1) {
          baos.write(buffer, 0, bytesRead);
        }
      }
      return baos.toByteArray();
    } catch (ArchiveException e) {

    } catch (IOException e) {

    } catch (IllegalArgumentException t) {

    } catch (ArrayIndexOutOfBoundsException t) {
      throw t;
    } catch (NullPointerException t) {
      throw t;
    }

    return null;
  }

  public byte[] archiveData(byte[] data, List<String> archiveEntries, String format) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ArchiveOutputStream aos = new ArchiveStreamFactory()
          .createArchiveOutputStream(format, baos)) {
      byte[] extraData = Arrays.copyOf(data, Math.min(data.length, 70001));
      // Add an entry to the archive
      for (String entry : archiveEntries) {
        ArchiveEntry archiveEntry = aos.createArchiveEntry(new File(entry), entry);
        aos.putArchiveEntry(archiveEntry);
        aos.write(extraData);
        aos.closeArchiveEntry();
      }
      aos.finish();
      aos.flush();
      return baos.toByteArray();
    } catch (ArchiveException e) {
      // ignore
    } catch (IOException e) {
      // ignore
    } catch (IllegalArgumentException e) {
      // ignore
    }
    return null;
  }

  /**
   * 7z is a separate compressor in commons-compress and has to be used separately
   */
  public byte[] SevenZipDecompress(byte[] data, byte[] password) {
    SeekableInMemoryByteChannel inMemoryByteChannel = new SeekableInMemoryByteChannel(data);
    try {
      SevenZFile sevenZFile = new SevenZFile(inMemoryByteChannel, password);
      while (true) {
        SevenZArchiveEntry entry = sevenZFile.getNextEntry();
        if (entry == null) {
          break;
        }
        byte[] content = new byte[(int) entry.getSize()];
        int offset = 0;
        while (offset < content.length) {
          int bytesRead = sevenZFile.read(content, offset, content.length - offset);
          if (bytesRead < 0) {
            break;
          }
          offset += bytesRead;
        }
      }
    } catch (IOException e) {
      // ignore
    } catch (ArrayIndexOutOfBoundsException t) {
      throw t;
    }
    return inMemoryByteChannel.array();
  }

  public byte[] SevenZipCompress(byte[] data, byte[] password, List<String> archiveEntries) {
    SeekableInMemoryByteChannel inMemoryByteChannel = new SeekableInMemoryByteChannel(data);
    try {
      SevenZOutputFile sevenZOutput = new SevenZOutputFile(inMemoryByteChannel);
      byte[] extraData = Arrays.copyOf(data, Math.min(data.length, 70001));
      for (String archiveEntry : archiveEntries) {
        SevenZArchiveEntry entry = sevenZOutput.createArchiveEntry(new File(archiveEntry), archiveEntry);
        sevenZOutput.putArchiveEntry(entry);
        sevenZOutput.write(extraData);
        sevenZOutput.closeArchiveEntry();
      }
      sevenZOutput.close();
      return inMemoryByteChannel.array();
    } catch (IOException e) {
      // ignore
    } catch (InvalidPathException e) {
      // ignore
    } catch (IllegalArgumentException e) {
      // ignore
    }

    return null;
  }

  public byte[] unarchiveZipFile(byte[] data, String encoding, boolean useUnicodeExtraFields, boolean ignoreLocalFileHeader) {
    SeekableInMemoryByteChannel inMemoryByteChannel = new SeekableInMemoryByteChannel(data);
    try {
      ZipFile zipFile = ZipFile.builder()
            .setCharset(encoding)
            .setChannel(inMemoryByteChannel)
            .setUseUnicodeExtraFields(useUnicodeExtraFields)
            .setIgnoreLocalFileHeader(ignoreLocalFileHeader)
            .get();
      //ZipFile zipFile = new ZipFile(inMemoryByteChannel, encoding);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      for (ZipArchiveEntry zipEntry : zipFile.entries()) {
        byte[] buffer = new byte[8192];
        try (BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(zipEntry))) {
          int bytesRead;
          while ((bytesRead = bis.read(buffer)) != -1) {
            // process buffer data
            baos.write(buffer, 0, bytesRead);
          }
        }
      }
      return baos.toByteArray();
    } catch (IOException e) {
      // ignore
    } catch (IllegalArgumentException e) {
      // ignore
    }
    return null;
  }

  public byte[] unarchiveZipInputStream(byte[] data) {
    if (data == null) {
      return null;
    }
    if (data.length == 0) {
      return new byte[0];
    }
    try (ZipArchiveInputStream ais = new ZipArchiveInputStream(new BufferedInputStream(new ByteArrayInputStream(data)))) {
      ArchiveEntry entry;
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      while ((entry = ais.getNextEntry()) != null) {
        if (!ais.canReadEntryData(entry)) {
          continue;
        }
        // try to stat the entry
        File f = new File(entry.getName());
        long size = entry.getSize();
        Files.getLastModifiedTime(f.toPath());

        byte[] buffer = new byte[8192];
        int bytesRead;

        while ((bytesRead = ais.read(buffer)) != -1) {
          baos.write(buffer, 0, bytesRead);
        }
      }
      return baos.toByteArray();
    } catch (ArchiveException e) {

    } catch (IOException e) {

    } catch (IllegalArgumentException t) {

    } catch (ArrayIndexOutOfBoundsException t) {
      throw t;
    } catch (NullPointerException t) {
      throw t;
    }

    return null;
  }

  public byte[] archiveZipOutputStream(byte[] data, boolean splitZip, int zipSplitSize, List<String> archiveEntries,
                                       long crc, ZipArchiveOutputStream.UnicodeExtraFieldPolicy unicodeExtraFieldPolicy) {
    try {
      if (splitZip) {
        Path tempFile = Files.createTempFile(TMP_DIR_PREFIX, ".zip");
        try {
          try (ZipArchiveOutputStream aos = new ZipArchiveOutputStream(tempFile.toFile(), zipSplitSize)) {
            writeZipEntries(data, archiveEntries, aos, crc, unicodeExtraFieldPolicy);
            aos.finish();
          }
          return Files.readAllBytes(tempFile);
        } finally {
          Arrays.stream(Objects.requireNonNull(tempFile.getParent().toFile().listFiles((dir, name) -> name.startsWith(TMP_DIR_PREFIX))))
                .forEach(
                      f -> {
                        try {
                          Files.deleteIfExists(f.toPath());
                        } catch (IOException e) {
                          // ignore
                        }
                      }
                );
          Files.deleteIfExists(tempFile);
        }
      } else {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipArchiveOutputStream aos = new ZipArchiveOutputStream(baos)) {
          writeZipEntries(data, archiveEntries, aos, crc, unicodeExtraFieldPolicy);
          aos.finish();
          return baos.toByteArray();
        }
      }
    } catch (IOException e) {
      // Handle archiving errors
    } catch (IllegalArgumentException e) {
      // Handle archiving errors
    }
    return null;
  }

  private void writeZipEntries(byte[] data, List<String> archiveEntries, ZipArchiveOutputStream aos,
                               long crc, ZipArchiveOutputStream.UnicodeExtraFieldPolicy unicodeExtraFieldPolicy) throws IOException {
    byte[] extraData = Arrays.copyOf(data, Math.min(data.length, 70001));
    // Add an entry to the archive
    for (String entry : archiveEntries) {
      ZipArchiveEntry archiveEntry = aos.createArchiveEntry(new File(entry), entry);
      aos.putArchiveEntry(archiveEntry);
      aos.write(extraData);
      aos.setCreateUnicodeExtraFields(unicodeExtraFieldPolicy);
      aos.closeArchiveEntry();
      archiveEntry.setCrc(crc);
      archiveEntry.setMethod(ZipArchiveEntry.STORED);
      aos.addRawArchiveEntry(archiveEntry, new ByteArrayInputStream(extraData));
    }

  }

  public byte[] unarchiveTarFile(byte[] data) {
    SeekableInMemoryByteChannel inMemoryByteChannel = new SeekableInMemoryByteChannel(data);
    try (TarFile tarFile = new TarFile(inMemoryByteChannel)) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      for (TarArchiveEntry tarEntry : tarFile.getEntries()) {
        byte[] buffer = new byte[8192];
        try (BufferedInputStream bis = new BufferedInputStream(tarFile.getInputStream(tarEntry))) {
          int bytesRead;
          while ((bytesRead = bis.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
          }
        }
      }
      return baos.toByteArray();
    } catch (IOException e) {
      // ignore
    } catch (IllegalArgumentException e) {
      // ignore
    }
    return null;
  }

  public byte[] unarchiveTarInputStream(byte[] data) {
    if (data == null) {
      return null;
    }
    if (data.length == 0) {
      return new byte[0];
    }

    try (TarArchiveInputStream ais = new TarArchiveInputStream(new BufferedInputStream(new ByteArrayInputStream(data)))) {
      TarArchiveEntry entry;
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      while ((entry = ais.getNextEntry()) != null) {
        if (!ais.canReadEntryData(entry)) {
          continue;
        }
        // try to stat the entry
        File f = new File(entry.getName());
        long size = entry.getSize();
        Files.getLastModifiedTime(f.toPath());

        byte[] buffer = new byte[8192];
        int bytesRead;

        while ((bytesRead = ais.read(buffer)) != -1) {
          baos.write(buffer, 0, bytesRead);
        }
      }
      return baos.toByteArray();
    } catch (IOException e) {

    } catch (IllegalArgumentException t) {

    }
    return null;
  }

  public byte[] archiveTarOutputStream(byte[] data, List<String> archiveEntries, boolean addPaxHeadersForNonAsciiNames, int bigNumberMode, int longFileMode) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (TarArchiveOutputStream aos = new TarArchiveOutputStream(baos)) {
      aos.setAddPaxHeadersForNonAsciiNames(addPaxHeadersForNonAsciiNames);
      aos.setBigNumberMode(bigNumberMode);
      aos.setLongFileMode(longFileMode);
      byte[] extraData = Arrays.copyOf(data, Math.min(data.length, 70001));
      // Add an entry to the archive
      for (String entry : archiveEntries) {
        TarArchiveEntry archiveEntry = aos.createArchiveEntry(new File(entry), entry);
        archiveEntry.isGlobalPaxHeader();
        aos.putArchiveEntry(archiveEntry);
        aos.write(extraData);
        aos.closeArchiveEntry();
      }
      aos.finish();
      aos.flush();
      return baos.toByteArray();
    } catch (IOException e) {
      // ignore
    } catch (IllegalArgumentException e) {
      // ignore
    }
    return null;
  }

  public enum CopyUnicodeExtraFieldPolicy {
    ALWAYS,
    NEVER,
    NOT_ENCODEABLE
  }
}

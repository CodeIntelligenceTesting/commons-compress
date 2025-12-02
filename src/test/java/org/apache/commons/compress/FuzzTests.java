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

import com.code_intelligence.jazzer.api.BugDetectors;
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
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntryPredicate;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;

import com.code_intelligence.jazzer.junit.FuzzTest;
import com.code_intelligence.jazzer.mutation.annotation.NotNull;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class FuzzTests {

  static {
    BugDetectors.setFilePathTraversalTarget(
          () -> Paths.get("../", "etc"));
  }

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

  final ZipArchiveEntryPredicate allFilesPredicate = zipArchiveEntry -> true;

  static Stream<?> compressedData() {
    String[] subdirs = {"bz2", "lz4", "xz", "gzip", "rar", "zip", "lrzip", "lzma"};
    String baseDir = "/home/peter/Documents/Programming/fuzzing/fuzzing-corpus";
    String resourcesDir = "/home/peter/Documents/Programming/fuzzing/commons-compress/src/test/resources";
    return Stream.concat(
                Arrays.stream(subdirs).map(
                      subdir -> Paths.get(baseDir, subdir)),
                Stream.of(Paths.get(resourcesDir)))
          .flatMap(FuzzTests::readAllFilesInDirectory);
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

  public enum ArchiveFormats {
    COMP_ROUNDTRIP,
    DECOMP_ROUNDTRIP,
    ARCHIVE_ONLY,
    DEARCHIVE_ONLY,
    COMP_SEVENZ_ROUNDTRIP,
    DECOMP_SEVENZ_ROUNDTRIP,
    ZIP_FILE,
    ZIP_ARCHIVE_INPUT_STREAM,
    ZIP_ARCHIVE_OUTPUT_STREAM
  }

  static final boolean CHECK_EQUALS_ARCHIVE = false;

  @FuzzTest(maxDuration = "0m", maxExecutions = 0)
  //@ValuePool(value={"compressionFormats", "compressedData", "passwords"})
   public void apacheCommonsCompress(byte @NotNull @WithLength(max = 100000)
                                             @ValuePool("compressedData")
                                             [] data,
                                     @ValuePool("compressionFormats")
                                     @NotNull String format,
                                     @NotNull @WithSize(max = 10) List<@NotNull String> archiveEntries,
                                     byte @WithLength(max = 20) // @ValuePool("passwords")
                                             [] password,
                                     @NotNull ArchiveFormats op,
                                     @NotNull String zipFileEncoding,
                                     boolean ignored
  ) {
//    if (op != ArchiveFormats.ZIP_FILE &&
//          op != ArchiveFormats.ZIP_ARCHIVE_INPUT_STREAM &&
//          op != ArchiveFormats.ZIP_ARCHIVE_OUTPUT_STREAM) {
//      return;
//    }

//    if (op != ArchiveFormats.DECOMP_ROUNDTRIP
//          || !format.equalsIgnoreCase(CompressorStreamFactory.PACK200)
//    ) {
//      return;
//    }

    switch (op) {
      case COMP_ROUNDTRIP:
        byte [] compressed = compress(data, format);
        if (compressed != null && compressed.length > 0 && data.length > 0) {
          byte [] decompressed = decompress(compressed, format);
          if (decompressed == null) {
            return;
          }
          if (CHECK_EQUALS_ARCHIVE) {
            if (!format.equalsIgnoreCase(CompressorStreamFactory.PACK200)) {
              assertArrayEquals(data, decompressed, "Decompressed data does not match original data for format: " + format);
            }
          }
        }
        break;
      case DECOMP_ROUNDTRIP:
        byte [] decompressed = decompress(data, format);
        if (decompressed == null) {
          return;
        }

//        byte [] recompressed = compress(decompressed, format);
//        if (recompressed == null) {
//          return;
//        }
//
//        byte[] redecompressed = decompress(recompressed, format);
//        if (redecompressed == null) {
//          return;
//        }
//
//        if (CHECK_EQUALS_ARCHIVE) {
//          if (!format.equalsIgnoreCase(CompressorStreamFactory.PACK200)) {
//            assertArrayEquals(decompressed, redecompressed, "Re-decompressed data does not match original decompressed data for format: " + format);
//          }
//        }

        break;
      case ARCHIVE_ONLY:
        byte[] archived = archiveData(data, archiveEntries, format);
        if (archived != null && data.length > 0) {
          byte [] extracted = extractArchive(archived, format);
          //assertArrayEquals(data, extracted, "Extracted data does not match original data for archive format: " + archiveFormat);
        }
        break;
      case DEARCHIVE_ONLY:
        byte[] extracted = extractArchive(data, format);
//        if (extracted != null && data.length > 0) {
//          byte [] rearchived = archiveData(extracted, archiveEntries, archiveFormat);
//          assertArrayEquals(data, rearchived, "Rearchived data does not match original data for archive format: " + archiveFormat);
//        }
        break;
      case COMP_SEVENZ_ROUNDTRIP:
        byte [] sevenzCompressed = SevenZipCompress(data, password, archiveEntries);
        if (sevenzCompressed != null && sevenzCompressed.length > 0 && data.length > 0) {
          byte [] sevenzDecompressed = SevenZipDecompress(sevenzCompressed, password);
          //assertArrayEquals(data, sevenzDecompressed, "7z Decompressed data does not match original data.");
        }
        break;
      case DECOMP_SEVENZ_ROUNDTRIP:
        byte [] sevenzDecompressed = SevenZipDecompress(data, password);
        if (sevenzDecompressed != null && data.length > 0) {
          byte [] sevenzRecompressed = SevenZipCompress(sevenzDecompressed, password, archiveEntries);
//          assertArrayEquals(data, sevenzRecompressed, "7z Recompressed data does not match original data.");
        }
        break;
      case ZIP_FILE:
        unarchiveZipFile(data, zipFileEncoding);
        break;
      case ZIP_ARCHIVE_INPUT_STREAM:
        unarchiveZipInputStream(data);
        break;
      case ZIP_ARCHIVE_OUTPUT_STREAM:
        archiveZipOutputStream(data, archiveEntries);
      default:
        break;
    }
//
////    byte [] compressed = compress(data, format);
////    if (compressed.length > 0 && data.length > 0) {
////      byte [] decompressed = decompress(compressed, format);
////      if (!format.equalsIgnoreCase(CompressorStreamFactory.PACK200)) {
////        Assertions.assertArrayEquals(data, decompressed, "Decompressed data does not match original data for format: " + format);
////      }
////      byte [] decompressed1 = decompress(compressed, uncompressionFormat);
////    }
////    SevenZipCompress(data, password);
////    byte [] decompressed2 = decompress(data, uncompressionFormat);
//    byte[] archived = extractArchive(data, archiveFormat);
//    SevenZipCompress(data, password, archiveEntries);
////    archiveData(data, archiveEntries, archiveFormat);
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
    } catch (Throwable t) {
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
    } catch (Error e) {
      if (e.getMessage() == null || !e.getMessage().equals("Got a pack200 exception. What to do?")) {
        throw e;
      }
    }
//    catch (ArrayIndexOutOfBoundsException t) {
//
//    } catch (NullPointerException t) {
//
//    }
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
      byte[] extraData = Arrays.copyOf(data, Math.min(data.length, 10000));
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
    } catch (Throwable e) {
      // Handle archiving errors
    }
    return null;
  }

  /**
   * 7z is a separate compressor in commons-compress and has to be used separately
   *
   * @param data
   * @return
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
      // ignore
    }
    return inMemoryByteChannel.array();
  }

  public byte[] SevenZipCompress(byte[] data, byte[] password, List<String> archiveEntries) {
    SeekableInMemoryByteChannel inMemoryByteChannel = new SeekableInMemoryByteChannel(data);
    try {
      SevenZOutputFile sevenZOutput = new SevenZOutputFile(inMemoryByteChannel);
      byte[] extraData = Arrays.copyOf(data, Math.min(data.length, 10000));
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
    }

    return null;
  }

  public byte[] unarchiveZipFile(byte[] data, String encoding) {
    SeekableInMemoryByteChannel inMemoryByteChannel = new SeekableInMemoryByteChannel(data);
    try {
      ZipFile zipFile = new ZipFile(inMemoryByteChannel, encoding);
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
    } catch (Exception e) {
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

  public byte[] archiveZipOutputStream(byte[] data, List<String> archiveEntries) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ZipArchiveOutputStream aos =  new ZipArchiveOutputStream(baos)) {
      byte[] extraData = Arrays.copyOf(data, Math.min(data.length, 10000));
      // Add an entry to the archive
      for (String entry : archiveEntries) {
        ZipArchiveEntry archiveEntry = aos.createArchiveEntry(new File(entry), entry);
        aos.putArchiveEntry(archiveEntry);
        aos.write(extraData);
        aos.closeArchiveEntry();
        aos.addRawArchiveEntry(archiveEntry, new ByteArrayInputStream(extraData));

      }
      aos.finish();
      aos.flush();
      return baos.toByteArray();
    } catch (Throwable e) {
      // Handle archiving errors
    }
    return null;
  }

  // traverse directory, and read all files and return a stream of Strings
  public static Stream<?> readAllFilesInDirectory(Path path) {
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

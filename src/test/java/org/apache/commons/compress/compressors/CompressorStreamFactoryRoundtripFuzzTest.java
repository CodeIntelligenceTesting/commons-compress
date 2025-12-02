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
package org.apache.commons.compress.compressors;

import com.code_intelligence.jazzer.junit.FuzzTest;
import com.code_intelligence.jazzer.mutation.annotation.InRange;
import com.code_intelligence.jazzer.mutation.annotation.NotNull;
import com.code_intelligence.jazzer.mutation.annotation.ValuePool;
import org.apache.commons.compress.FuzzingHelpers;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CompressorStreamFactoryRoundtripFuzzTest {
    private static final String[] COMPRESSOR_TYPES = {
//            CompressorStreamFactory.BROTLI,
        CompressorStreamFactory.BZIP2,
        CompressorStreamFactory.DEFLATE,
        CompressorStreamFactory.DEFLATE64,
        CompressorStreamFactory.GZIP,
//            CompressorStreamFactory.PACK200,
        CompressorStreamFactory.LZ4_FRAMED,
        CompressorStreamFactory.LZ4_BLOCK,
//            CompressorStreamFactory.LZMA,
//            CompressorStreamFactory.SNAPPY_FRAMED,
//            CompressorStreamFactory.SNAPPY_RAW,
//            CompressorStreamFactory.Z,
//            CompressorStreamFactory.ZSTANDARD,
        CompressorStreamFactory.XZ
    };

    static Stream<?> compressedData() {
        return Stream.of(Paths.get("src", "test",  "resources"))
                .flatMap(FuzzingHelpers::readAllFilesInDirectory);
    }

    @FuzzTest(maxDuration = "30m")
    public void fuzzCompressors(@InRange(min = 0, max = 6) int compressor, byte @NotNull @ValuePool("compressedData") [] data) {
        String compressorType = COMPRESSOR_TYPES[compressor];
        CompressorStreamProvider factory = new CompressorStreamFactory();
        ByteArrayOutputStream compressedOs = new ByteArrayOutputStream();
        try (CompressorOutputStream<?> compressorOutputStream = factory.createCompressorOutputStream(compressorType, compressedOs)) {
            compressorOutputStream.write(data);
            compressorOutputStream.flush();

            ByteArrayInputStream is = new ByteArrayInputStream(compressedOs.toByteArray());
            CompressorInputStream compressorInputStream = factory.createCompressorInputStream(compressorType, is, false);
            ByteArrayOutputStream decompressedOs = new ByteArrayOutputStream();
            IOUtils.copy(compressorInputStream, decompressedOs);
            compressorInputStream.close();
            decompressedOs.flush();
            decompressedOs.close();
            assertEquals(data, decompressedOs.toByteArray());

        } catch (IOException ignored) {
        }
    }
}

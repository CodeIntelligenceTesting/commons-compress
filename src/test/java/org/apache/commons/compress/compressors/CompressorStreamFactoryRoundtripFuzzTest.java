package org.apache.commons.compress.compressors;

import com.code_intelligence.jazzer.junit.FuzzTest;
import com.code_intelligence.jazzer.mutation.annotation.InRange;
import com.code_intelligence.jazzer.mutation.annotation.NotNull;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

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

    @FuzzTest(maxDuration = "30m")
    public void fuzzCompressors(@InRange(min = 0, max = 6) int compressor, byte @NotNull [] data) {
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

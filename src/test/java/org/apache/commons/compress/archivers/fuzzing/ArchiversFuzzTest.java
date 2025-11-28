package org.apache.commons.compress.archivers.fuzzing;

import com.code_intelligence.jazzer.junit.FuzzTest;
import com.code_intelligence.jazzer.mutation.annotation.InRange;
import com.code_intelligence.jazzer.mutation.annotation.NotNull;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.junit.jupiter.api.BeforeAll;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.LogManager;


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

    @BeforeAll
    public static void setup() {
        LogManager.getLogManager().reset();
    }

    @FuzzTest
    public void fuzzArchivers(@InRange(min = 0, max = 11) int archive, byte @NotNull [] data) {
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

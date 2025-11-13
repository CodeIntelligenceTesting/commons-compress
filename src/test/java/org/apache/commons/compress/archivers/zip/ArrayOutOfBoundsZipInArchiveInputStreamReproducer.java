package org.apache.commons.compress.archivers.zip;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ArrayOutOfBoundsZipInArchiveInputStreamReproducer {

    @Test
    public void fuzzerTestOneInput() {
        byte[] data = new byte[] {80, 75, 3, 4, 19, 7, 0, 1, 1, 0, -1, 1, 120, 8, 84, -99, 3, 48, 45, 1, 119, -70, 110, 61, 65, 104, 0, 0, 0, 0, 59, -4, -1, -1, -1, -1, -33, 0, -1, 0, 5, 0, -1, -1, -1, -1, -1, -1, -1, 0, 122};
        try {
            ZipArchiveInputStream is = new ZipArchiveInputStream(new ByteArrayInputStream(data));
            ArchiveEntry entry;
            while ((entry = is.getNextEntry()) != null) {
                is.read(new byte[1024]);
            }
            is.close();
        } catch (IOException ignored) {
        }
    }
}

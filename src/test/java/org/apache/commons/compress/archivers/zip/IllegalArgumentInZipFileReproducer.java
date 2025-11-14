package org.apache.commons.compress.archivers.zip;

import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

public class IllegalArgumentInZipFileReproducer {

    @Test
    public void illegalArgumentReproducer() {
        byte[] data = new byte[] {80, 75, 5, 6, -127, 80, 75, 5, 6, 7, -127, -127, -127, 80, 74, 7, 8, -127, -127, -127, -127, -127};
        try {
            ZipFile zf =  ZipFile.builder().setSeekableByteChannel(new SeekableInMemoryByteChannel(data)).get();
            Enumeration<? extends ZipArchiveEntry> entries = zf.getEntries();
            while(entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                InputStream is = zf.getInputStream(entry);
                is.read(new byte[1024]);
            }
            zf.close();
        } catch (IOException ignored) {
        }
    }
}

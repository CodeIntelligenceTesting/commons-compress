package org.apache.commons.compress.archivers.ar;

import com.code_intelligence.jazzer.mutation.annotation.InRange;
import com.code_intelligence.jazzer.mutation.annotation.NotNull;
import com.code_intelligence.jazzer.mutation.annotation.ValuePool;
import org.apache.commons.compress.FuzzingHelpers;
import org.apache.commons.compress.archivers.*;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArArchiveRoundtripMissmatchReproducer {
    private static class ArchiveEntryAndDataWrapper {
        private  final ArchiveEntry entry;
        private final byte[] data;

        private ArchiveEntryAndDataWrapper(ArchiveEntry entry, byte[] data) {
            this.entry = entry;
            this.data = data;
        }

        @Override
        public boolean equals(Object o) {
            try {
                if (o instanceof ArArchiveRoundtripMissmatchReproducer.ArchiveEntryAndDataWrapper) {
                    ArArchiveRoundtripMissmatchReproducer.ArchiveEntryAndDataWrapper other = (ArArchiveRoundtripMissmatchReproducer.ArchiveEntryAndDataWrapper) o;
                    return entry.equals(other.entry) && Arrays.equals(data, other.data);
                }
            } catch (ClassCastException e) {
                return false;
            }
            return false;
        }
    }

    @Test
    public void arArchiveRoundtripMissmatchReproducer() {
        byte[] data = {33, 60, 97, 114, 99, 104, 62, 10, 47, 47, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 54, 56, 32, 32, 32, 32, 32, 32, 32, 32, 96, 10, 116, 104, 105, 115, 95, 105, 115, 95, 97, 95, 108, 111, 110, 103, 95, 102, 105, 108, 101, 95, 110, 97, 109, 101, -55, -117, -121, -117, -48, -11, -117, -105, 105, 115, 95, 105, 115, 95, 97, 95, 108, 111, 110, 103, 95, 102, 105, 108, 101, 95, 110, 97, 109, 101, 95, 97, 115, 95, 119, 101, 108, 108, 46, 116, 120, -116, -48, -11, -41, 50, 57, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 32, 49, 52, 53, 52, 54, 57, 52, 48, 49, 54, 32, 32, 49, 48, 48, 48, 32, 32, 49, 48, 48, 48, 32, 32, 49, 48, 48, 54, 54, 52, 32, 32, 52, 32, 32, 32, 32, 32, 32, 32, 32, 32, 96, 10, 66, 121, 101, 10};
        ArchiveStreamFactory factory = new ArchiveStreamFactory();
        List<ArchiveEntryAndDataWrapper> decompList1 = new ArrayList<>();
        List<ArchiveEntryAndDataWrapper> decompList2 = new ArrayList<>();
        byte[] comp1 = new byte[0];
        String extractedArchiveType = "";

        try (ArchiveInputStream<? extends ArchiveEntry> in = factory.createArchiveInputStream(new ByteArrayInputStream(data))) {
            // First trying to understand what we are actually extracting here...
            ArchiveEntry entry = in.getNextEntry();
            if (entry != null ) {
                extractedArchiveType = FuzzingHelpers.getArchiveTypeFromArchiveEntryInstanceClassName(entry.getClass());
            }
            // ... then saving the entries to insert them for the next round.
            while ( entry != null) {
                decompList1.add(new ArchiveEntryAndDataWrapper(entry, IOUtils.toByteArray(in)));
                entry = in.getNextEntry();
            }
        } catch (IOException | IllegalArgumentException | IllegalStateException ignored) {
            return;
        }

        // Writing the extracted data back to an archive.
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ArchiveOutputStream<ArchiveEntry> out = factory.createArchiveOutputStream(extractedArchiveType, baos)) {
            for (ArchiveEntryAndDataWrapper decomp : decompList1) {
                out.putArchiveEntry(decomp.entry);
                out.write(decomp.data);
                out.closeArchiveEntry();
            }
            out.finish();
            baos.flush();
            comp1 = baos.toByteArray();

        } catch (IOException | IllegalArgumentException | IllegalStateException ignored) {
            return;
        }

        // Extracting the archive again.
        try (ArchiveInputStream<? extends ArchiveEntry> in = factory.createArchiveInputStream(new ByteArrayInputStream(comp1))) {
            for (ArchiveEntry entry = in.getNextEntry(); entry != null; entry = in.getNextEntry()) {
                decompList2.add(new ArchiveEntryAndDataWrapper(entry, IOUtils.toByteArray(in)));
            }
        } catch (IOException | IllegalArgumentException | IllegalStateException ignored) {
            return;
        }

        // Roundtrip to check that checks that decomp(comp(decomp(data))) == decomp(data)
        Assertions.assertEquals(decompList1, decompList2);
    }
}

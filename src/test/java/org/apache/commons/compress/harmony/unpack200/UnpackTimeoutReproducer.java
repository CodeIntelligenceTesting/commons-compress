package org.apache.commons.compress.harmony.unpack200;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.jar.JarOutputStream;

public class UnpackTimeoutReproducer {

    @Test
    void reproduceTimeoutUnpack() {
        final byte[] data = new byte[] {-54, -2, -48, 13, 7, -106, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 31, 0, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 78, 83, 73, -1, 7, 0, 0, 0, 0, 0, 0, 0, 86, 79, 76, 65, 73, 76, 69, 7, 7, 7, 7, 7, 7, 7, 7, 7, 0, -7, 7, 0, -7, 7, 7, 7, 7, 7, 7, -1, -1, -1, -1, 0, -4, 0, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, -1, 61, 7, 7, 7, 7, 7, 7, 7, 3, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, -1, -1, -1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 4, 4, 20, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 1, 0, 0, 1, 0, 0, 0, 4, 4, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7, 7, 7, 7, 7, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, -4, 0, 7, 7, 7, 7, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 42, 7, -106};

        try {
            try (ByteArrayInputStream in = new ByteArrayInputStream(data);
                 ByteArrayOutputStream out = new ByteArrayOutputStream();
                 JarOutputStream jarOut = new JarOutputStream(out)) {

                final Archive archive = new Archive(in, jarOut);

                try {
                    /*
                     * The timeout issue comes from a for-loop in org.apache.commons.compress.harmony.unpack200.NewAttributeBands$Replication.addToAttribute(NewAttributeBands.java:418)
                     * that gets its upper bound from unsanitized input data. The input data is read-in in the attrDefinitionBands.read(in); call in the high-level function
                     * org.apache.commons.compress.harmony.unpack200.Segment.readSegment(Segment.java:496), that calls the org.apache.commons.compress.harmony.unpack200.AttrDefinitionBands.read(AttrDefinitionBands.java:81) function.
                     * I'm unsure which line exactly reads the problematic value, but it is likely AttrDefinitionBands.java:85,
                     * that then gets transformed and saved as part of the attributeDefinitionMap in org.apache.commons.compress.harmony.unpack200.AttributeLayoutMap.add(AttributeLayoutMap:141).
                     */
                    archive.unpack();
                } catch (final IOException | RuntimeException e) {
                    // Expected for most inputs which are not valid Pack200 or compressed streams
                    // Swallow to let Jazzer focus on crashes (e.g., unchecked exceptions, verifier errors)
                }
            } catch (final IOException ignored) {
                // Ignore stream setup errors in fuzzing context
            }
        } catch (RuntimeException e) {
            // ignored
        }
    }
}

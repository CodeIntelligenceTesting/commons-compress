package org.apache.commons.compress;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FuzzingHelpers {

    // traverse directory, and read all files and return a stream of Strings
    public static Stream<byte[]> readAllFilesInDirectory(Path path) {
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

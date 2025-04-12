package com.example.ProcessExcelFile.FileService;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WriteRegistry {

    private final String outputDir;
    private final Map<Long, BufferedWriter> writerMap = new ConcurrentHashMap<>();

    public WriteRegistry(String outputDir) {
        this.outputDir = outputDir;
    }

    public synchronized void writeLines(Long origin, List<String> lines) throws IOException {
        BufferedWriter writer =
                writerMap.computeIfAbsent(
                        origin,
                        key -> {
                            try {
                                Path outputPath = Paths.get(outputDir, origin + ".txt");
                                return Files.newBufferedWriter(
                                        outputPath,
                                        StandardOpenOption.CREATE,
                                        StandardOpenOption.APPEND);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });

        String joined = String.join("\n", lines);
        writer.write(joined + "\n");
    }

    public void closeAll() {
        for (BufferedWriter writer : writerMap.values()) {
            try {
                writer.close();
            } catch (IOException e) {
                System.err.println("Error closing writer: " + e.getMessage());
            }
        }
    }
}

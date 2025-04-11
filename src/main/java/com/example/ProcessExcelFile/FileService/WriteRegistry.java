package com.example.ProcessExcelFile.FileService;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WriteRegistry {
    private final String outputDir;
    private final Map<Long, BufferedWriter> writers = new HashMap<>();

    public WriteRegistry(String outputDir) {
        this.outputDir = outputDir;
    }

    public synchronized void writeLines(Long origin, List<String> lines) {
        try {
            BufferedWriter writer = writers.computeIfAbsent(origin, key -> {
                try {
                    String fileName = Paths.get(outputDir, origin + ".txt").toString();
                    return new BufferedWriter(new FileWriter(fileName, true));
                } catch (IOException e) {
                    throw new RuntimeException("Failed to create writer", e);
                }
            });

            writer.write(String.join("\n", lines));
            writer.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeAll() {
        for (BufferedWriter writer : writers.values()) {
            try {
                writer.flush();
                writer.close();
            } catch (IOException ignored) {}
        }
    }
}
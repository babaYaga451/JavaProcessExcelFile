package com.example.ProcessExcelFile.FileService;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class WriteRegistry {
    private final Map<Long, BufferedWriter> writerMap = new ConcurrentHashMap<>();
    private final String outputDir;

    public WriteRegistry(String outputDir) {
        this.outputDir = outputDir;
    }

    public void writeLine(Long origin, String line) {
        BufferedWriter writer = writerMap.computeIfAbsent(origin, this::createWriterForOrigin);
        synchronized (writer) {
            try {
                writer.write(line);
                writer.newLine();
            } catch (IOException e) {
                throw new UncheckedIOException("❌ Failed to write line for origin: " + origin, e);
            }
        }
    }

    private BufferedWriter createWriterForOrigin(Long origin) {
        try {
            var path = Path.of(outputDir, "shipper_" + origin + ".csv");
            System.out.println("Creating writer for file: " + path);
            return Files.newBufferedWriter(
                    path, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("❌ Failed to create writer for origin: " + origin, e);
        }
    }

    public void closeAll() {
        for (BufferedWriter writer : writerMap.values()) {
            try {
                writer.flush(); // optional, but good to be explicit
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

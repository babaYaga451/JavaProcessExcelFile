package com.example.ProcessExcelFile.FileService;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
        BufferedWriter writer =
                writerMap.computeIfAbsent(
                        origin,
                        o -> {
                            try {
                                String filePath = outputDir + "/" + "shipper_" + o + ".csv";
                                System.out.println("Writing to file" + filePath);
                                return Files.newBufferedWriter(
                                        Paths.get(filePath),
                                        StandardOpenOption.CREATE,
                                        StandardOpenOption.APPEND);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });

        synchronized (writer) {
            try {
                writer.write(line);
                writer.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void closeAll() {
        for (BufferedWriter writer : writerMap.values()) {
            try {
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

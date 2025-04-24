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
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Stream;

public class WriteRegistry {

    private final String outputDirAutomation;
    private final String outputDirProship;

    private final Map<String, BufferedWriter> writerPipedMap = new ConcurrentHashMap<>();
    private final Map<String, BufferedWriter> writerCsvMap = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> pipedLocks = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> csvLocks = new ConcurrentHashMap<>();

    public WriteRegistry(String outputDirAutomation, String outputDirProship) {
        this.outputDirAutomation = outputDirAutomation;
        this.outputDirProship = outputDirProship;
    }

    public void writeLinesPiped(List<String> lines, List<String> shippers) throws IOException {
        writeLines(
                lines,
                shippers,
                writerPipedMap,
                pipedLocks,
                shipper -> Paths.get(outputDirAutomation, shipper + ".txt"));
    }

    public void writeLinesCsv(List<String> lines, List<String> shippers) throws IOException {
        writeLines(
                lines,
                shippers,
                writerCsvMap,
                csvLocks,
                shipper -> Paths.get(outputDirProship, shipper + ".csv"));
    }

    private void writeLines(
            List<String> lines,
            List<String> shippers,
            Map<String, BufferedWriter> writerMap,
            Map<String, ReentrantLock> lockMap,
            Function<String, Path> pathResolver)
            throws IOException {
        String joined = String.join("\n", lines) + "\n";

            for (String shipper : shippers) {
                ReentrantLock lock = lockMap.computeIfAbsent(shipper, s -> new ReentrantLock());
                lock.lock();
                try {
                    BufferedWriter writer =
                            writerMap.computeIfAbsent(
                                    shipper, key -> createWriter(pathResolver.apply(key)));
                    writer.write(joined);
                } finally {
                    lock.unlock();
                }
            }
    }

    private BufferedWriter createWriter(Path outputPath) {
        try {
            return Files.newBufferedWriter(
                    outputPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void closeAll() {
        Stream.concat(writerPipedMap.values().stream(), writerCsvMap.values().stream())
                .forEach(
                        writer -> {
                            try {
                                writer.close();
                            } catch (IOException e) {
                                System.err.println("Error closing writer: " + e.getMessage());
                            }
                        });
    }
}


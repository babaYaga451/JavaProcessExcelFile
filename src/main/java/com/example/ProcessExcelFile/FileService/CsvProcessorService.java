package com.example.ProcessExcelFile.FileService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class CsvProcessorService {

    @Value("${input.file}")
    private String inputFileName;

    @Value("${output.dir}")
    private String outputDir;

    private static final int BATCH_SIZE = 10_000;

    public void processCsvFile() throws IOException, InterruptedException, ExecutionException {
        File input = new File(inputFileName);
        if (!input.exists()) throw new IllegalArgumentException("Input file doesn't exist");

        Files.createDirectories(Paths.get(outputDir));

        WriteRegistry registry = new WriteRegistry(outputDir);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        List<Future<?>> futures = new ArrayList<>();

        List<String> batch = new ArrayList<>(BATCH_SIZE);

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(inputFileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.toLowerCase().contains("origin")) continue;

                batch.add(line);
                if (batch.size() == BATCH_SIZE) {
                    List<String> currentBatch = new ArrayList<>(batch);
                    futures.add(executor.submit(() -> processBatch(currentBatch, registry)));
                    batch.clear();
                }
            }

            // Final batch
            if (!batch.isEmpty()) {
                List<String> currentBatch = new ArrayList<>(batch);
                futures.add(executor.submit(() -> processBatch(currentBatch, registry)));
            }
        }

        for (Future<?> future : futures) future.get();
        executor.shutdown();
        registry.closeAll();

        System.out.println("✅ Processing complete.");
    }

    private void processBatch(List<String> lines, WriteRegistry registry) {
        Map<Long, List<String>> grouped = new HashMap<>();

        for (String line : lines) {
            String[] parts = line.split(",");
            if (parts.length >= 0) {
                try {
                    long origin = Long.parseLong(parts[0].trim().replaceAll("^\"|\"$", ""));
                    String pipeLine =
                            String.join("|", Arrays.stream(parts).map(String::trim).toList());
                    grouped.computeIfAbsent(origin, k -> new ArrayList<>()).add(pipeLine);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid origin: " + parts[0]);
                }
            }
        }

        grouped.forEach(
                (origin, values) -> {
                    try {
                        registry.writeLines(origin, values);
                    } catch (IOException e) {
                        System.err.println("Failed to write batch for origin " + origin);
                        e.printStackTrace();
                    }
                });
    }
}

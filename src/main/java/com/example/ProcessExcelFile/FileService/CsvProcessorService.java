package com.example.ProcessExcelFile.FileService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class CsvProcessorService {

    @Value("${input.file}")
    private String inputFileName;

    @Value("${output.dir}")
    private String outputDir;

    /**
     * @throws IOException
     */
    public void processCsvFile() throws IOException, ExecutionException, InterruptedException {
        File input = new File(inputFileName);
        if (!input.exists()) throw new IllegalArgumentException("Input file doesn't exist");

        Files.createDirectories(Paths.get(outputDir));

        WriteRegistry registry = new WriteRegistry(outputDir);

        var executor = Executors.newVirtualThreadPerTaskExecutor();
        List<Future<?>> futures = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(inputFileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                final String currentLine = line;
                if (currentLine.isBlank() || currentLine.toLowerCase().contains("origin")) continue;

                futures.add(executor.submit(() -> {
                    String[] parts = currentLine.split(",");
                    if (parts.length >= 6) {
                        try {
                            long origin = Long.parseLong(parts[0].trim().replaceAll("^\"|\"$", ""));
                            registry.writeLine(origin, currentLine);
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid origin: " + parts[0]);
                        }
                    }
                }));
            }
        }

        for (Future<?> f : futures) f.get();
        registry.closeAll();
        executor.shutdown();

        System.out.println("✅ Processing complete.");
    }
}

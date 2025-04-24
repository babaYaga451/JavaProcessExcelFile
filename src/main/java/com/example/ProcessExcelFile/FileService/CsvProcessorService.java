package com.example.ProcessExcelFile.FileService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CsvProcessorService {

    @Value("${input.file}")
    private String inputFileName;

    @Value("${output.dir.proship}")
    private String outputDirProship;

    @Value("${output.dir.automation}")
    private String outputDirAutomation;

    private static final int BATCH_SIZE = 10_000;

    public void processCsvFile() throws IOException, InterruptedException, ExecutionException {
        Map<Long, List<String>> originShipperMap = loadOriginToShipperMap();

        File input = new File(inputFileName);
        if (!input.exists()) throw new IllegalArgumentException("Input file doesn't exist");

        Files.createDirectories(Paths.get(outputDirProship));
        Files.createDirectories(Paths.get(outputDirAutomation));

        WriteRegistry registry = new WriteRegistry(outputDirAutomation, outputDirProship);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        List<Future<?>> futures = new ArrayList<>();

        List<String> batch = new ArrayList<>(BATCH_SIZE);

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(inputFileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.toLowerCase().contains("origin")) continue;

                batch.add(line);
                if (batch.size() == BATCH_SIZE) {
                    List<String> currentBatch = List.copyOf(batch);
                    futures.add(
                            executor.submit(
                                    () -> processBatch(currentBatch, registry, originShipperMap)));
                    batch.clear();
                }
            }

            // Final batch
            if (!batch.isEmpty()) {
                List<String> currentBatch = List.copyOf(batch);
                futures.add(
                        executor.submit(
                                () -> processBatch(currentBatch, registry, originShipperMap)));
            }
        }

        for (Future<?> future : futures) future.get();
        executor.shutdown();
        registry.closeAll();

        System.out.println("✅ Processing complete.");
    }

    private void processBatch(
            List<String> lines, WriteRegistry registry, Map<Long, List<String>> originShipperMap) {
        Map<Long, List<String>> groupedPiped = new HashMap<>();
        Map<Long, List<String>> groupedCsv = new HashMap<>();

        for (String line : lines) {
            String[] parts = line.split(",");
            if (parts.length > 0) {
                try {
                    long origin = Long.parseLong(parts[0].trim().replaceAll("^\"|\"$", ""));
                    List<String> tntList = List.of(parts[0], parts[1], parts[3]);
                    String pipeLine = String.join("|", tntList.stream().map(String::trim).toList());
                    String csv = String.join(",", tntList.stream().map(String::trim).toList());
                    groupedPiped.computeIfAbsent(origin, k -> new ArrayList<>()).add(pipeLine);
                    groupedCsv.computeIfAbsent(origin, k -> new ArrayList<>()).add(csv);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid origin: " + parts[0]);
                }
            }
        }

        groupedPiped.forEach(
                (origin, values) -> {
                    try {
                        List<String> shippers = originShipperMap.get(origin);
                        if (Objects.nonNull(shippers)) {
                            registry.writeLinesPiped(values, shippers);
                        }
                    } catch (IOException e) {
                        System.err.println("Failed to write batch for origin " + origin);
                        e.printStackTrace();
                    }
                });

        groupedCsv.forEach(
                (origin, values) -> {
                    try {
                        List<String> shippers = originShipperMap.get(origin);
                        if (Objects.nonNull(shippers)) {
                            registry.writeLinesCsv(values, shippers);
                        }
                    } catch (IOException e) {
                        System.err.println("Failed to write batch for origin " + origin);
                        e.printStackTrace();
                    }
                });
    }

    private Map<Long, List<String>> loadOriginToShipperMap() throws IOException {
        try (Stream<String> stream =
                Files.lines(Paths.get("src/main/resources/origin_shipper.csv")).parallel()) {
            return stream.filter(line -> !line.trim().isEmpty())
                    .filter(line -> !line.contains("postal_code"))
                    .map(line -> line.split(",", 2))
                    .collect(
                            Collectors.groupingByConcurrent(
                                    arr -> Long.parseLong(arr[0].trim()),
                                    Collectors.mapping(arr -> arr[1].trim(), Collectors.toList())));
        }
    }
}

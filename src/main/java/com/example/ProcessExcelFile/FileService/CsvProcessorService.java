package com.example.ProcessExcelFile.FileService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

@Service
public class CsvProcessorService {

    @Value("${input.file}")
    private String inputFileName;

    @Value("${output.dir}")
    private String outputDir;

    private static final int CHUNK_SIZE = 64 * 1024 * 1024;
    private static final int BATCH_SIZE = 10_000;

    public void processCsvFile() throws IOException, ExecutionException, InterruptedException {
        File input = new File(inputFileName);
        if (!input.exists()) throw new IllegalArgumentException("Input file doesn't exist");

        Files.createDirectories(Paths.get(outputDir));
        FileChannel channel = new RandomAccessFile(input, "r").getChannel();
        long fileSize = channel.size();

        var registry = new WriteRegistry(outputDir);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        List<Future<?>> futures = new ArrayList<>();

        long position = 0;
        boolean isFirstChunk = true;

        while (position < fileSize) {
            long chunkStart = position;
            long sizeToRead = Math.min(CHUNK_SIZE, fileSize - chunkStart);
            long nextPosition = chunkStart + sizeToRead;

            // Extend to next newline
            if (nextPosition < fileSize) {
                channel.position(nextPosition);
                byte[] tail = new byte[1024];
                int bytesRead = channel.read(java.nio.ByteBuffer.wrap(tail));
                for (int i = 0; i < bytesRead; i++) {
                    if (tail[i] == '\n') {
                        sizeToRead += i + 1;
                        break;
                    }
                }
            }

            final long finalChunkStart = chunkStart;
            final int finalSizeToRead = (int) sizeToRead;
            final boolean isFirst = isFirstChunk;

            futures.add(executor.submit(() -> {
                try {
                    processChunk(channel, finalChunkStart, finalSizeToRead, isFirst, registry);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));

            position += sizeToRead;
            isFirstChunk = false;
        }

        for (Future<?> f : futures) f.get();
        registry.closeAll();
        executor.shutdown();
        System.out.println("✅ Processing complete.");
    }

    private void processChunk(FileChannel channel, long chunkStart, int sizeToRead, boolean isFirstChunk, WriteRegistry registry) throws IOException {
        MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, chunkStart, sizeToRead);
        StringBuilder lineBuilder = new StringBuilder();
        boolean skipUntilNewline = !isFirstChunk;

        Map<Long, List<String>> originBatchMap = new HashMap<>();

        while (buffer.hasRemaining()) {
            char c = (char) buffer.get();

            if (skipUntilNewline) {
                if (c == '\n') {
                    skipUntilNewline = false;
                }
                continue;
            }

            lineBuilder.append(c);
            if (c == '\n') {
                String line = lineBuilder.toString().trim();
                lineBuilder.setLength(0);

                if (!line.isEmpty() && !line.toLowerCase().contains("origin")) {
                    String[] parts = line.split(",");
                    if (parts.length >= 6) {
                        try {
                            long origin = Long.parseLong(parts[0].replaceAll("^\"|\"$", "").trim());
                            String pipeLine = String.join("|", parts).trim();
                            originBatchMap
                                .computeIfAbsent(origin, k -> new ArrayList<>())
                                .add(pipeLine);

                            if (originBatchMap.get(origin).size() >= BATCH_SIZE) {
                                List<String> batch = originBatchMap.remove(origin);
                                registry.writeLines(origin, batch);
                            }
                        } catch (NumberFormatException ignored) {
                            System.out.println("Invalid origin: " + parts[0]);
                        }
                    }
                }
            }
        }

        // flush remaining lines
        for (Map.Entry<Long, List<String>> entry : originBatchMap.entrySet()) {
            registry.writeLines(entry.getKey(), entry.getValue());
        }
    }
}
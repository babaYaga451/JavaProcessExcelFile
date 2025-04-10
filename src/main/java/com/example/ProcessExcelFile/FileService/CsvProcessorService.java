package com.example.ProcessExcelFile.FileService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
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

    private static final int CHUNK_SIZE = 64 * 1024 * 1024;

    /**
     * @throws IOException
     */
    public void processCsvFile() throws IOException, ExecutionException, InterruptedException {
        File input = new File(inputFileName);
        if (!input.exists()) throw new IllegalArgumentException("Input file doesn't exist");

        Files.createDirectories(Paths.get(outputDir));

        FileChannel channel = new RandomAccessFile(inputFileName, "r").getChannel();
        long size = channel.size();

        WriteRegistry registry = new WriteRegistry(outputDir);

        var executors = Executors.newVirtualThreadPerTaskExecutor();
        List<Future<?>> futures = new ArrayList<>();
        long position = 0;

        while (position < size) {
            long chunkStart = position;
            long sizeToRead = Math.min(CHUNK_SIZE, size - position);
            position += sizeToRead;

            futures.add(
                    executors.submit(
                            () -> {
                                try {
                                    processChunk(channel, chunkStart, (int) sizeToRead, registry);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }));
        }

        for (Future<?> f : futures) f.get();
        registry.closeAll();
        executors.shutdown();

        System.out.println("✅ Processing complete.");
    }

    private void processChunk(
            FileChannel channel, long chunkStart, int sizeToRead, WriteRegistry registry)
            throws IOException {
        var buffer = channel.map(FileChannel.MapMode.READ_ONLY, chunkStart, sizeToRead);
        StringBuilder sb = new StringBuilder();

        while (buffer.hasRemaining()) {
            char c = (char) buffer.get();
            sb.append(c);
            if (c == '\n') {
                String line = sb.toString().trim();
                sb.setLength(0);

                if (!line.isEmpty() && !line.toLowerCase().contains("origin")) {
                    String[] parts = line.split(",");
                    if (parts.length >= 6) {
                        try {
                            Long origin = Long.parseLong(parts[0]);
                            registry.writeLine(origin, line);
                        } catch (NumberFormatException ignored) {
                            System.out.println("failed to parse " + ignored.getMessage());
                        }
                    }
                }
            }
        }
    }
}

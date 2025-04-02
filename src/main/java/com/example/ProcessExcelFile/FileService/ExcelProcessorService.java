package com.example.ProcessExcelFile.FileService;

import com.example.ProcessExcelFile.Data.Data;
import com.poiji.bind.Poiji;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@NoArgsConstructor
@AllArgsConstructor
@Service
@Slf4j
public class ExcelProcessorService {
    public static final Map<Long, String> shipperMap =
            Map.of(
                    10000L,
                    "Shipper 1",
                    10001L,
                    "Shipper 2",
                    10002L,
                    "Shipper 3",
                    10003L,
                    "Shipper 4",
                    10004L,
                    "Shipper 5",
                    10005L,
                    "Shipper 6",
                    10006L,
                    "Shipper 7",
                    10007L,
                    "Shipper 8",
                    10008L,
                    "Shipper 9",
                    10009L,
                    "Shipper 10");

    @Value("${input.file}")
    private String inputFileName;

    @Value("${output.dir}")
    private String outputDir;

    public void ProcessFile() throws IOException {
        File file = new File(inputFileName);
        if (!file.exists()) {
            throw new IllegalArgumentException("File " + inputFileName + " does not exist.");
        }

        Files.createDirectories(Paths.get(outputDir));
        List<Data> records = Poiji.fromExcel(file, Data.class);
        ExecutorService executor = Executors.newFixedThreadPool(4);
        records.parallelStream()
                .collect(Collectors.groupingBy(Data::getOrigin))
                .forEach(
                        (origin, recordsByOrigin) ->
                                executor.submit(() -> saveToCSV(origin, recordsByOrigin)));
        executor.shutdown();
    }

    private void saveToCSV(Long origin, List<Data> recordsByOrigin) {
        log.info("Processing origin: " + origin);
        String shipper = shipperMap.get(origin);

        String filePath = outputDir + "/" + shipper + ".csv";
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath))) {
            writer.write("Origin,GND TNT Days,GND Zone,Surepost TNT Days,Surepost Zone\n");
            for (Data data : recordsByOrigin) {
                writer.write(
                        String.format(
                                "%s,%s,%s,%s,%s\n",
                                data.getOrigin(),
                                data.getGndTntDays(),
                                data.getGndZone(),
                                data.getSurePostTntDays(),
                                data.getSurePostZone()));
            }
            System.out.println("✅ Saved: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

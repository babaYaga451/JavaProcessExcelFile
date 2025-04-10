package com.example.ProcessExcelFile;

import com.example.ProcessExcelFile.FileService.CsvProcessorService;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProcessExcelFileApplication implements CommandLineRunner {
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
    //    @Autowired ExcelProcessorService excelProcessorService;
    @Autowired CsvProcessorService csvProcessorService;

    public static void main(String[] args) {
        SpringApplication.run(ProcessExcelFileApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        //        excelProcessorService.ProcessFile();
        System.out.println("Start " + System.currentTimeMillis());
        var start = System.currentTimeMillis();
        csvProcessorService.processCsvFile();
        System.out.println("Time taken: " + (System.currentTimeMillis() - start));
    }
}

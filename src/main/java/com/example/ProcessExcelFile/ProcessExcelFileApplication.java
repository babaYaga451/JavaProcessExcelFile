package com.example.ProcessExcelFile;

import com.example.ProcessExcelFile.FileService.CsvProcessorService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProcessExcelFileApplication implements CommandLineRunner {
    @Autowired CsvProcessorService csvProcessorService;

    public static void main(String[] args) {
        SpringApplication.run(ProcessExcelFileApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        var start = System.currentTimeMillis();
        csvProcessorService.processCsvFile();
        var end = System.currentTimeMillis();
        System.out.println("Time taken: " + (end - start));
    }
}

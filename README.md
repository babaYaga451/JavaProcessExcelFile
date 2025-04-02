# JavaProcessExcelFile

## Features

- Read Excel files and map data to Java records
- Uses Apache POI for Excel handling
- Multi-threaded processing with Java ExecutorService
- Supports VM arguments for dynamic file input
- Generates output files under `target/` directory

## Prerequisites

- Java 17+
- Maven 3.6+
- Git (for version control)

## Installation

1. Clone the repository:

   ```sh
   git clone https://github.com/babaYaga451/JavaProcessExcelFile.git
   cd ProcessExcelFile
   ```

2. Initialize Maven:
   ```sh
   mvn clean install
   ```

## Configuration

Update the `application.properties` file:

```properties
input.file=${INPUT_FILE}
```


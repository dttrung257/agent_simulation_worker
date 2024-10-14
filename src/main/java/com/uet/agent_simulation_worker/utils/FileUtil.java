package com.uet.agent_simulation_worker.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@Slf4j
public class FileUtil {
    /**
     * Check if a file exists
     *
     * @param path the path to the file
     * @return true if the file exists, false otherwise
     */
    public boolean fileExists(String path) {
        final var file = new File(path);

        return file.exists() && !file.isDirectory();
    }

    /**
     * Check if a directory exists
     *
     * @param path the path to the directory
     * @return true if the directory exists, false otherwise
     */
    public boolean directoryExists(String path) {
        final var file = new File(path);

        return file.exists() && file.isDirectory();
    }

    /**
     * Create a directory
     *
     * @param path the path to the directory
     * @return true if the directory is created, false otherwise
     */
    public boolean findAndWrite(String path, String key, String value) {
        try {
            final var content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(key)) {
                        content.append("  ").append(key).append(": ").append(value).append("\n");
                        continue;
                    }
                    content.append(line).append("\n");
                }
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
                writer.write(content.toString());
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the value by key in a file
     *
     * @param path the path to the file
     * @param key the key to get the value
     * @return the value of the key
     */
    public String getValueByKey(String path, String key) {
        try {
            try (BufferedReader reader = new BufferedReader(new FileReader(path));) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(key)) {
                        String[] parts = line.split(":");
                        if (parts.length == 2) {
                            return parts[1].trim();
                        }

                        return "";
                    }
                }
            }

            return "";
        } catch (Exception e) {
            log.error("Cannot get value by key: {}", key, e);

            return null;
        }
    }

    /**
     * Read the last line of a file
     *
     * @param pathToFile the path to the file
     * @param key the key must be in the line
     *
     * @return the last line of the file
     */
    public String readLastLine(String pathToFile, String key) {
        String lastStepLine = null;

        try {
            final var path = Paths.get(pathToFile);
            List<String> lines = Files.readAllLines(path);

            for (int i = lines.size() - 1; i >= 0; i--) {
                String line = lines.get(i);
                if (line.contains(key)) {
                    lastStepLine = line;
                    break;
                }
            }

        } catch (Exception e) {
            log.error("Error while reading last line of file: {}", e.getMessage());
        }

        return lastStepLine;
    }

    /**
     * Get the file name by prefix
     *
     * @param folderPath the path to the folder
     * @param prefix the prefix of the file name
     *
     * @return the file name
     */
    public String getFileNameByPrefix(String folderPath, String prefix) {
        try {
            final var path = Paths.get(folderPath);

            try (var files = Files.list(path)) {
                final var file = files.filter(f -> f.getFileName().toString().startsWith(prefix)).findFirst();

                return file.map(Path::getFileName).map(Path::toString).orElse(null);
            }
        } catch (Exception e) {
            log.error("Error while getting file name by prefix: {}", e.getMessage());
        }

        return null;
    }
}

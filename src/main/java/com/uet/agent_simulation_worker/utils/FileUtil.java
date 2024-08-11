package com.uet.agent_simulation_worker.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;

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
}

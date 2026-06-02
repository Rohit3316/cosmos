package org.eclipse.hawkbit.repository.file.utils;

import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@UtilityClass
public class FileHelper {

    /**
     * Creates a directory at the specified path if it does not exist.
     *
     * @param path The path of the directory to be created.
     * @throws IOException If an I/O error occurs while creating the directory.
     */
    public void createDirectoryPathIfNotExist(String path) throws IOException {
        Path directoryPath = Paths.get(path);
        if (!Files.exists(directoryPath)) {
            try {
                Files.createDirectories(directoryPath);
            } catch (IOException e) {
                throw new IOException("Failed to create directory: " + path, e);
            }
        }
    }

    /**
     * Creates a file from the given input stream in the specified directory.
     *
     * @param inputStream The input stream from which to read the file content.
     * @param directoryPath The path of the directory where the file should be created.
     * @param fileName The name of the file to be created.
     * @throws IOException If an I/O error occurs while creating the file.
     */
    public void createFileFromInputStream(InputStream inputStream, String directoryPath, String fileName) throws IOException {
        createDirectoryPathIfNotExist(directoryPath);
        File file = new File(directoryPath + File.separator + fileName);
        try (inputStream; FileOutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }
}

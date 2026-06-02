package org.eclipse.hawkbit.repository.file.supportpackage;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * This interface provides methods for interacting with a file system to support the operations related to
 * downloading files with their hash values.
 */
public interface SupportPackageFileSystem {

    /**
     * Downloads a file from a given URL, calculates its hash value, and compares it with the provided hash value.
     * If the hash values match, the file is saved with the specified file name in the file system.
     *
     * @param attributes The attributes of the file path, including the expected hash value.
     * @param fileUrl The URL from which to download the file.
     * @param fileName The name to save the downloaded file in the file system.
     * @return The absolute path of the downloaded file if the hash values match; otherwise, null.
     * @throws IOException If an I/O error occurs during the download or file saving process.
     * @throws NoSuchAlgorithmException If the specified hash algorithm is not available.
     */
    String downloadFileWithHash(SupportPackageFilePathAttributes attributes, String fileUrl, String fileName) throws IOException, NoSuchAlgorithmException;
}
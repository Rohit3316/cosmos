package org.eclipse.hawkbit.repository.jpa.service;

import lombok.experimental.UtilityClass;
import org.cosmos.sns.models.CdnDeleteRequest;

@UtilityClass
public class CDNFileUtil {

/**
 * Builds a CdnDeleteRequest with the specified parameters.
 *
 * @param fileId   the ID of the file to be deleted
 * @param filePath the path of the file to be deleted
 * @param fileType the type of the file to be deleted
 * @param tenantId the ID of the tenant
 * @return a CdnDeleteRequest object
 */
public CdnDeleteRequest buildDeletionRequest(Long fileId, String filePath, String fileType, Long tenantId) {
    return CdnDeleteRequest.builder()
            .fileId(fileId)
            .filePath(filePath)
            .fileType(fileType)
            .tenantId(tenantId)
            .build();
}

/**
 * Generates the CDN file path for the specified parameters.
 *
 * @param directoryPlaceHolder the placeholder for the directory
 * @param tenant               the tenant
 * @param sha256               the SHA256 hash of the file
 * @param fileName             the name of the file
 * @param fileType             the type of the file
 * @return the CDN file path
 */
public String getCdnFilePath(String directoryPlaceHolder, String tenant, String sha256, String fileName, String fileType) {
    final String cdnPath = getCdnFilePath(directoryPlaceHolder, tenant, sha256, fileType);
    return cdnPath + fileName;
}
    /**
     * Generates the CDN path for the artifact.
     *
     * @param tenant the tenant
     * @param sha256 the SHA256 hash
     * @return the CDN path
     *
     */
    public String getCdnFilePath(String directoryPlaceHolder, String tenant, String sha256, String fileType) {
        return directoryPlaceHolder
                .replace("{tenant}", tenant)
                .replace("{type}", fileType)
                .replace("{SHA256}", sha256.replaceAll("(.{2})", "$1/"));
    }
}

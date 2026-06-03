package org.eclipse.hawkbit.repository.jpa.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.eclipse.hawkbit.repository.model.BaseSupportPackage;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * This class represents an abstract base entity for support packages.
 * It extends {@link AbstractJpaTenantAwareBaseEntity} and implements {@link BaseSupportPackage}.
 * The class is annotated with {@link MappedSuperclass} to indicate that it is not a persistent entity itself,
 * but serves as a base for other persistent entities.
 * <p>
 * The class contains various fields representing different attributes of a support package,
 * such as file name, file URL, SHA-256 hash, MD5 hash, file version, content description,
 * info URL, metadata, and deletion status.
 * <p>
 * The {@link #getMetadata()} method is overridden to parse and return the metadata stored in the
 * {@link #fileMetadata} field as a map of string keys and values.
 * If the parsing fails due to a {@link JsonProcessingException}, an empty map is returned.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class AbstractBaseSupportPackage extends AbstractJpaTenantAwareBaseEntity implements BaseSupportPackage {

    /**
     * A string representing the name of the file.It is annotated with @Column to specify the database column name,
     *
     * @NotNull to indicate that it cannot be null  and has a length of 128 characters.
     */
    @Column(name = "file_name", length = 128, nullable = false)
    @NotNull
    private String fileName;

    /**
     * A string representing the URL of the file.It is annotated with @Column to specify the database column name,
     *
     * @NotNull to indicate that it cannot be null and has a length of 128 characters.
     */
    @Column(name = "file_url", columnDefinition = "TEXT", nullable = false)
    private String fileUrl;

    /**
     * A string representing the SHA-256 hash of the file.It is annotated with @Column to specify the database column name,
     *
     * @NotNull to indicate that it cannot be null and has a length of 64 characters.
     */
    @Column(name = "sha_256", length = 64)
    @NotNull
    private String sha256Hash;

    /**
     * A string representing the MD5 hash of the file.It is annotated with @Column to specify the database column name,
     *
     * @NotNull to indicate that it cannot be null and has a length of 32 characters.
     */
    @Column(name = "md5", length = 32)
    private String md5Hash;

    /**
     * A string representing the version of the file.It is annotated with @Column to specify the database column name,
     *
     * @NotNull to indicate that it cannot be null and has a length of 50 characters.
     */
    @Column(name = "file_version", length = 50)
    private String fileVersion;

    /**
     * A string representing the description of the file content.It is annotated with @Column to specify the database column name,
     *
     * @NotNull to indicate that it cannot be null.
     */
    @Column(name = "file_content_description")
    private String fileContentDescription;

    /**
     * A string representing the URL of the file information.It is annotated with @Column to specify the database column name,
     *
     * @NotNull to indicate that it cannot be null.
     */
    @Column(name = "file_info_url", columnDefinition = "TEXT")
    @NotNull
    private String fileInfoUrl;

    /**
     * A string representing the metadata of the file.It is annotated with @Column to specify the database column name,
     *
     * @NotNull to indicate that it cannot be null.
     */
    @Column(name = "file_metadata")
    @JsonIgnore
    @NotNull
    private String fileMetadata;

    /**
     * A string representing the file status.It is annotated with @Column to specify the database column name,
     */
    @Column(name = "file_status")
    private String fileStatus;

    @Override
    public Map<String, String> getMetadata() {

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(getFileMetadata(), new TypeReference<>() {
            });

        } catch (JsonProcessingException ex) {
            return Map.of();
        }

    }

    @Override
    public FileTransferStatus getSupportPackageFileStatus() {
        String fileStatus=getFileStatus();
        if( fileStatus== null) {
            return null;
        }
        return FileTransferStatus.valueOf(fileStatus);
    }
}

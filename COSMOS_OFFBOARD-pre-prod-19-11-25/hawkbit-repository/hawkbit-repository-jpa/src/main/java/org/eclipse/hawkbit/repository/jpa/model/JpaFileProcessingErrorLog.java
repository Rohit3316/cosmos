package org.eclipse.hawkbit.repository.jpa.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cosmos.models.sqs.ActionType;
import org.cosmos.models.sqs.FileType;
import org.cosmos.models.sqs.StorageType;
import org.eclipse.hawkbit.repository.model.FileProcessingErrorLog;
import org.eclipse.persistence.annotations.ConversionValue;
import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.ObjectTypeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Entity to store File processing error logs in the database
 */
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "sp_file_processing_error_log")
public class JpaFileProcessingErrorLog  extends AbstractJpaTenantAwareBaseEntity implements FileProcessingErrorLog {

    /**
     * The type of file being processed - ARTIFACT, ESP or RSP
     */
    @Column(name = "file_type", nullable = false)
    @NotNull
    @ObjectTypeConverter(name = "file_type", objectType = FileType.class, dataType = String.class,
            conversionValues = {
            @ConversionValue(objectValue = "ARTIFACT", dataValue = "ARTIFACT"),
            @ConversionValue(objectValue = "ESP", dataValue = "ESP"),
            @ConversionValue(objectValue = "RSP", dataValue = "RSP")
    })
    @Convert("file_type")
    private FileType fileType;


    /**
     * The error message
     */
    @Column(name = "log_message", nullable = false)
    @NotBlank
    private String logMessage;

    /**
     * The ID of the file type in the log
     */
    @Column(name = "log_type_id", nullable = false)
    @NotNull
    private Long logTypeId;

    /**
     * The type of storage - S3 or CDN
     */
    @Column(name = "storage_type", nullable = false)
    @NotNull
    @ObjectTypeConverter(name = "storage_type", objectType = StorageType.class, dataType = String.class,
            conversionValues = {
                    @ConversionValue(objectValue = "S3", dataValue = "S3"),
                    @ConversionValue(objectValue = "CDN", dataValue = "CDN")
            })
    @Convert("storage_type")
    private StorageType storageType;

    /**
     * The number of times the file processing has been retried
     */
    @Column(name = "retry_count", nullable = false)
    @NotNull
    private Integer retryCount;


    /**
     * The action - UPLOAD or DELETE
     */
    @Column(name = "action", nullable = false)
    @NotNull
    @ObjectTypeConverter(name = "action", objectType = ActionType.class, dataType = String.class,
            conversionValues = {
                    @ConversionValue(objectValue = "UPLOAD", dataValue = "UPLOAD"),
                    @ConversionValue(objectValue = "DOWNLOAD", dataValue = "DOWNLOAD"),
                    @ConversionValue(objectValue = "DELETE", dataValue = "DELETE")
            })
    @Convert("action")
    private ActionType action;
}

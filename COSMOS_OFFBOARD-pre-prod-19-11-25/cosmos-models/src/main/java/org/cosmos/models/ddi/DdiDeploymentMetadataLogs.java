package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * This class represents a Deployment Metadata logs configurations in the DDI model.
 * It is annotated with JsonIgnoreProperties to ignore unknown properties during deserialization.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class DdiDeploymentMetadataLogs {

    /**
     * This attribute denotes Number of log files that can be uploaded
     */
    @JsonProperty("maxNumberOfFiles")
    @Schema(example = "5")
    private Integer maxNumberOfFiles;

    /**
     * This attribute denotes Total Size of all files in bytes that can be uploaded
     */
    @JsonProperty("maxAllFileSize")
    @Schema(example = "1048576")
    private Integer maxAllFileSize;

    /**
     * This attribute denotes Total Size of each files in bytes that can be uploaded
     */
    @JsonProperty("maxEachFileSize")
    @Schema(example = "209715")
    private Integer maxEachFileSize;

}
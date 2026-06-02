/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.artifacts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cosmos.annotations.TraceableField;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request Body for Artifacts POST.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MgmtArtifactsRequest {

    @JsonProperty(required = true)
    @Schema(example = "https://example.com/file.zip")
    private String fileURL;

    @JsonProperty(required = false)
    @Schema(example = "binary")
    private MultipartFile file;

    @JsonProperty(required = false)
    @Schema(example = "myfile.cdp")
    @TraceableField
    private String filename;

    @JsonProperty(required = true)
    @Schema(example = "FULL")
    private String fileType;

    @JsonProperty(required = true)
    @Schema(example = "description")
    private String description;

    @JsonProperty(required = true)
    @Schema(example = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
    @TraceableField
    private String sha256;

    @JsonProperty(required = true)
    @Schema(example = "1709481600")
    private Long signatureExpiryDate;

}
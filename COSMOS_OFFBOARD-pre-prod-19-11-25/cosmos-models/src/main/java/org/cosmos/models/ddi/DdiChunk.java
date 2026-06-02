/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.List;
import jakarta.validation.constraints.NotNull;

/**
 * Deployment chunks.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiChunk {

    @JsonProperty("part")
    @NotNull
    @Schema(example = "bApp")
    private String part;


    @JsonProperty("format")
    @NotNull
    @Schema(example = "1.2.0")
    private String format;

    @JsonProperty("name")
    @NotNull
    @Schema(example = "oneApp")
    private String name;

    @JsonProperty("encrypted")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(example = "true")
    private Boolean encrypted;

    @JsonProperty("artifacts")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<DdiArtifact> artifacts;

    @JsonProperty("chunkMetadata")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private DdiMetadata metadata;

    public DdiChunk() {
        // needed for json create
    }

    /**
     * Constructor.
     *
     * @param part      of the deployment chunk
     * @param name      of the artifact
     * @param encrypted if artifacts are encrypted
     * @param artifacts download information
     * @param metadata  optional as additional information for the target/device
     * @author T7437JK
     * @modified on 03/08/2023
     * change list of DdiMetadata metadata into an object
     */
    public DdiChunk(final String part, final String name, final Boolean encrypted,
                    final List<DdiArtifact> artifacts, final DdiMetadata metadata, final String format) {
        this.part = part;
        this.name = name;
        this.encrypted = encrypted;
        this.artifacts = artifacts;
        this.metadata = metadata;
        this.format = format;
    }

    public String getFormat() {
        return format;
    }

    public String getPart() {
        return part;
    }


    public String getName() {
        return name;
    }

    public Boolean isEncrypted() {
        return encrypted;
    }

    public List<DdiArtifact> getArtifacts() {
        if (artifacts == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(artifacts);
    }

    public DdiMetadata getMetadata() {
        return metadata;
    }

}


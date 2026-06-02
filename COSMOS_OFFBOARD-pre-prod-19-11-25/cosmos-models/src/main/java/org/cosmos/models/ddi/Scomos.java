package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Objects;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Scomos implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    private String scomoId;

    @NotNull
    private String swVersion;

    @JsonProperty("swSignature")
    private String swSignature;

    @JsonProperty("swsignatureType")
    private String swSignatureType;

    @JsonProperty("swFingerPrint")
    private String swFingerPrint;

    public String getScomoId() {
        return scomoId;
    }

    public void setScomoId(String scomoId) {
        this.scomoId = scomoId;
    }

    public String getSwVersion() {
        return swVersion;
    }

    public void setSwVersion(String swVersion) {
        this.swVersion = swVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Scomos scomos = (Scomos) o;
        return Objects.equals(scomoId, scomos.scomoId) && Objects.equals(swVersion, scomos.swVersion) && Objects.equals(swSignature, scomos.swSignature) && Objects.equals(swSignatureType, scomos.swSignatureType) && Objects.equals(swFingerPrint, scomos.swFingerPrint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scomoId, swVersion, swSignature, swSignatureType, swFingerPrint);
    }
}

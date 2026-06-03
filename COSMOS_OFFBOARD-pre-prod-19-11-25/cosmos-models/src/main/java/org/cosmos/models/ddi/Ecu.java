package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Ecu implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty(value = "nodeAddr")
    @NotNull
    private String nodeAddress;

    @JsonProperty(value = "partnumber")
    @NotNull
    private String partNumber;

    private String hwVersion;

    private String hwSignature;

    @JsonProperty("hwsignatureType")
    private String hwSignatureType;

    private String serialNumber;

    private List<Scomos> scomos;

    public String getNodeAddress() {
        return nodeAddress;
    }

    public void setNodeAddress(String nodeAddress) {
        this.nodeAddress = nodeAddress;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public String getHwVersion() {
        return hwVersion;
    }

    public void setHwVersion(String hwVersion) {
        this.hwVersion = hwVersion;
    }

    public String getHwSignature() {
        return hwSignature;
    }

    public void setHwSignature(String hwSignature) {
        this.hwSignature = hwSignature;
    }

    public String getHwSignatureType() {
        return hwSignatureType;
    }

    public void setHwSignatureType(String hwSignatureType) {
        this.hwSignatureType = hwSignatureType;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public List<Scomos> getScomos() {
        return scomos;
    }

    public void setScomos(List<Scomos> scomos) {
        this.scomos = scomos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ecu ecu = (Ecu) o;
        return Objects.equals(nodeAddress, ecu.nodeAddress) && Objects.equals(partNumber, ecu.partNumber) && Objects.equals(hwVersion, ecu.hwVersion) && Objects.equals(hwSignature, ecu.hwSignature) && Objects.equals(hwSignatureType, ecu.hwSignatureType) && Objects.equals(serialNumber, ecu.serialNumber) && Objects.equals(scomos, ecu.scomos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeAddress, partNumber, hwVersion, hwSignature, hwSignatureType, serialNumber, scomos);
    }
}

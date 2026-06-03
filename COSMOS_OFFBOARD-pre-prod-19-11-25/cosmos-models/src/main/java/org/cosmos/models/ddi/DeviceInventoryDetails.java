package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceInventoryDetails implements Serializable {

    private static final long serialVersionUID = 1L;

    private String date;

    private String error;

    private String vin;

    private String proxyString;

    @NotNull
    private List<Ecu> ecuList;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public String getProxyString() {
        return proxyString;
    }

    public void setProxyString(String proxyString) {
        this.proxyString = proxyString;
    }

    public List<Ecu> getEcuList() {
        return ecuList;
    }

    public void setEcuList(List<Ecu> ecuList) {
        this.ecuList = ecuList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceInventoryDetails that = (DeviceInventoryDetails) o;
        return Objects.equals(date, that.date) && Objects.equals(error, that.error) && Objects.equals(vin, that.vin) && Objects.equals(proxyString, that.proxyString) && Objects.equals(ecuList, that.ecuList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, error, vin, proxyString, ecuList);
    }
}

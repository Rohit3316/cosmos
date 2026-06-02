package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

/**
 * This class represents the ESP types in the DDI model.
 * It includes properties like varientCoding, license, udsFlow and ecuScript.
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DdiEspTypes {

    /**
     * The varientCoding property of the ESP types.
     */

    @JsonProperty("variantCoding")
    public DdiEsp variantCoding;

    /**
     * The license property of the ESP types.
     */
    @JsonProperty("license")
    public DdiEsp license;

    /**
     * The udsFlow property of the ESP types.
     */
    @JsonProperty("udsFlow")
    public DdiEsp udsFlow;

    /**
     * The ecuScript property of the ESP types.
     */
    @JsonProperty("ecuScript")
    public DdiEsp ecuScript;

    @JsonProperty("adaLicense")
    public DdiEsp adaLicense;

    @JsonProperty("adaCertificate")
    public DdiEsp adaCertificate;


    public DdiEspTypes(DdiEsp variantCoding, DdiEsp license, DdiEsp udsFlow, DdiEsp ecuScript) {
        this.variantCoding = variantCoding;
        this.license = license;
        this.udsFlow = udsFlow;
        this.ecuScript = ecuScript;
    }

    public DdiEspTypes() {
    }
    public void  setVariantCoding(DdiEsp variantCoding) {
        this.variantCoding = variantCoding;
    }

    /**
     * Set the license property of the ESP types.
     * @param license The license property of the ESP types.
     */
    public void setLicense(DdiEsp license) {
        this.license = license;
    }

    /**
     * Set the udsFlow property of the ESP types.
     * @param udsFlow The udsFlow property of the ESP types.
     */
    public void setUdsFlow(DdiEsp udsFlow) {
        this.udsFlow = udsFlow;
    }

    /**
     * Set the ecuScript property of the ESP types.
     * @param ecuScript The ecuScript property of the ESP types.
     */
    public void setEcuScript(DdiEsp ecuScript) {
        this.ecuScript = ecuScript;
    }

    public void setAdaLicense(DdiEsp adaLicense) {
        this.adaLicense=adaLicense;
    }
    public void setAdaCertificate(DdiEsp adaCertificate){
        this.adaCertificate=adaCertificate;
    }
}

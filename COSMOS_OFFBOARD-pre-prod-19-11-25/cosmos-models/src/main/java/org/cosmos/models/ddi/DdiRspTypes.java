package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * This class represents the RSP types in the DDI model.
 * It includes properties like baselineInventory, installationRollbackPlan, dtcBlacklist, ruleEngineConfig, proxi, proxiSignature, adaCertificate, adaLicense.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class DdiRspTypes {

    /**
     * The baselineInventory property of the RSP types.
     */
    @JsonProperty("baselineInventory")
    public DdiRsp baselineInventory;

    /**
     * The installationRollbackPlan property of the RSP types.
     */
    @JsonProperty("installationRollbackPlan")
    public DdiRsp installationRollbackPlan;

    /**
     * The dtcBlacklist property of the RSP types.
     */
    @JsonProperty("dtcBlacklist")
    public DdiRsp dtcBlacklist;

    /**
     * The ruleEngineConfig property of the RSP types.
     */
    @JsonProperty("ruleEngineConfig")
    public DdiRsp ruleEngineConfig;

    /**
     * The proxi property of the RSP types.
     */
    @JsonProperty("proxi")
    public DdiRsp proxi;

    /**
     * The proxiSignature property of the RSP types.
     */
    @JsonProperty("proxiSignature")
    public DdiRsp proxiSignature;

    /**
     * The Whats New property of the RSP types.
     */
    @JsonProperty("whatsNew")
    public DdiRsp whatsNew;

    /**
     * The Uds Global Pre Install property of the RSP types.
     */
    @JsonProperty("udsGlobalPreInstall")
    public DdiRsp udsGlobalPreInstall;

    /**
     * The Uds Global Post Install property of the RSP types.
     */
    @JsonProperty("udsGlobalPostInstall")
    public DdiRsp udsGlobalPostInstall;
}

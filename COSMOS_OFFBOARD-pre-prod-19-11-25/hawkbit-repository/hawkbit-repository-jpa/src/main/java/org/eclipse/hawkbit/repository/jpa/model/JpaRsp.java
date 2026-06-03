package org.eclipse.hawkbit.repository.jpa.model;

import lombok.Getter;
import lombok.Setter;
import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;
import org.eclipse.hawkbit.repository.model.Rsp;
import org.eclipse.hawkbit.repository.model.RspRollout;
import org.eclipse.persistence.annotations.ConversionValue;
import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.ObjectTypeConverter;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * This class represents a RSP in the Cosmos system. It extends the AbstractBaseSupportPackage class and implements the Rsp interface.
 * The class is annotated with JPA annotations to define its mapping to the database table 'sp_rsp'.
 * It contains fields for the file type, a list of RSP rollouts, and overrides the getRspRollouts method to provide a list of RspRollout objects.
 */
@Entity
@Table(name = "sp_rsp")
@Getter
@Setter
public class JpaRsp extends AbstractBaseSupportPackage implements Rsp {

    /**
     * The file type of the RSP. It is annotated with JPA annotations to define its mapping to the 'file_type' column in the database table.
     * It is also annotated with @NotNull to ensure that a non-null value is always assigned.
     * The file type is converted to an integer using the ObjectTypeConverter annotation and the conversion values are defined in the annotation.
     */
    @Column(name = "file_type", nullable = false)
    @NotNull
    @ObjectTypeConverter(name = "rsp_file_type", objectType = MgmtSupportPackageFileType.class, dataType = Integer.class, conversionValues = {
            @ConversionValue(objectValue = "BASELINE_INVENTORY", dataValue = "0"),
            @ConversionValue(objectValue = "INSTALLATION_ROLLBACK_PLAN", dataValue = "1"),
            @ConversionValue(objectValue = "DTC_BLACKLIST", dataValue = "2"),
            @ConversionValue(objectValue = "RULE_ENGINE_CONFIG", dataValue = "3"),
            @ConversionValue(objectValue = "PROXI", dataValue = "4"),
            @ConversionValue(objectValue = "PROXI_SIGNATURE", dataValue = "5"),
            @ConversionValue(objectValue = "WHATS_NEW", dataValue = "6"),
            @ConversionValue(objectValue = "UDS_GLOBAL_PRE_INSTALL", dataValue = "7"),
            @ConversionValue(objectValue = "UDS_GLOBAL_POST_INSTALL", dataValue = "8")
    })
    @Convert("rsp_file_type")
    @NotNull
    private MgmtSupportPackageFileType fileType;

    /**
     * A list of RSP rollouts associated with this RSP. It is annotated with JPA annotations to define its mapping to the 'rsp_rollouts' table in the database.
     * The mappedBy attribute is used to specify that the 'supportPackage' field in the JpaRspRollout class is the owner of the relationship.
     * The list is initialized as an empty list in the constructor.
     */
    @OneToMany(mappedBy = "supportPackage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JpaRspRollout> rspRollouts;

    /**
     * The size of the file associated with this RSP, in bytes.
     * This field is nullable and maps to the `file_size` column in the database.
     */
    @Column(name = "file_size", nullable = true)
    private Long fileSize;


    /**
     * Overrides the getRspRollouts method from the Rsp interface to provide a list of RspRollout objects.
     * It converts the list of JpaRspRollout objects to a list of RspRollout objects using a stream and the map function.
     *
     * @return A list of RspRollout objects associated with this RSP.
     */
    @Override
    public List<RspRollout> getRspRollouts() {
        return rspRollouts.stream().map(RspRollout.class::cast).toList();
    }

    @Override
    public MgmtSupportPackageFileType getRollout() {
        return this.fileType;
    }
}

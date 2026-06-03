package org.eclipse.hawkbit.repository.jpa.model;

import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;
import org.eclipse.hawkbit.repository.model.Esp;
import org.eclipse.hawkbit.repository.model.EspEcuRollout;
import org.eclipse.persistence.annotations.ConversionValue;
import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.ObjectTypeConverter;

/**
 * This class represents an Entity in the database that extends {@link AbstractBaseSupportPackage} and implements the {@link Esp} interface.
 * It is annotated with JPA annotations to define its mapping to the database table "sp_esp".
 * <p>
 * The class has two fields:
 * - {@link #fileType}: Represents the type of the ESP (Management Support Package) file. It is annotated with JPA annotations to define its column name,
 * non-null constraint, object type converter, and conversion values.
 * - {@link #espEcuRollouts}: Represents the list of {@link JpaEspEcuRollout} objects associated with this ESP. It is annotated with JPA annotations to define
 * the mappedBy field, cascade type, and orphan removal.
 * <p>
 * The class overrides the {@link #getEspEcuRollouts()} method from the {@link Esp} interface. It returns a list of {@link EspEcuRollout} objects by converting
 * the {@link JpaEspEcuRollout} objects in the {@link #espEcuRollouts} list.
 */
@Entity
@Table(name = "sp_esp")
@Getter
@Setter
public class JpaEsp extends AbstractBaseSupportPackage implements Esp {
    @Column(name = "file_type", nullable = false)
    @NotNull
    @ObjectTypeConverter(name = "esp_file_type", objectType = MgmtSupportPackageFileType.class, dataType = Integer.class, conversionValues = {
            @ConversionValue(objectValue = "VARIANT_CODING", dataValue = "0"),
            @ConversionValue(objectValue = "LICENSE", dataValue = "1"),
            @ConversionValue(objectValue = "UDS_FLOW", dataValue = "2"),
            @ConversionValue(objectValue = "ECU_SCRIPT", dataValue = "3"),
            @ConversionValue(objectValue = "ADA_CERTIFICATE", dataValue = "4"),
            @ConversionValue(objectValue = "ADA_LICENSE", dataValue = "5")
    })
    @Convert("esp_file_type")
    @NotNull
    private MgmtSupportPackageFileType fileType;

    @OneToMany(mappedBy = "supportPackage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JpaEspEcuRollout> espEcuRollouts;

    /**
     * The size of the file associated with this ESP, in bytes.
     * This field is nullable and maps to the `file_size` column in the database.
     */
    @Column(name = "file_size", nullable = true)
    private Long fileSize;

    @Override
    public List<EspEcuRollout> getEspEcuRollouts() {
        return espEcuRollouts.stream().map(EspEcuRollout.class::cast).toList();
    }
}

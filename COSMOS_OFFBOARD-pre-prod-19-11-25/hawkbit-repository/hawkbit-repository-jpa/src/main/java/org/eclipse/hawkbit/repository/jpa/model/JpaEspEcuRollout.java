package org.eclipse.hawkbit.repository.jpa.model;

import jakarta.persistence.FetchType;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.hawkbit.repository.model.EspEcuRollout;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * This class represents a relationship between a support package, a rollout, and an ECU node address.
 * It is a JPA entity that maps to the "sp_esp_ecu_rollout" table in the database.
 * The class extends AbstractJpaTenantAwareBaseEntity and implements the EspEcuRollout interface.
 *
 * The class has three fields:
 * - supportPackage: A reference to the support package (JpaEsp) associated with this relationship.
 * - controllerId: A string representing the Vehicle Identification Number (VIN) associated with this relationship.
 * - rollout: A reference to the rollout (JpaRollout) associated with this relationship.
 * - ecuNodeAddress: A string representing the ECU node address associated with this relationship.
 *
 * The class is annotated with @Entity, @Table, and @Getter/@Setter to enable JPA mapping and automatic generation of getters and setters.
 * The @Table annotation specifies the table name and a unique constraint on the combination of package_id, rollout_id, and ecu_node_addr.
 * The @ManyToOne annotations define the relationships between this entity and other entities (JpaEsp and JpaRollout).
 * The @JoinColumn annotations specify the column names and foreign key constraints for these relationships.
 *
 */
@Entity
@Table(name = "sp_esp_ecu_rollout",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"package_id", "rollout_id", "ecu_node_addr"}, name = "uk_sp_rsp_rollout")})
@Getter
@Setter
public class JpaEspEcuRollout  extends AbstractJpaTenantAwareBaseEntity implements EspEcuRollout {

    @ManyToOne(cascade = CascadeType.ALL, optional = false, targetEntity = JpaEsp.class,fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_esp_package_id"))
    private JpaEsp supportPackage;

    @Column(name="controller_id")
    private String controllerId;

    @ManyToOne(cascade = {CascadeType.DETACH,CascadeType.MERGE,CascadeType.PERSIST,CascadeType.REFRESH}, optional = false, targetEntity = JpaRollout.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "rollout_id", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_esp_rollout_id"))
    private JpaRollout rollout;

    @Column(name = "ecu_node_addr")
    private String ecuNodeAddress;

}

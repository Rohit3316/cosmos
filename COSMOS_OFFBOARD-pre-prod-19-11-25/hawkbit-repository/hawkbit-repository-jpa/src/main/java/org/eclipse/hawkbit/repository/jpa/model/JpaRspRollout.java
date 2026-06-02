package org.eclipse.hawkbit.repository.jpa.model;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.hawkbit.repository.model.RspRollout;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Represents an Rsp and a rollout in the Cosmos repository.
 * This entity is mapped to the "sp_rsp_rollout" table in the database.
 */
@Entity
@Table(name = "sp_rsp_rollout", uniqueConstraints = {@UniqueConstraint(columnNames = {"package_id", "rollout_id"}, name = "uk_sp_esp_ecu_rollout")})
@Getter
@Setter
public class JpaRspRollout extends AbstractJpaTenantAwareBaseEntity implements RspRollout {

    /**
     * The support package associated with this relationship.
     * This field is mapped to the "package_id" column in the database.
     * It is a required field and cannot be null.
     *
     * @see JpaRsp
     */
    @ManyToOne(cascade = CascadeType.ALL, optional = false, targetEntity = JpaRsp.class)
    @JoinColumn(name = "package_id", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_rsp_package_id"))
    private JpaRsp supportPackage;

    /**
     * The rollout associated with this relationship.
     * This field is mapped to the "rollout_id" column in the database.
     * It is a required field and cannot be null.
     *
     * @see JpaRollout
     */
    @ManyToOne(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, optional = false, targetEntity = JpaRollout.class)
    @JoinColumn(name = "rollout_id", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_rsp_rollout_id"))
    private JpaRollout rollout;

}

package org.eclipse.hawkbit.repository.jpa.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.eclipse.persistence.annotations.ConversionValue;
import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.ObjectTypeConverter;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Entity to store the userAcceptanceStatus.
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "sp_action_status_user_acceptance")
@Entity
public class JpaActionStatusUserAcceptance implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sp_action_status_user_acceptance_seq")
    @SequenceGenerator(name = "sp_action_status_user_acceptance_seq", sequenceName = "sp_action_status_user_acceptance_seq", allocationSize = 1)
    private Long id;

    @Column(name = "time_stamp_of_prompt", updatable = false)
    private long timeStampOfPrompt;

    @Column(name = "user_response", nullable = false, updatable = false)
    @ObjectTypeConverter(name = "user_response", objectType = DeviceActionStatus.class, dataType = Integer.class, conversionValues = {
            @ConversionValue(objectValue = "USER_SCHEDULED", dataValue = "26"),
            @ConversionValue(objectValue = "USER_ACCEPTED", dataValue = "27"),
            @ConversionValue(objectValue = "USER_IGNORED", dataValue = "28")
    })
    @Convert("user_response")
    @NotNull
    private DeviceActionStatus userResponse;

    @Column(name = "prompt", updatable = false)
    private String prompt;

    @Column(name = "vin", updatable = false)
    private String vin;

    @Column(name = "ota_master_serial_number", updatable = false)
    private String otaMasterSerialNumber;

    @Column(name = "ecu_hmi_serial_number", updatable = false)
    private String ecuHMISerialNumber;

    @Column(name = "scheduled_time", updatable = false)
    private long scheduledTime;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "action_status_id", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_act_stat_action"))
    @NotNull
    private JpaActionStatus actionStatus;

}

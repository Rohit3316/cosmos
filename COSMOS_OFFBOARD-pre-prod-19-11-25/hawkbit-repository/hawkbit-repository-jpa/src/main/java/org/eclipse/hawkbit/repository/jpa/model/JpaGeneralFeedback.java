package org.eclipse.hawkbit.repository.jpa.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cosmos.models.ddi.ExecutionType;
import org.eclipse.hawkbit.repository.model.GeneralFeedback;
import org.eclipse.persistence.annotations.ConversionValue;
import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.ObjectTypeConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * This class represents an Entity in the database that extends {@link AbstractJpaBaseEntity} and implements the {@link GeneralFeedback} interface.
 * It is annotated with JPA annotations to define its mapping to the database table "sp_general_feedback".
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table(name = "sp_general_feedback")
@Entity
public class JpaGeneralFeedback extends AbstractJpaBaseEntity implements GeneralFeedback {

    /**
     * Represents the targetId (sp_target)
     */
    @Column(name = "target_id", nullable = false)
    @NotNull
    private Long targetId;

    /**
     * Represents the feedback code
     */
    @Column(name = "code", nullable = false)
    @NotNull
    private int code;

    /**
     * Represents the list of feedback details
     */
    @Column(name = "details", nullable = false)
    @NotNull
    private String details;

    /**
     * Represents the feedback type from {@link ExecutionType}
     */
    @Column(name = "execution", nullable = false)
    @NotNull
    @ObjectTypeConverter(name = "execution_type", objectType = ExecutionType.class, dataType = String.class, conversionValues = {
            @ConversionValue(objectValue = "IDLE", dataValue = "IDLE"),
            @ConversionValue(objectValue = "ERC", dataValue = "ERC")
    })
    @Convert("execution_type")
    private ExecutionType execution;

    /**
     * Represents the list of error codes
     */
    @Column(name = "error_code")
    private String errorCode;

    /**
     * Represents the timestamp from feedback
     */
    @Column(name = "time_stamp", nullable = false)
    @NotNull
    private long timestamp;

}

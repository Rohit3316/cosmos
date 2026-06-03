package org.eclipse.hawkbit.repository.jpa.model;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.hawkbit.repository.model.DeploymentLog;
import org.eclipse.persistence.annotations.CascadeOnDelete;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * JPA implementation of {@link DeploymentLog}.
 */
@Entity
@Table(name = "sp_deployment_log")
@Getter
@Setter
public class JpaDeploymentLog extends AbstractJpaTenantAwareBaseEntity implements DeploymentLog {

    private static final long serialVersionUID = 1L;

    @Column(name = "action", nullable = false)
    private Long action;

    @Column(name = "file_original_name", length = 256)
    private String fileOriginalName;

    @Column(name = "file_name", length = 256)
    private String fileName;

    @Column(name = "sequence")
    private Integer sequence;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "byte_size")
    private Long byteSize;

    @Column(name = "byte_range")
    private Long byteRange;

    @Column(name = "is_last_chunk")
    private Boolean isLastChunk;

    @Column(name = "is_last_file", nullable = false)
    private Boolean isLastFile;

    @Column(name = "sha256_hash", length = 64)
    private String sha256Hash;

    @Column(name = "file_path")
    private String filePath;

    @CascadeOnDelete
    @ManyToOne
    @JoinColumn(name = "action", referencedColumnName = "id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_act_log_action"))
    private JpaAction actionLog;

    public JpaDeploymentLog() {
        // Default constructor
    }


    public JpaDeploymentLog(Long action, String fileName, Integer sequence, Long fileSize,
                            Long byteSize, Long byteRange, Boolean isLastChunk, Boolean isLastFile, String sha256Hash, String filePath) {
        //Parameterized constructor
    }

}
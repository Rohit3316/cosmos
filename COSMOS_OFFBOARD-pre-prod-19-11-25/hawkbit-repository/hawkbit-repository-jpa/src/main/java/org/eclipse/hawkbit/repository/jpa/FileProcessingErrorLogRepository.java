package org.eclipse.hawkbit.repository.jpa;

import org.cosmos.models.sqs.FileType;
import org.eclipse.hawkbit.repository.jpa.model.JpaFileProcessingErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 *  Repository for {@link JpaFileProcessingErrorLog}.
 */
@Transactional(readOnly = true)
@Repository
public interface FileProcessingErrorLogRepository extends JpaRepository<JpaFileProcessingErrorLog, Long>{

    /**
     * Find log messages by log type id and file type
     * @param logTypeId - log type id
     * @param fileType - file type
     * @return - list of log messages
     */
    List<JpaFileProcessingErrorLog> findLogMessageByLogTypeIdAndFileType(Long logTypeId, FileType fileType);


}

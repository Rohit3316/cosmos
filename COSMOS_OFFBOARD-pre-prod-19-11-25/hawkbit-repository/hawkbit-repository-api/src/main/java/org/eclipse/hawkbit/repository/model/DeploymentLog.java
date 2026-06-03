package org.eclipse.hawkbit.repository.model;

public interface DeploymentLog extends TenantAwareBaseEntity {

	
	Long getAction(); 
	
	String getFileOriginalName();
	
	String getFileName();
	
	Integer getSequence();
	
	Long getFileSize();

	Long getByteSize();

	Long getByteRange();

	Boolean getIsLastChunk();

	Boolean getIsLastFile();
	
	String getSha256Hash();

	String getFilePath();
	
	Action getActionLog();
	
}

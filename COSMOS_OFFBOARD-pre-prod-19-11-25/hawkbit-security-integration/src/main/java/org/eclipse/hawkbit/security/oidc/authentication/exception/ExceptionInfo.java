package org.eclipse.hawkbit.security.oidc.authentication.exception;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExceptionInfo {
    private String name;
    private String message;
}

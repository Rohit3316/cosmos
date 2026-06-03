/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

/**
 * <p>
 * The {@link Version} is the user that can do operation on the platform
 * </p>
 */
public interface Version extends BaseEntity {
	
	
	/**
	 * Maximum Size of Version name
	 */
	int NAME_MAX_SIZE = 100;

    /**
     * Maximum length of name value.
     */
    int DESCRIPTION_MAX_SIZE = 100;
    
    /**
	 * Maximum Size of Version Number
	 */
	int NUMBER_MAX_SIZE = 9;

    /**
     *
     * @return the username of the user
     */
    String getName();

    /**
     *
     * @return the password of the user
     */
    Integer getNumber();

    /**
     *
     * @return the first name of the user
     */
    SoftwareModule getSoftwareModuleId();

    /**
     *
     * @return the lastname of the user
     */
    String getDescription();
}

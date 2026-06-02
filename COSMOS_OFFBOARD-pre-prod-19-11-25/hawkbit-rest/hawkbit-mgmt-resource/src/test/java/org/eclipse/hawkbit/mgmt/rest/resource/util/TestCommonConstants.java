package org.eclipse.hawkbit.mgmt.rest.resource.util;

/**
 * This class consists of all the common constants that are used across multiple test classes.
 */
public class TestCommonConstants {

    private TestCommonConstants() {
    }

    /**
     * TenantID - to be replaced with actual tenantId in test cases.
     */


    public static final String TENANT_ID = "{tenantId}";

    public static final String SCHEDULE = "schedule";
    public static final String DURATION = "duration";
    public static final String TIMEZONE = "timezone";

    public static final String FORCED_UPDATE_TYPE = "forced";

    public static final String VERSION = "version";

    public static final String FORWARD_SLASH = "/";

    public static final String OPEN_FLOWER_BRACKET = "{";

    public static final String CLOSE_FLOWER_BRACKET = "}";

    public static final String KEY = "key";

    public static final String METADATA = "metadata";

    public static final String SOFTWARE_MODULE = FORWARD_SLASH + "softwaremodules";

    public static final String JSON_PATH_FOR_SIZE = "$.size";

    public static final String TARGETS = FORWARD_SLASH + "targets";

    public static final String TARGET_FILTERS = "target-filters";

    public static final String KNOWN_TARGET_ID = "knownTargetId";

    public static final String ANOTHER_VERSION = "anotherVersion";

    public static final String DS_ID = FORWARD_SLASH + OPEN_FLOWER_BRACKET + "dsId" + CLOSE_FLOWER_BRACKET;

    public static final String KNOWN_KEY = "knownKey";

    public static final String KNOWN_VALUE = "knownValue";

    public static final String TOTAL = "total";

    public static final String TARGET_AND_FILTERS_URL_PATH = TARGETS + FORWARD_SLASH + TARGET_FILTERS;

    public static final String DS_METADATA_KEY_URL = DS_ID + FORWARD_SLASH + METADATA + FORWARD_SLASH + OPEN_FLOWER_BRACKET
            + KEY + CLOSE_FLOWER_BRACKET;
}

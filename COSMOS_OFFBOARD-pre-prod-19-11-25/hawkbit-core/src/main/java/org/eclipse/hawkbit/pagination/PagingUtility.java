/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.pagination;

import org.cosmos.models.mgmt.MgmtRestConstants;
import org.eclipse.hawkbit.repository.ActionFields;
import org.eclipse.hawkbit.repository.ActionStatusFields;
import org.eclipse.hawkbit.repository.ArtifactsFields;
import org.eclipse.hawkbit.repository.DistributionSetFields;
import org.eclipse.hawkbit.repository.DistributionSetMetadataFields;
import org.eclipse.hawkbit.repository.DistributionSetTypeFields;
import org.eclipse.hawkbit.repository.InventoryFields;
import org.eclipse.hawkbit.repository.PollingFeedbackFields;
import org.eclipse.hawkbit.repository.RolloutFields;
import org.eclipse.hawkbit.repository.RolloutGroupFields;
import org.eclipse.hawkbit.repository.SoftwareModuleFields;
import org.eclipse.hawkbit.repository.SoftwareModuleMetadataFields;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeFields;
import org.eclipse.hawkbit.repository.TagFields;
import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.TargetFilterQueryFields;
import org.eclipse.hawkbit.repository.TargetTypeFields;
import org.eclipse.hawkbit.repository.TenantFields;
import org.eclipse.hawkbit.repository.SupportPackageFields;
import org.eclipse.hawkbit.repository.VehicleFields;
import org.eclipse.hawkbit.repository.EcuModelFields;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;



/**
 * Utility class for for paged body generation.
 */
public final class PagingUtility {
    /*
     * utility constructor private.
     */
    private PagingUtility() {
    }

    public static int sanitizeOffsetParam(final int offset) {
        if (offset < 0) {
            return MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET_VALUE;
        }
        return offset;
    }

    public static int sanitizePageLimitParam(final int pageLimit) {
        if (pageLimit < 1) {
            return MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE;
        } else if (pageLimit > MgmtRestConstants.REQUEST_PARAMETER_PAGING_MAX_LIMIT) {
            return MgmtRestConstants.REQUEST_PARAMETER_PAGING_MAX_LIMIT;
        }
        return pageLimit;
    }

    public static Sort sanitizeTargetSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Sort.Direction.ASC, TargetFields.CONTROLLERID.getFieldName());
        }
        return Sort.by(SortUtility.parse(TargetFields.class, sortParam));
    }

    public static Sort sanitizeTargetTypeSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Sort.Direction.ASC, TargetTypeFields.ID.getFieldName());
        }
        return Sort.by(SortUtility.parse(TargetFields.class, sortParam));
    }

    public static Sort sanitizeTagSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Sort.Direction.ASC, TagFields.ID.getFieldName());
        }
        return Sort.by(SortUtility.parse(TagFields.class, sortParam));
    }

    public static Sort sanitizeTargetFilterQuerySortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Sort.Direction.ASC, TargetFilterQueryFields.ID.getFieldName());
        }
        return Sort.by(SortUtility.parse(TargetFilterQueryFields.class, sortParam));
    }

    public static Sort sanitizePollingFeedbackSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, PollingFeedbackFields.POLLING_ID.getFieldName());
        }
        return Sort.by(SortUtility.parse(PollingFeedbackFields.class, sortParam));
    }

    public static Sort sanitizeSoftwareModuleSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, SoftwareModuleFields.ID.getFieldName());
        }
        return Sort.by(SortUtility.parse(SoftwareModuleFields.class, sortParam));
    }

    /**
     * Returns a {@link Sort} object for artifact sorting based on the provided sort parameter.
     * <p>
     * If the sort parameter is {@code null}, empty, or blank, the default sort is applied
     * (ascending by artifact ID). Otherwise, the sort parameter is parsed and used.
     * </p>
     *
     * @param sortParam the sorting parameter (e.g., "filename:DESC"), may be {@code null} or blank
     * @return a {@link Sort} object for artifact sorting
     */
    public static Sort sanitizeArtifactsSortParam(final String sortParam) {
        if (sortParam == null || sortParam.trim().isEmpty()) {
            // Default: ascending by artifact ID
            return Sort.by(Direction.ASC, ArtifactsFields.ID.getFieldName());
        }
        // Parse and apply custom sort
        return Sort.by(SortUtility.parse(ArtifactsFields.class, sortParam.trim()));
    }

    public static Sort sanitizeSoftwareModuleTypeSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, SoftwareModuleTypeFields.ID.getFieldName());
        }
        return Sort.by(SortUtility.parse(SoftwareModuleTypeFields.class, sortParam));
    }

    public static Sort sanitizeDistributionSetSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, DistributionSetFields.ID.getFieldName());
        }
        return Sort.by(SortUtility.parse(DistributionSetFields.class, sortParam));
    }

    public static Sort sanitizeDistributionSetTypeSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, DistributionSetTypeFields.ID.getFieldName());
        }
        return Sort.by(SortUtility.parse(DistributionSetTypeFields.class, sortParam));
    }

    public static Sort sanitizeActionSortParam(final String sortParam) {
        if (sortParam == null) {
            // default sort is DESC in case of action to match behavior
            // of management UI (last entry on top)
            return Sort.by(Direction.DESC, ActionFields.ID.getFieldName());
        }
        return Sort.by(SortUtility.parse(ActionFields.class, sortParam));
    }

    public static Sort sanitizeActionStatusSortParam(final String sortParam) {
        if (sortParam == null) {
            // default sort is DESC in case of action status to match behavior
            // of management UI (last entry on top)
            return Sort.by(Direction.DESC, ActionStatusFields.ID.getFieldName());
        }
        return Sort.by(SortUtility.parse(ActionStatusFields.class, sortParam));
    }

    public static Sort sanitizeDistributionSetMetadataSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, DistributionSetMetadataFields.KEY.getFieldName());
        }
        return Sort.by(SortUtility.parse(DistributionSetMetadataFields.class, sortParam));
    }

    public static Sort sanitizeSoftwareModuleMetadataSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, SoftwareModuleMetadataFields.KEY.getFieldName());
        }
        return Sort.by(SortUtility.parse(SoftwareModuleMetadataFields.class, sortParam));
    }

    public static Sort sanitizeRolloutSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, RolloutFields.ID.getFieldName());
        }
        return Sort.by(SortUtility.parse(RolloutFields.class, sortParam));
    }

    public static Sort sanitizeRolloutGroupSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, RolloutGroupFields.ID.getFieldName());
        }
        return Sort.by(SortUtility.parse(RolloutGroupFields.class, sortParam));
    }

    public static Sort sanitizeTenantSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, TenantFields.ID.getFieldName());
        }
        return Sort.by(SortUtility.parse(TenantFields.class, sortParam));
    }
    public static Sort sanitizeInventoryFieldsSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, InventoryFields.ID.getFieldName());
        }
        return Sort.by(SortUtility.parse(InventoryFields.class, sortParam));
    }


    public  static Sort sanitizeVehicleSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, VehicleFields.ID.getFieldName());
        }
        return Sort.by(SortUtility.parse(VehicleFields.class, sortParam));
    }

   public static Sort sanitizeSupportPackagesParam(final String sortParam){
        if(sortParam==null){
            //default
            return Sort.by(Direction.ASC, SupportPackageFields.ID.getFieldName());
        }
        return Sort.by(SortUtility.parse(SupportPackageFields.class, sortParam));
    }

    public static Sort sanitizeECUModuleSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, EcuModelFields.ID.getFieldName());
        }
        return Sort.by(SortUtility.parse(EcuModelFields.class, sortParam));
    }
}

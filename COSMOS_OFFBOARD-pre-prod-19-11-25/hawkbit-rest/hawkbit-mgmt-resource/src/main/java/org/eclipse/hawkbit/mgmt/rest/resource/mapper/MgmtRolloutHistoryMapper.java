package org.eclipse.hawkbit.mgmt.rest.resource.mapper;

import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutHistoryResponse;
import org.eclipse.hawkbit.repository.jpa.DistributionSetModuleRepository;
import org.eclipse.hawkbit.repository.jpa.model.DistributionSetModule;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.stream.Collectors;

public final class MgmtRolloutHistoryMapper {

    private MgmtRolloutHistoryMapper() {
        // Utility class
    }

    public static List<MgmtRolloutHistoryResponse> toResponse(List<DistributionSetModule> dsModules, Rollout rollout) {
        return dsModules.stream().map(dsModule -> {
            MgmtRolloutHistoryResponse dto = new MgmtRolloutHistoryResponse();
            dto.setScomoid(dsModule.getSm().getName());
            dto.setTargetVersion(dsModule.getVersion().getName());
            dto.setRolloutName(rollout.getName());
            dto.setRolloutStartDate(rollout.getStartAt() != null ? rollout.getStartAt().toString() : null);
            dto.setRolloutEndDate(rollout.getEndAt() != null ? rollout.getEndAt().toString() : null);
            dto.setLatestStatus(rollout.getStatus() != null ? rollout.getStatus().name() : null);
            dto.setMessage(rollout.getDescription());
            dto.setLatestStatusDate(String.valueOf(rollout.getLastModifiedAt()));
            return dto;
        }).collect(Collectors.toList());
    }

    public static List<MgmtRolloutHistoryResponse> mapToRolloutHistoryResponse(
            List<Rollout> rollouts,
            DistributionSetModuleRepository dsModuleRepo) {
        return rollouts.stream()
                .flatMap(rollout -> {
                    List<DistributionSetModule> dsModules =
                            dsModuleRepo.findByDsSet(rollout.getDistributionSet().getId(), Sort.unsorted());
                    return toResponse(dsModules, rollout).stream();
                })
                .collect(Collectors.toList());
    }
}


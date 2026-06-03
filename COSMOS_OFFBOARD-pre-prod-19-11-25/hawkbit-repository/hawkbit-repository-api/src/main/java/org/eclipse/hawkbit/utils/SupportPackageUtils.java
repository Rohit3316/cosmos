package org.eclipse.hawkbit.utils;

import lombok.extern.slf4j.Slf4j;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.SupportPackageManagement;
import org.eclipse.hawkbit.repository.model.BaseSupportPackage;
import org.eclipse.hawkbit.repository.model.Esp;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Rsp;
import org.eclipse.hawkbit.repository.model.Target;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import jakarta.validation.ValidationException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;


@Slf4j
public final class SupportPackageUtils {

    private static final int FIRST_PAGE_NUMBER = 0;
    private static final int PAGE_SIZE = 100;
    RolloutGroupManagement rolloutGroupManagement;
    SupportPackageManagement supportPackageManagement;

    private SupportPackageUtils(RolloutGroupManagement rolloutGroupManagement, SupportPackageManagement supportPackageManagement) {
        this.supportPackageManagement = supportPackageManagement;
        this.rolloutGroupManagement = rolloutGroupManagement;
    }


    public static SupportPackageUtils withEspValidationContextForFrozenRollout(final RolloutGroupManagement rolloutGroupManagement, final SupportPackageManagement supportPackageManagement) {
        return new SupportPackageUtils(rolloutGroupManagement, supportPackageManagement);
    }

    public static void validateRspToHandleFrozenRollout(final List<Rsp> rspList,
                                                        final List<MgmtSupportPackageFileType> mandatoryRspFileTypes,
                                                        final Rollout rollout) {
        log.debug("Validating RSPs for frozen rollout: rolloutId={}, rolloutName={}, totalRspCount={}",
                rollout.getId(), rollout.getName(), rspList.size());
        validateRspUploadStatus(rspList);
        log.debug("All RSPs are verified as uploaded to storage for rolloutId={}", rollout.getId());
        if (!mandatoryRspFileTypes.isEmpty()) {
            log.debug("Validating mandatory RSPs for rolloutId={}, mandatoryRspCount={}",
                    rollout.getId(), mandatoryRspFileTypes.size());
            checkIfMandatoryRspExists(rollout, mandatoryRspFileTypes, rspList);
        } else {
            log.debug("No mandatory RSPs required for rolloutId={}", rollout.getId());
        }
    }


    /**
     * Validates that all RSPs have been successfully uploaded to storage or are in the process of being uploaded to the CDN.
     * Checks if the RSPs have one of the valid statuses: STORAGE_UPLOAD_SUCCESSFUL, UPLOADING_TO_CDN, or CDN_UPLOAD_SUCCESSFUL.
     *
     * @param rspList the list of RSPs to validate
     * @throws ValidationException if any RSP has an invalid status
     */
    private static void validateRspUploadStatus(final List<Rsp> rspList) {
        log.debug("Starting validation of RSP upload statuses. Total RSPs to validate: {}", rspList.size());

        final EnumSet<FileTransferStatus> validStatuses = EnumSet.of(
                FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL,
                FileTransferStatus.UPLOADING_TO_CDN,
                FileTransferStatus.CDN_UPLOAD_SUCCESSFUL
        );

        log.debug("Valid statuses for RSPs: {}", validStatuses);

        List<Long> unUploadedRspIds = rspList.stream()
                .filter(rsp -> !validStatuses.contains(rsp.getSupportPackageFileStatus()))
                .map(Rsp::getId)
                .distinct()
                .toList();

        if (!unUploadedRspIds.isEmpty()) {
            log.error("Validation failed: RSPs with IDs {} are not uploaded. Valid statuses: {}",
                    unUploadedRspIds, validStatuses);
            throw new ValidationException("Validation failed: One or more RSPs have an invalid status. " +
                    "Valid statuses are: " + validStatuses);
        }

        log.info("Validation successful: All RSPs have valid upload statuses.");
    }

    public static void checkIfMandatoryRspExists(final Rollout rollout, final List<MgmtSupportPackageFileType> mandatoryRspFileTypes, final List<Rsp> rspList) throws ValidationException {

        //find rollout groups and iterate over them and for each rollout group iterate over targets
        List<MgmtSupportPackageFileType> rspFileTypesList = rspList.stream()
                .map(BaseSupportPackage::getFileType)
                .toList();
        if (mandatoryRspFileTypes.isEmpty()) {
            return;
        }
        log.info("mandatory rsp:,{}", mandatoryRspFileTypes);
        if (mandatoryRspFileTypes.size() > rspFileTypesList.size() || !containsAllEnums(mandatoryRspFileTypes, rspFileTypesList)) {

            throw new ValidationException("rollout " + rollout.getName() + " do not contains the mandatory RSP");
        }
    }

    private static <E extends Enum<E>> boolean containsAllEnums(final List<E> list1, final List<E> list2) throws ValidationException {
        // Convert the first list to a Set for O(1) lookup
        Set<E> set2 = EnumSet.copyOf(list2);
        return set2.containsAll(list1);
    }

    /**
     * Validates that all ESPs have been successfully uploaded to storage or are in the process of being uploaded to the CDN.
     * Checks if the ESPs have one of the valid statuses: STORAGE_UPLOAD_SUCCESSFUL, UPLOADING_TO_CDN, or CDN_UPLOAD_SUCCESSFUL.
     *
     * @param espList the list of RSPs to validate
     * @throws ValidationException if any ESP has an invalid status
     */
    public static void validateEspUploadStatus(List<Esp> espList) {
        log.debug("Starting validation of ESP upload statuses. Total ESPs to validate: {}", espList.size());

        final EnumSet<FileTransferStatus> validStatuses = EnumSet.of(
                FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL,
                FileTransferStatus.UPLOADING_TO_CDN,
                FileTransferStatus.CDN_UPLOAD_SUCCESSFUL
        );

        log.debug("Valid statuses for ESPs: {}", validStatuses);

        List<Long> unUploadedEspIds = espList.stream()
                .filter(esp -> !validStatuses.contains(esp.getSupportPackageFileStatus()))
                .map(Esp::getId)
                .distinct()
                .toList();

        if (!unUploadedEspIds.isEmpty()) {
            log.error("Validation failed: ESPs with IDs {} are not uploaded. Valid statuses: {}",
                    unUploadedEspIds, validStatuses);
            throw new ValidationException("Validation failed: One or more ESPs have an invalid status. " +
                    "Valid statuses are: " + validStatuses);
        }

        log.info("Validation successful: All ESPs have valid upload statuses.");
    }

    public void checkAllEspUploadedAndMandatoryEspAvailable(final List<MgmtSupportPackageFileType> mandatoryEspFileTypes, final Long rolloutId, final RolloutGroup group) throws ValidationException {
        int page = FIRST_PAGE_NUMBER;
        Page<Target> targetPage;
        do {
            targetPage = this.rolloutGroupManagement.findTargetsOfRolloutGroup(PageRequest.of(page, PAGE_SIZE), group.getId());
            targetPage.forEach(target -> {
                String controllerId = target.getControllerId();
                List<Esp> espList = this.supportPackageManagement.getEspByRolloutIdAndControllerId(controllerId, rolloutId);
                List<MgmtSupportPackageFileType> espFileTypeList = espList.stream()
                        .map(BaseSupportPackage::getFileType)
                        .toList();
                if (mandatoryEspFileTypes.isEmpty()) {
                    return;
                }
                if (espFileTypeList.size() < mandatoryEspFileTypes.size() || !containsAllEnums(mandatoryEspFileTypes, espFileTypeList)) {
                    throw new ValidationException("controller with controllerId " + target.getControllerId() + " do not contain all the mandatory Esp");
                }
            });
            page++;

        } while (targetPage.hasNext());
    }
}

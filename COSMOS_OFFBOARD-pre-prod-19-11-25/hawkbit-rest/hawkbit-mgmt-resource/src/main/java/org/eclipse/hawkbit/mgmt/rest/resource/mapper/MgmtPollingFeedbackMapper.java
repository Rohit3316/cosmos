package org.eclipse.hawkbit.mgmt.rest.resource.mapper;

import java.util.Collection;
import java.util.List;
import org.cosmos.models.mgmt.polling.dto.MgmtPollingFeedback;
import org.cosmos.models.mgmt.polling.dto.MgmtPollingFeedback.FeedbackContent;
import org.cosmos.models.mgmt.polling.dto.MgmtPollingFeedback.FeedbackLinks;
import org.eclipse.hawkbit.repository.model.PollingFeedback;

public class MgmtPollingFeedbackMapper {

    // Private constructor to prevent instantiation
    private MgmtPollingFeedbackMapper() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Mapper to map polling feedback into mgmt response
     *
     * @param pollingFeedbackList list of the {@link PollingFeedback}
     * @return {@link MgmtPollingFeedback}
     */
    public static MgmtPollingFeedback toResponse(final Collection<PollingFeedback> pollingFeedbackList) {
        MgmtPollingFeedback mgmtPollingFeedback = new MgmtPollingFeedback();

        List<FeedbackContent> content = pollingFeedbackList.stream().map(pollingFeedback -> {
            FeedbackContent feedbackContent = new FeedbackContent();
            feedbackContent.setFeedback(pollingFeedback.getFeedback());
            feedbackContent.setCreatedAt(pollingFeedback.getCreatedAt());
            return feedbackContent;
        }).toList();

        mgmtPollingFeedback.setContent(content);
        FeedbackLinks feedbackLinks = new FeedbackLinks();
        feedbackLinks.setEmpty(true);
        mgmtPollingFeedback.setFeedbackLinks(feedbackLinks);
        mgmtPollingFeedback.setSize((long) content.size());
        mgmtPollingFeedback.setTotal((long) content.size());

        return mgmtPollingFeedback;
    }
}

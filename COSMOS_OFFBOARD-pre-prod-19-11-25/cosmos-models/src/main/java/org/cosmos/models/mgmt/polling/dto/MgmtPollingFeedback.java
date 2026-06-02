package org.cosmos.models.mgmt.polling.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.cosmos.models.mgmt.MgmtNamedEntity;

import java.util.List;
import java.util.Objects;

/**
 * A json annotated rest model for PollingFeedback to RESTful API representation.
 */
@JsonInclude(Include.NON_NULL)
public class MgmtPollingFeedback extends MgmtNamedEntity {
    @JsonProperty("content")
    private List<FeedbackContent> content;

    @JsonProperty("links")
    private FeedbackLinks feedbackLinks;

    @JsonProperty("size")
    private Long size;

    @JsonProperty("total")
    private Long total;


    public List<FeedbackContent> getContent() {
        return content;
    }

    public void setContent(List<FeedbackContent> content) {
        this.content = content;
    }

    public FeedbackLinks getFeedbackLinks() {
        return feedbackLinks;
    }

    public void setFeedbackLinks(FeedbackLinks feedbackLinks) {
        this.feedbackLinks = feedbackLinks;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MgmtPollingFeedback that = (MgmtPollingFeedback) o;
        return Objects.equals(content, that.content) && Objects.equals(feedbackLinks, that.feedbackLinks) && Objects.equals(size, that.size) && Objects.equals(total, that.total);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), content, feedbackLinks, size, total);
    }

    public static class FeedbackContent {
        @JsonProperty("feedback")
        private String feedback;

        @JsonProperty("createdAt")
        private Long createdAt;

        public String getFeedback() {
            return feedback;
        }

        public void setFeedback(String feedback) {
            this.feedback = feedback;
        }

        public Long getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Long createdAt) {
            this.createdAt = createdAt;
        }
    }

    public static class FeedbackLinks {
        @JsonProperty("empty")
        private boolean empty;

        public boolean isEmpty() {
            return empty;
        }

        public void setEmpty(boolean empty) {
            this.empty = empty;
        }
    }
}
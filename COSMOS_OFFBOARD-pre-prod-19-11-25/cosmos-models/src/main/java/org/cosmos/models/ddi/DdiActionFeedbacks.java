package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiActionFeedbacks {


    @Schema(example = "1737079413000")
    private final Long time;

    @NotNull
    @Valid
    private final List<DdiStatus> statuses;

    /**
     * Constructs an action-feedback
     *
     * @param time   time of feedback
     * @param statuses status to be appended to the action
     */
    @JsonCreator
    public DdiActionFeedbacks(@JsonProperty(value = "time") final Long time,
                              @JsonProperty(value = "statuses", required = true) final List<DdiStatus> statuses) {
        this.time = time;
        this.statuses = statuses;
    }

    public Long getTime() {
        return time;
    }

    public List<DdiStatus> getStatuses() {
        return statuses;
    }

    @Override
    public String toString() {
        return "ActionFeedback [time=" + time + ", statuses=" + statuses + "]";
    }


}

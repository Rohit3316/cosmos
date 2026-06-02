package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * <p>
 * After the HawkBit Target has executed an action, received by a GET(URL)
 * request it reports the completion of it to the HawkBit Server with a action
 * status message, i.e. with a PUT message to the feedback channel, i.e. PUT
 * URL/feedback. This message could be used not only at the end of execution but
 * also as status updates during a longer lasting execution period. The format
 * of each action answer message is defined below at each action. But it is
 * expected, that the contents of the message answers have all a similar
 * structure: The content starts with a generic header and additional elements.
 * *
 * </p>
 *
 * <p>
 * The answer header would look like: { "time": "20140511T121314", "status": {
 * "execution": "closed", "result": { "final": "success", "progress": {} }
 * "details": [], } }
 * </p>
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Deprecated
public class DdiActionFeedback {

    @Schema(example = "1750005813123L", required = true)
    private final Long time;

    @NotNull
    @Valid
    @Schema(required = true)
    private final DdiStatus status;

    /**
     * Constructs an action-feedback
     *
     * @param time
     *            time of feedback
     * @param status
     *            status to be appended to the action
     */
    @JsonCreator
    public DdiActionFeedback(@JsonProperty(value = "time") final Long time,
                             @JsonProperty(value = "status", required = true) final DdiStatus status) {
        this.time = time;
        this.status = status;
    }

    public Long getTime() {
        return time;
    }

    public DdiStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "ActionFeedback [time=" + time + ", status=" + status + "]";
    }

}

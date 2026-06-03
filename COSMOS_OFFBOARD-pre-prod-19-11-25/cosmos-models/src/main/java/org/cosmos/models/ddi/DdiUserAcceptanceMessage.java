package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Model for user acceptance message.
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiUserAcceptanceMessage {

    @NotNull
    private Long timeStampOfPrompt;

    @NotNull
    private DdiStatus.ExecutionStatus userResponse;

    @NotBlank
    private String prompt;

    @NotBlank
    private String vin;

    @NotBlank
    private String otaMasterSN;

    @NotBlank
    private String ecuHMISN;

    private Long scheduledTime;

    /**
     * Returns a JSON formatted message of this class to send to DOCG
     * @return a JSON formatted message.
     */
    public String toJsonFormattedMessage(){

        return "{"
                + "\"timeStampOfPrompt\":" + timeStampOfPrompt
                + ", \"userResponse\": \"" + userResponse + "\""
                + ", \"prompt\": \"" + prompt + "\""
                + ", \"vin\": \"" + vin + "\""
                + ", \"otaMasterSN\": \"" + otaMasterSN + "\""
                + ", \"ecuHMISN\": \"" + ecuHMISN + "\""
                + ", \"scheduledTime\":" + scheduledTime
                + "}";
    }

}

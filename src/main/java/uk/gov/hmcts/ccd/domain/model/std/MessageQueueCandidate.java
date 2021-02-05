package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import javax.persistence.Id;
import java.time.LocalDateTime;

@Data
public class MessageQueueCandidate {

    @Id
    private Long id;
    @JsonProperty("message_type")
    private String messageType;
    @JsonProperty("time_stamp")
    private LocalDateTime timeStamp;
    @JsonProperty("published")
    private LocalDateTime published;
    @JsonProperty("message_information")
    private JsonNode messageInformation;
}

package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import uk.gov.hmcts.ccd.data.JsonDataConverter;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Table(name = "message_queue_candidates")
@Entity
@Data
public class MessageQueueCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JsonProperty("message_type")
    private String messageType;
    @JsonProperty("time_stamp")
    private LocalDateTime timeStamp;
    @JsonProperty("published")
    private LocalDateTime published;
    @JsonProperty("message_information")
    @Convert(converter = JsonDataConverter.class)
    private JsonNode messageInformation;
}

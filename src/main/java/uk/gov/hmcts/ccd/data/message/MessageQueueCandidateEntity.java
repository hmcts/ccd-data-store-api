package uk.gov.hmcts.ccd.data.message;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
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
public class MessageQueueCandidateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String messageType;
    @CreationTimestamp
    private LocalDateTime timeStamp;
    private LocalDateTime published;
    @Convert(converter = JsonDataConverter.class)
    private JsonNode messageInformation;
}

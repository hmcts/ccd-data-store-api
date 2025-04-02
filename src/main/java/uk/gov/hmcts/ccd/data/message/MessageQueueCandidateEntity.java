package uk.gov.hmcts.ccd.data.message;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import uk.gov.hmcts.ccd.data.JsonDataConverter;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

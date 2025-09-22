package uk.gov.hmcts.ccd.data.message;

import uk.gov.hmcts.ccd.data.JsonDataConverter;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;

@Table(name = "message_queue_candidates")
@Entity
@Data
public class MessageQueueCandidateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, 
        generator = "message_queue_candidates_id_seq_generator")
    @SequenceGenerator(name = "message_queue_candidates_id_seq_generator", 
        sequenceName = "message_queue_candidates_id_seq", allocationSize = 1)
    private Long id;
    private String messageType;
    @CreationTimestamp(source = SourceType.DB)
    private LocalDateTime timeStamp;
    private LocalDateTime published;
    @Convert(converter = JsonDataConverter.class)
    private JsonNode messageInformation;
}

package uk.gov.hmcts.ccd.data.casedetails;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import uk.gov.hmcts.ccd.data.JsonDataConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Version;

@SuppressWarnings("checkstyle:OperatorWrap")
// too many legacy OperatorWrap occurrences on JSON strings so suppress until move to Java12+
@Table(name = "case_data_logstash_queue")
@Entity
@Getter
public class CaseDetailsLogstashQueueEntity {

    @Id
    private Long id;
    @Column(name = "case_data_id")
    private Long caseDataId;
}

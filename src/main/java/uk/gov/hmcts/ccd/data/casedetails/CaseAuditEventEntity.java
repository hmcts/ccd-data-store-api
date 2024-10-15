package uk.gov.hmcts.ccd.data.casedetails;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import uk.gov.hmcts.ccd.data.JsonDataConverter;
import uk.gov.hmcts.ccd.data.SignificantItemEntity;

import javax.persistence.CascadeType;
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
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;

import static uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventEntity.FIND_BY_CASE_DATA_ID_HQL;
import static uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventEntity.FIND_BY_CASE_DATA_ID_HQL_EXCLUDE_DATA;
import static uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventEntity.FIND_BY_ID_HQL;

@NamedQueries({
    @NamedQuery(name = CaseAuditEventEntity.FIND_BY_CASE, query =
        FIND_BY_CASE_DATA_ID_HQL_EXCLUDE_DATA
            + " ORDER BY cae.createdDate DESC"
        ),
    @NamedQuery(name = CaseAuditEventEntity.FIND_CREATE_EVENT, query =
        FIND_BY_CASE_DATA_ID_HQL
            + " AND cae.createdDate = "
            + "(select min(caeDate.createdDate) from CaseAuditEventEntity caeDate WHERE caeDate.caseDataId = :"
            + CaseAuditEventEntity.CASE_DATA_ID + ")"
        ),
    @NamedQuery(name = CaseAuditEventEntity.FIND_BY_ID, query =
        FIND_BY_ID_HQL
        )
})
@Data
@Table(name = "case_event")
@Entity
public class CaseAuditEventEntity {

    static final String FIND_BY_CASE_DATA_ID_HQL = "SELECT cae FROM CaseAuditEventEntity cae"
        + " LEFT JOIN FETCH cae.significantItemEntity WHERE cae.caseDataId = :" + CaseAuditEventEntity.CASE_DATA_ID;

    static final String FIND_BY_CASE_DATA_ID_HQL_EXCLUDE_DATA =
        "SELECT cae.id as id, cae.userId as userId, cae.eventId as eventId, cae.eventName as eventName,"
            + " cae.userFirstName as userFirstName, cae.userLastName as userLastName, cae.summary as summary,"
            + " cae.description as description, cae.createdDate as createdDate, cae.stateId as stateId,"
            + " cae.stateName as stateName, cae.securityClassification as securityClassification,"
            + " cae.caseTypeId as caseTypeId, cae.caseDataId as caseDataId, cae.caseTypeVersion as caseTypeVersion,"
            + " cae.proxiedBy as proxiedBy, cae.proxiedByLastName as proxiedByLastName,"
            + " cae.proxiedByFirstName as proxiedByFirstName"
            + " FROM CaseAuditEventEntity cae LEFT JOIN cae.significantItemEntity as significantItemEntity"
            + " WHERE cae.caseDataId = :" + CaseAuditEventEntity.CASE_DATA_ID;

    static final String FIND_BY_ID_HQL = "SELECT cae FROM CaseAuditEventEntity cae"
        + " WHERE cae.id = :" + CaseAuditEventEntity.EVENT_ID;

    static final String FIND_BY_CASE = "CaseAuditEventEntity_FIND_BY_CASE";

    static final String FIND_CREATE_EVENT = "CaseAuditEventEntity_FIND_CREATE_EVENT";

    static final String FIND_BY_ID = "CaseAuditEventEntity_FIND_BY_ID";

    static final String CASE_DATA_ID = "CASE_DATA_ID";

    static final String EVENT_ID = "EVENT_ID";

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id")
    private String userId;
    @Column(name = "user_last_name")
    private String userLastName;
    @Column(name = "user_first_name")
    private String userFirstName;
    @Column(name = "event_id")
    private String eventId;
    @Column(name = "event_name")
    private String eventName;
    @Column(name = "summary")
    private String summary;
    @Column(name = "description")
    private String description;
    @Column(name = "case_data_id")
    private Long caseDataId;
    @Column(name = "created_date")
    private LocalDateTime createdDate;
    @Column(name = "state_id")
    private String stateId;
    @Column(name = "state_name")
    private String stateName;
    @Column(name = "case_type_id")
    private String caseTypeId;
    @Column(name = "case_type_version")
    private Integer caseTypeVersion;
    @Enumerated(EnumType.STRING)
    @Column(name = "security_classification", nullable = false)
    private SecurityClassification securityClassification;
    @Column(name = "data", nullable = false)
    @Convert(converter = JsonDataConverter.class)
    private JsonNode data;
    @Column(name = "data_classification", nullable = false)
    @Convert(converter = JsonDataConverter.class)
    private JsonNode dataClassification;

    @Column(name = "proxied_by")
    private String proxiedBy;

    @Column(name = "proxied_by_last_name")
    private String proxiedByLastName;

    @Column(name = "proxied_by_first_name")
    private String proxiedByFirstName;

    @OneToOne(mappedBy = "caseEvent", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private SignificantItemEntity significantItemEntity;
}

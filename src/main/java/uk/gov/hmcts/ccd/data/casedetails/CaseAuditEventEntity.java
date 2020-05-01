package uk.gov.hmcts.ccd.data.casedetails;

import com.fasterxml.jackson.databind.JsonNode;
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
import static uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventEntity.FIND_BY_ID_HQL;

@NamedQueries({
    @NamedQuery(name = CaseAuditEventEntity.FIND_BY_CASE, query =
        FIND_BY_CASE_DATA_ID_HQL
            + " ORDER BY cae.createdDate DESC"
    ),
    @NamedQuery(name = CaseAuditEventEntity.FIND_CREATE_EVENT, query =
        FIND_BY_CASE_DATA_ID_HQL
            + " AND createdDate = "
            + "(select min(caeDate.createdDate) from CaseAuditEventEntity caeDate WHERE caeDate.caseDataId = :"
            + CaseAuditEventEntity.CASE_DATA_ID + ")"
    ),
    @NamedQuery(name = CaseAuditEventEntity.FIND_BY_ID, query =
        FIND_BY_ID_HQL
    )
})
@Table(name = "case_event")
@Entity
public class CaseAuditEventEntity {

    static final String FIND_BY_CASE_DATA_ID_HQL = "SELECT cae FROM CaseAuditEventEntity cae"
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

    @OneToOne(mappedBy = "caseEvent", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private SignificantItemEntity significantItemEntity;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserLastName() {
        return userLastName;
    }

    public void setUserLastName(String userLastName) {
        this.userLastName = userLastName;
    }

    public String getUserFirstName() {
        return userFirstName;
    }

    public void setUserFirstName(String userFirstName) {
        this.userFirstName = userFirstName;
    }

    public Long getCaseDataId() {
        return caseDataId;
    }

    public void setCaseDataId(Long caseDataId) {
        this.caseDataId = caseDataId;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getStateId() {
        return stateId;
    }

    public void setStateId(String stateId) {
        this.stateId = stateId;
    }

    public String getStateName() {
        return stateName;
    }

    public void setStateName(String stateName) {
        this.stateName = stateName;
    }

    public String getCaseTypeId() {
        return caseTypeId;
    }

    public void setCaseTypeId(String caseTypeId) {
        this.caseTypeId = caseTypeId;
    }

    public Integer getCaseTypeVersion() {
        return caseTypeVersion;
    }

    public void setCaseTypeVersion(Integer caseTypeVersion) {
        this.caseTypeVersion = caseTypeVersion;
    }

    public JsonNode getData() {
        return data;
    }

    public void setData(JsonNode data) {
        this.data = data;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String shortComment) {
        this.summary = shortComment;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String longComment) {
        this.description = longComment;
    }

    public SecurityClassification getSecurityClassification() {
        return securityClassification;
    }

    public void setSecurityClassification(SecurityClassification securityClassification) {
        this.securityClassification = securityClassification;
    }

    public JsonNode getDataClassification() {
        return dataClassification;
    }

    public void setDataClassification(JsonNode dataClassification) {
        this.dataClassification = dataClassification;
    }

    public SignificantItemEntity getSignificantItemEntity() {
        return significantItemEntity;
    }

    public void setSignificantItemEntity(SignificantItemEntity significantItemEntity) {
        this.significantItemEntity = significantItemEntity;
    }

}

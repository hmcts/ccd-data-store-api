package uk.gov.hmcts.ccd.data.casedetails;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.data.JsonDataConverter;

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
import java.time.LocalDateTime;

@SuppressWarnings("checkstyle:OperatorWrap")
// too many legacy OperatorWrap occurrences on JSON strings so suppress until move to Java12+
@NamedQueries({
    @NamedQuery(name = CaseDetailsEntity.FIND_BY_METADATA, query =
        "SELECT cd FROM CaseDetailsEntity cd " +
        "WHERE UPPER(cd.jurisdiction) LIKE UPPER(:" + CaseDetailsEntity.JURISDICTION_ID_PARAM + ") " +
        "AND UPPER(cd.caseType) LIKE UPPER(:" + CaseDetailsEntity.CASE_TYPE_PARAM + ") " +
        "AND UPPER(cd.state) LIKE UPPER(:" + CaseDetailsEntity.STATE_PARAM + ") " +
        "ORDER BY cd.createdDate ASC "
    ),

    @NamedQuery(name = CaseDetailsEntity.FIND_CASE, query =
        "SELECT cd FROM CaseDetailsEntity cd " +
        "WHERE UPPER(cd.jurisdiction) = UPPER(:" + CaseDetailsEntity.JURISDICTION_ID_PARAM + ") " +
        "AND UPPER(cd.caseType) = UPPER(:" + CaseDetailsEntity.CASE_TYPE_PARAM + ") " +
        "AND cd.reference = :" + CaseDetailsEntity.CASE_REFERENCE_PARAM
    ),

    @NamedQuery(name = CaseDetailsEntity.FIND_BY_REFERENCE, query =
        "SELECT cd FROM CaseDetailsEntity cd " +
        "WHERE cd.reference = :" + CaseDetailsEntity.CASE_REFERENCE_PARAM
    ),

    @NamedQuery(
        name = CaseDetailsEntity.FIND_BY_REF_AND_JURISDICTION,
        query = "SELECT cd FROM CaseDetailsEntity cd" +
            " WHERE cd.jurisdiction = :" + CaseDetailsEntity.JURISDICTION_ID_PARAM +
            " AND cd.reference = :" + CaseDetailsEntity.CASE_REFERENCE_PARAM
    ),

    @NamedQuery(
        name = CaseDetailsEntity.CASES_COUNT_BY_CASE_TYPE,
        query = "SELECT cd.caseType, COUNT(cd) FROM CaseDetailsEntity cd GROUP BY cd.caseType"
    )
})
@Table(name = "case_data")
@Entity
public class CaseDetailsEntity {
    static final String FIND_BY_METADATA = "CaseDataEntity_FIND_BY_PARAMS";
    static final String FIND_CASE = "CaseDataEntity_FIND_CASE";
    static final String FIND_BY_REFERENCE = "CaseDataEntity_FIND_BY_REFERENCE";
    static final String FIND_BY_REF_AND_JURISDICTION = "CaseDataEntity_FIND_BY_REFERENCE_AND_JURISDICTION";
    static final String CASES_COUNT_BY_CASE_TYPE = "CaseDataEntity_CASES_COUNT_BY_CASE_TYPE";

    static final String JURISDICTION_ID_PARAM = "JURISDICTION_ID_PARAM";
    static final String CASE_TYPE_PARAM = "CASE_TYPE_PARAM";
    static final String CASE_REFERENCE_PARAM = "CASE_REFERENCE_PARAM";
    static final String STATE_PARAM = "STATE_PARAM";
    public static final String ID_FIELD_COL = "id";
    public static final String STATE_FIELD_COL = "state";
    public static final String JURISDICTION_FIELD_COL = "jurisdiction";
    public static final String CASE_TYPE_ID_FIELD_COL = "case_type_id";
    public static final String REFERENCE_FIELD_COL = "reference";
    public static final String CREATED_DATE_FIELD_COL = "created_date";
    public static final String LAST_MODIFIED_FIELD_COL = "last_modified";
    public static final String LAST_STATE_MODIFIED_DATE_FIELD_COL = "last_state_modified_date";
    public static final String SECURITY_CLASSIFICATION_FIELD_COL = "security_classification";
    public static final String DATA_COL = "data";
    public static final String DATA_CLASSIFICATION_COL = "data_classification";
    public static final String SUPPLEMENTARY_DATA_COL = "supplementary_data";



    @Id
    @Column(name = ID_FIELD_COL)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = REFERENCE_FIELD_COL, nullable = false)
    private Long reference;
    @Column(name = JURISDICTION_FIELD_COL, nullable = false)
    private String jurisdiction;
    @Column(name = CASE_TYPE_ID_FIELD_COL, nullable = false)
    private String caseType;
    @Column(name = CREATED_DATE_FIELD_COL, nullable = false)
    private LocalDateTime createdDate;
    @Column(name = LAST_MODIFIED_FIELD_COL)
    private LocalDateTime lastModified;
    @Column(name = LAST_STATE_MODIFIED_DATE_FIELD_COL)
    private LocalDateTime lastStateModifiedDate;
    // TODO Rename to state_id
    @Column(name = STATE_FIELD_COL, nullable = false)
    private String state;
    @Enumerated(EnumType.STRING)
    @Column(name = SECURITY_CLASSIFICATION_FIELD_COL, nullable = false)
    private SecurityClassification securityClassification;
    @Column(name = DATA_COL, nullable = false)
    @Convert(converter = JsonDataConverter.class)
    private JsonNode data;
    @Column(name = DATA_CLASSIFICATION_COL, nullable = false)
    @Convert(converter = JsonDataConverter.class)
    private JsonNode dataClassification;
    @Column(name = SUPPLEMENTARY_DATA_COL)
    @Convert(converter = JsonDataConverter.class)
    private JsonNode supplementaryData;

    @Version
    private Integer version;

    public Long getId() {
        return id;
    }

    public void setId(Long uuid) {
        this.id = uuid;
    }

    public Long getReference() {
        return reference;
    }

    public void setReference(Long reference) {
        this.reference = reference;
    }

    public String getCaseType() {
        return caseType;
    }

    public void setCaseType(String caseType) {
        this.caseType = caseType;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public void setJurisdiction(String jurisdiction) {
        this.jurisdiction = jurisdiction;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public SecurityClassification getSecurityClassification() {
        return securityClassification;
    }

    public void setSecurityClassification(SecurityClassification securityClassification) {
        this.securityClassification = securityClassification;
    }

    public JsonNode getData() {
        return data;
    }

    public void setData(JsonNode data) {
        this.data = data;
    }

    public JsonNode getDataClassification() {
        return dataClassification;
    }

    public void setDataClassification(JsonNode dataClassification) {
        this.dataClassification = dataClassification;
    }

    public JsonNode getSupplementaryData() {
        return supplementaryData;
    }

    public void setSupplementaryData(JsonNode supplementaryData) {
        this.supplementaryData = supplementaryData;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }

    public LocalDateTime getLastStateModifiedDate() {
        return lastStateModifiedDate;
    }

    public void setLastStateModifiedDate(LocalDateTime lastStateModifiedDate) {
        this.lastStateModifiedDate = lastStateModifiedDate;
    }
}

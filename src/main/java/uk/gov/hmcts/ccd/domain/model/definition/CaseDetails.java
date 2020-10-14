package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.callbacks.AfterSubmitCallbackResponse;

import static java.util.Optional.ofNullable;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.CASE_REFERENCE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.CASE_TYPE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.CREATED_DATE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.JURISDICTION;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.LAST_MODIFIED_DATE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.LAST_STATE_MODIFIED_DATE;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.SECURITY_CLASSIFICATION;
import static uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField.STATE;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.CASE_PAYMENT_HISTORY_VIEWER;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.LABEL;

// partial javadoc attributes added prior to checkstyle implementation in module
@SuppressWarnings("checkstyle:SummaryJavadoc")
public class CaseDetails implements Cloneable {
    private static final Logger LOG = LoggerFactory.getLogger(CaseDetails.class);
    public static final String DRAFT_ID = "DRAFT%s";

    private String id;

    @JsonIgnore
    private Long reference;

    @JsonProperty("version")
    private Integer version;

    private String jurisdiction;

    @JsonProperty("case_type_id")
    private String caseTypeId;

    @JsonProperty("created_date")
    private LocalDateTime createdDate;

    @JsonProperty("last_modified")
    private LocalDateTime lastModified;

    @JsonProperty("last_state_modified_date")
    private LocalDateTime lastStateModifiedDate;

    private String state;

    @JsonProperty("security_classification")
    private SecurityClassification securityClassification;

    @JsonProperty("case_data")
    @ApiModelProperty("Case data as defined in case type definition. See `docs/api/case-data.md` for data structure.")
    private Map<String, JsonNode> data;

    @JsonProperty("data_classification")
    @ApiModelProperty("Same structure as `case_data` with classification (`PUBLIC`, `PRIVATE`, `RESTRICTED`) "
        + "as field's value.")
    private Map<String, JsonNode> dataClassification;

    @JsonProperty("supplementary_data")
    private Map<String, JsonNode> supplementaryData;

    /**
     * Attribute passed to UI layer, does not need persistence.
     */
    @JsonProperty("after_submit_callback_response")
    private AfterSubmitCallbackResponse afterSubmitCallbackResponse;

    /**
     * Attribute passed to UI layer, does not need persistence.
     */
    @JsonProperty("callback_response_status_code")
    private Integer callbackResponseStatusCode;

    /**
     * Attribute passed to UI layer, does not need persistence.
     */
    @JsonProperty("callback_response_status")
    private String callbackResponseStatus;

    /**
     * Attribute passed to UI layer, does not need persistence.
     */
    @JsonProperty("delete_draft_response_status_code")
    private Integer deleteDraftResponseStatusCode;


    /**
     * Attribute passed to UI layer, does not need persistence.
     */
    @JsonProperty("delete_draft_response_status")
    private String deleteDraftResponseStatus;


    @JsonIgnore
    private final Map<String, Object> metadata = new HashMap<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonGetter("id")
    public Long getReference() {
        return reference;
    }

    @JsonIgnore
    public String getReferenceAsString() {
        return reference != null ? reference.toString() : null;
    }

    @JsonSetter("id")
    public void setReference(Long reference) {
        this.reference = reference;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }

    public String getCaseTypeId() {
        return caseTypeId;
    }

    public void setCaseTypeId(String caseTypeId) {
        this.caseTypeId = caseTypeId;
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

    public Map<String, JsonNode> getData() {
        return data;
    }

    public void setData(Map<String, JsonNode> data) {
        this.data = data;
    }

    public Integer getCallbackResponseStatusCode() {
        return callbackResponseStatusCode;
    }

    public String getCallbackResponseStatus() {
        return callbackResponseStatus;
    }

    public LocalDateTime getLastStateModifiedDate() {
        return lastStateModifiedDate;
    }

    public void setLastStateModifiedDate(LocalDateTime lastStateModifiedDate) {
        this.lastStateModifiedDate = lastStateModifiedDate;
    }

    /**
     * @deprecated Will be removed in version 2.x. Use {@link CaseDetails#dataClassification} instead.
     */
    @Deprecated
    @JsonGetter("security_classifications")
    @ApiModelProperty("Deprecated. Use `data_classification` instead.")
    public Map<String, JsonNode> getSecurityClassifications() {
        return dataClassification;
    }

    public Map<String, JsonNode> getDataClassification() {
        return dataClassification;
    }

    public void setDataClassification(Map<String, JsonNode> dataClassification) {
        this.dataClassification = dataClassification;
    }

    public Map<String, JsonNode> getSupplementaryData() {
        return supplementaryData;
    }

    public void setSupplementaryData(Map<String, JsonNode> supplementaryData) {
        this.supplementaryData = supplementaryData;
    }

    public AfterSubmitCallbackResponse getAfterSubmitCallbackResponse() {
        return afterSubmitCallbackResponse;
    }

    public void setIncompleteCallbackResponse() {
        this.callbackResponseStatusCode = SC_OK;  // Front end cannot handle anything other than status 200
        this.callbackResponseStatus = "INCOMPLETE_CALLBACK";
    }

    public void setIncompleteDeleteDraftResponse() {
        this.deleteDraftResponseStatusCode = SC_OK;  // Front end cannot handle anything other than status 200
        this.deleteDraftResponseStatus = "INCOMPLETE_DELETE_DRAFT";
    }

    public boolean existsInData(CaseTypeTabField caseTypeTabField) {
        return isFieldWithNoValue(caseTypeTabField)
            || hasDataForTabField(caseTypeTabField)
            || getMetadata().containsKey(caseTypeTabField.getCaseFieldDefinition().getId());
    }

    private boolean hasDataForTabField(CaseTypeTabField caseTypeTabField) {
        return data.keySet().contains(caseTypeTabField.getCaseFieldDefinition().getId());
    }

    private boolean isFieldWithNoValue(CaseTypeTabField caseTypeTabField) {
        return caseTypeTabField.getCaseFieldDefinition()
            .getFieldTypeDefinition().getType().equals(LABEL)
            || caseTypeTabField.getCaseFieldDefinition()
            .getFieldTypeDefinition().getType().equals(CASE_PAYMENT_HISTORY_VIEWER);
    }

    @JsonIgnore
    public CaseDetails shallowClone() throws CloneNotSupportedException {
        return (CaseDetails) super.clone();
    }

    @JsonIgnore
    public void setDeleteDraftResponseEntity(final String draftId, final ResponseEntity<Void>
                                                         draftResponse) {
        if (SC_OK == draftResponse.getStatusCodeValue()) {
            setDeleteDraftResponseEntity();
        } else {
            LOG.warn("Incomplete delete draft response for draft={}, statusCode={}",
                     draftId,
                     draftResponse.getStatusCodeValue());
            setIncompleteDeleteDraftResponse();
        }
    }

    private void setDeleteDraftResponseEntity() {
        this.deleteDraftResponseStatusCode =  SC_OK;
        this.deleteDraftResponseStatus = "DELETE_DRAFT_COMPLETED";
    }

    @JsonIgnore
    @SuppressWarnings("java:S2259")
    public void setAfterSubmitCallbackResponseEntity(final ResponseEntity<AfterSubmitCallbackResponse>
                                                         callBackResponse) {
        if (SC_OK == callBackResponse.getStatusCodeValue()) {
            setAfterSubmitCallbackResponseEntity(callBackResponse.getBody());
        } else {
            LOG.warn("Incomplete call back response for case {} (db id={}); status code {}, body {}",
                     reference,
                     id,
                     callBackResponse.getStatusCodeValue(),
                     callBackResponse.getBody().toJson());
            setIncompleteCallbackResponse();
        }
    }

    private void setAfterSubmitCallbackResponseEntity(final AfterSubmitCallbackResponse response) {
        this.afterSubmitCallbackResponse = response;
        this.callbackResponseStatusCode = SC_OK;
        this.callbackResponseStatus = "CALLBACK_COMPLETED";
    }

    @JsonIgnore
    public Map<String, Object> getMetadata() {
        if (metadata.isEmpty()) {
            metadata.put(JURISDICTION.getReference(), getJurisdiction());
            metadata.put(CASE_TYPE.getReference(), getCaseTypeId());
            metadata.put(STATE.getReference(), getState());
            metadata.put(CASE_REFERENCE.getReference(), getReference() != null ? getReference() : getId());
            metadata.put(CREATED_DATE.getReference(), getCreatedDate());
            metadata.put(LAST_MODIFIED_DATE.getReference(), getLastModified());
            metadata.put(LAST_STATE_MODIFIED_DATE.getReference(), getLastStateModifiedDate());
            metadata.put(SECURITY_CLASSIFICATION.getReference(), getSecurityClassification());
        }
        return metadata;
    }

    public Integer getDeleteDraftResponseStatusCode() {
        return deleteDraftResponseStatusCode;
    }

    public String getDeleteDraftResponseStatus() {
        return deleteDraftResponseStatus;
    }

    @JsonIgnore
    public Map<String, Object> getCaseDataAndMetadata() {
        Map<String, Object> allData = new HashMap<>(getMetadata());
        ofNullable(getData()).ifPresent(allData::putAll);
        return allData;
    }

    @JsonIgnore
    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

    @JsonIgnore
    public boolean hasCaseReference() {
        return getReference() != null;
    }

    @JsonIgnore
    public Map<String, JsonNode> getCaseEventData(Set<String> caseFieldIds) {
        Map<String, JsonNode> caseEventData = new HashMap<>();
        if (this.data != null) {
            for (String caseFieldId : caseFieldIds) {
                JsonNode value = this.data.get(caseFieldId);
                if (value != null) {
                    caseEventData.put(caseFieldId, value);
                }
            }
        }
        return caseEventData;
    }

}

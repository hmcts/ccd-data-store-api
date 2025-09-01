package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.callbacks.AfterSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.service.common.JcLogger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPONENT_LAUNCHER;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.FLAG_LAUNCHER;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.LABEL;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.WAYS_TO_PAY;


// partial javadoc attributes added prior to checkstyle implementation in module
@SuppressWarnings("checkstyle:SummaryJavadoc")
public class CaseDetails implements Cloneable {
    private final JcLogger jclogger = new JcLogger("CaseDetails", true);

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

    @JsonIgnore
    private LocalDate resolvedTTL;

    private void jcdebug(final String method) {
        try {
            final String thisAsString = jclogger.printObjectToString(this);
            if (thisAsString.contains("Confidential or sensitive information")) {
                jclogger.jclog(method + " " + thisAsString);
                jclogger.jclog(method + " CALL STACK = " + jclogger.getCallStackAsString());
            }
        } catch (Exception e) {
            // Do nothing
        }
    }

    public String getId() {
        jcdebug("getId()");
        return id;
    }

    public void setId(String id) {
        jcdebug("setId()");
        this.id = id;
    }

    @JsonGetter("id")
    public Long getReference() {
        jcdebug("getReference()");
        return reference;
    }

    @JsonIgnore
    public String getReferenceAsString() {
        jcdebug("getReferenceAsString()");
        return reference != null ? reference.toString() : null;
    }

    @JsonSetter("id")
    public void setReference(Long reference) {
        jcdebug("setReference()");
        this.reference = reference;
    }

    public Integer getVersion() {
        jcdebug("getVersion()");
        return version;
    }

    public void setVersion(final Integer version) {
        jcdebug("setVersion()");
        this.version = version;
    }

    public String getCaseTypeId() {
        jcdebug("getCaseTypeId()");
        return caseTypeId;
    }

    public void setCaseTypeId(String caseTypeId) {
        jcdebug("setCaseTypeId()");
        this.caseTypeId = caseTypeId;
    }

    public String getJurisdiction() {
        jcdebug("getJurisdiction()");
        return jurisdiction;
    }

    public void setJurisdiction(String jurisdiction) {
        jcdebug("setJurisdiction()");
        this.jurisdiction = jurisdiction;
    }

    public LocalDateTime getCreatedDate() {
        jcdebug("getCreatedDate()");
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        jcdebug("setCreatedDate()");
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastModified() {
        jcdebug("getLastModified()");
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        jcdebug("setLastModified()");
        this.lastModified = lastModified;
    }

    public String getState() {
        jcdebug("getState()");
        return state;
    }

    public void setState(String state) {
        jcdebug("setState()");
        this.state = state;
    }

    public SecurityClassification getSecurityClassification() {
        jcdebug("getSecurityClassification()");
        return securityClassification;
    }

    public void setSecurityClassification(SecurityClassification securityClassification) {
        jcdebug("setSecurityClassification()");
        this.securityClassification = securityClassification;
    }

    public Map<String, JsonNode> getData() {
        jcdebug("getData()");
        return data;
    }

    public void setData(Map<String, JsonNode> data) {
        jcdebug("setData()");
        this.data = data;
    }

    public Integer getCallbackResponseStatusCode() {
        jcdebug("getCallbackResponseStatusCode()");
        return callbackResponseStatusCode;
    }

    public String getCallbackResponseStatus() {
        jcdebug("getCallbackResponseStatus()");
        return callbackResponseStatus;
    }

    public LocalDateTime getLastStateModifiedDate() {
        jcdebug("getLastStateModifiedDate()");
        return lastStateModifiedDate;
    }

    public void setLastStateModifiedDate(LocalDateTime lastStateModifiedDate) {
        jcdebug("setLastStateModifiedDate()");
        this.lastStateModifiedDate = lastStateModifiedDate;
    }

    public Map<String, JsonNode> getDataClassification() {
        jcdebug("getDataClassification()");
        return dataClassification;
    }

    public void setDataClassification(Map<String, JsonNode> dataClassification) {
        jcdebug("setDataClassification()");
        this.dataClassification = dataClassification;
    }

    public Map<String, JsonNode> getSupplementaryData() {
        jcdebug("getSupplementaryData()");
        return supplementaryData;
    }

    public void setSupplementaryData(Map<String, JsonNode> supplementaryData) {
        jcdebug("setSupplementaryData()");
        this.supplementaryData = supplementaryData;
    }

    public AfterSubmitCallbackResponse getAfterSubmitCallbackResponse() {
        jcdebug("getAfterSubmitCallbackResponse()");
        return afterSubmitCallbackResponse;
    }

    public void setIncompleteCallbackResponse() {
        jcdebug("setIncompleteCallbackResponse()");
        this.callbackResponseStatusCode = SC_OK;  // Front end cannot handle anything other than status 200
        this.callbackResponseStatus = "INCOMPLETE_CALLBACK";
    }

    public void setIncompleteDeleteDraftResponse() {
        jcdebug("setIncompleteDeleteDraftResponse()");
        this.deleteDraftResponseStatusCode = SC_OK;  // Front end cannot handle anything other than status 200
        this.deleteDraftResponseStatus = "INCOMPLETE_DELETE_DRAFT";
    }

    public boolean existsInData(CaseTypeTabField caseTypeTabField) {
        jcdebug("existsInData()");
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
            .getFieldTypeDefinition().getType().equals(CASE_PAYMENT_HISTORY_VIEWER)
            || caseTypeTabField.getCaseFieldDefinition()
            .getFieldTypeDefinition().getType().equals(WAYS_TO_PAY)
            || caseTypeTabField.getCaseFieldDefinition().getFieldTypeDefinition().getType().equals(FLAG_LAUNCHER)
            || caseTypeTabField.getCaseFieldDefinition().getFieldTypeDefinition().getType().equals(COMPONENT_LAUNCHER);
    }

    @JsonIgnore
    public CaseDetails shallowClone() throws CloneNotSupportedException {
        jcdebug("shallowClone()");
        return (CaseDetails) super.clone();
    }

    @JsonIgnore
    public void setDeleteDraftResponseEntity(final String draftId, final ResponseEntity<Void>
                                                         draftResponse) {
        jcdebug("setDeleteDraftResponseEntity()");
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
        jcdebug("setAfterSubmitCallbackResponseEntity()");
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
        jcdebug("getMetadata()");
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
        jcdebug("getDeleteDraftResponseStatusCode()");
        return deleteDraftResponseStatusCode;
    }

    public String getDeleteDraftResponseStatus() {
        jcdebug("getDeleteDraftResponseStatusStatus()");
        return deleteDraftResponseStatus;
    }

    @JsonIgnore
    public Map<String, Object> getCaseDataAndMetadata() {
        jcdebug("getCaseDataAndMetadata()");
        Map<String, Object> allData = new HashMap<>(getMetadata());
        ofNullable(getData()).ifPresent(allData::putAll);
        return allData;
    }

    @JsonIgnore
    @Override
    public String toString() {
        jcdebug("toString()");
        return ReflectionToStringBuilder.toString(this);
    }

    @JsonIgnore
    public boolean hasCaseReference() {
        jcdebug("hasCaseReference()");
        return getReference() != null;
    }

    @JsonIgnore
    public Map<String, JsonNode> getCaseEventData(Set<String> caseFieldIds) {
        jcdebug("getCaseEventData()");
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

    public void setResolvedTTL(LocalDate resolvedTTL) {
        jcdebug("setResolvedTTL()");
        this.resolvedTTL = resolvedTTL;
    }

    public LocalDate getResolvedTTL() {
        jcdebug("getResolvedTTL()");
        return resolvedTTL;
    }
}

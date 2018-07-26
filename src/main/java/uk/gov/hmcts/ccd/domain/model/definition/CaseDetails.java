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

import java.time.LocalDateTime;
import java.util.Map;

import static org.apache.http.HttpStatus.SC_OK;

public class CaseDetails implements Cloneable {
    private static final Logger LOG = LoggerFactory.getLogger(CaseDetails.class);
    private static final String LABEL_FIELD_TYPE = "Label";
    private static final String CASE_PAYMENT_HISTORY_VIEWER_FIELD_TYPE = "CasePaymentHistoryViewer";

    private Long id;

    @JsonIgnore
    private Long reference;

    private String jurisdiction;

    @JsonProperty("case_type_id")
    private String caseTypeId;

    @JsonProperty("created_date")
    private LocalDateTime createdDate;

    @JsonProperty("last_modified")
    private LocalDateTime lastModified;

    private String state;

    @JsonProperty("security_classification")
    private SecurityClassification securityClassification;

    @JsonProperty("case_data")
    @ApiModelProperty("Case data as defined in case type definition. See `docs/api/case-data.md` for data structure.")
    private Map<String, JsonNode> data;

    @JsonProperty("data_classification")
    @ApiModelProperty("Same structure as `case_data` with classification (`PUBLIC`, `PRIVATE`, `RESTRICTED`) as field's value.")
    private Map<String, JsonNode> dataClassification;

    /** Attribute passed to UI layer, does not need persistence */
    @JsonProperty("after_submit_callback_response")
    private AfterSubmitCallbackResponse afterSubmitCallbackResponse;

    /** Attribute passed to UI layer, does not need persistence */
    @JsonProperty("callback_response_status_code")
    private Integer callbackResponseStatusCode;

    /** Attribute passed to UI layer, does not need persistence */
    @JsonProperty("callback_response_status")
    private String callbackResponseStatus;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @JsonGetter("id")
    public Long getReference() {
        return reference;
    }

    @JsonSetter("id")
    public void setReference(Long reference) {
        this.reference = reference;
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

    /**
     *
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

    public AfterSubmitCallbackResponse getAfterSubmitCallbackResponse() {
        return afterSubmitCallbackResponse;
    }

    private void setAfterSubmitCallbackResponseEntity(final AfterSubmitCallbackResponse response) {
        this.afterSubmitCallbackResponse = response;
        this.callbackResponseStatusCode =  SC_OK;
        this.callbackResponseStatus = "COMPLETED";
    }

    public void setIncompleteCallbackResponse() {
        this.callbackResponseStatusCode = SC_OK;  // Front end cannot handle anything other than status 200
        this.callbackResponseStatus = "INCOMPLETE";
    }

    public boolean existsInData(CaseTypeTabField caseTypeTabField) {
        return isFieldWithNoValue(caseTypeTabField)
            || data.keySet().contains(caseTypeTabField.getCaseField().getId());
    }

    private boolean isFieldWithNoValue(CaseTypeTabField caseTypeTabField) {
        return caseTypeTabField.getCaseField().getFieldType().getType().equals(LABEL_FIELD_TYPE) ||
            caseTypeTabField.getCaseField().getFieldType().getType().equals(CASE_PAYMENT_HISTORY_VIEWER_FIELD_TYPE);
    }

    @JsonIgnore
    public CaseDetails shallowClone() throws CloneNotSupportedException {
        return (CaseDetails) super.clone();
    }

    @JsonIgnore
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

    @JsonIgnore
    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

}

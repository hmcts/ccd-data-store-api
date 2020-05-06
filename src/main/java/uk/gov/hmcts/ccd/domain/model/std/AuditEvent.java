package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModelProperty;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.callbacks.SignificantItem;

import java.time.LocalDateTime;
import java.util.Map;

@SuppressWarnings("checkstyle:SummaryJavadoc") // Javadoc predates checkstyle implementation in module
public class AuditEvent extends Event {
    @JsonIgnore
    private Long id;
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("user_last_name")
    private String userLastName;
    @JsonProperty("user_first_name")
    private String userFirstName;
    @JsonProperty("event_name")
    private String eventName;
    @JsonIgnore
    private String caseDataId;
    @JsonProperty("created_date")
    private LocalDateTime createdDate;
    @JsonProperty("case_type_id")
    private String caseTypeId;
    @JsonProperty("case_type_version")
    private Integer caseTypeVersion;
    @JsonProperty("state_id")
    private String stateId;
    @JsonProperty("state_name")
    private String stateName;
    @JsonProperty("data")
    @ApiModelProperty("Case data as defined in case type definition. See `docs/api/case-data.md` for data structure.")
    private Map<String, JsonNode> data;
    @JsonProperty("data_classification")
    @ApiModelProperty("Same structure as `data` with classification (`PUBLIC`, `PRIVATE`, `RESTRICTED`) as field's value.")
    private Map<String, JsonNode> dataClassification;
    @JsonProperty("security_classification")
    private SecurityClassification securityClassification;
    @JsonProperty("significant_item")
    private SignificantItem significantItem;


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

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getCaseDataId() {
        return caseDataId;
    }

    public void setCaseDataId(String caseDataId) {
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

    public Map<String, JsonNode> getData() {
        return data;
    }

    public void setData(Map<String, JsonNode> data) {
        this.data = data;
    }

    public SignificantItem getSignificantItem() {
        return significantItem;
    }

    public void setSignificantItem(SignificantItem significantItem) {
        this.significantItem = significantItem;
    }

    /**
     *
     * @deprecated Will be removed in version 2.x. Use {@link AuditEvent#dataClassification} instead.
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

    public SecurityClassification getSecurityClassification() {
        return securityClassification;
    }

    public void setSecurityClassification(SecurityClassification securityClassification) {
        this.securityClassification = securityClassification;
    }
}

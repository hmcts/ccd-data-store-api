package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class CaseDetailsElasticDTO {

    public String id;

    public String reference;

    public String jurisdiction;

    public String caseTypeId;

    public LocalDateTime createdDate;

    public LocalDateTime lastModified;

    public String state;

    public SecurityClassification securityClassification;

    public Map<String, JsonNode> data;

    public Map<String, JsonNode> dataClassification;
}

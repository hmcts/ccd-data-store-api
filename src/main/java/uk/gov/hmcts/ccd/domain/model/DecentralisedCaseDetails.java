package uk.gov.hmcts.ccd.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class DecentralisedCaseDetails {

    private Long reference;
    private Integer version;
    private String jurisdiction;
    private String caseTypeId;
    private Instant createdDate;
    private Instant lastModified;
    private Instant lastStateModifiedDate;
    private String state;
    private SecurityClassification securityClassification;
    @ApiModelProperty("Case data as defined in case type definition. See `docs/api/case-data.md` for data structure.")
    private Map<String, JsonNode> data;
    private Map<String, JsonNode> supplementaryData;
    private Instant resolvedTTL;

    public CaseDetails toCaseDetails() {
        var tz = TimeZone.getTimeZone("Europe/London").toZoneId();
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setReference(reference);
        caseDetails.setVersion(version);
        caseDetails.setJurisdiction(jurisdiction);
        caseDetails.setCaseTypeId(caseTypeId);
        caseDetails.setCreatedDate(LocalDateTime.ofInstant(createdDate, tz));
        caseDetails.setLastModified(LocalDateTime.ofInstant(lastModified, tz));
        caseDetails.setLastStateModifiedDate(LocalDateTime.ofInstant(lastStateModifiedDate, tz));
        caseDetails.setState(state);
        caseDetails.setSecurityClassification(securityClassification);
        caseDetails.setData(data);
        caseDetails.setSupplementaryData(supplementaryData);
        caseDetails.setResolvedTTL(LocalDate.ofInstant(resolvedTTL, tz));
        return caseDetails;
    }
}

package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.Version;

import java.io.Serializable;
import java.util.List;

@ToString
public class CaseTypeLiteDefinition implements Serializable {

    @Getter
    private final String id;
    @Getter
    private final String description;
    @Getter
    private final Version version;
    @Getter
    private final String name;
    @Getter
    @JsonProperty("security_classification")
    private final SecurityClassification securityClassification;
    @Getter
    private final List<CaseEventDefinition> events;
    @Getter
    private final List<CaseStateDefinition> states;

    public CaseTypeLiteDefinition(CaseTypeDefinition caseTypeDefinition) {
        this.id = caseTypeDefinition.getId();
        this.description = caseTypeDefinition.getDescription();
        this.version = caseTypeDefinition.getVersion();
        this.name = caseTypeDefinition.getName();
        this.securityClassification = caseTypeDefinition.getSecurityClassification();
        this.events = caseTypeDefinition.getEvents();
        this.states = caseTypeDefinition.getStates();
    }
}

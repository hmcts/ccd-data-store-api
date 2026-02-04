package uk.gov.hmcts.ccd.domain.model.aggregated.lite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
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
public class CaseTypeLite implements Serializable {

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
    private final List<CaseEventLite> events;
    @Getter
    private final List<CaseStateLite> states;

    public CaseTypeLite(CaseTypeDefinition caseTypeDefinition) {
        this.id = caseTypeDefinition.getId();
        this.description = caseTypeDefinition.getDescription();
        this.version = caseTypeDefinition.getVersion();
        this.name = caseTypeDefinition.getName();
        this.securityClassification = caseTypeDefinition.getSecurityClassification();
        this.events =  createLiteEvents(caseTypeDefinition.getEvents());
        this.states =  createLiteStates(caseTypeDefinition.getStates());
    }

    @JsonCreator
    public CaseTypeLite(
            @JsonProperty("id") String id,
            @JsonProperty("description") String description,
            @JsonProperty("version") Version version,
            @JsonProperty("name") String name,
            @JsonProperty("security_classification") SecurityClassification securityClassification,
            @JsonProperty("events") List<CaseEventDefinition> events,
            @JsonProperty("states") List<CaseStateDefinition> states) {
        this.id = id;
        this.description = description;
        this.version = version;
        this.name = name;
        this.securityClassification = securityClassification;
        this.events = createLiteEvents(events);
        this.states = createLiteStates(states);
    }

    private List<CaseEventLite> createLiteEvents(List<CaseEventDefinition> events) {
        if (states != null) {
            return events.stream()
                .map(event -> {
                    CaseEventLite caseEventLite = new CaseEventLite();
                    caseEventLite.setId(event.getId());
                    caseEventLite.setName(event.getName());
                    caseEventLite.setDescription(event.getDescription());
                    caseEventLite.setPreStates(event.getPreStates());
                    caseEventLite.setAccessControlLists(event.getAccessControlLists());
                    return caseEventLite;
                }).toList();
        }
        return Lists.newArrayList();
    }

    private List<CaseStateLite> createLiteStates(List<CaseStateDefinition> states) {
        if (states != null) {
            return states.stream()
                .map(state -> {
                    CaseStateLite caseStateLite = new CaseStateLite();
                    caseStateLite.setId(state.getId());
                    caseStateLite.setName(state.getName());
                    caseStateLite.setDescription(state.getDescription());
                    return caseStateLite;
                }).toList();
        }
        return Lists.newArrayList();
    }
}

package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CaseTypeTabsDefinition implements Serializable {

    @JsonProperty("case_type_id")
    private String caseTypeId = null;
    private List<String> channels = new ArrayList<>();
    private List<CaseTypeTabDefinition> tabs = new ArrayList<>();

    public String getCaseTypeId() {
        return caseTypeId;
    }

    public void setCaseTypeId(String caseTypeId) {
        this.caseTypeId = caseTypeId;
    }

    public List<String> getChannels() {
        return channels;
    }

    public void setChannels(List<String> channels) {
        this.channels = channels;
    }

    public List<CaseTypeTabDefinition> getTabs() {
        return tabs;
    }

    public void setTabs(List<CaseTypeTabDefinition> tabs) {
        this.tabs = tabs;
    }

    public boolean hasTabFieldType(String tabFieldType) {
        return this.tabs.stream()
                        .flatMap(tab -> tab.getTabFields().stream())
                        .anyMatch(field -> field.getCaseFieldDefinition()
                                                .getFieldTypeDefinition()
                                                .getType()
                                                .equals(tabFieldType));
    }
}

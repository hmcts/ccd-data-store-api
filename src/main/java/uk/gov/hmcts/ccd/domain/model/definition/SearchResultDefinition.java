package uk.gov.hmcts.ccd.domain.model.definition;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class SearchResultDefinition implements Serializable {
    private SearchResultField[] fields;

    public SearchResultField[] getFields() {
        return fields;
    }

    public void setFields(SearchResultField[] fields) {
        this.fields = fields;
    }

    public List<SearchResultField> getFieldsWithPaths() {
        return Arrays.stream(fields)
            .filter(f -> StringUtils.isNotBlank(f.getCaseFieldPath()))
            .collect(Collectors.toList());
    }

    public Map<String, String> getFieldsUserRoles(SearchResultDefinition searchResultDefinition) {
        Map<String, String> fields = new HashMap<>();
        for (SearchResultField srf : searchResultDefinition.getFields()) {
            fields.put(srf.getCaseFieldId(), srf.getRole());
        }
        return fields;
    }

    public boolean fieldExists(String caseFieldId, SearchResultDefinition searchResultDefinition) {
        Map<String, String> fields = getFieldsUserRoles(searchResultDefinition);
        if (!fields.containsKey(caseFieldId)) {
            return false;
        }
        return true;
    }

    public boolean fieldHasRole(String caseFieldId, SearchResultDefinition searchResultDefinition, Set<String> roles) {
        Map<String, String> fields = getFieldsUserRoles(searchResultDefinition);
        String role = fields.get(caseFieldId);
        if (role != null && !roles.contains(role)) {
            return false;
        }
        return true;
    }
}

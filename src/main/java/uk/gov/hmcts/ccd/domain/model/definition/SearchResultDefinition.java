package uk.gov.hmcts.ccd.domain.model.definition;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    public Map<String, String> getFieldsUserRoles() {
        Map<String, String> fields = new HashMap<>();
        for (SearchResultField srf : this.fields) {
            fields.put(srf.getCaseFieldId(), srf.getRole());
        }
        return fields;
    }

    public boolean fieldExists(String caseFieldId) {
        Map<String, String> fields = getFieldsUserRoles();
        return fields.containsKey(caseFieldId);
    }

    public boolean fieldHasRole(String caseFieldId, Set<String> roles) {
        Map<String, String> fields = getFieldsUserRoles();
        String role = fields.get(caseFieldId);
        if (role != null && !roles.contains(role)) {
            return false;
        }
        return true;
    }
}

package uk.gov.hmcts.ccd.domain.model.definition;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
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

    public Map<String, List<String>> getFieldsUserRoles() {
        Map<String, List<String>> fields = new HashMap<>();
        for (SearchResultField srf : this.fields) {
            if (fields.containsKey(srf.getCaseFieldId())) {
                fields.get(srf.getCaseFieldId()).add(srf.getRole());
            } else {
                List<String> roles = new ArrayList<>();
                roles.add(srf.getRole());
                fields.put(srf.getCaseFieldId(), roles);
            }
        }
        return fields;
    }

    public boolean fieldExists(String caseFieldId) {
        Map<String, List<String>> fields = getFieldsUserRoles();
        return fields.containsKey(caseFieldId);
    }

    public boolean fieldHasRole(String caseFieldId, Set<String> roles) {
        Map<String, List<String>> fields = getFieldsUserRoles();
        List<String> userRoles = fields.get(caseFieldId);
        if (userRoles != null) {
            long count = userRoles.stream()
                .filter(userRole -> userRole == null || roles.contains(userRole))
                .count();
            return count != 0;
        }
        return false;
    }
}

package uk.gov.hmcts.ccd.domain.model.definition;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SearchResultDefinition implements Serializable, Copyable<SearchResultDefinition> {

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
        Map<String, List<String>> fieldsUserRoles = new HashMap<>();
        for (SearchResultField srf : fields) {
            if (fieldsUserRoles.containsKey(srf.getCaseFieldId())) {
                fieldsUserRoles.get(srf.getCaseFieldId()).add(srf.getRole());
            } else {
                fieldsUserRoles.put(srf.getCaseFieldId(), new ArrayList<>(Collections.singletonList(srf.getRole())));
            }
        }
        return fieldsUserRoles;
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

    @Override
    public SearchResultDefinition createCopy() {
        SearchResultDefinition copy = new SearchResultDefinition();
        if (fields != null) {
            SearchResultField[] copiedFields = Arrays.stream(fields)
                .map(originalField -> originalField != null ? originalField.createCopy() : null)
                .toArray(SearchResultField[]::new);
            copy.setFields(copiedFields);
        } else {
            copy.setFields(null);
        }
        return copy;
    }
}

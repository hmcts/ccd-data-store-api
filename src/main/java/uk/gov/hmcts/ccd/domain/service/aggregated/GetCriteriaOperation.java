package uk.gov.hmcts.ccd.domain.service.aggregated;

import static uk.gov.hmcts.ccd.domain.model.search.CriteriaType.WORKBASKET;

import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.CriteriaField;
import uk.gov.hmcts.ccd.domain.model.search.CriteriaInput;
import uk.gov.hmcts.ccd.domain.model.search.CriteriaType;
import uk.gov.hmcts.ccd.domain.model.search.Field;
import uk.gov.hmcts.ccd.domain.model.search.SearchInput;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;

import java.util.List;
import java.util.function.Predicate;

public interface GetCriteriaOperation {

    <T> List<? extends CriteriaInput> execute(
        final String caseTypeId,
        Predicate<AccessControlList> access, CriteriaType criteriaType);

    default CriteriaInput toCriteriaInput(final CriteriaField in, final CaseType caseType, CriteriaType criteriaType) {
        CriteriaInput result;
        if (criteriaType.equals(WORKBASKET)) {
            result = new WorkbasketInput();
        } else {  //criteriaType.equals(SEARCH)
            result = new SearchInput();
        }
        result.setLabel(in.getLabel());
        result.setOrder(in.getDisplayOrder());
        result.setRole(in.getRole());
        final Field field = new Field();
        field.setId(in.getCaseFieldId());
        CaseField caseField = getCaseField(in.getCaseFieldId(), caseType);
        field.setType(caseField.getFieldType());
        field.setMetadata(caseField.isMetadata());
        result.setField(field);
        return result;
    }

    default CaseField getCaseField(final String fieldId, final CaseType caseType) {
        return caseType.getCaseFields().stream()
            .filter(c -> c.getId().equals(fieldId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException(String.format("FieldId %s not found", fieldId)));
    }
}

package uk.gov.hmcts.ccd.domain.service.aggregated;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.ccd.domain.model.search.CriteriaType.SEARCH;
import static uk.gov.hmcts.ccd.domain.model.search.CriteriaType.WORKBASKET;

import com.google.common.base.Strings;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.CriteriaField;
import uk.gov.hmcts.ccd.domain.model.definition.SearchInputDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.WorkbasketInputDefinition;
import uk.gov.hmcts.ccd.domain.model.search.CriteriaInput;
import uk.gov.hmcts.ccd.domain.model.search.CriteriaType;
import uk.gov.hmcts.ccd.domain.model.search.Field;
import uk.gov.hmcts.ccd.domain.model.search.SearchInput;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import java.util.List;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier(DefaultGetCriteriaOperation.QUALIFIER)
public class DefaultGetCriteriaOperation implements GetCriteriaOperation {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultGetCriteriaOperation.class);

    public static final String QUALIFIER = "default";
    private static final String CASE_FIELD_NOT_FOUND = "CaseField with id=[%s] and path=[%s] not found";
    private final UIDefinitionRepository uiDefinitionRepository;
    private final CaseDefinitionRepository caseDefinitionRepository;

    public DefaultGetCriteriaOperation(UIDefinitionRepository uiDefinitionRepository,
                                       @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository) {
        this.uiDefinitionRepository = uiDefinitionRepository;
        this.caseDefinitionRepository = caseDefinitionRepository;
    }

    @Override
    public <T> List<? extends CriteriaInput> execute(final String caseTypeId, final Predicate<AccessControlList> access, CriteriaType criteriaType) {
        LOG.debug("Finding WorkbasketInput fields for caseType={}", caseTypeId);

        final CaseType caseType = caseDefinitionRepository.getCaseType(caseTypeId);
        List<CriteriaInput> criteriaInputs;
        if (criteriaType.equals(WORKBASKET)) {
            WorkbasketInputDefinition workbasketInputDefinition = uiDefinitionRepository.getWorkbasketInputDefinitions(caseTypeId);
            criteriaInputs = workbasketInputDefinition.getFields()
                .stream()
                .map(field -> toCriteriaInput(field, caseType, criteriaType))
                .collect(toList());
        } else if (criteriaType.equals(SEARCH)) {
            final SearchInputDefinition searchInputDefinition = uiDefinitionRepository.getSearchInputDefinitions(caseTypeId);
            criteriaInputs = searchInputDefinition.getFields()
                .stream()
                .map(field -> toCriteriaInput(field, caseType, criteriaType))
                .collect(toList());
        } else {
            throw new IllegalArgumentException("Unknown criteria type");
        }
        return criteriaInputs;
    }

    private CriteriaInput toCriteriaInput(final CriteriaField in, final CaseType caseType, CriteriaType criteriaType) {
        CriteriaInput result;
        if (criteriaType.equals(WORKBASKET)) {
            result = new WorkbasketInput();
        } else {  //criteriaType.equals(SEARCH)
            result = new SearchInput();
        }
        result.setLabel(in.getLabel());
        result.setOrder(in.getDisplayOrder());
        result.setRole(in.getRole());

        CaseField caseField = caseType.getCaseField(in.getCaseFieldId())
            .orElseThrow(() -> new BadRequestException(format(CASE_FIELD_NOT_FOUND, in.getCaseFieldId(), in.getCaseFieldPath())));

        CaseField caseFieldByPath = (CaseField) caseField.getComplexFieldNestedField(in.getCaseFieldPath())
            .orElseThrow(() -> new BadRequestException(format(CASE_FIELD_NOT_FOUND, caseField.getId(), in.getCaseFieldPath())));

        result.setDisplayContextParameter(
            Strings.isNullOrEmpty(in.getCaseFieldPath()) ?
                in.getDisplayContextParameter() :
                caseFieldByPath.getDisplayContextParameter()
        );

        final Field field = new Field();
        field.setId(in.getCaseFieldId());
        field.setType(caseFieldByPath.getFieldType());
        field.setElementPath(in.getCaseFieldPath());
        field.setMetadata(caseFieldByPath.isMetadata());
        field.setShowCondition(in.getShowCondition());
        result.setField(field);
        return result;
    }

}

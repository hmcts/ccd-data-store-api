package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.google.common.base.Strings;
import java.util.List;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CriteriaField;
import uk.gov.hmcts.ccd.domain.model.definition.SearchInputFieldsDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.WorkbasketInputFieldsDefinition;
import uk.gov.hmcts.ccd.domain.model.search.CriteriaInput;
import uk.gov.hmcts.ccd.domain.model.search.CriteriaType;
import uk.gov.hmcts.ccd.domain.model.search.Field;
import uk.gov.hmcts.ccd.domain.model.search.SearchInput;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.ccd.domain.model.search.CriteriaType.SEARCH;
import static uk.gov.hmcts.ccd.domain.model.search.CriteriaType.WORKBASKET;

@Service
@Qualifier(DefaultGetCriteriaOperation.QUALIFIER)
public class DefaultGetCriteriaOperation implements GetCriteriaOperation {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultGetCriteriaOperation.class);

    public static final String QUALIFIER = "default";
    private static final String CASE_FIELD_NOT_FOUND = "CaseField with id=[%s] and path=[%s] not found";
    private final UIDefinitionRepository uiDefinitionRepository;
    private final CaseDefinitionRepository caseDefinitionRepository;

    public DefaultGetCriteriaOperation(UIDefinitionRepository uiDefinitionRepository,
                                       @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                       final CaseDefinitionRepository caseDefinitionRepository) {
        this.uiDefinitionRepository = uiDefinitionRepository;
        this.caseDefinitionRepository = caseDefinitionRepository;
    }

    @Override
    public <T> List<? extends CriteriaInput> execute(final String caseTypeId,
                                                     final Predicate<AccessControlList> access,
                                                     CriteriaType criteriaType) {
        LOG.debug("Finding WorkbasketInput fields for caseType={}", caseTypeId);

        final CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseTypeId);
        List<CriteriaInput> criteriaInputs;
        if (criteriaType.equals(WORKBASKET)) {
            WorkbasketInputFieldsDefinition workbasketInputFieldsDefinition =
                uiDefinitionRepository.getWorkbasketInputDefinitions(caseTypeId);
            criteriaInputs = workbasketInputFieldsDefinition.getFields()
                .stream()
                .map(field -> toCriteriaInput(field, caseTypeDefinition, criteriaType))
                .collect(toList());
        } else if (criteriaType.equals(SEARCH)) {
            final SearchInputFieldsDefinition searchInputFieldsDefinition =
                uiDefinitionRepository.getSearchInputFieldDefinitions(caseTypeId);
            criteriaInputs = searchInputFieldsDefinition.getFields()
                .stream()
                .map(field -> toCriteriaInput(field, caseTypeDefinition, criteriaType))
                .collect(toList());
        } else {
            throw new IllegalArgumentException("Unknown criteria type");
        }
        return criteriaInputs;
    }

    private CriteriaInput toCriteriaInput(final CriteriaField in, final CaseTypeDefinition caseTypeDefinition,
                                          CriteriaType criteriaType) {
        CriteriaInput result;
        if (criteriaType.equals(WORKBASKET)) {
            result = new WorkbasketInput();
        } else {  //criteriaType.equals(SEARCH)
            result = new SearchInput();
        }
        result.setLabel(in.getLabel());
        result.setOrder(in.getDisplayOrder());
        result.setRole(in.getRole());

        CaseFieldDefinition caseFieldDefinition = caseTypeDefinition.getCaseField(in.getCaseFieldId())
            .orElseThrow(() -> new BadRequestException(format(CASE_FIELD_NOT_FOUND, in.getCaseFieldId(),
                in.getCaseFieldPath())));

        CaseFieldDefinition caseFieldDefinitionByPath =
            (CaseFieldDefinition) caseFieldDefinition.getComplexFieldNestedField(in.getCaseFieldPath())
            .orElseThrow(() -> new BadRequestException(
                format(CASE_FIELD_NOT_FOUND, caseFieldDefinition.getId(), in.getCaseFieldPath())));

        result.setDisplayContextParameter(
            Strings.isNullOrEmpty(in.getCaseFieldPath())
                ? in.getDisplayContextParameter()
                : caseFieldDefinitionByPath.getDisplayContextParameter()
        );

        final Field field = new Field();
        field.setId(in.getCaseFieldId());
        field.setType(caseFieldDefinitionByPath.getFieldTypeDefinition());
        field.setElementPath(in.getCaseFieldPath());
        field.setMetadata(caseFieldDefinitionByPath.isMetadata());
        field.setShowCondition(in.getShowCondition());
        result.setField(field);
        return result;
    }

}

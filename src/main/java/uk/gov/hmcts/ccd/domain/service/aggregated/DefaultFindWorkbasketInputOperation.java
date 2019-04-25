package uk.gov.hmcts.ccd.domain.service.aggregated;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.WorkbasketInputDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.WorkbasketInputField;
import uk.gov.hmcts.ccd.domain.model.search.Field;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import java.util.List;
import java.util.function.Predicate;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

@Service
@Qualifier(DefaultFindWorkbasketInputOperation.QUALIFIER)
public class DefaultFindWorkbasketInputOperation implements FindWorkbasketInputOperation {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultFindWorkbasketInputOperation.class);

    public static final String QUALIFIER = "default";
    private static final String CASE_FIELD_NOT_FOUND = "CaseField with id=[%s] and path=[%s] not found";

    private final UIDefinitionRepository uiDefinitionRepository;
    private final CaseDefinitionRepository caseDefinitionRepository;

    public DefaultFindWorkbasketInputOperation(UIDefinitionRepository uiDefinitionRepository,
                                               @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository) {
        this.uiDefinitionRepository = uiDefinitionRepository;
        this.caseDefinitionRepository = caseDefinitionRepository;
    }

    @Override
    public List<WorkbasketInput> execute(String caseTypeId, Predicate<AccessControlList> access) {
        LOG.debug("Finding WorkbasketInput fields for caseType={}", caseTypeId);

        final CaseType caseType = caseDefinitionRepository.getCaseType(caseTypeId);

        final WorkbasketInputDefinition workbasketInputDefinition = uiDefinitionRepository.getWorkbasketInputDefinitions(caseTypeId);

        return workbasketInputDefinition.getFields()
            .stream()
            .map(field -> toWorkbasketInput(field, caseType))
            .collect(toList());
    }

    private WorkbasketInput toWorkbasketInput(final WorkbasketInputField in, final CaseType caseType) {
        final WorkbasketInput result = new WorkbasketInput();
        result.setLabel(in.getLabel());
        result.setOrder(in.getOrder());

        CaseField caseField = caseType.getCaseFieldByPath(in.getCaseFieldId(), in.getCaseFieldElementPath())
            .orElseThrow(() -> new ResourceNotFoundException(format(CASE_FIELD_NOT_FOUND,
                in.getCaseFieldId(), in.getCaseFieldElementPath())));

        final Field field = new Field();
        field.setId(in.getCaseFieldId());
        field.setType(caseField.getFieldType());
        field.setElementPath(in.getCaseFieldElementPath());
        field.setMetadata(caseField.isMetadata());
        result.setField(field);

        return result;
    }
}

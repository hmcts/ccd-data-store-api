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
import uk.gov.hmcts.ccd.domain.model.definition.SearchInputDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.SearchInputField;
import uk.gov.hmcts.ccd.domain.model.search.Field;
import uk.gov.hmcts.ccd.domain.model.search.SearchInput;

import java.util.List;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;

@Service
@Qualifier(DefaultFindSearchInputOperation.QUALIFIER)
public class DefaultFindSearchInputOperation implements FindSearchInputOperation {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultFindSearchInputOperation.class);
    public static final String QUALIFIER = "default";

    private final UIDefinitionRepository uiDefinitionRepository;
    private final CaseDefinitionRepository caseDefinitionRepository;
    public DefaultFindSearchInputOperation(final UIDefinitionRepository uiDefinitionRepository,
                                              @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository) {
        this.uiDefinitionRepository = uiDefinitionRepository;
        this.caseDefinitionRepository = caseDefinitionRepository;
    }

    @Override
    public List<SearchInput> execute(final String jurisdictionId, final String caseTypeId, Predicate<AccessControlList> access) {
        LOG.debug("Finding SearchInput fields for jurisdiction={}, caseType={}", jurisdictionId, caseTypeId);
        final CaseType caseType = caseDefinitionRepository.getCaseType(caseTypeId);
        final SearchInputDefinition searchInputDefinition = uiDefinitionRepository.getSearchInputDefinitions(caseTypeId);

        return searchInputDefinition.getFields()
            .stream()
            .map(field -> toSearchInput(field, caseType))
            .collect(toList());
    }

    private SearchInput toSearchInput(final SearchInputField in, final CaseType caseType){
        final SearchInput result = new SearchInput();
        result.setLabel(in.getLabel());
        result.setOrder(in.getDisplayOrder());
        final Field field =new Field();
        field.setId(in.getCaseFieldId());
        CaseField caseField = getCaseField(in.getCaseFieldId(), caseType);
        field.setType(caseField.getFieldType());
        field.setMetadata(caseField.isMetadata());
        result.setField(field);
        return result;
    }

    private CaseField getCaseField(final String fieldId, final CaseType caseType) {
        return caseType.getCaseFields().stream()
            .filter(c -> c.getId().equals(fieldId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException(String.format("FieldId %s not found", fieldId)));
    }
}

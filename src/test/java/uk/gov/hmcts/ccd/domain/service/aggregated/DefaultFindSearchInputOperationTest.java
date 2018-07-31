package uk.gov.hmcts.ccd.domain.service.aggregated;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.domain.model.definition.SearchInputDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.SearchInputField;
import uk.gov.hmcts.ccd.domain.model.search.SearchInput;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

class DefaultFindSearchInputOperationTest {
    @Mock
    private UIDefinitionRepository uiDefinitionRepository;
    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    private final CaseType caseType = new CaseType();
    private DefaultFindSearchInputOperation findSearchInputOperation;
    private final CaseField caseField1 = new CaseField();
    private final CaseField caseField2 = new CaseField();
    private final CaseField caseField3 = new CaseField();
    private final CaseField caseField4 = new CaseField();

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
        FieldType fieldType = new FieldType();
        caseField1.setId("field1");
        caseField1.setFieldType(fieldType);
        caseField2.setId("field2");
        caseField2.setFieldType(fieldType);
        caseField3.setId("field3");
        caseField3.setFieldType(fieldType);
        caseField4.setId("field4");
        caseField4.setFieldType(fieldType);
        caseField4.setMetadata(true);
        caseType.setId("Test case type");
        caseType.setCaseFields(Arrays.asList(caseField1, caseField2, caseField3, caseField4));

        findSearchInputOperation = new DefaultFindSearchInputOperation(uiDefinitionRepository, caseDefinitionRepository);

        doReturn(caseType).when(caseDefinitionRepository).getCaseType(caseType.getId());
        doReturn(generateSearchInput()).when(uiDefinitionRepository).getSearchInputDefinitions(caseType.getId());
    }

    @Test
    void shouldReturnSearchInputs() {
        List<SearchInput> searchInputs = findSearchInputOperation.execute("TEST", caseType.getId(), CAN_READ);

        assertAll(
            () -> assertThat(searchInputs.size(), is(4)),
            () -> assertThat(searchInputs.get(0).getField().getId(), is("field1")),
            () -> assertThat(searchInputs.get(3).getField().isMetadata(), is(true))
        );
    }

    private SearchInputDefinition generateSearchInput() {
        SearchInputDefinition searchInputDefinition = new SearchInputDefinition();
        searchInputDefinition.setCaseTypeId(caseType.getId());
        searchInputDefinition.setFields(Arrays.asList(getField(caseField1.getId(), 1),
                                                      getField(caseField2.getId(), 2),
                                                      getField(caseField3.getId(), 3),
                                                      getField(caseField4.getId(), 4)));
        return searchInputDefinition;

    }

    private SearchInputField getField(String id, int order) {
        SearchInputField searchInputField = new SearchInputField();
        searchInputField.setCaseFieldId(id);
        searchInputField.setLabel(id);
        searchInputField.setDisplayOrder(order);
        return searchInputField;
    }

}

package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPageField;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class FieldProcessorServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<HashMap<String, JsonNode>> STRING_JSON_MAP = new TypeReference<HashMap<String, JsonNode>>() {};
    private static final String CASE_TYPE_ID = "CaseType";
    private static final String EVENT_ID = "EventId";
    private static final String ID1 = "ID1";
    private static final String ID2 = "ID2";

    private List<WizardPage> wizardPages = new ArrayList<>();
    private WizardPageField wizardPageField1 = wizardPageField(ID1);
    private WizardPageField wizardPageField2 = wizardPageField(ID2);
    private List<CaseDataFieldProcessor> caseDataFieldProcessors = new ArrayList<>();
    private List<CaseViewFieldProcessor> caseViewFieldProcessors = new ArrayList<>();

    private FieldProcessorService fieldProcessorService;

    @Mock
    private UIDefinitionRepository uiDefinitionRepository;

    @Mock
    private EventTriggerService eventTriggerService;

    @Mock
    private CaseDataFieldProcessor caseDataFieldProcessor1;

    @Mock
    private CaseDataFieldProcessor caseDataFieldProcessor2;

    @Mock
    private CaseViewFieldProcessor caseViewFieldProcessor1;

    @Mock
    private CaseViewFieldProcessor caseViewFieldProcessor2;

    @Mock
    private CaseTypeDefinition caseType;

    @Mock
    private CaseEventDefinition event;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        caseDataFieldProcessors.add(caseDataFieldProcessor1);
        caseDataFieldProcessors.add(caseDataFieldProcessor2);
        caseViewFieldProcessors.add(caseViewFieldProcessor1);
        caseViewFieldProcessors.add(caseViewFieldProcessor2);

        fieldProcessorService = new FieldProcessorService(
            caseDataFieldProcessors,
            caseViewFieldProcessors,
            uiDefinitionRepository,
            eventTriggerService
        );

        when(caseType.getId()).thenReturn(CASE_TYPE_ID);
        when(event.getId()).thenReturn(EVENT_ID);

        WizardPage wizardPage1 = new WizardPage();
        WizardPage wizardPage2 = new WizardPage();
        wizardPage1.setWizardPageFields(Collections.singletonList(wizardPageField1));
        wizardPage2.setWizardPageFields(Collections.singletonList(wizardPageField2));
        wizardPages.add(wizardPage1);
        wizardPages.add(wizardPage2);
        when(uiDefinitionRepository.getWizardPageCollection(Mockito.eq(CASE_TYPE_ID), Mockito.eq(EVENT_ID))).thenReturn(wizardPages);
    }

    @Nested
    @DisplayName("processCaseViewField tests")
    class ProcessCaseViewFieldTest {

        @Test
        void shouldExecuteAgainstAllCaseViewFieldProcessors() {
            CaseViewField caseViewField1 = new CaseViewField();
            CaseViewField caseViewField2 = new CaseViewField();
            CaseViewField caseViewField3 = new CaseViewField();
            when(caseViewFieldProcessor1.execute(caseViewField1, null)).thenReturn(caseViewField2);
            when(caseViewFieldProcessor2.execute(caseViewField2, null)).thenReturn(caseViewField3);

            final CaseViewField result = fieldProcessorService.processCaseViewField(caseViewField1);

            verify(caseViewFieldProcessor1).execute(caseViewField1, null);
            verify(caseViewFieldProcessor2).execute(caseViewField2, null);
            verifyNoMoreInteractions(caseViewFieldProcessor1, caseViewFieldProcessor2);
            assertAll(
                () -> assertThat(result, is(caseViewField3))
            );
        }
    }

    @Nested
    @DisplayName("processCaseViewFields tests")
    class ProcessCaseViewFieldsTest {

        @Test
        void shouldProcessAllCaseFieldsWithAllProcessors() {
            CaseViewField caseViewField1 = caseViewField(ID1);
            CaseViewField caseViewField2 = caseViewField(ID2);
            when(caseViewFieldProcessor1.execute(Mockito.any(CaseViewField.class), Mockito.any(WizardPageField.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            when(caseViewFieldProcessor2.execute(Mockito.any(CaseViewField.class), Mockito.any(WizardPageField.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            List<CaseViewField> caseViewFields = Arrays.asList(caseViewField1, caseViewField2);

            final List<CaseViewField> result = fieldProcessorService.processCaseViewFields(caseViewFields, caseType, event);

            verify(caseViewFieldProcessor1).execute(caseViewField1, wizardPageField1);
            verify(caseViewFieldProcessor1).execute(caseViewField2, wizardPageField2);
            verify(caseViewFieldProcessor2).execute(caseViewField1, wizardPageField1);
            verify(caseViewFieldProcessor2).execute(caseViewField2, wizardPageField2);
            verifyNoMoreInteractions(caseViewFieldProcessor1, caseViewFieldProcessor2);
            assertAll(
                () -> assertThat(result.size(), is(2)),
                () -> assertThat(result.get(0), is(caseViewField1)),
                () -> assertThat(result.get(1), is(caseViewField2))
            );
        }
    }

    @Nested
    @DisplayName("processData tests")
    class ProcessDataTest {

        @Test
        void shouldProcessDataWithAllProcessors() throws IOException {
            CaseFieldDefinition caseField1 = caseField(ID1);
            CaseFieldDefinition caseField2 = caseField(ID2);
            CaseEventFieldDefinition caseEventField1 = caseEventField(ID1);
            CaseEventFieldDefinition caseEventField2 = caseEventField(ID2);
            when(caseType.getCaseField(eq(ID1))).thenReturn(Optional.of(caseField1));
            when(caseType.getCaseField(eq(ID2))).thenReturn(Optional.of(caseField2));
            when(event.getCaseEventField(eq(ID1))).thenReturn(Optional.of(caseEventField1));
            when(event.getCaseEventField(eq(ID2))).thenReturn(Optional.of(caseEventField2));
            when(caseDataFieldProcessor1
                .execute(Mockito.any(JsonNode.class),
                    Mockito.any(CaseFieldDefinition.class),
                    Mockito.any(CaseEventFieldDefinition.class),
                    Mockito.any(WizardPageField.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            when(caseDataFieldProcessor2
                .execute(Mockito.any(JsonNode.class),
                    Mockito.any(CaseFieldDefinition.class),
                    Mockito.any(CaseEventFieldDefinition.class),
                    Mockito.any(WizardPageField.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            final Map<String, JsonNode> result = fieldProcessorService.processData(data(), caseType, event);

            verify(caseDataFieldProcessor1).execute(Mockito.any(JsonNode.class), eq(caseField1), eq(caseEventField1), eq(wizardPageField1));
            verify(caseDataFieldProcessor1).execute(Mockito.any(JsonNode.class), eq(caseField2), eq(caseEventField2), eq(wizardPageField2));
            verify(caseDataFieldProcessor2).execute(Mockito.any(JsonNode.class), eq(caseField1), eq(caseEventField1), eq(wizardPageField1));
            verify(caseDataFieldProcessor2).execute(Mockito.any(JsonNode.class), eq(caseField2), eq(caseEventField2), eq(wizardPageField2));
            verifyNoMoreInteractions(caseDataFieldProcessor1, caseDataFieldProcessor2);
            assertAll(
                () -> assertThat(result.size(), is(2)),
                () -> assertThat(result.get(ID1).asText(), is("Value 1")),
                () -> assertThat(result.get(ID2).asText(), is("Value 2"))
            );
        }

        @Test
        void shouldReturnBackEmptyData() {
            Map<String, JsonNode> data = new HashMap<>();

            final Map<String, JsonNode> result = fieldProcessorService.processData(data, caseType, event);

            verifyNoMoreInteractions(caseDataFieldProcessor1, caseDataFieldProcessor2);
            assertAll(
                () -> assertThat(result.size(), is(0)),
                () -> assertThat(result, is(data))
            );
        }

        @Test
        void shouldNotProcessEmptyField() throws IOException {
            String json = "{\"ID1\":\"  \"}";
            Map<String, JsonNode> data = MAPPER.convertValue(MAPPER.readTree(json), STRING_JSON_MAP);

            final Map<String, JsonNode> result = fieldProcessorService.processData(data, caseType, event);

            verifyNoMoreInteractions(caseDataFieldProcessor1, caseDataFieldProcessor2);
            assertAll(
                () -> assertThat(result.size(), is(1)),
                () -> assertThat(result, is(data))
            );
        }

        private Map<String, JsonNode> data() throws IOException {
            String json = "{\"ID1\":\"Value 1\",\"ID2\":\"Value 2\"}";
            return MAPPER.convertValue(MAPPER.readTree(json), STRING_JSON_MAP);
        }
    }

    private WizardPageField wizardPageField(String id) {
        WizardPageField wizardPageField = new WizardPageField();
        wizardPageField.setCaseFieldId(id);
        return wizardPageField;
    }

    private CaseFieldDefinition caseField(String id) {
        CaseFieldDefinition caseField = new CaseFieldDefinition();
        caseField.setId(id);
        return caseField;
    }

    private CaseEventFieldDefinition caseEventField(String id) {
        CaseEventFieldDefinition caseEventField = new CaseEventFieldDefinition();
        caseEventField.setCaseFieldId(id);
        return caseEventField;
    }

    private CaseViewField caseViewField(String id) {
        CaseViewField caseViewField = new CaseViewField();
        caseViewField.setId(id);
        return caseViewField;
    }
}

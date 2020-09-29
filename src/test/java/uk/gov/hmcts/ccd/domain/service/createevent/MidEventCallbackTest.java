package uk.gov.hmcts.ccd.domain.service.createevent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPageComplexFieldOverride;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPageField;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;

class MidEventCallbackTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String JURISDICTION_ID = "jurisdictionId";
    private static final String CASE_TYPE_ID = "caseTypeId";
    private static final Boolean IGNORE_WARNINGS = Boolean.FALSE;
    private static final String CASE_REFERENCE = "12356878998658";

    @InjectMocks
    private MidEventCallback midEventCallback;

    @Mock
    private CallbackInvoker callbackInvoker;
    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;
    @Mock
    private EventTriggerService eventTriggerService;
    @Mock
    private UIDefinitionRepository uiDefinitionRepository;
    @Mock
    private CaseService caseService;

    @Mock
    private CaseEventDefinition caseEventDefinition;
    @Mock
    private CaseTypeDefinition caseTypeDefinition;
    private Event event;
    private final Map<String, JsonNode> data = new HashMap<>();
    private CaseDetails caseDetails;
    private WizardPage wizardPageWithCallback;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        event = new Event();
        event.setEventId("createCase");

        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(caseTypeDefinition);
        given(eventTriggerService.findCaseEvent(caseTypeDefinition, event.getEventId()))
            .willReturn(caseEventDefinition);
        given(caseTypeDefinition.getJurisdictionId()).willReturn(JURISDICTION_ID);
        caseDetails = caseDetails(data);
        given(caseService.createNewCaseDetails(CASE_TYPE_ID, JURISDICTION_ID, data)).willReturn(caseDetails);

        wizardPageWithCallback = createWizardPage("createCase1", "http://some-callback-url.com");
        WizardPage wizardPageWithoutCallback = createWizardPage("createCase2");
        given(uiDefinitionRepository.getWizardPageCollection(CASE_TYPE_ID, event.getEventId()))
            .willReturn(asList(wizardPageWithCallback, wizardPageWithoutCallback));
    }

    @Test
    @DisplayName("should invoke MidEvent callback for a defined url")
    void shouldInvokeMidEventCallbackWhenUrlDefined() {
        CaseDataContent build = newCaseDataContent().withEvent(event).withCaseReference(CASE_REFERENCE)
            .withData(data).withIgnoreWarning(IGNORE_WARNINGS).build();
        CaseDetails existingCaseDetails = caseDetails(data);
        when(caseService.getCaseDetails(caseDetails.getJurisdiction(), CASE_REFERENCE))
            .thenReturn(existingCaseDetails);
        when(caseService.clone(existingCaseDetails)).thenReturn(existingCaseDetails);

        given(callbackInvoker.invokeMidEventCallback(wizardPageWithCallback,
            caseTypeDefinition,
                caseEventDefinition,
            existingCaseDetails,
            caseDetails,
            IGNORE_WARNINGS)).willReturn(caseDetails);

        given(caseService.populateCurrentCaseDetailsWithEventFields(build, existingCaseDetails))
            .willReturn(caseDetails);


        midEventCallback.invoke(CASE_TYPE_ID,
            build,
            "createCase1"
        );

        verify(callbackInvoker).invokeMidEventCallback(wizardPageWithCallback,
            caseTypeDefinition,
                caseEventDefinition,
            existingCaseDetails,
            caseDetails,
            IGNORE_WARNINGS);
    }

    @Test
    @DisplayName("should update data from MidEvent callback")
    void shouldUpdateCaseDetailsFromMidEventCallback() throws Exception {

        final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
            "{\n"
                + "  \"PersonFirstName\": \"First Name\",\n"
                + "  \"PersonLastName\": \"Last Name\"\n"
                + "}"));
        CaseDetails updatedCaseDetails = caseDetails(data);
        CaseDataContent content = newCaseDataContent().withEvent(event).withData(data)
            .withIgnoreWarning(IGNORE_WARNINGS)
            .build();
        when(caseService.clone(updatedCaseDetails)).thenReturn(updatedCaseDetails);
        given(callbackInvoker.invokeMidEventCallback(wizardPageWithCallback,
            caseTypeDefinition,
                caseEventDefinition,
            null,
            caseDetails,
            IGNORE_WARNINGS)).willReturn(updatedCaseDetails);

        given(caseService.createNewCaseDetails(CASE_TYPE_ID, JURISDICTION_ID, data)).willReturn(caseDetails);


        JsonNode result = midEventCallback.invoke(CASE_TYPE_ID,
            content,
            "createCase1");

        final JsonNode expectedResponse = MAPPER.readTree(
            "{"
                + "\"data\": {\n"
                + "  \"PersonFirstName\": \"First Name\",\n"
                + "  \"PersonLastName\": \"Last Name\"\n"
                + "}}");
        assertThat(result, is(expectedResponse));
    }

    @Test
    @DisplayName("test no interaction when pageId not present")
    void testNoInteractionWhenMidEventCallbackUrlNotPresent() throws IOException {
        JsonNode result = midEventCallback.invoke(CASE_TYPE_ID,
            newCaseDataContent().withEvent(event).withData(data).withIgnoreWarning(IGNORE_WARNINGS).build(),
            "");

        final JsonNode expectedResponse = MAPPER.readTree("{\"data\": {}}");
        assertThat("Data should stay unchanged", result, is(expectedResponse));
        verifyNoMoreInteractions(callbackInvoker, caseDefinitionRepository, eventTriggerService,
            uiDefinitionRepository, caseService);
    }

    @Test
    @DisplayName("should pass event data to MidEvent callback when available")
    void shouldPassEventDataToMidEventCallback() throws Exception {

        Map<String, JsonNode> eventData = JacksonUtils.convertValue((MAPPER.readTree(
            "{\n"
                + "  \"PersonFirstName\": \"First Name\",\n"
                + "  \"PersonLastName\": \"Last Name\"\n"
                + "}")));
        CaseDetails updatedCaseDetails = caseDetails(eventData);

        CaseDataContent content = newCaseDataContent()
            .withEvent(event)
            .withData(data)
            .withEventData(eventData)
            .withIgnoreWarning(IGNORE_WARNINGS)
            .build();

        when(callbackInvoker.invokeMidEventCallback(wizardPageWithCallback,
            caseTypeDefinition,
                caseEventDefinition,
            null,
            caseDetails,
            IGNORE_WARNINGS)).thenReturn(updatedCaseDetails);
        when(caseService.createNewCaseDetails(CASE_TYPE_ID, JURISDICTION_ID, eventData)).thenReturn(caseDetails);
        given(caseService.populateCurrentCaseDetailsWithEventFields(content, updatedCaseDetails)).willReturn(null);

        JsonNode result = midEventCallback.invoke(CASE_TYPE_ID,
            content,
            "createCase1");

        JsonNode expectedResponse = MAPPER.readTree(
            "{"
                + "\"data\": {\n"
                + "  \"PersonFirstName\": \"First Name\",\n"
                + "  \"PersonLastName\": \"Last Name\"\n"
                + "}}");

        assertAll(
            () -> assertThat(result, is(expectedResponse)),
            () -> verify(callbackInvoker).invokeMidEventCallback(wizardPageWithCallback,
                caseTypeDefinition,
                caseEventDefinition,
                null,
                caseDetails,
                IGNORE_WARNINGS),
            () -> verify(caseService, never()).createNewCaseDetails(CASE_TYPE_ID, JURISDICTION_ID, data),
            () -> verify(caseService).createNewCaseDetails(CASE_TYPE_ID, JURISDICTION_ID, eventData));
    }

    @Test
    @DisplayName("should include data from existing case reference during a midevent callback")
    void shouldContainAllDataFromExistingCaseReferenceDuringAMidEventCallback() throws Exception {

        Map<String, JsonNode> eventData = JacksonUtils.convertValue((MAPPER.readTree(
            "{\n"
                + "  \"PersonFirstName\": \"First Name\",\n"
                + "  \"PersonLastName\": \"Last Name\"\n"
                + "}")));

        Map<String, JsonNode> existingData = JacksonUtils.convertValue((MAPPER.readTree(
            "{\n"
                + "  \"PersonFirstName\": \"First Name\",\n"
                + "  \"PersonMiddleName\": \"Middle Name\"\n"
                + "}")));
        Map<String, JsonNode> combineData = JacksonUtils.convertValue((MAPPER.readTree(
            "{\n"
                + "  \"PersonFirstName\": \"First Name\",\n"
                + "  \"PersonLastName\": \"Last Name\",\n"
                + "  \"PersonMiddleName\": \"Middle Name\"\n"
                + "}")));
        CaseDetails existingCaseDetails = caseDetails(existingData);
        CaseDetails combineCaseDetails = caseDetails(combineData);
        CaseDataContent content = newCaseDataContent()
            .withEvent(event)
            .withData(data)
            .withCaseReference(CASE_REFERENCE)
            .withEventData(eventData)
            .withIgnoreWarning(IGNORE_WARNINGS)
            .build();

        when(callbackInvoker.invokeMidEventCallback(wizardPageWithCallback,
            caseTypeDefinition,
                caseEventDefinition,
            existingCaseDetails,
            combineCaseDetails,
            IGNORE_WARNINGS)).thenReturn(combineCaseDetails);
        when(caseService.clone(existingCaseDetails)).thenReturn(existingCaseDetails);
        when(caseService.createNewCaseDetails(Mockito.eq(CASE_TYPE_ID), Mockito.eq(JURISDICTION_ID),
            Mockito.isA(Map.class))).thenReturn(combineCaseDetails);
        given(caseService.getCaseDetails(JURISDICTION_ID, content.getCaseReference())).willReturn(existingCaseDetails);
        given(caseService.populateCurrentCaseDetailsWithEventFields(content, existingCaseDetails))
            .willReturn(combineCaseDetails);


        JsonNode result = midEventCallback.invoke(CASE_TYPE_ID,
            content,
            "createCase1");

        JsonNode expectedResponse = MAPPER.readTree(
            "{"
                + "\"data\": {\n"
                + "  \"PersonFirstName\": \"First Name\",\n"
                + "  \"PersonLastName\": \"Last Name\",\n"
                + "  \"PersonMiddleName\": \"Middle Name\"\n"
                + "}}");

        assertAll(
            () -> assertThat(result, is(expectedResponse)),
            () -> verify(callbackInvoker)
                .invokeMidEventCallback(wizardPageWithCallback,
                                            caseTypeDefinition, caseEventDefinition, existingCaseDetails,
                                            combineCaseDetails, IGNORE_WARNINGS),
            () -> verify(caseService, never()).createNewCaseDetails(CASE_TYPE_ID, JURISDICTION_ID, combineData));
    }

    @Test
    @DisplayName("should call filter case data content when wizard page order exists")
    void shouldCallFilterCaseDataContentWhenWizardPageOrderExists() {
        given(uiDefinitionRepository.getWizardPageCollection(CASE_TYPE_ID, event.getEventId()))
            .willReturn(asList(wizardPageWithCallback));
        CaseDetails existingCaseDetails = caseDetails(data);
        when(caseService.getCaseDetails(caseDetails.getJurisdiction(), CASE_REFERENCE))
            .thenReturn(existingCaseDetails);
        when(caseService.clone(existingCaseDetails)).thenReturn(existingCaseDetails);
        wizardPageWithCallback.setOrder(1);

        given(callbackInvoker.invokeMidEventCallback(wizardPageWithCallback,
            caseTypeDefinition,
            caseEventDefinition,
            existingCaseDetails,
            caseDetails,
            IGNORE_WARNINGS)).willReturn(caseDetails);

        CaseDataContent build = newCaseDataContent().withEvent(event).withCaseReference(CASE_REFERENCE)
            .withData(data).withIgnoreWarning(IGNORE_WARNINGS).build();
        given(caseService.populateCurrentCaseDetailsWithEventFields(build, existingCaseDetails))
            .willReturn(caseDetails);


        midEventCallback.invoke(CASE_TYPE_ID,
            build,
            "createCase1"
        );

        verify(callbackInvoker).invokeMidEventCallback(wizardPageWithCallback,
            caseTypeDefinition,
            caseEventDefinition,
            existingCaseDetails,
            caseDetails,
            IGNORE_WARNINGS);
    }

    @Test
    @DisplayName("should filter case data content when wizard page order exists")
    void shouldFilterCaseDataContentWhenWizardPageOrderExists() {
        WizardPage wizardPageWithoutCallback = createWizardPage("createCase2");
        wizardPageWithoutCallback.setOrder(2);
        WizardPageField pageField = createWizardPageField("createCase2_field1");
        pageField.setComplexFieldOverrides(Lists.newArrayList(createComplexFieldOverrides(
            "createCase2_field1_complex1")));
        wizardPageWithoutCallback.setWizardPageFields(Lists.newArrayList(pageField));
        given(uiDefinitionRepository.getWizardPageCollection(CASE_TYPE_ID, event.getEventId()))
            .willReturn(asList(wizardPageWithCallback, wizardPageWithoutCallback));
        Map<String, JsonNode> data = createData();
        CaseDetails existingCaseDetails = caseDetails(data);
        when(caseService.getCaseDetails(caseDetails.getJurisdiction(), CASE_REFERENCE))
            .thenReturn(existingCaseDetails);
        when(caseService.clone(existingCaseDetails)).thenReturn(existingCaseDetails);
        wizardPageWithCallback.setOrder(1);

        given(callbackInvoker.invokeMidEventCallback(wizardPageWithCallback,
            caseTypeDefinition,
            caseEventDefinition,
            existingCaseDetails,
            caseDetails,
            IGNORE_WARNINGS)).willReturn(caseDetails);

        CaseDataContent build = newCaseDataContent().withEvent(event).withCaseReference(CASE_REFERENCE)
            .withData(data).withIgnoreWarning(IGNORE_WARNINGS).build();
        given(caseService.populateCurrentCaseDetailsWithEventFields(build, existingCaseDetails))
            .willReturn(caseDetails);


        midEventCallback.invoke(CASE_TYPE_ID,
            build,
            "createCase1"
        );

        verify(callbackInvoker).invokeMidEventCallback(wizardPageWithCallback,
            caseTypeDefinition,
            caseEventDefinition,
            existingCaseDetails,
            caseDetails,
            IGNORE_WARNINGS);
    }

    private Map<String, JsonNode> createData() {
        Map<String, JsonNode> data = new HashMap<>();
        data.put("createCase2_field1", new TextNode("test1"));
        data.put("createCase2_field1_complex1", new TextNode("complex1"));
        return data;
    }

    private WizardPageField createWizardPageField(String caseFieldId) {
        WizardPageField wizardPageField = new WizardPageField();
        wizardPageField.setCaseFieldId(caseFieldId);
        return wizardPageField;
    }

    private WizardPageComplexFieldOverride createComplexFieldOverrides(String elementId) {
        WizardPageComplexFieldOverride wizardPageComplexFieldOverride = new WizardPageComplexFieldOverride();
        wizardPageComplexFieldOverride.setComplexFieldElementId(elementId);
        return wizardPageComplexFieldOverride;
    }

    private CaseDetails caseDetails(Map<String, JsonNode> data) {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setJurisdiction(JURISDICTION_ID);
        caseDetails.setData(data);
        return caseDetails;
    }

    private WizardPage createWizardPage(String wizardPageId) {
        return createWizardPage(wizardPageId, null);
    }

    private WizardPage createWizardPage(String wizardPageId,
                                        String midEventUrl) {
        WizardPage wizardPage = new WizardPage();
        wizardPage.setId(wizardPageId);
        if (!StringUtils.isBlank(midEventUrl)) {
            wizardPage.setCallBackURLMidEvent(midEventUrl);
        }
        return wizardPage;
    }
}

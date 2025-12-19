package uk.gov.hmcts.ccd.domain.service.createevent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
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
        MockitoAnnotations.openMocks(this);

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
            """
                {
                  "PersonFirstName": "First Name",
                  "PersonLastName": "Last Name"
                }"""));
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


        Map<String, JsonNode> result = midEventCallback.invoke(CASE_TYPE_ID,
            content,
            "createCase1");

        Map<String, JsonNode> expectedResponse = JacksonUtils.convertValue((MAPPER.readTree(
            """
                {
                  "PersonFirstName": "First Name",
                  "PersonLastName": "Last Name"
                }""")));
        assertThat(result, is(expectedResponse));
    }

    @Test
    @DisplayName("test no interaction when pageId not present")
    void testNoInteractionWhenMidEventCallbackUrlNotPresent() throws IOException {
        Map<String, JsonNode> result = midEventCallback.invoke(CASE_TYPE_ID,
            newCaseDataContent().withEvent(event).withData(data).withIgnoreWarning(IGNORE_WARNINGS).build(),
            "");

        final Map<String, JsonNode> expectedResponse = JacksonUtils.convertValue((MAPPER.readTree("{}")));
        assertThat("Data should stay unchanged", result, is(expectedResponse));
        verifyNoMoreInteractions(callbackInvoker, caseDefinitionRepository, eventTriggerService,
            uiDefinitionRepository, caseService);
    }

    @Test
    @DisplayName("should pass event data to MidEvent callback when available")
    void shouldPassEventDataToMidEventCallback() throws Exception {

        Map<String, JsonNode> eventData = JacksonUtils.convertValue((MAPPER.readTree(
            """
                {
                  "PersonFirstName": "First Name",
                  "PersonLastName": "Last Name"
                }""")));
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

        Map<String, JsonNode> result = midEventCallback.invoke(CASE_TYPE_ID,
            content,
            "createCase1");

        Map<String, JsonNode> expectedResponse = JacksonUtils.convertValue((MAPPER.readTree(
            """
                {
                  "PersonFirstName": "First Name",
                  "PersonLastName": "Last Name"
                }""")));

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
            """
                {
                  "PersonFirstName": "First Name",
                  "PersonLastName": "Last Name"
                }""")));

        Map<String, JsonNode> existingData = JacksonUtils.convertValue((MAPPER.readTree(
            """
                {
                  "PersonFirstName": "First Name",
                  "PersonMiddleName": "Middle Name"
                }""")));
        Map<String, JsonNode> combineData = JacksonUtils.convertValue((MAPPER.readTree(
            """
                {
                  "PersonFirstName": "First Name",
                  "PersonLastName": "Last Name",
                  "PersonMiddleName": "Middle Name"
                }""")));
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


        Map<String, JsonNode> result = midEventCallback.invoke(CASE_TYPE_ID,
            content,
            "createCase1");

        Map<String, JsonNode> expectedResponse = JacksonUtils.convertValue((MAPPER.readTree(
            """
                {
                  "PersonFirstName": "First Name",
                  "PersonLastName": "Last Name",
                  "PersonMiddleName": "Middle Name"
                }""")));

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
            .willReturn(Collections.singletonList(wizardPageWithCallback));
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

    @Test
    @DisplayName("should retain mid-event mutations across sequential callback invocations")
    void shouldRetainMidEventMutationsAcrossSequentialInvocations() {
        AtomicInteger callbackCount = new AtomicInteger(0);
        List<Map<String, JsonNode>> seenInputs = new ArrayList<>();
        List<Map<String, JsonNode>> seenOutputs = new ArrayList<>();

        when(caseService.getCaseDetails(caseDetails.getJurisdiction(), CASE_REFERENCE))
            .thenAnswer(invocation -> cloneCaseDetails(baseCaseDetails()));
        when(caseService.clone(any(CaseDetails.class)))
            .thenAnswer(invocation -> cloneCaseDetails(invocation.getArgument(0)));
        when(caseService.populateCurrentCaseDetailsWithEventFields(any(CaseDataContent.class), any(CaseDetails.class)))
            .thenAnswer(invocation -> {
                CaseDataContent content = invocation.getArgument(0);
                CaseDetails details = cloneCaseDetails(invocation.getArgument(1));
                Map<String, JsonNode> payload = Optional.ofNullable(content.getEventData())
                    .orElse(content.getData());
                payload.forEach(details.getData()::put);
                return details;
            });

        when(callbackInvoker.invokeMidEventCallback(eq(wizardPageWithCallback),
            eq(caseTypeDefinition),
            eq(caseEventDefinition),
            any(CaseDetails.class),
            any(CaseDetails.class),
            eq(IGNORE_WARNINGS)))
            .thenAnswer(invocation -> {
                CaseDetails supplied = invocation.getArgument(4);
                Map<String, JsonNode> suppliedData = deepCopyData(supplied.getData());
                seenInputs.add(deepCopyData(suppliedData));

                int index = callbackCount.getAndIncrement();

                String currentYesNo = yesOrNoValue(suppliedData);
                String nextYesNo = currentYesNo == null ? "Yes"
                    : ("Yes".equalsIgnoreCase(currentYesNo) ? "No" : "Yes");

                ArrayNode letters = suppliedData.containsKey("letters")
                    ? ((ArrayNode) suppliedData.get("letters")).deepCopy()
                    : JsonNodeFactory.instance.arrayNode();
                letters.insert(0, JsonNodeFactory.instance.textNode("Callback_" + (index + 1)));
                suppliedData.put("letters", letters);
                suppliedData.put("y_or_n",
                    nextYesNo == null ? JsonNodeFactory.instance.nullNode()
                        : JsonNodeFactory.instance.textNode(nextYesNo));

                CaseDetails response = new CaseDetails();
                response.setData(suppliedData);
                return response;
            });

        CaseDataContent content = newCaseDataContent()
            .withEvent(event)
            .withCaseReference(CASE_REFERENCE)
            .withIgnoreWarning(IGNORE_WARNINGS)
            .build();

        Map<String, JsonNode> payload = baseData();
        for (int i = 0; i < 3; i++) {
            content.setEventData(deepCopyData(payload));
            Map<String, JsonNode> payloadBeforeCall = deepCopyData(payload);
            payload = midEventCallback.invoke(CASE_TYPE_ID, content, wizardPageWithCallback.getId());
            seenOutputs.add(deepCopyData(payload));

            int callIndex = i;
            String messagePrefix = "Mid-event call " + (i + 1) + " ";
            String beforeYesNo = yesOrNoValue(payloadBeforeCall);
            String inputYesNo = yesOrNoValue(seenInputs.get(i));
            String outputYesNo = yesOrNoValue(payload);
            String inputLetters = lettersFrom(seenInputs.get(i)).toString();
            String outputLetters = lettersFrom(payload).toString();
            String expectedOutputYesNo = switch (callIndex) {
                case 0 -> "Yes";
                case 1 -> "No";
                default -> "Yes";
            };

            assertAll(
                () -> assertEquals(beforeYesNo, inputYesNo,
                    messagePrefix + "input Yes/No should match client-submitted payload; input=" + inputYesNo),
                () -> assertEquals(expectedOutputYesNo, outputYesNo,
                    messagePrefix + "output should toggle Yes/No; expected=" + expectedOutputYesNo
                        + ", before=" + beforeYesNo
                        + ", input=" + inputYesNo + ", output=" + outputYesNo),
                () -> org.junit.jupiter.api.Assertions.assertTrue(
                    outputLetters.startsWith("[Callback_") || callIndex == 0,
                    messagePrefix + "output letters should prepend callback marker; input=" + inputLetters
                        + ", output=" + outputLetters)
            );
        }

        Map<String, JsonNode> finalPayload = payload;

        assertAll(
            () -> assertEquals(List.of("B", "A"), lettersFrom(seenInputs.get(0))),
            () -> assertNull(yesOrNoValue(seenInputs.get(0))),
            () -> assertEquals(List.of("Callback_1", "B", "A"), lettersFrom(seenOutputs.get(0))),
            () -> assertEquals("Yes", yesOrNoValue(seenOutputs.get(0))),
            () -> assertEquals(List.of("Callback_2", "Callback_1", "B", "A"), lettersFrom(seenOutputs.get(1))),
            () -> assertEquals("No", yesOrNoValue(seenOutputs.get(1))),
            () -> assertEquals(List.of("Callback_3", "Callback_2", "Callback_1", "B", "A"),
                lettersFrom(seenOutputs.get(2))),
            () -> assertEquals("Yes", yesOrNoValue(seenOutputs.get(2))),
            () -> assertEquals(List.of("Callback_3", "Callback_2", "Callback_1", "B", "A"), lettersFrom(finalPayload),
                "Final payload should retain collection returned by last mid-event"),
            () -> assertEquals("Yes", yesOrNoValue(finalPayload),
                "Final payload should end with Yes after third mid-event (before submission toggle)")
        );
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

    private Map<String, JsonNode> baseData() {
        Map<String, JsonNode> startingData = new HashMap<>();
        ArrayNode letters = JsonNodeFactory.instance.arrayNode();
        letters.add("B");
        letters.add("A");
        startingData.put("letters", letters);
        return startingData;
    }

    private Map<String, JsonNode> buildCallbackData(List<String> letters, String yesOrNo) {
        Map<String, JsonNode> data = new HashMap<>();
        ArrayNode lettersNode = JsonNodeFactory.instance.arrayNode();
        letters.forEach(lettersNode::add);
        data.put("letters", lettersNode);
        data.put("y_or_n", yesOrNo == null ? JsonNodeFactory.instance.nullNode()
            : JsonNodeFactory.instance.textNode(yesOrNo));
        return data;
    }

    private CaseDetails baseCaseDetails() {
        return caseDetails(baseData());
    }

    private Map<String, JsonNode> deepCopyData(Map<String, JsonNode> source) {
        return JacksonUtils.convertValue(JacksonUtils.convertValueJsonNode(source));
    }

    private CaseDetails cloneCaseDetails(CaseDetails source) {
        return caseDetails(deepCopyData(source.getData()));
    }

    private List<String> lettersFrom(Map<String, JsonNode> data) {
        JsonNode lettersNode = data.get("letters");
        List<String> letters = new ArrayList<>();
        if (lettersNode != null && lettersNode.isArray()) {
            lettersNode.forEach(node -> letters.add(node.asText()));
        }
        return letters;
    }

    private String yesOrNoValue(Map<String, JsonNode> data) {
        JsonNode yesOrNo = data.get("y_or_n");
        return yesOrNo == null || yesOrNo.isNull() ? null : yesOrNo.asText();
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

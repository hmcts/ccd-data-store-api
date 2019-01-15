package uk.gov.hmcts.ccd.domain.service.createevent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;

class MidEventCallbackTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference STRING_JSON_MAP = new TypeReference<HashMap<String, JsonNode>>() {
    };
    private static final String JURISDICTION_ID = "jurisdictionId";
    private static final String CASE_TYPE_ID = "caseTypeId";
    private static final Boolean IGNORE_WARNINGS = Boolean.FALSE;

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
    private CaseEvent caseEvent;
    @Mock
    private CaseType caseType;
    private Event event;
    private Map<String, JsonNode> data = new HashMap<>();
    private CaseDetails caseDetails;
    private WizardPage wizardPageWithCallback;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        event = new Event();
        event.setEventId("createCase");

        given(caseDefinitionRepository.getCaseType(CASE_TYPE_ID)).willReturn(caseType);
        given(eventTriggerService.findCaseEvent(caseType, event.getEventId())).willReturn(caseEvent);
        given(caseType.getJurisdictionId()).willReturn(JURISDICTION_ID);
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

        given(callbackInvoker.invokeMidEventCallback(wizardPageWithCallback,
                                                     caseType,
                                                     caseEvent,
                                                     null,
                                                     caseDetails,
                                                     IGNORE_WARNINGS)).willReturn(caseDetails);

        midEventCallback.invoke(CASE_TYPE_ID,
                                newCaseDataContent().withEvent(event).withData(data).withIgnoreWarning(IGNORE_WARNINGS).build(),
                                "createCase1"
        );

        verify(callbackInvoker).invokeMidEventCallback(wizardPageWithCallback,
                                                       caseType,
                                                       caseEvent,
                                                       null,
                                                       caseDetails,
                                                       IGNORE_WARNINGS);
    }

    @Test
    @DisplayName("should update data from MidEvent callback")
    void shouldUpdateCaseDetailsFromMidEventCallback() throws Exception {

        final Map<String, JsonNode> data = MAPPER.convertValue(MAPPER.readTree(
            "{\n"
                + "  \"PersonFirstName\": \"First Name\",\n"
                + "  \"PersonLastName\": \"Last Name\"\n"
                + "}"), STRING_JSON_MAP);
        CaseDetails updatedCaseDetails = caseDetails(data);
        given(callbackInvoker.invokeMidEventCallback(wizardPageWithCallback,
                                                     caseType,
                                                     caseEvent,
                                                     null,
                                                     caseDetails,
                                                     IGNORE_WARNINGS)).willReturn(updatedCaseDetails);
        given(caseService.createNewCaseDetails(CASE_TYPE_ID, JURISDICTION_ID, data)).willReturn(caseDetails);

        JsonNode result = midEventCallback.invoke(CASE_TYPE_ID,
                                                  newCaseDataContent().withEvent(event).withData(data).withIgnoreWarning(IGNORE_WARNINGS).build(),
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

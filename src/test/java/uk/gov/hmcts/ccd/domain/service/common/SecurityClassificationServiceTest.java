package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PRIVATE;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PUBLIC;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.RESTRICTED;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;

// too many legacy OperatorWrap occurrences on JSON strings so suppress until move to Java12+
@SuppressWarnings("checkstyle:OperatorWrap")
public class SecurityClassificationServiceTest {

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String JURISDICTION_ID = "PROBATE";
    private static final ObjectMapper MAPPER = JacksonUtils.MAPPER;

    private SecurityClassificationServiceImpl securityClassificationService;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private CaseDataAccessControl caseDataAccessControl;

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    @Mock
    private PersistenceStrategyResolver resolver;

    private CaseDetails caseDetails;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        caseDetails = new CaseDetails();
        caseDetails.setJurisdiction(JURISDICTION_ID);

        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        securityClassificationService = spy(new SecurityClassificationServiceImpl(caseDataAccessControl,
            caseDefinitionRepository));
    }

    @Nested
    @DisplayName("Check security classification for a field")
    class CheckSecurityClassificationForField {
        private static final String CASE_TYPE_ONE = "CaseTypeOne";
        private static final String SC_PUBLIC = "PUBLIC";
        private static final String SC_RESTRICTED = "RESTRICTED";
        private static final String CASE_FIELD_ID_1_1 = "CASE_FIELD_1_1";
        private static final String CASE_FIELD_ID_1_2 = "CASE_FIELD_1_2";
        private final CaseFieldDefinition testCaseField11 =
            newCaseField().withId(CASE_FIELD_ID_1_1).withSC(SC_PUBLIC).build();
        private final CaseFieldDefinition testCaseField12 =
            newCaseField().withId(CASE_FIELD_ID_1_2).withSC(SC_RESTRICTED).build();
        private final CaseTypeDefinition testCaseTypeDefinition = newCaseType()
            .withId(CASE_TYPE_ONE)
            .withField(testCaseField11)
            .withField(testCaseField12)
            .build();
    }

    @Nested
    @DisplayName("getUserClassification()")
    class GetUserClassification {

        @Test
        @DisplayName("should retrieve user classifications from user repository")
        public void shouldRetrieveClassificationsFromRepository() {
            when(caseDataAccessControl.getUserClassifications(any(CaseTypeDefinition.class), anyBoolean()))
                .thenReturn(newHashSet(PUBLIC, PRIVATE));
            securityClassificationService.getUserClassification(new CaseTypeDefinition(), true);

            verify(caseDataAccessControl, times(1))
                .getUserClassifications(any(CaseTypeDefinition.class), anyBoolean());
        }

        @Test
        @DisplayName("should keep highest ranked classification")
        public void shouldRetrieveHigherRankedRole() {
            when(caseDataAccessControl.getUserClassifications(any(CaseTypeDefinition.class), anyBoolean()))
                .thenReturn(newHashSet(PUBLIC, PRIVATE));

            Optional<SecurityClassification> userClassification = securityClassificationService
                .getUserClassification(new CaseTypeDefinition(), true);

            assertEquals(PRIVATE,
                userClassification.get(),
                "The user's security classification for jurisdiction is incorrect");
        }

        @Test
        @DisplayName("should retrieve no security classification if empty list of classifications returned by user "
            + "repository")
        public void shouldRetrieveNoSecurityClassificationIfEmptyListOfClassifications() {
            when(caseDataAccessControl.getUserClassifications(any(CaseTypeDefinition.class), anyBoolean()))
                .thenReturn(newHashSet());
            when(caseDataAccessControl.getUserClassifications(any(CaseTypeDefinition.class), anyBoolean()))
                .thenReturn(newHashSet());

            Optional<SecurityClassification> userClassification = securityClassificationService.getUserClassification(
                new CaseTypeDefinition(), true);

            assertFalse(userClassification.isPresent(), "Should not have classification");
        }
    }

    @Nested
    @DisplayName("Apply to CaseDetails")
    class ApplyToCaseDetails {

        private CaseDetails caseDetails;

        @BeforeEach
        void setUp() throws IOException {
            caseDetails = new CaseDetails();
            caseDetails.setJurisdiction(JURISDICTION_ID);
        }

        Optional<CaseDetails> applyClassification(SecurityClassification userClassification,
                                                  SecurityClassification caseClassification) {
            when(caseDataAccessControl.getUserClassifications(any(CaseTypeDefinition.class), anyBoolean()))
                .thenReturn(newHashSet(userClassification));

            when(caseDataAccessControl.getUserClassifications(any(CaseDetails.class)))
                .thenReturn(newHashSet(userClassification));

            caseDetails.setSecurityClassification(caseClassification);

            return securityClassificationService.applyClassification(caseDetails);
        }

        @Test
        @DisplayName("should return null when user has no classification")
        void shouldReturnNullWhenUserNoClassification() {
            assertThat(applyClassification(null, RESTRICTED).isPresent(), is(false));
        }

        @Test
        @DisplayName("should return null when user has lower classification")
        void shouldReturnNullWhenUserLowerClassification() {
            assertThat(applyClassification(PUBLIC, RESTRICTED).isPresent(), is(false));
        }

        @Test
        @DisplayName("should return case when user has same classification")
        void shouldReturnCaseWhenUserSameClassification() {
            assertThat(applyClassification(PRIVATE, PRIVATE).get(), sameInstance(caseDetails));
        }

        @Test
        @DisplayName("should return case when user has higher classification")
        void shouldReturnCaseWhenUserHigherClassification() {
            assertThat(applyClassification(RESTRICTED, PUBLIC).get(), sameInstance(caseDetails));
        }

    }

    @Nested
    @DisplayName("CaseDetails case access categories")
    class ApplyCaseAccessCategoriesToCaseDetails {

        private CaseDetails caseDetails;

        @BeforeEach
        void setUp() throws IOException {
            caseDetails = new CaseDetails();

            final Map<String, JsonNode> data = JacksonUtils.convertValue(MAPPER.readTree(
                "{  \"CaseAccessCategory\": \"Civil/Standard\"\n," +
                    "       \"Note2\": \"note2\"\n" +
                    "    }\n"
            ));
            caseDetails.setData(data);
            caseDetails.setJurisdiction(JURISDICTION_ID);
        }

        Optional<CaseDetails> applyClassification(SecurityClassification userClassification,
                                                  SecurityClassification caseClassification,
                                                  String caseAccessCategories) {
            when(caseDataAccessControl.getUserClassifications(any(CaseTypeDefinition.class), anyBoolean()))
                .thenReturn(newHashSet(userClassification));

            when(caseDataAccessControl.getUserClassifications(any(CaseDetails.class)))
                .thenReturn(newHashSet(userClassification));

            AccessProfile accessProfile = new AccessProfile("Test");
            accessProfile.setCaseAccessCategories(caseAccessCategories);
            when(caseDataAccessControl.generateAccessProfilesByCaseDetails(any(CaseDetails.class)))
                .thenReturn(newHashSet(accessProfile));

            caseDetails.setSecurityClassification(caseClassification);

            return securityClassificationService.applyClassification(caseDetails);
        }

        @Test
        @DisplayName("should return null when case has no matching case access categories")
        void shouldReturnNullWhenCaseHasNoCaseAccessCategories() {
            assertThat(applyClassification(PUBLIC, RESTRICTED, "Civil/Test").isPresent(), is(false));
        }

        @Test
        @DisplayName("should return case details when case and access profiles has matching case access categories")
        void shouldReturnCaseDetailsWhenCaseAndAccessProfileHasSameCategories() {
            assertThat(applyClassification(PUBLIC, RESTRICTED, "Civil/Standard,Crime/Standard").isPresent(), is(false));
        }

    }

    @Nested
    @DisplayName("Apply to List of Events")
    class ApplyToEventList {

        private AuditEvent publicEvent;
        private AuditEvent privateEvent;
        private AuditEvent restrictedEvent;

        @BeforeEach
        void setUp() {
            publicEvent = new AuditEvent();
            publicEvent.setSecurityClassification(PUBLIC);

            privateEvent = new AuditEvent();
            privateEvent.setSecurityClassification(PRIVATE);

            restrictedEvent = new AuditEvent();
            restrictedEvent.setSecurityClassification(RESTRICTED);

            doReturn(Optional.empty()).when(securityClassificationService)
                .getUserClassification(any(CaseTypeDefinition.class), anyBoolean());
        }

        @Test
        @DisplayName("should return empty list when given null")
        void shouldReturnEmptyListInsteadOfNull() {
            final List<AuditEvent> classifiedEvents =
                securityClassificationService.applyClassification(caseDetails, null);

            assertAll(
                () -> assertThat(classifiedEvents, is(notNullValue())),
                () -> assertThat(classifiedEvents, hasSize(0))
            );
        }

        @Test
        @DisplayName("should return all events when user has higher classification")
        void shouldReturnAllEventsWhenUserHigherClassification() {
            when(caseDataAccessControl.getUserClassifications(any(CaseDetails.class)))
                .thenReturn(newHashSet(RESTRICTED));

            final List<AuditEvent> classifiedEvents =
                securityClassificationService.applyClassification(caseDetails,
                                                                Arrays.asList(publicEvent,
                                                                    privateEvent,
                                                                    restrictedEvent));

            assertAll(
                () -> assertThat(classifiedEvents, hasSize(3)),
                () -> assertThat(classifiedEvents, hasItems(publicEvent, privateEvent, restrictedEvent))
            );
        }

        @Test
        @DisplayName("should filter out events with higher classification")
        void shouldFilterOutEventsHigherClassification() {
            when(caseDataAccessControl.getUserClassifications(any(CaseDetails.class)))
                .thenReturn(newHashSet(PUBLIC));

            final List<AuditEvent> classifiedEvents =
                securityClassificationService.applyClassification(caseDetails,
                                                                    Arrays.asList(publicEvent,
                                                                        privateEvent,
                                                                        restrictedEvent));

            assertAll(
                () -> assertThat(classifiedEvents, hasSize(1)),
                () -> assertThat(classifiedEvents, hasItems(publicEvent))
            );
        }

        @Test
        @DisplayName("should return empty list when user has no classification")
        void shouldReturnEmptyListWhenNoUserClassification() {

            final List<AuditEvent> classifiedEvents =
                securityClassificationService.applyClassification(caseDetails,
                                                                    Arrays.asList(publicEvent,
                                                                        privateEvent,
                                                                        restrictedEvent));

            assertThat(classifiedEvents, hasSize(0));
        }
    }

    @Nested
    @DisplayName("getClassificationForEvent()")
    class GetSecurityClassificationForEvent {

        private final CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();

        @BeforeEach
        void setUp() throws IOException {
            CaseEventDefinition createEvent = new CaseEventDefinition();
            createEvent.setId("createEvent");
            createEvent.setSecurityClassification(RESTRICTED);
            CaseEventDefinition updateEvent = new CaseEventDefinition();
            updateEvent.setId("updateEvent");
            updateEvent.setSecurityClassification(PRIVATE);
            List<CaseEventDefinition> events = Arrays.asList(createEvent, updateEvent);
            caseTypeDefinition.setEvents(events);
        }

        @Test
        @DisplayName("should return classification relevant for event")
        void shouldGetClassificationForEvent() {
            CaseEventDefinition eventTrigger = new CaseEventDefinition();
            eventTrigger.setId("createEvent");
            SecurityClassification result = securityClassificationService.getClassificationForEvent(caseTypeDefinition,
                eventTrigger);

            assertThat(result, is(equalTo(RESTRICTED)));
        }

        @Test
        @DisplayName("should fail to return fields when event not found")
        void shouldThrowRuntimeExceptionIfEventNotFound() {
            CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
            caseEventDefinition.setId("unknown");

            assertThrows(RuntimeException.class, () ->
                securityClassificationService.getClassificationForEvent(caseTypeDefinition, caseEventDefinition));
        }
    }
}

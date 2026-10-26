package uk.gov.hmcts.ccd.domain.service.getevents;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.decentralised.client.ServicePersistenceClient;
import uk.gov.hmcts.ccd.decentralised.service.DecentralisedAuditEventLoader;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.ComplexACL;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlServiceImpl;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.domain.service.common.CompoundAccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.PersistenceStrategyResolver;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationServiceImpl;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.CreatorGetCaseOperation;
import uk.gov.hmcts.ccd.v2.external.controller.CaseController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

/**
 * CCD-8136: populated historical snapshots must respect the caller's current field read access.
 * A supplied snapshot proves the shared authorisation gap, not that local list queries load data.
 */
class EventHistoryFieldAccessTest {
    private static final String CASE_REFERENCE = "1504259907353529";
    private static final String CASE_TYPE = "HistoryFieldAccess";
    private static final String READER = "history-reader";
    private static final String STAFF = "internal-staff";
    private static final Set<AccessProfile> PROFILES = Set.of(
        AccessProfile.builder().accessProfile(READER).readOnly(false).build());

    private final CaseDefinitionRepository definitions = mock(CaseDefinitionRepository.class);
    private final CaseAccessService caseAccess = mock(CaseAccessService.class);
    private final GetEventsOperation source = mock(GetEventsOperation.class);
    private final AccessControlServiceImpl accessControl = new AccessControlServiceImpl(
        mock(ApplicationParams.class), new CompoundAccessControlService());
    private CaseTypeDefinition caseType;
    private CaseDetails caseDetails;
    private AuditEvent event;
    private AuthorisedGetEventsOperation authorised;

    @BeforeEach
    void setUp() throws Exception {
        CaseFieldDefinition party = field("party", READER, "Complex");
        party.getFieldTypeDefinition().setComplexFields(List.of(
            field("name", READER, "Text"), field("privateContact", STAFF, "Text")));
        party.setComplexACLs(List.of(childAcl("privateContact")));
        CaseFieldDefinition documents = field("documents", READER, "Collection");
        FieldTypeDefinition document = new FieldTypeDefinition();
        document.setType("Complex");
        document.setComplexFields(List.of(
            field("title", READER, "Text"), field("internalReference", STAFF, "Text")));
        documents.getFieldTypeDefinition().setCollectionFieldTypeDefinition(document);
        documents.setComplexACLs(List.of(childAcl("internalReference")));

        CaseEventDefinition eventDefinition = new CaseEventDefinition();
        eventDefinition.setId("update");
        eventDefinition.setAccessControlLists(List.of(acl(READER)));
        caseType = new CaseTypeDefinition();
        caseType.setId(CASE_TYPE);
        caseType.setAccessControlLists(List.of(acl(READER)));
        caseType.setEvents(List.of(eventDefinition));
        caseType.setCaseFieldDefinitions(List.of(
            field("publicNote", READER, "Text"), field("internalNote", STAFF, "Text"), party, documents));

        caseDetails = new CaseDetails();
        caseDetails.setId("123");
        caseDetails.setReference(Long.valueOf(CASE_REFERENCE));
        caseDetails.setCaseTypeId(CASE_TYPE);
        event = new AuditEvent();
        event.setCaseDataId("123");
        event.setCaseTypeId(CASE_TYPE);
        event.setEventId("update");
        event.setEventName("Case updated");
        event.setSummary("Public event summary");
        event.setSecurityClassification(SecurityClassification.PUBLIC);
        event.setData(JacksonUtils.convertValue(JacksonUtils.MAPPER.readTree("""
            {
              "publicNote": "Visible progress",
              "internalNote": "Confidential staff assessment",
              "party": {"name": "Example person", "privateContact": "private@example.invalid"},
              "documents": [
                {"id": "doc-1", "value": {"title": "Letter", "internalReference": "STAFF-001"}},
                {"id": "doc-2", "value": {"title": "Form", "internalReference": "STAFF-002"}}
              ]
            }
            """)));
        event.setDataClassification(JacksonUtils.convertValue(JacksonUtils.MAPPER.readTree("""
            {"publicNote": "PUBLIC", "internalNote": "PUBLIC"}
            """)));
        when(definitions.getCaseType(CASE_TYPE)).thenReturn(caseType);
        when(caseAccess.getAccessProfilesByCaseReference("123")).thenReturn(PROFILES);
        authorised = new AuthorisedGetEventsOperation(source, definitions, accessControl, caseAccess);
    }

    @Test
    @DisplayName("Control: ordinary field filtering removes the same confidential values")
    void ordinaryFieldReadRemovesDeniedValues() {
        JsonNode filtered = accessControl.filterCaseFieldsByAccess(snapshot().deepCopy(),
            caseType.getCaseFieldDefinitions(), PROFILES, CAN_READ, false);
        assertSecureData(filtered);
        // Recursive filtering must not accidentally sanitise the source used by the history tests.
        assertThat(snapshot().has("internalNote")).isTrue();
        assertThat(snapshot().path("party").has("privateContact")).isTrue();
        assertThat(snapshot().path("documents").get(0).path("value").has("internalReference")).isTrue();
    }

    @Test
    @DisplayName("History must omit a top-level field the caller cannot read")
    void populatedHistoryMustHideDeniedTopLevelFields() {
        when(source.getEvents(CASE_REFERENCE)).thenReturn(List.of(event));
        AuditEvent result = authorised.getEvents(CASE_REFERENCE).get(0);
        assertMetadata(result);
        assertTopLevel(JacksonUtils.convertValueJsonNode(result.getData()));
    }

    @Test
    @DisplayName("History must omit denied classification entries as well as values")
    void populatedHistoryMustHideDeniedClassificationEntries() {
        when(source.getEvents(CASE_REFERENCE)).thenReturn(List.of(event));
        AuditEvent result = authorised.getEvents(CASE_REFERENCE).get(0);
        JsonNode classification = JacksonUtils.convertValueJsonNode(result.getDataClassification());
        assertThat(classification.path("publicNote").asText()).isEqualTo("PUBLIC");
        assertAbsent(classification, "internalNote");
    }

    @Test
    @DisplayName("History must recursively omit denied complex and collection children")
    void populatedHistoryMustHideDeniedChildren() {
        when(source.getEvents(CASE_REFERENCE)).thenReturn(List.of(event));
        AuditEvent result = authorised.getEvents(CASE_REFERENCE).get(0);
        assertMetadata(result);
        assertChildren(JacksonUtils.convertValueJsonNode(result.getData()));
    }

    @Test
    @DisplayName("Single-event authorisation must enforce field read access on snapshots")
    void singleEventMustHideDeniedValues() {
        when(source.getEvent(caseDetails, CASE_TYPE, 42L)).thenReturn(Optional.of(event));
        AuditEvent result = authorised.getEvent(caseDetails, CASE_TYPE, 42L).orElseThrow();
        assertMetadata(result);
        assertAll(
            () -> assertSecureData(JacksonUtils.convertValueJsonNode(result.getData())),
            () -> assertAbsent(JacksonUtils.convertValueJsonNode(result.getDataClassification()), "internalNote"));
    }

    @Test
    @DisplayName("Public response must filter snapshots supplied by decentralised persistence")
    void decentralisedPublicResponseMustHideDeniedValues() {
        ServicePersistenceClient client = mock(ServicePersistenceClient.class);
        PersistenceStrategyResolver resolver = mock(PersistenceStrategyResolver.class);
        CreatorGetCaseOperation getCase = mock(CreatorGetCaseOperation.class);
        UIDService uid = mock(UIDService.class);
        CaseDataAccessControl caseDataAccess = mock(CaseDataAccessControl.class);
        when(uid.validateUID(CASE_REFERENCE)).thenReturn(true);
        when(getCase.execute(CASE_REFERENCE)).thenReturn(Optional.of(caseDetails));
        when(resolver.isDecentralised(caseDetails)).thenReturn(true);
        when(client.getCaseHistory(caseDetails)).thenReturn(List.of(event));
        when(caseDataAccess.getUserClassifications(caseDetails))
            .thenReturn(Set.of(SecurityClassification.PUBLIC));

        GetEventsOperation delegated = new DelegatingGetEventsOperation(resolver,
            new DecentralisedAuditEventLoader(client), mock(LocalAuditEventLoader.class), getCase, uid);
        GetEventsOperation classified = new ClassifiedGetEventsOperation(delegated,
            new SecurityClassificationServiceImpl(caseDataAccess, definitions), getCase);
        GetEventsOperation secured = new AuthorisedGetEventsOperation(
            classified, definitions, accessControl, caseAccess);
        CaseController controller = new CaseController(getCase, null, null, uid, secured, null, null, null, null);

        JsonNode response = JacksonUtils.MAPPER.valueToTree(controller.getCaseEvents(CASE_REFERENCE).getBody());
        verify(client).getCaseHistory(caseDetails);
        JsonNode events = response.path("auditEvents");
        assertThat(events.size()).isEqualTo(1);
        assertThat(events.get(0).path("id").asText()).isEqualTo("update");
        assertThat(events.get(0).path("event_name").asText()).isEqualTo("Case updated");
        assertAll(
            () -> assertSecureData(events.get(0).path("data")),
            () -> assertAbsent(events.get(0).path("data_classification"), "internalNote"));
    }

    @Test
    @DisplayName("Control: unreadable events remain excluded despite having populated snapshots")
    void eventWithoutReadAccessIsExcluded() {
        when(source.getEvents(CASE_REFERENCE)).thenReturn(List.of(event));
        caseType.getEvents().get(0).setAccessControlLists(List.of(acl(STAFF)));
        assertThat(authorised.getEvents(CASE_REFERENCE)).isEmpty();
    }

    @Test
    @DisplayName("Local-style history retains null snapshots and event metadata")
    void historyPreservesNullSnapshots() {
        event.setData(null);
        event.setDataClassification(null);
        when(source.getEvents(CASE_REFERENCE)).thenReturn(List.of(event));

        AuditEvent result = authorised.getEvents(CASE_REFERENCE).get(0);

        assertMetadata(result);
        assertThat(result.getData()).isNull();
        assertThat(result.getDataClassification()).isNull();
    }

    @Test
    @DisplayName("Populated history is filtered even when classification data is absent")
    void historyFiltersDataWithoutClassification() {
        event.setDataClassification(null);
        when(source.getEvents(CASE_REFERENCE)).thenReturn(List.of(event));

        AuditEvent result = authorised.getEvents(CASE_REFERENCE).get(0);

        assertSecureData(JacksonUtils.convertValueJsonNode(result.getData()));
        assertThat(result.getDataClassification()).isNull();
    }

    @Test
    @DisplayName("Historical fields missing from the current definition are omitted")
    void historyOmitsUnknownHistoricalFields() {
        event.getData().put("retiredField", JacksonUtils.MAPPER.getNodeFactory().textNode("Old confidential value"));
        event.getDataClassification().put("retiredField", JacksonUtils.MAPPER.getNodeFactory().textNode("PUBLIC"));
        when(source.getEvents(CASE_REFERENCE)).thenReturn(List.of(event));

        AuditEvent result = authorised.getEvents(CASE_REFERENCE).get(0);

        assertSecureData(JacksonUtils.convertValueJsonNode(result.getData()));
        assertAbsent(JacksonUtils.convertValueJsonNode(result.getData()), "retiredField");
        assertAbsent(JacksonUtils.convertValueJsonNode(result.getDataClassification()), "retiredField");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("History omits unknown children recursively with or without complex ACL overrides")
    void historyOmitsUnknownNestedFields(boolean complexOverrides) throws Exception {
        addHistoricalNestedFields(complexOverrides);
        when(source.getEvents(CASE_REFERENCE)).thenReturn(List.of(event));

        AuditEvent result = authorised.getEvents(CASE_REFERENCE).get(0);

        JsonNode data = JacksonUtils.convertValueJsonNode(result.getData());
        assertSecureData(data);
        assertHistoricalNestedFields(data);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Shared case-read filter omits unknown children recursively without changing ACL semantics")
    void sharedFilterOmitsUnknownNestedFields(boolean complexOverrides) throws Exception {
        addHistoricalNestedFields(complexOverrides);
        JsonNode original = snapshot();

        JsonNode result = accessControl.filterCaseFieldsByAccess(original.deepCopy(),
            caseType.getCaseFieldDefinitions(), PROFILES, CAN_READ, false);

        assertSecureData(result);
        assertHistoricalNestedFields(result);
        assertThat(original.path("historicalDetails").has("retiredSecret")).isTrue();
    }

    @Test
    @DisplayName("Three-argument history returns an empty list when its source returns no events")
    void threeArgumentHistoryHandlesEmptyEvents() {
        when(source.getEvents("jurisdiction", CASE_TYPE, CASE_REFERENCE)).thenReturn(List.of());

        assertThat(authorised.getEvents("jurisdiction", CASE_TYPE, CASE_REFERENCE)).isEmpty();
    }

    @Test
    @DisplayName("Three-argument history returns an empty list when its source returns null")
    void threeArgumentHistoryHandlesNullEvents() {
        when(source.getEvents("jurisdiction", CASE_TYPE, CASE_REFERENCE)).thenReturn(null);

        assertThat(authorised.getEvents("jurisdiction", CASE_TYPE, CASE_REFERENCE)).isEmpty();
    }

    private void addHistoricalNestedFields(boolean complexOverrides) throws Exception {
        CaseFieldDefinition address = field("address", READER, "Complex");
        address.getFieldTypeDefinition().setComplexFields(List.of(field("postcode", READER, "Text")));
        CaseFieldDefinition entries = field("entries", READER, "Collection");
        FieldTypeDefinition entry = new FieldTypeDefinition();
        entry.setType("Complex");
        entry.setComplexFields(List.of(address));
        entries.getFieldTypeDefinition().setCollectionFieldTypeDefinition(entry);
        CaseFieldDefinition details = field("historicalDetails", READER, "Complex");
        details.getFieldTypeDefinition().setComplexFields(List.of(entries));
        if (complexOverrides) {
            ComplexACL override = childAcl("entries");
            override.setAccessProfile(READER);
            details.setComplexACLs(List.of(override));
        }
        List<CaseFieldDefinition> fields = new ArrayList<>(caseType.getCaseFieldDefinitions());
        fields.add(details);
        caseType.setCaseFieldDefinitions(fields);
        event.getData().put("historicalDetails", JacksonUtils.MAPPER.readTree("""
            {
              "retiredSecret": "Old complex secret",
              "entries": [
                {"id": "entry-1", "value": {
                  "retiredSecret": "Old collection secret",
                  "address": {"postcode": "EX1 1AA", "retiredSecret": "Old nested secret"}
                }},
                {"id": "entry-2", "value": {
                  "retiredSecret": "Another old collection secret",
                  "address": {"postcode": "EX2 2BB", "retiredSecret": "Another old nested secret"}
                }}
              ]
            }
            """));
    }

    private void assertHistoricalNestedFields(JsonNode data) {
        JsonNode details = data.path("historicalDetails");
        assertAbsent(details, "retiredSecret");
        JsonNode entries = details.path("entries");
        assertThat(entries.size()).isEqualTo(2);
        for (int i = 0; i < entries.size(); i++) {
            assertThat(entries.get(i).path("id").asText()).isEqualTo("entry-" + (i + 1));
            JsonNode value = entries.get(i).path("value");
            assertAbsent(value, "retiredSecret");
            assertAbsent(value.path("address"), "retiredSecret");
            assertThat(value.path("address").path("postcode").asText())
                .isEqualTo(i == 0 ? "EX1 1AA" : "EX2 2BB");
        }
    }

    private JsonNode snapshot() {
        return JacksonUtils.convertValueJsonNode(event.getData());
    }

    private void assertMetadata(AuditEvent result) {
        assertThat(result.getEventId()).isEqualTo("update");
        assertThat(result.getEventName()).isEqualTo("Case updated");
        assertThat(result.getSummary()).isEqualTo("Public event summary");
    }

    private void assertSecureData(JsonNode data) {
        assertAll(() -> assertTopLevel(data), () -> assertChildren(data));
    }

    private void assertTopLevel(JsonNode data) {
        assertThat(data.path("publicNote").asText()).isEqualTo("Visible progress");
        assertAbsent(data, "internalNote");
    }

    private void assertChildren(JsonNode data) {
        assertThat(data.path("party").path("name").asText()).isEqualTo("Example person");
        JsonNode documents = data.path("documents");
        assertThat(documents.size()).isEqualTo(2);
        assertThat(documents.get(0).path("id").asText()).isEqualTo("doc-1");
        assertThat(documents.get(1).path("id").asText()).isEqualTo("doc-2");
        assertThat(documents.get(0).path("value").path("title").asText()).isEqualTo("Letter");
        assertThat(documents.get(1).path("value").path("title").asText()).isEqualTo("Form");
        assertAll(
            () -> assertAbsent(data.path("party"), "privateContact"),
            () -> assertAbsent(documents.get(0).path("value"), "internalReference"),
            () -> assertAbsent(documents.get(1).path("value"), "internalReference"));
    }

    private void assertAbsent(JsonNode parent, String field) {
        assertThat(parent.has(field))
            .as("Denied field %s disclosed: %s", field, parent.get(field))
            .isFalse();
    }

    private AccessControlList acl(String profile) {
        AccessControlList acl = new AccessControlList();
        acl.setAccessProfile(profile);
        acl.setRead(true);
        return acl;
    }

    private ComplexACL childAcl(String path) {
        ComplexACL acl = new ComplexACL();
        acl.setListElementCode(path);
        acl.setAccessProfile(STAFF);
        acl.setRead(true);
        return acl;
    }

    private CaseFieldDefinition field(String id, String profile, String type) {
        CaseFieldDefinition field = new CaseFieldDefinition();
        field.setId(id);
        field.setAccessControlLists(List.of(acl(profile)));
        FieldTypeDefinition definition = new FieldTypeDefinition();
        definition.setType(type);
        field.setFieldTypeDefinition(definition);
        return field;
    }
}

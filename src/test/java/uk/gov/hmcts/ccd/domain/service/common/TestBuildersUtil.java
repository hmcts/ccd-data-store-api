package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.aggregated.*;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.domain.model.draft.*;
import uk.gov.hmcts.ccd.domain.model.search.Field;
import uk.gov.hmcts.ccd.domain.model.search.SearchInput;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Consumer;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;

public class TestBuildersUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TestBuildersUtil() {}

    public static class CallbackResponseBuilder {
        private final CallbackResponse callbackResponse;

        public CallbackResponseBuilder() {
            callbackResponse = new CallbackResponse();
        }

        public CallbackResponseBuilder withSecurityClassification(SecurityClassification securityClassification) {
            callbackResponse.setSecurityClassification(securityClassification);
            return this;
        }

        public CallbackResponseBuilder withDataClassification(Map<String, JsonNode> dataClassification) {
            callbackResponse.setDataClassification(dataClassification);
            return this;
        }

        public CallbackResponse build() {
            return callbackResponse;
        }

        public static CallbackResponseBuilder aCallbackResponse() {
            return new CallbackResponseBuilder();
        }
    }

    public static class DraftResponseBuilder {
        private final DraftResponse draftResponse;

        private DraftResponseBuilder() {
            draftResponse = new DraftResponse();
        }

        public DraftResponseBuilder withId(String id) {
            this.draftResponse.setId(id);
            return this;
        }

        public DraftResponseBuilder withId(Long id) {
            this.draftResponse.setId(String.valueOf(id));
            return this;
        }

        public DraftResponseBuilder withDocument(CaseDraft document) {
            this.draftResponse.setDocument(document);
            return this;
        }

        public DraftResponseBuilder withType(String type) {
            this.draftResponse.setType(type);
            return this;
        }

        public DraftResponseBuilder withCreated(LocalDateTime created) {
            this.draftResponse.setCreated(created);
            return this;
        }

        public DraftResponseBuilder withUpdated(LocalDateTime updated) {
            this.draftResponse.setUpdated(updated);
            return this;
        }

        public static DraftResponseBuilder newDraftResponse() {
            return new DraftResponseBuilder();
        }

        public DraftResponse build() {
            return this.draftResponse;
        }
    }

    public static class DraftBuilder {
        private final Draft draft;

        private DraftBuilder() {
            draft = new Draft();
        }

        public DraftBuilder withId(String id) {
            this.draft.setId(id);
            return this;
        }

        public DraftBuilder withId(Long id) {
            this.draft.setId(String.valueOf(id));
            return this;
        }

        public DraftBuilder withDocument(JsonNode document) {
            this.draft.setDocument(document);
            return this;
        }

        public DraftBuilder withType(String type) {
            this.draft.setType(type);
            return this;
        }

        public DraftBuilder withCreated(ZonedDateTime created) {
            this.draft.setCreated(created);
            return this;
        }

        public DraftBuilder withUpdated(ZonedDateTime updated) {
            this.draft.setUpdated(updated);
            return this;
        }

        public static DraftBuilder anDraft() {
            return new DraftBuilder();
        }

        public Draft build() {
            return this.draft;
        }
    }

    public static class CaseDataContentBuilder {
        private final CaseDataContent caseDataContent;

        private CaseDataContentBuilder() {
            this.caseDataContent = new CaseDataContent();
        }

        public CaseDataContentBuilder withEvent(Event event) {
            this.caseDataContent.setEvent(event);
            return this;
        }

        public CaseDataContentBuilder withData(Map<String, JsonNode> data) {
            this.caseDataContent.setData(data);
            return this;
        }

        public CaseDataContentBuilder withDraftId(String draftId) {
            this.caseDataContent.setDraftId(draftId);
            return this;
        }

        public CaseDataContentBuilder withSecurityClassification(String securityClassification) {
            this.caseDataContent.setSecurityClassification(securityClassification);
            return this;
        }

        public CaseDataContentBuilder withDataClassification(Map<String, JsonNode> dataClassification) {
            this.caseDataContent.setDataClassification(dataClassification);
            return this;
        }

        public CaseDataContentBuilder withToken(String token) {
            this.caseDataContent.setToken(token);
            return this;
        }

        public CaseDataContentBuilder withIgnoreWarning(Boolean ignoreWarning) {
            this.caseDataContent.setIgnoreWarning(ignoreWarning);
            return this;
        }

        public static CaseDataContentBuilder newCaseDataContent() {
            return new CaseDataContentBuilder();
        }

        public CaseDataContent build() {
            return this.caseDataContent;
        }
    }

    public static class CaseDraftBuilder {
        private final CaseDraft caseDraft;

        private CaseDraftBuilder() {
            this.caseDraft = new CaseDraft();
        }

        public CaseDraftBuilder withUserId(String userId) {
            this.caseDraft.setUserId(userId);
            return this;
        }

        public CaseDraftBuilder withJurisdictionId(String jurisdictionId) {
            this.caseDraft.setJurisdictionId(jurisdictionId);
            return this;
        }

        public CaseDraftBuilder withCaseTypeId(String caseTypeId) {
            this.caseDraft.setCaseTypeId(caseTypeId);
            return this;
        }

        public CaseDraftBuilder withEventTriggerId(String eventTriggerId) {
            this.caseDraft.setEventTriggerId(eventTriggerId);
            return this;
        }

        public CaseDraftBuilder withCaseDataContent(CaseDataContent caseDataContent) {
            this.caseDraft.setCaseDataContent(caseDataContent);
            return this;
        }

        public static CaseDraftBuilder newCaseDraft() {
            return new CaseDraftBuilder();
        }

        public CaseDraft build() {
            return this.caseDraft;
        }
    }

    public static class CreateCaseDraftBuilder {

        private final CreateCaseDraftRequest createCaseDraftRequest;

        private CreateCaseDraftBuilder() {
            this.createCaseDraftRequest = new CreateCaseDraftRequest();
        }

        public CreateCaseDraftBuilder withDocument(CaseDraft document) {
            this.createCaseDraftRequest.setDocument(document);
            return this;
        }

        public CreateCaseDraftBuilder withType(String type) {
            this.createCaseDraftRequest.setType(type);
            return this;
        }

        public CreateCaseDraftBuilder withTTLDays(Integer TTLDays) {
            this.createCaseDraftRequest.setMaxTTLDays(TTLDays);
            return this;
        }

        public static CreateCaseDraftBuilder aCreateCaseDraft() {
            return new CreateCaseDraftBuilder();
        }

        public CreateCaseDraftRequest build() {
            return this.createCaseDraftRequest;
        }
    }

    public static class UpdateCaseDraftBuilder {
        private final UpdateCaseDraftRequest updateCaseDraftRequest;

        private UpdateCaseDraftBuilder() {
            this.updateCaseDraftRequest = new UpdateCaseDraftRequest();
        }

        public UpdateCaseDraftBuilder withDocument(CaseDraft document) {
            this.updateCaseDraftRequest.setDocument(document);
            return this;
        }

        public UpdateCaseDraftBuilder withType(String type) {
            this.updateCaseDraftRequest.setType(type);
            return this;
        }

        public static UpdateCaseDraftBuilder anUpdateCaseDraft() {
            return new UpdateCaseDraftBuilder();
        }

        public UpdateCaseDraftRequest build() {
            return this.updateCaseDraftRequest;
        }
    }

    public static class CaseDetailsBuilder {
        private final CaseDetails caseDetails;

        public CaseDetailsBuilder() {
            caseDetails = new CaseDetails();
        }

        public CaseDetailsBuilder withSecurityClassification(SecurityClassification securityClassification) {
            caseDetails.setSecurityClassification(securityClassification);
            return this;
        }

        public CaseDetailsBuilder withData(Map<String, JsonNode> data) {
            caseDetails.setData(data);
            return this;
        }

        public CaseDetailsBuilder withDataClassification(Map<String, JsonNode> dataClassification) {
            caseDetails.setDataClassification(dataClassification);
            return this;
        }

        public CaseDetailsBuilder withId(String id) {
            caseDetails.setId(id);
            return this;
        }

        public CaseDetailsBuilder withJurisdiction(String jurisdictionId) {
            caseDetails.setJurisdiction(jurisdictionId);
            return this;
        }

        public CaseDetailsBuilder withCaseTypeId(String caseTypeId) {
            caseDetails.setCaseTypeId(caseTypeId);
            return this;
        }

        public CaseDetails build() {
            return caseDetails;
        }

        public static CaseDetailsBuilder newCaseDetails() {
            return new CaseDetailsBuilder();
        }
    }

    public static class DataClassificationBuilder {
        private final Map<String, JsonNode> dataClassification;

        public DataClassificationBuilder() {
            dataClassification = Maps.newHashMap();
        }

        public static DataClassificationBuilder aClassificationBuilder() {
            return new DataClassificationBuilder();
        }

        public DataClassificationBuilder withData(String key, JsonNode value) {
            dataClassification.put(key, value);
            return this;
        }

        public DataClassificationBuilder withData(String key, List value) {
            dataClassification.put(key, MAPPER.convertValue(value, JsonNode.class));
            return this;
        }

        public Map<String, JsonNode> buildAsMap() {
            return dataClassification;
        }

        public JsonNode buildAsNode() {
            return MAPPER.convertValue(dataClassification, JsonNode.class);
        }
    }

    public static class CaseTypeBuilder {
        private final CaseType caseType;
        private final List<AccessControlList> acls = newArrayList();

        private CaseTypeBuilder() {
            this.caseType = new CaseType();
            caseType.setJurisdiction(new Jurisdiction());
        }

        public static CaseTypeBuilder newCaseType() {
            return new CaseTypeBuilder();
        }

        public CaseTypeBuilder withId(String id) {
            caseType.setId(id);
            return this;

        }

        public CaseTypeBuilder withJurisdiction(Jurisdiction jurisdiction) {
            caseType.setJurisdiction(jurisdiction);
            return this;
        }

        public CaseTypeBuilder withCaseTypeId(String caseTypeId) {
            caseType.setId(caseTypeId);
            return this;
        }

        public CaseTypeBuilder withEvent(CaseEvent event) {
            caseType.getEvents().add(event);
            return this;
        }

        public CaseTypeBuilder withState(CaseState state) {
            caseType.getStates().add(state);
            return this;
        }

        public CaseTypeBuilder withField(CaseField field) {
            caseType.getCaseFields().add(field);
            return this;
        }

        public CaseTypeBuilder withAcl(AccessControlList accessControlList) {
            this.acls.add(accessControlList);
            return this;
        }

        public CaseType build() {
            caseType.setAccessControlLists(this.acls);
            return caseType;
        }

        public CaseTypeBuilder withSecurityClassification(SecurityClassification securityClassification) {
            caseType.setSecurityClassification(securityClassification);
            return this;
        }
    }

    public static class CaseViewBuilder {
        private final CaseView caseView;
        private final List<CaseViewTrigger> caseViewTriggers = newArrayList();

        private CaseViewBuilder() {
            this.caseView = new CaseView();
        }

        public static CaseViewBuilder aCaseView() {
            return new CaseViewBuilder();
        }

        public CaseViewBuilder withCaseViewTrigger(CaseViewTrigger caseViewTrigger) {
            this.caseViewTriggers.add(caseViewTrigger);
            return this;
        }

        public CaseViewBuilder withState(ProfileCaseState state) {
            caseView.setState(state);
            return this;
        }

        public CaseView build() {
            caseView.setTriggers(caseViewTriggers.toArray(new CaseViewTrigger[]{}));
            return caseView;
        }
    }

    public static class CaseStateBuilder {
        private final CaseState caseState;
        private final List<AccessControlList> acls = newArrayList();

        private CaseStateBuilder() {
            this.caseState = new CaseState();
        }

        public static CaseStateBuilder newState() {
            return new CaseStateBuilder();
        }

        public CaseStateBuilder withAcl(AccessControlList accessControlList) {
            this.acls.add(accessControlList);
            return this;
        }

        public CaseStateBuilder withId(String id) {
            caseState.setId(id);
            return this;
        }

        public CaseState build() {
            caseState.setAccessControlLists(this.acls);
            return caseState;
        }
    }

    public static class AccessControlListBuilder {
        private final AccessControlList accessControlList;

        private AccessControlListBuilder() {
            this.accessControlList = new AccessControlList();
        }

        public static AccessControlListBuilder anAcl() {
            return new AccessControlListBuilder();
        }

        public AccessControlListBuilder withRole(String role) {
            this.accessControlList.setRole(role);
            return this;
        }

        public AccessControlListBuilder withCreate(boolean create) {
            this.accessControlList.setCreate(create);
            return this;
        }

        public AccessControlListBuilder withUpdate(boolean update) {
            this.accessControlList.setUpdate(update);
            return this;
        }

        public AccessControlListBuilder withRead(boolean read) {
            this.accessControlList.setRead(read);
            return this;
        }

        public AccessControlList build() {
            return accessControlList;
        }
    }

    public static class CaseEventBuilder {
        private final CaseEvent caseEvent;
        private final List<AccessControlList> accessControlLists = newArrayList();

        private CaseEventBuilder() {
            this.caseEvent = new CaseEvent();
        }

        public static CaseEventBuilder anCaseEvent() {
            return new CaseEventBuilder();
        }

        public CaseEventBuilder withAcl(AccessControlList accessControlList) {
            accessControlLists.add(accessControlList);
            return this;
        }

        public CaseEventBuilder withId(String id) {
            caseEvent.setId(id);
            return this;
        }

        public CaseEvent build() {
            caseEvent.setAccessControlLists(accessControlLists);
            return caseEvent;
        }

        public CaseEventBuilder withCanSaveDraft(Boolean canSaveDraft) {
            caseEvent.setCanSaveDraft(canSaveDraft);
            return this;
        }

        public CaseEventBuilder withName(String name) {
            caseEvent.setName(name);
            return this;
        }
    }

    public static class CaseViewTriggerBuilder {
        private final CaseViewTrigger caseViewTrigger;

        private CaseViewTriggerBuilder() {
            this.caseViewTrigger = new CaseViewTrigger();
        }

        public static CaseViewTriggerBuilder aViewTrigger() {
            return new CaseViewTriggerBuilder();
        }

        public CaseViewTriggerBuilder withId(String id) {
            caseViewTrigger.setId(id);
            return this;
        }

        public CaseViewTrigger build() {
            return caseViewTrigger;
        }
    }

    public static class CaseEventTriggerBuilder {
        private final CaseEventTrigger caseEventTrigger;
        private final List<CaseViewField> caseFields = Lists.newArrayList();
        private final List<WizardPage> wizardPages = Lists.newArrayList();

        private CaseEventTriggerBuilder() {
            this.caseEventTrigger = new CaseEventTrigger();
        }

        public static CaseEventTriggerBuilder anEventTrigger() {
            return new CaseEventTriggerBuilder();
        }

        public CaseEventTriggerBuilder withId(String id) {
            caseEventTrigger.setId(id);
            return this;
        }

        public CaseEventTriggerBuilder withWizardPage(WizardPage wizardPage) {
            this.wizardPages.add(wizardPage);
            return this;
        }

        public CaseEventTriggerBuilder withField(CaseViewField caseField) {
            caseFields.add(caseField);
            return this;
        }

        public CaseEventTrigger build() {
            this.caseEventTrigger.setCaseFields(this.caseFields);
            caseEventTrigger.setWizardPages(this.wizardPages);
            return caseEventTrigger;
        }
    }

    public static class WizardPageBuilder {
        private final WizardPage wizardPage;
        private final List<WizardPageField> wizardPageFields = Lists.newArrayList();

        private WizardPageBuilder() {
            this.wizardPage = new WizardPage();
        }

        static WizardPageBuilder aWizardPage() {
            return new WizardPageBuilder();
        }

        public WizardPageBuilder withId(String id) {
            wizardPage.setId(id);
            return this;
        }

        public WizardPageBuilder withField(CaseViewField caseField) {
            WizardPageField wizardPageField = new WizardPageField();
            wizardPageField.setCaseFieldId(caseField.getId());
            wizardPageField.setPageColumnNumber(1);
            wizardPageField.setOrder(1);
            wizardPageFields.add(wizardPageField);
            return this;
        }

        public WizardPage build() {
            this.wizardPage.setWizardPageFields(this.wizardPageFields);
            return wizardPage;
        }
    }

    public static class CaseFieldBuilder {
        private final CaseField caseField;
        private final List<AccessControlList> accessControlLists = newArrayList();
        private FieldType caseFieldType;

        private CaseFieldBuilder() {
            this.caseField = new CaseField();
        }

        public static CaseFieldBuilder aCaseField() {
            return new CaseFieldBuilder();
        }

        public CaseFieldBuilder withId(String id) {
            caseField.setId(id);
            return this;
        }

        public CaseFieldBuilder withSC(String securityClassification) {
            caseField.setSecurityLabel(securityClassification);
            return this;
        }

        public CaseFieldBuilder withFieldType(FieldType fieldType) {
            caseFieldType = fieldType;
            return this;
        }

        public CaseFieldBuilder withAcl(AccessControlList accessControlList) {
            accessControlLists.add(accessControlList);
            return this;
        }

        public CaseField build() {
            caseField.setAccessControlLists(accessControlLists);
            caseField.setFieldType(caseFieldType);
            return caseField;
        }
    }

    public static class FieldTypeBuilder {
        private final FieldType fieldType;
        private final List<CaseField> complexFields;

        private FieldTypeBuilder() {
            this.fieldType = new FieldType();
            this.complexFields = Lists.newArrayList();
        }

        public static FieldTypeBuilder aFieldType() {
            return new FieldTypeBuilder();
        }

        public FieldTypeBuilder withId(String id) {
            fieldType.setId(id);
            return this;
        }

        public FieldTypeBuilder withType(String type) {
            fieldType.setType(type);
            return this;
        }

        public FieldTypeBuilder withComplexField(CaseField complexField) {
            complexFields.add(complexField);
            return this;
        }

        public FieldTypeBuilder withCollectionFieldType(FieldType collectionFieldType) {
            fieldType.setCollectionFieldType(collectionFieldType);
            return this;
        }

        public FieldTypeBuilder withCollectionField(CaseField complexField) {
            complexFields.add(complexField);
            return this;
        }

        public FieldType build() {
            fieldType.setComplexFields(complexFields);
            return fieldType;
        }
    }

    public static class CaseViewFieldBuilder {
        private final CaseViewField caseViewField;

        private CaseViewFieldBuilder() {
            this.caseViewField = new CaseViewField();
        }

        public static CaseViewFieldBuilder aViewField() {
            return new CaseViewFieldBuilder();
        }

        public CaseViewFieldBuilder withId(String id) {
            caseViewField.setId(id);
            return this;
        }

        public CaseViewField build() {
            return caseViewField;
        }
    }

    public static class AuditEventBuilder {
        private final AuditEvent auditEvent;

        private AuditEventBuilder() {
            this.auditEvent = new AuditEvent();
        }

        public static AuditEventBuilder anAuditEvent() {
            return new AuditEventBuilder();
        }

        public AuditEventBuilder withEventId(String id) {
            auditEvent.setEventId(id);
            return this;
        }

        public AuditEvent build() {
            return auditEvent;
        }
    }

    public static class JurisdictionBuilder {
        private Jurisdiction jurisdiction;

        public static JurisdictionBuilder newJurisdiction() {
            return new JurisdictionBuilder();
        }
        private JurisdictionBuilder() {
            this.jurisdiction = new Jurisdiction();
        }

        public JurisdictionBuilder withJurisdictionId(String id) {
            jurisdiction.setId(id);
            return this;
        }

        public JurisdictionBuilder withName(String name) {
            jurisdiction.setName(name);
            return this;
        }

        public JurisdictionBuilder withDescription(String description) {
            jurisdiction.setDescription(description);
            return this;
        }

        public JurisdictionBuilder withCaseType(CaseType caseType) {
            jurisdiction.getCaseTypes().add(caseType);
            return this;
        }

        public Jurisdiction build() {
            return jurisdiction;
        }
    }


    public static class WorkbasketInputBuilder {
        private final WorkbasketInput workbasketInput;

        private WorkbasketInputBuilder() {
            this.workbasketInput = new WorkbasketInput();
        }

        public static WorkbasketInputBuilder aWorkbasketInput() {
            return new WorkbasketInputBuilder();
        }

        public WorkbasketInputBuilder withFieldId(String fieldId) {
            Field f = new Field();
            f.setId(fieldId);
            this.workbasketInput.setField(f);
            return this;
        }

        public WorkbasketInput build() {
            return this.workbasketInput;
        }
    }

    public static class SearchInputBuilder {
        private final SearchInput searchInput;

        private SearchInputBuilder() {
            this.searchInput = new SearchInput();
        }

        public static SearchInputBuilder aSearchInput() {
            return new SearchInputBuilder();
        }

        public SearchInputBuilder withFieldId(String fieldId) {
            Field f = new Field();
            f.setId(fieldId);
            this.searchInput.setField(f);
            return this;
        }

        public SearchInput build() {
            return this.searchInput;
        }
    }

    public static class CaseHistoryViewBuilder {
        private final CaseHistoryView caseHistoryView;

        private CaseHistoryViewBuilder() {
            this.caseHistoryView = new CaseHistoryView();
        }

        public static CaseHistoryViewBuilder aCaseHistoryView() {
            return new CaseHistoryViewBuilder();
        }

        public CaseHistoryViewBuilder withEvent(CaseViewEvent caseViewEvent) {
            this.caseHistoryView.setEvent(caseViewEvent);
            return this;
        }

        public CaseHistoryView build() {
            return caseHistoryView;
        }
    }

    public static class CaseViewEventBuilder {
        private final CaseViewEvent caseViewEvent;

        private CaseViewEventBuilder() {
            this.caseViewEvent = new CaseViewEvent();
        }

        public static CaseViewEventBuilder aCaseViewEvent() {
            return new CaseViewEventBuilder();
        }

        public CaseViewEventBuilder withId(String eventId) {
            this.caseViewEvent.setEventId(eventId);
            return this;
        }

        public CaseViewEvent build() {
            return caseViewEvent;
        }
    }

    public static class CaseTabCollectionBuilder {
        private final CaseTabCollection caseTabCollection;

        private CaseTabCollectionBuilder() {
            this.caseTabCollection = new CaseTabCollection();
        }

        public static CaseTabCollectionBuilder newCaseTabCollection() {
            return new CaseTabCollectionBuilder();
        }

        public CaseTabCollectionBuilder withFieldIds(String... caseFieldIds) {
            CaseTypeTab tab = new CaseTypeTab();
            List<CaseTypeTabField> tabFields = new ArrayList<>();
            asList(caseFieldIds).forEach(caseFieldId -> {
                CaseField caseField = new CaseField();
                caseField.setId(caseFieldId);
                FieldType fieldType = new FieldType();
                fieldType.setType("YesOrNo");
                caseField.setFieldType(fieldType);
                CaseTypeTabField tabField = new CaseTypeTabField();
                tabField.setCaseField(caseField);
                tabField.setShowCondition(caseFieldId + "-fieldShowCondition");
                tabFields.add(tabField);
            });
            tab.setShowCondition("tabShowCondition");
            tab.setTabFields(tabFields);
            List<CaseTypeTab> tabs = new ArrayList<>();
            tabs.add(tab);
            caseTabCollection.setTabs(tabs);

            return this;
        }

        public CaseTabCollection build() {
            return caseTabCollection;
        }
    }

    public static class CaseDataBuilder {

        private final HashMap<String, JsonNode> caseData;

        private CaseDataBuilder() {
            caseData = new HashMap<>();
        }

        private Consumer<JsonNode> putFn(String fieldId) {
            return (JsonNode node) -> caseData.put(fieldId, node);
        }

        public static CaseDataBuilder newCaseData() {
            return new CaseDataBuilder();
        }

        public CaseDataBuilder withPair(String key, JsonNode value) {
            caseData.put(key, value);
            return this;
        }

        public CaseDataFieldBuilder withField(String fieldId) {
            return new CaseDataFieldBuilder(this, putFn(fieldId));
        }

        public Map<String, JsonNode> build() {
            return caseData;
        }
    }

    public static class CaseDataFieldBuilder {
        private final CaseDataBuilder caseDataBuilder;
        private final Consumer<JsonNode> putFn;

        CaseDataFieldBuilder(CaseDataBuilder caseDataBuilder, Consumer<JsonNode> putFn) {
            this.caseDataBuilder = caseDataBuilder;
            this.putFn = putFn;
        }

        public CaseDataBuilder asCollectionOf(JsonNode... nodes) {
            final ArrayNode collection = JsonNodeFactory.instance.arrayNode();
            Arrays.stream(nodes).forEach(collection::add);
            putFn.accept(collection);
            return caseDataBuilder;
        }
    }

    public static JsonNode collectionItem(String id, String value) {
        return collectionItem(id, JsonNodeFactory.instance.textNode(value));
    }

    public static JsonNode collectionItem(String id, JsonNode value) {
        final ObjectNode item = JsonNodeFactory.instance.objectNode();
        item.put("id", id);
        item.set("value", value);
        return item;
    }

    public static class CaseDataClassificationBuilder {

        private final HashMap<String, JsonNode> dataClassification;

        private CaseDataClassificationBuilder() {
            dataClassification = new HashMap<>();
        }

        private Consumer<JsonNode> putFn(String fieldId) {
            return (JsonNode node) -> dataClassification.put(fieldId, node);
        }

        public static CaseDataClassificationBuilder dataClassification() {
            return new CaseDataClassificationBuilder();
        }

        public CaseDataClassificationFieldBuilder withField(String fieldId) {
            return new CaseDataClassificationFieldBuilder(this, putFn(fieldId));
        }

        public Map<String, JsonNode> build() {
            return dataClassification;
        }
    }

    public static class CaseDataClassificationFieldBuilder {
        private final CaseDataClassificationBuilder caseDataClassificationBuilder;
        private final Consumer<JsonNode> putFn;

        CaseDataClassificationFieldBuilder(CaseDataClassificationBuilder caseDataClassificationBuilder,
                                           Consumer<JsonNode> putFn) {
            this.caseDataClassificationBuilder = caseDataClassificationBuilder;
            this.putFn = putFn;
        }

        public CaseDataClassificationBuilder asCollectionOf(String classification, JsonNode... nodes) {
            final ObjectNode collection = JsonNodeFactory.instance.objectNode();
            final ArrayNode collectionValue = JsonNodeFactory.instance.arrayNode();
            Arrays.stream(nodes).forEach(collectionValue::add);

            collection.put("classification", classification);
            collection.set("value", collectionValue);

            putFn.accept(collection);
            return caseDataClassificationBuilder;
        }
    }

    public static JsonNode collectionClassification(String id, String classification) {
        return collectionItem(id, JsonNodeFactory.instance.textNode(classification));
    }

    public static JsonNode collectionClassification(String id, JsonNode classification) {
        final ObjectNode item = JsonNodeFactory.instance.objectNode();
        item.put("id", id);
        item.set("classification", classification);
        return item;
    }

    public static class UserRoleBuilder {
        private final UserRole userRole;

        private UserRoleBuilder() {
            this.userRole = new UserRole();
        }

        public static UserRoleBuilder aUserRole() {
            return new UserRoleBuilder();
        }

        public UserRoleBuilder withRole(String role) {
            userRole.setRole(role);
            return this;
        }

        public UserRoleBuilder withSecurityClassification(SecurityClassification securityClassification) {
            userRole.setSecurityClassification(securityClassification.name());
            return this;
        }

        public UserRole build() {
            return this.userRole;
        }
    }
}

package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseHistoryView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseUpdateViewEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewActionableEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewTab;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewType;
import uk.gov.hmcts.ccd.domain.model.aggregated.DefaultSettings;
import uk.gov.hmcts.ccd.domain.model.aggregated.JurisdictionDisplayProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.ProfileCaseState;
import uk.gov.hmcts.ccd.domain.model.aggregated.User;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserProfile;
import uk.gov.hmcts.ccd.domain.model.aggregated.WorkbasketDefault;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventResult;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.Banner;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldComplexDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeTabDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeTabField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeTabsDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.ComplexACL;
import uk.gov.hmcts.ccd.domain.model.definition.DisplayContext;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FixedListItemDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionUiConfigDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.UserRole;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPageComplexFieldOverride;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPageField;
import uk.gov.hmcts.ccd.domain.model.draft.CaseDraft;
import uk.gov.hmcts.ccd.domain.model.draft.CreateCaseDraftRequest;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.model.draft.UpdateCaseDraftRequest;
import uk.gov.hmcts.ccd.domain.model.search.Field;
import uk.gov.hmcts.ccd.domain.model.search.SearchInput;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;

@SuppressWarnings("checkstyle:MethodName") // method naming predates checkstyle implementation in module
public class TestBuildersUtil {

    private TestBuildersUtil() {
    }

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

        public CaseDataContentBuilder withEventData(Map<String, JsonNode> eventData) {
            this.caseDataContent.setEventData(eventData);
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

        public CaseDataContentBuilder withCaseReference(String caseReference) {
            this.caseDataContent.setCaseReference(caseReference);
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

        public CaseDraftBuilder withEventId(String eventId) {
            this.caseDraft.setEventId(eventId);
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

        public CreateCaseDraftBuilder withTTLDays(Integer ttlDays) {
            this.createCaseDraftRequest.setMaxTTLDays(ttlDays);
            return this;
        }

        public static CreateCaseDraftBuilder newCreateCaseDraft() {
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

        public static UpdateCaseDraftBuilder newUpdateCaseDraft() {
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

        public CaseDetailsBuilder withReference(Long reference) {
            caseDetails.setReference(reference);
            return this;
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
            dataClassification.put(key, JacksonUtils.convertValueJsonNode(value));
            return this;
        }

        public Map<String, JsonNode> buildAsMap() {
            return dataClassification;
        }

        public JsonNode buildAsNode() {
            return JacksonUtils.convertValueJsonNode(dataClassification);
        }
    }

    public static class CaseTypeBuilder {
        private final CaseTypeDefinition caseTypeDefinition;
        private final List<AccessControlList> acls = newArrayList();

        private CaseTypeBuilder() {
            this.caseTypeDefinition = new CaseTypeDefinition();
            caseTypeDefinition.setJurisdictionDefinition(new JurisdictionDefinition());
        }

        public static CaseTypeBuilder newCaseType() {
            return new CaseTypeBuilder();
        }

        public CaseTypeBuilder withId(String id) {
            caseTypeDefinition.setId(id);
            return this;

        }

        public CaseTypeBuilder withJurisdiction(JurisdictionDefinition jurisdictionDefinition) {
            caseTypeDefinition.setJurisdictionDefinition(jurisdictionDefinition);
            return this;
        }

        public CaseTypeBuilder withCaseTypeId(String caseTypeId) {
            caseTypeDefinition.setId(caseTypeId);
            return this;
        }

        public CaseTypeBuilder withEvent(CaseEventDefinition event) {
            caseTypeDefinition.getEvents().add(event);
            return this;
        }

        public CaseTypeBuilder withEvents(List<CaseEventDefinition> event) {
            caseTypeDefinition.getEvents().addAll(event);
            return this;
        }

        public CaseTypeBuilder withCaseFields(List<CaseFieldDefinition> fields) {
            caseTypeDefinition.getCaseFieldDefinitions().addAll(fields);
            return this;
        }

        public CaseTypeBuilder withState(CaseStateDefinition state) {
            caseTypeDefinition.getStates().add(state);
            return this;
        }

        public CaseTypeBuilder withField(CaseFieldDefinition field) {
            caseTypeDefinition.getCaseFieldDefinitions().add(field);
            return this;
        }

        public CaseTypeBuilder withAcl(AccessControlList accessControlList) {
            this.acls.add(accessControlList);
            return this;
        }

        public CaseTypeDefinition build() {
            caseTypeDefinition.setAccessControlLists(this.acls);
            return caseTypeDefinition;
        }

        public CaseTypeBuilder withSecurityClassification(SecurityClassification securityClassification) {
            caseTypeDefinition.setSecurityClassification(securityClassification);
            return this;
        }
    }

    public static class BannerBuilder {

        private final Banner banner;

        private BannerBuilder() {
            this.banner = new Banner();
            this.banner.setJurisdictionDefinition(new JurisdictionDefinition());
        }

        public static BannerBuilder newBanner() {
            return new BannerBuilder();
        }

        public BannerBuilder withBannerEnabled(Boolean bannerEnabled) {
            this.banner.setBannerEnabled(bannerEnabled);
            return this;

        }

        public BannerBuilder withBannerDescription(String bannerDescription) {
            this.banner.setBannerDescription(bannerDescription);
            return this;
        }

        public BannerBuilder withBannerUrl(String bannerUrl) {
            banner.setBannerUrl(bannerUrl);
            return this;
        }

        public BannerBuilder withBannerUrlText(String bannerUrlText) {
            banner.setBannerUrlText(bannerUrlText);
            return this;
        }

        public Banner build() {
            return banner;
        }
    }

    public static class JurisdictionUiConfigBuilder {

        private final JurisdictionUiConfigDefinition jurisdictionUiConfigDefinition;

        private JurisdictionUiConfigBuilder() {
            this.jurisdictionUiConfigDefinition = new JurisdictionUiConfigDefinition();
        }

        public static JurisdictionUiConfigBuilder newJurisdictionUiConfig() {
            return new JurisdictionUiConfigBuilder();
        }

        public JurisdictionUiConfigBuilder withShutteredEnabled(Boolean shuttered) {
            this.jurisdictionUiConfigDefinition.setShuttered(shuttered);
            return this;
        }

        public JurisdictionUiConfigBuilder withId(String id) {
            this.jurisdictionUiConfigDefinition.setId(id);
            return this;
        }

        public JurisdictionUiConfigBuilder withName(String name) {
            this.jurisdictionUiConfigDefinition.setName(name);
            return this;
        }

        public JurisdictionUiConfigDefinition build() {
            return jurisdictionUiConfigDefinition;
        }
    }

    public static class CaseViewBuilder {
        private final CaseView caseView;
        private final List<CaseViewActionableEvent> caseViewActionableEvents = newArrayList();

        private CaseViewBuilder() {
            this.caseView = new CaseView();
            this.caseView.setTabs(new CaseViewTab[0]);
        }

        public static CaseViewBuilder aCaseView() {
            return new CaseViewBuilder();
        }

        public CaseViewBuilder withCaseViewActionableEvent(CaseViewActionableEvent caseViewActionableEvent) {
            this.caseViewActionableEvents.add(caseViewActionableEvent);
            return this;
        }

        public CaseViewBuilder withCaseId(String caseId) {
            this.caseView.setCaseId(caseId);
            return this;
        }

        public CaseViewBuilder withCaseViewType(CaseViewType caseType) {
            this.caseView.setCaseType(caseType);
            return this;
        }

        public CaseViewBuilder withState(ProfileCaseState state) {
            caseView.setState(state);
            return this;
        }

        public CaseViewBuilder addCaseViewTab(CaseViewTab caseViewTab) {
            CaseViewTab[] newTabs = new CaseViewTab[caseView.getTabs().length + 1];
            System.arraycopy(caseView.getTabs(), 0, newTabs, 0, caseView.getTabs().length);
            newTabs[newTabs.length - 1] = caseViewTab;
            caseView.setTabs(newTabs);
            return this;
        }

        public CaseView build() {
            caseView.setActionableEvents(caseViewActionableEvents.toArray(new CaseViewActionableEvent[]{}));
            return caseView;
        }
    }

    public static class CaseViewTabBuilder {
        private final CaseViewTab caseViewTab;

        private CaseViewTabBuilder() {
            this.caseViewTab = new CaseViewTab();
            caseViewTab.setFields(new CaseViewField[0]);
        }

        public static CaseViewTabBuilder newCaseViewTab() {
            return new CaseViewTabBuilder();
        }

        public CaseViewTabBuilder withId(String id) {
            caseViewTab.setId(id);
            return this;
        }

        public CaseViewTabBuilder withRole(String role) {
            caseViewTab.setRole(role);
            return this;
        }

        public CaseViewTabBuilder addCaseViewField(CaseViewField caseViewField) {
            CaseViewField[] newFields = new CaseViewField[caseViewTab.getFields().length + 1];
            System.arraycopy(caseViewTab.getFields(), 0, newFields, 0, caseViewTab.getFields().length);
            newFields[newFields.length - 1] = caseViewField;
            caseViewTab.setFields(newFields);
            return this;
        }

        public CaseViewTab build() {
            return caseViewTab;
        }
    }

    public static class CaseStateBuilder {
        private final CaseStateDefinition caseStateDefinition;
        private final List<AccessControlList> acls = newArrayList();

        private CaseStateBuilder() {
            this.caseStateDefinition = new CaseStateDefinition();
        }

        public static CaseStateBuilder newState() {
            return new CaseStateBuilder();
        }

        public CaseStateBuilder withAcl(AccessControlList accessControlList) {
            this.acls.add(accessControlList);
            return this;
        }

        public CaseStateBuilder withId(String id) {
            caseStateDefinition.setId(id);
            return this;
        }

        public CaseStateDefinition build() {
            caseStateDefinition.setAccessControlLists(this.acls);
            return caseStateDefinition;
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

        public AccessControlListBuilder withDelete(boolean delete) {
            this.accessControlList.setDelete(delete);
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

    public static class ComplexACLBuilder {
        private final ComplexACL complexACL;

        private ComplexACLBuilder() {
            this.complexACL = new ComplexACL();
        }

        public static ComplexACLBuilder aComplexACL() {
            return new ComplexACLBuilder();
        }

        public ComplexACLBuilder withListElementCode(String code) {
            this.complexACL.setListElementCode(code);
            return this;
        }

        public ComplexACLBuilder withRole(String role) {
            this.complexACL.setRole(role);
            return this;
        }

        public ComplexACLBuilder withCreate(boolean create) {
            this.complexACL.setCreate(create);
            return this;
        }

        public ComplexACLBuilder withDelete(boolean delete) {
            this.complexACL.setDelete(delete);
            return this;
        }

        public ComplexACLBuilder withUpdate(boolean update) {
            this.complexACL.setUpdate(update);
            return this;
        }

        public ComplexACLBuilder withRead(boolean read) {
            this.complexACL.setRead(read);
            return this;
        }

        public ComplexACL build() {
            return complexACL;
        }

    }

    public static class CaseEventBuilder {
        private final CaseEventDefinition caseEventDefinition;
        private final List<AccessControlList> accessControlLists = newArrayList();

        private CaseEventBuilder() {
            this.caseEventDefinition = new CaseEventDefinition();
        }

        public static CaseEventBuilder newCaseEvent() {
            return new CaseEventBuilder();
        }

        public CaseEventBuilder withAcl(AccessControlList accessControlList) {
            accessControlLists.add(accessControlList);
            return this;
        }

        public CaseEventBuilder withCaseFields(List<CaseEventFieldDefinition> caseEventFieldDefinitions) {
            caseEventDefinition.setCaseFields(caseEventFieldDefinitions);
            return this;
        }

        public CaseEventBuilder withId(String id) {
            caseEventDefinition.setId(id);
            return this;
        }

        public CaseEventDefinition build() {
            caseEventDefinition.setAccessControlLists(accessControlLists);
            return caseEventDefinition;
        }

        public CaseEventBuilder withCanSaveDraft(Boolean canSaveDraft) {
            caseEventDefinition.setCanSaveDraft(canSaveDraft);
            return this;
        }

        public CaseEventBuilder withName(String name) {
            caseEventDefinition.setName(name);
            return this;
        }

        public CaseEventBuilder withDescription(String description) {
            caseEventDefinition.setDescription(description);
            return this;
        }

        public CaseEventBuilder withShowSummary(Boolean showSummary) {
            caseEventDefinition.setShowSummary(showSummary);
            return this;
        }

        public CaseEventBuilder withShowEventNotes(Boolean showEventNotes) {
            caseEventDefinition.setShowEventNotes(showEventNotes);
            return this;
        }
    }

    public static class CaseEventFieldDefinitionBuilder {
        private final CaseEventFieldDefinition caseField;
        private final List<CaseEventFieldComplexDefinition> complexFieldDefinitions = new ArrayList<>();

        private CaseEventFieldDefinitionBuilder() {
            this.caseField = new CaseEventFieldDefinition();
        }

        public static CaseEventFieldDefinitionBuilder newCaseEventField() {
            return new CaseEventFieldDefinitionBuilder();
        }

        public CaseEventFieldDefinitionBuilder withCaseFieldId(String caseFieldId) {
            caseField.setCaseFieldId(caseFieldId);
            return this;
        }

        public CaseEventFieldDefinitionBuilder addCaseEventFieldComplexDefinitions(
            CaseEventFieldComplexDefinition complexFieldDefinition) {
            complexFieldDefinitions.add(complexFieldDefinition);
            return this;
        }

        public CaseEventFieldDefinitionBuilder withDisplayContext(DisplayContext displayContext) {
            caseField.setDisplayContext(displayContext.toString());
            return this;
        }

        public CaseEventFieldDefinitionBuilder withPublish(boolean publish) {
            caseField.setPublish(publish);
            return this;
        }

        public CaseEventFieldDefinitionBuilder withPublishAs(String publishAs) {
            caseField.setPublishAs(publishAs);
            return this;
        }

        public CaseEventFieldDefinition build() {
            caseField.setCaseEventFieldComplexDefinitions(complexFieldDefinitions);
            return caseField;
        }
    }

    public static class EventBuilder {
        private final Event event;

        private EventBuilder() {
            this.event = new Event();
        }

        public static EventBuilder newEvent() {
            return new EventBuilder();
        }

        public EventBuilder withEventId(String eventId) {
            event.setEventId(eventId);
            return this;
        }

        public Event build() {
            return event;
        }

        public EventBuilder withSummary(String summary) {
            event.setSummary(summary);
            return this;
        }

        public EventBuilder withDescription(String description) {
            event.setDescription(description);
            return this;
        }
    }

    public static class CaseViewActionableEventBuilder {
        private final CaseViewActionableEvent caseViewActionableEvent;

        private CaseViewActionableEventBuilder() {
            this.caseViewActionableEvent = new CaseViewActionableEvent();
        }

        public static CaseViewActionableEventBuilder aViewTrigger() {
            return new CaseViewActionableEventBuilder();
        }

        public CaseViewActionableEventBuilder withId(String id) {
            caseViewActionableEvent.setId(id);
            return this;
        }

        public CaseViewActionableEvent build() {
            return caseViewActionableEvent;
        }
    }

    public static class CaseUpdateViewEventBuilder {
        private final CaseUpdateViewEvent caseUpdateViewEvent;
        private final List<CaseViewField> caseFields = Lists.newArrayList();
        private final List<WizardPage> wizardPages = Lists.newArrayList();

        private CaseUpdateViewEventBuilder() {
            this.caseUpdateViewEvent = new CaseUpdateViewEvent();
        }

        public static CaseUpdateViewEventBuilder newCaseUpdateViewEvent() {
            return new CaseUpdateViewEventBuilder();
        }

        public CaseUpdateViewEventBuilder withId(String id) {
            caseUpdateViewEvent.setId(id);
            return this;
        }

        public CaseUpdateViewEventBuilder withWizardPage(WizardPage wizardPage) {
            this.wizardPages.add(wizardPage);
            return this;
        }

        public CaseUpdateViewEventBuilder withCaseId(String caseId) {
            this.caseUpdateViewEvent.setCaseId(caseId);
            return this;
        }

        public CaseUpdateViewEventBuilder withField(CaseViewField caseField) {
            caseFields.add(caseField);
            return this;
        }

        public CaseUpdateViewEventBuilder withName(String name) {
            this.caseUpdateViewEvent.setName(name);
            return this;
        }

        public CaseUpdateViewEventBuilder withDescription(String description) {
            this.caseUpdateViewEvent.setDescription(description);
            return this;
        }

        public CaseUpdateViewEventBuilder withEventToken(String token) {
            this.caseUpdateViewEvent.setEventToken(token);
            return this;
        }

        public CaseUpdateViewEventBuilder withShowSummary(Boolean isShowSummary) {
            this.caseUpdateViewEvent.setShowSummary(isShowSummary);
            return this;
        }

        public CaseUpdateViewEventBuilder withShowEventNotes(Boolean isShowEventNotes) {
            this.caseUpdateViewEvent.setShowEventNotes(isShowEventNotes);
            return this;
        }

        public CaseUpdateViewEventBuilder withEndButtonLabel(String endButtonLabel) {
            this.caseUpdateViewEvent.setEndButtonLabel(endButtonLabel);
            return this;
        }

        public CaseUpdateViewEventBuilder withCanSaveDraft(boolean isSaveDraft) {
            this.caseUpdateViewEvent.setCanSaveDraft(isSaveDraft);
            return this;
        }

        public CaseUpdateViewEvent build() {
            this.caseUpdateViewEvent.setCaseFields(this.caseFields);
            caseUpdateViewEvent.setWizardPages(this.wizardPages);
            return caseUpdateViewEvent;
        }
    }

    public static class WizardPageComplexFieldOverrideBuilder {
        private final WizardPageComplexFieldOverride wizardPageComplexFieldOverride;

        private WizardPageComplexFieldOverrideBuilder() {
            this.wizardPageComplexFieldOverride = new WizardPageComplexFieldOverride();
        }

        public static WizardPageComplexFieldOverrideBuilder newWizardPageComplexFieldOverride() {
            return new WizardPageComplexFieldOverrideBuilder();
        }

        public WizardPageComplexFieldOverrideBuilder withComplexFieldId(String complexFieldId) {
            this.wizardPageComplexFieldOverride.setComplexFieldElementId(complexFieldId);
            return this;
        }

        public WizardPageComplexFieldOverrideBuilder withDisplayContext(String displayContext) {
            this.wizardPageComplexFieldOverride.setDisplayContext(displayContext);
            return this;
        }

        public WizardPageComplexFieldOverrideBuilder withLabel(String label) {
            this.wizardPageComplexFieldOverride.setLabel(label);
            return this;
        }

        public WizardPageComplexFieldOverrideBuilder withHintText(String hintText) {
            this.wizardPageComplexFieldOverride.setHintText(hintText);
            return this;
        }

        public WizardPageComplexFieldOverrideBuilder withShowCondition(String showCondition) {
            this.wizardPageComplexFieldOverride.setShowCondition(showCondition);
            return this;
        }

        public WizardPageComplexFieldOverrideBuilder withRetainHiddenvalue(Boolean retainHiddenvalue) {
            this.wizardPageComplexFieldOverride.setRetainHiddenValue(retainHiddenvalue);
            return this;
        }


        public WizardPageComplexFieldOverride build() {
            return this.wizardPageComplexFieldOverride;
        }
    }

    public static class StartEventResultBuilder {
        private final StartEventResult startEventResult;

        private StartEventResultBuilder() {
            this.startEventResult = new StartEventResult();
        }

        public static StartEventResultBuilder newStartEventTrigger() {
            return new StartEventResultBuilder();
        }

        public StartEventResultBuilder withCaseDetails(CaseDetails caseDetails) {
            this.startEventResult.setCaseDetails(caseDetails);
            return this;
        }

        public StartEventResultBuilder withEventToken(String token) {
            this.startEventResult.setToken(token);
            return this;
        }

        public StartEventResult build() {
            return startEventResult;
        }
    }

    public static class WizardPageBuilder {
        private final WizardPage wizardPage;
        private final List<WizardPageField> wizardPageFields = Lists.newArrayList();

        private WizardPageBuilder() {
            this.wizardPage = new WizardPage();
        }

        public static WizardPageBuilder newWizardPage() {
            return new WizardPageBuilder();
        }

        public WizardPageBuilder withId(String id) {
            wizardPage.setId(id);
            return this;
        }

        public WizardPageBuilder withOrder(Integer order) {
            wizardPage.setOrder(order);
            return this;
        }

        public WizardPageBuilder withCallBackURLMidEvent(String callBackURLMidEvent) {
            wizardPage.setCallBackURLMidEvent(callBackURLMidEvent);
            return this;
        }

        public WizardPageBuilder withField(CaseViewField caseField) {
            WizardPageField wizardPageField = new WizardPageField();
            wizardPageField.setCaseFieldId(caseField.getId());
            wizardPageField.setPageColumnNumber(1);
            wizardPageField.setOrder(caseField.getOrder() != null ? caseField.getOrder() : 1);
            wizardPageField.setComplexFieldOverrides(emptyList());
            wizardPageFields.add(wizardPageField);
            return this;
        }

        public WizardPageBuilder withField(CaseViewField caseField,
                                           List<WizardPageComplexFieldOverride> complexFieldOverrides) {
            WizardPageField wizardPageField = new WizardPageField();
            wizardPageField.setCaseFieldId(caseField.getId());
            wizardPageField.setPageColumnNumber(1);
            wizardPageField.setOrder(1);
            wizardPageField.setComplexFieldOverrides(complexFieldOverrides);
            wizardPageFields.add(wizardPageField);
            return this;
        }

        public WizardPage build() {
            this.wizardPage.setWizardPageFields(this.wizardPageFields);
            return wizardPage;
        }
    }

    public static class CaseFieldBuilder {
        private final CaseFieldDefinition caseFieldDefinition;
        private final List<AccessControlList> accessControlLists = newArrayList();
        private final List<ComplexACL> complexACLs = newArrayList();
        private FieldTypeDefinition caseFieldTypeDefinition;

        private CaseFieldBuilder() {
            this.caseFieldDefinition = new CaseFieldDefinition();
        }

        public static CaseFieldBuilder newCaseField() {
            return new CaseFieldBuilder();
        }

        public CaseFieldBuilder withId(String id) {
            caseFieldDefinition.setId(id);
            return this;
        }

        public CaseFieldBuilder withSC(String securityClassification) {
            caseFieldDefinition.setSecurityLabel(securityClassification);
            return this;
        }

        public CaseFieldBuilder withFieldType(FieldTypeDefinition fieldTypeDefinition) {
            caseFieldTypeDefinition = fieldTypeDefinition;
            return this;
        }

        public CaseFieldBuilder withFieldLabelText(String label) {
            caseFieldDefinition.setLabel(label);
            return this;
        }

        public CaseFieldBuilder withAcl(AccessControlList accessControlList) {
            accessControlLists.add(accessControlList);
            return this;
        }

        public CaseFieldBuilder withComplexACL(ComplexACL complexACL) {
            complexACLs.add(complexACL);
            return this;
        }

        public CaseFieldBuilder withOrder(final int order) {
            caseFieldDefinition.setOrder(order);
            return this;
        }

        public CaseFieldBuilder withDisplayContextParameter(final String displayContextParameter) {
            caseFieldDefinition.setDisplayContextParameter(displayContextParameter);
            return this;
        }

        public CaseFieldBuilder withMetadata(final boolean asMetadata) {
            caseFieldDefinition.setMetadata(asMetadata);
            return this;
        }

        public CaseFieldBuilder withCaseTypeId(final String caseTypeId) {
            caseFieldDefinition.setCaseTypeId(caseTypeId);
            return this;
        }

        public CaseFieldDefinition build() {
            caseFieldDefinition.setAccessControlLists(accessControlLists);
            caseFieldDefinition.setComplexACLs(complexACLs);
            caseFieldDefinition.setFieldTypeDefinition(caseFieldTypeDefinition);
            return caseFieldDefinition;
        }
    }

    public static class FixedListItemBuilder {
        private final FixedListItemDefinition fixedListItemDefinition;

        public FixedListItemBuilder() {
            this.fixedListItemDefinition = new FixedListItemDefinition();
        }

        public static FixedListItemBuilder aFixedListItem() {
            return new FixedListItemBuilder();
        }

        public FixedListItemBuilder withCode(String code) {
            this.fixedListItemDefinition.setCode(code);
            return this;
        }

        public FixedListItemBuilder withOrder(String order) {
            this.fixedListItemDefinition.setOrder(order);
            return this;
        }

        public FixedListItemDefinition build() {
            return fixedListItemDefinition;
        }
    }

    public static class FieldTypeBuilder {
        private final FieldTypeDefinition fieldTypeDefinition;
        private final List<CaseFieldDefinition> complexFields;

        private FieldTypeBuilder() {
            this.fieldTypeDefinition = new FieldTypeDefinition();
            this.complexFields = Lists.newArrayList();
        }

        public static FieldTypeBuilder aFieldType() {
            return new FieldTypeBuilder();
        }

        public FieldTypeBuilder withId(String id) {
            fieldTypeDefinition.setId(id);
            return this;
        }

        public FieldTypeBuilder withType(String type) {
            fieldTypeDefinition.setType(type);
            return this;
        }

        public FieldTypeBuilder withComplexField(CaseFieldDefinition complexField) {
            complexFields.add(complexField);
            return this;
        }

        public FieldTypeBuilder withCollectionFieldType(FieldTypeDefinition collectionFieldTypeDefinition) {
            fieldTypeDefinition.setCollectionFieldTypeDefinition(collectionFieldTypeDefinition);
            return this;
        }

        public FieldTypeBuilder withCollectionField(CaseFieldDefinition complexField) {
            fieldTypeDefinition.setCollectionFieldTypeDefinition(aFieldType()
                .withComplexField(complexField)
                .withType(COMPLEX)
                .build());
            return this;
        }

        public FieldTypeBuilder withFixedListItems(final FixedListItemDefinition... fixedListItemDefinitions) {
            fieldTypeDefinition.setFixedListItemDefinitions(Lists.newArrayList(fixedListItemDefinitions));
            return this;
        }

        public FieldTypeBuilder withFixedListItems(final List<FixedListItemDefinition> fixedListItemDefinitions) {
            fieldTypeDefinition.setFixedListItemDefinitions(fixedListItemDefinitions);
            return this;
        }

        public FieldTypeDefinition build() {
            fieldTypeDefinition.setComplexFields(complexFields);
            return fieldTypeDefinition;
        }
    }

    public static class CaseViewFieldBuilder {
        private final CaseViewField caseViewField;
        private final List<AccessControlList> acls = newArrayList();
        private FieldTypeDefinition caseFieldTypeDefinition;

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

        public CaseViewFieldBuilder withValue(Object value) {
            caseViewField.setValue(value);
            return this;
        }

        public CaseViewFieldBuilder withOrder(Integer order) {
            caseViewField.setOrder(order);
            return this;
        }

        public CaseViewFieldBuilder withFieldType(FieldTypeDefinition fieldTypeDefinition) {
            this.caseFieldTypeDefinition = fieldTypeDefinition;
            return this;
        }

        public CaseViewFieldBuilder withACL(AccessControlList acl) {
            acls.add(acl);
            return this;
        }

        public CaseViewField build() {
            this.caseViewField.setAccessControlLists(acls);
            this.caseViewField.setFieldTypeDefinition(this.caseFieldTypeDefinition);
            return this.caseViewField;
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
        private final JurisdictionDefinition jurisdictionDefinition;

        public static JurisdictionBuilder newJurisdiction() {
            return new JurisdictionBuilder();
        }

        private JurisdictionBuilder() {
            this.jurisdictionDefinition = new JurisdictionDefinition();
        }

        public JurisdictionBuilder withJurisdictionId(String id) {
            jurisdictionDefinition.setId(id);
            return this;
        }

        public JurisdictionBuilder withName(String name) {
            jurisdictionDefinition.setName(name);
            return this;
        }

        public JurisdictionBuilder withDescription(String description) {
            jurisdictionDefinition.setDescription(description);
            return this;
        }

        public JurisdictionBuilder withCaseType(CaseTypeDefinition caseTypeDefinition) {
            jurisdictionDefinition.getCaseTypeDefinitions().add(caseTypeDefinition);
            return this;
        }

        public JurisdictionDefinition build() {
            return jurisdictionDefinition;
        }
    }


    public static class WorkbasketInputBuilder {
        private final WorkbasketInput workbasketInput;
        private final Field field;

        private WorkbasketInputBuilder() {
            this.workbasketInput = new WorkbasketInput();
            this.field = new Field();
        }

        public static WorkbasketInputBuilder aWorkbasketInput() {
            return new WorkbasketInputBuilder();
        }

        public WorkbasketInputBuilder withFieldId(String fieldId) {
            field.setId(fieldId);
            this.workbasketInput.setField(field);
            return this;
        }

        public WorkbasketInputBuilder withFieldId(String fieldId, String elementPath) {
            field.setId(fieldId);
            field.setElementPath(elementPath);
            this.workbasketInput.setField(field);
            return this;
        }

        public WorkbasketInputBuilder withShowCondition(String showCondition) {
            field.setShowCondition(showCondition);
            this.workbasketInput.setField(field);
            return this;
        }

        public WorkbasketInputBuilder withUserRole(String role) {
            this.workbasketInput.setRole(role);
            return this;
        }

        public WorkbasketInputBuilder withDisplayContextParameter(String displayContextParameter) {
            this.workbasketInput.setDisplayContextParameter(displayContextParameter);
            return this;
        }

        public WorkbasketInput build() {
            return this.workbasketInput;
        }
    }

    public static class SearchInputBuilder {
        private final SearchInput searchInput;
        private final Field field;

        private SearchInputBuilder() {
            this.searchInput = new SearchInput();
            this.field = new Field();
        }

        public static SearchInputBuilder aSearchInput() {
            return new SearchInputBuilder();
        }

        public SearchInputBuilder withUserRole(String role) {
            this.searchInput.setRole(role);
            return this;
        }

        public SearchInputBuilder withFieldId(String fieldId) {
            field.setId(fieldId);
            this.searchInput.setField(field);
            return this;
        }

        public SearchInputBuilder withFieldId(String fieldId, String elementPath) {
            field.setId(fieldId);
            field.setElementPath(elementPath);
            this.searchInput.setField(field);
            return this;
        }

        public SearchInputBuilder withShowCondition(String showCondition) {
            field.setShowCondition(showCondition);
            this.searchInput.setField(field);
            return this;
        }

        public SearchInputBuilder withDisplayContextParameter(String displayContextParameter) {
            this.searchInput.setDisplayContextParameter(displayContextParameter);
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
            this.caseHistoryView.setTabs(new CaseViewTab[0]);
        }

        public static CaseHistoryViewBuilder aCaseHistoryView() {
            return new CaseHistoryViewBuilder();
        }

        public CaseHistoryViewBuilder withEvent(CaseViewEvent caseViewEvent) {
            this.caseHistoryView.setEvent(caseViewEvent);
            return this;
        }

        public CaseHistoryViewBuilder addCaseHistoryViewTab(CaseViewTab caseViewTab) {
            CaseViewTab[] newTabs = new CaseViewTab[caseHistoryView.getTabs().length + 1];
            System.arraycopy(caseHistoryView.getTabs(), 0, newTabs, 0, caseHistoryView.getTabs().length);
            newTabs[newTabs.length - 1] = caseViewTab;
            caseHistoryView.setTabs(newTabs);
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

    public static class CaseTypeTabFieldBuilder {
        private final CaseTypeTabField caseTypeTabField;

        private CaseTypeTabFieldBuilder() {
            this.caseTypeTabField = new CaseTypeTabField();
        }

        public CaseTypeTabFieldBuilder withCaseField(CaseFieldDefinition caseFieldDefinition) {
            this.caseTypeTabField.setCaseFieldDefinition(caseFieldDefinition);
            return this;
        }

        public CaseTypeTabField build() {
            return this.caseTypeTabField;
        }

        public static CaseTypeTabFieldBuilder newCaseTabField() {
            return new CaseTypeTabFieldBuilder();
        }

        public CaseTypeTabFieldBuilder withDisplayContextParameter(final String displayContextParameter) {
            caseTypeTabField.setDisplayContextParameter(displayContextParameter);
            return this;
        }

    }

    public static class CaseTypeTabBuilder {
        private final CaseTypeTabDefinition caseTypeTabDefinition;
        private final List<CaseTypeTabField> caseTypeTabFields;

        private CaseTypeTabBuilder() {
            this.caseTypeTabFields = newArrayList();
            this.caseTypeTabDefinition = new CaseTypeTabDefinition();
            this.caseTypeTabDefinition.setTabFields(caseTypeTabFields);
        }

        public CaseTypeTabBuilder withTabField(CaseTypeTabField field) {
            this.caseTypeTabFields.add(field);
            return this;
        }

        public CaseTypeTabDefinition build() {
            return this.caseTypeTabDefinition;
        }

        public static CaseTypeTabBuilder newCaseTab() {
            return new CaseTypeTabBuilder();
        }

    }

    public static class CaseTabCollectionBuilder {
        private final List<CaseTypeTabDefinition> tabs;
        private final CaseTypeTabsDefinition caseTypeTabsDefinition;

        private CaseTabCollectionBuilder() {
            this.tabs = newArrayList();
            this.caseTypeTabsDefinition = new CaseTypeTabsDefinition();
            this.caseTypeTabsDefinition.setTabs(tabs);
        }

        public static CaseTabCollectionBuilder newCaseTabCollection() {
            return new CaseTabCollectionBuilder();
        }

        public CaseTabCollectionBuilder withFieldIds(String... caseFieldIds) {
            CaseTypeTabDefinition tab = new CaseTypeTabDefinition();
            List<CaseTypeTabField> tabFields = new ArrayList<>();
            asList(caseFieldIds).forEach(caseFieldId -> {
                CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();
                caseFieldDefinition.setId(caseFieldId);
                FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
                fieldTypeDefinition.setType("YesOrNo");
                caseFieldDefinition.setFieldTypeDefinition(fieldTypeDefinition);
                CaseTypeTabField tabField = new CaseTypeTabField();
                tabField.setCaseFieldDefinition(caseFieldDefinition);
                tabField.setShowCondition(caseFieldId + "-fieldShowCondition");
                tabField.setDisplayContextParameter("#TABLE(Title, FirstName, MiddleName)");
                tabFields.add(tabField);
            });
            tab.setShowCondition("tabShowCondition");
            tab.setTabFields(tabFields);
            List<CaseTypeTabDefinition> tabs = new ArrayList<>();
            tabs.add(tab);
            caseTypeTabsDefinition.setTabs(tabs);

            return this;
        }

        public CaseTabCollectionBuilder withTab(CaseTypeTabDefinition tab) {
            tabs.add(tab);
            return this;
        }

        public CaseTypeTabsDefinition build() {
            return caseTypeTabsDefinition;
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

    public static class WorkbasketDefaultBuilder {
        private final WorkbasketDefault workbasketDefault;

        private WorkbasketDefaultBuilder() {
            this.workbasketDefault = new WorkbasketDefault();
        }

        public static WorkbasketDefaultBuilder newWorkbasketDefault() {
            return new WorkbasketDefaultBuilder();
        }

        public WorkbasketDefaultBuilder withCaseTypeId(String caseTypeId) {
            this.workbasketDefault.setCaseTypeId(caseTypeId);
            return this;
        }

        public WorkbasketDefaultBuilder withJurisdictionId(String jurisdictionId) {
            this.workbasketDefault.setJurisdictionId(jurisdictionId);
            return this;
        }

        public WorkbasketDefaultBuilder withStateId(String stateId) {
            this.workbasketDefault.setStateId(stateId);
            return this;
        }

        public WorkbasketDefault build() {
            return this.workbasketDefault;
        }
    }

    public static class DefaultSettingsBuilder {
        private final DefaultSettings defaultSettings;

        private DefaultSettingsBuilder() {
            this.defaultSettings = new DefaultSettings();
        }

        public static DefaultSettingsBuilder newDefaultSettings() {
            return new DefaultSettingsBuilder();
        }

        public DefaultSettingsBuilder withWorkbasketDefault(WorkbasketDefault workbasketDefault) {
            defaultSettings.setWorkbasketDefault(workbasketDefault);
            return this;
        }

        public DefaultSettings build() {
            return this.defaultSettings;
        }
    }

    public static class UserProfileBuilder {
        private final UserProfile userProfile;

        private UserProfileBuilder() {
            this.userProfile = new UserProfile();
        }

        public static UserProfileBuilder newUserProfile() {
            return new UserProfileBuilder();
        }

        public UserProfileBuilder withJurisdictionDisplayProperties(JurisdictionDisplayProperties[]
                                                                            jurisdictionDisplayProperties) {
            userProfile.setJurisdictions(jurisdictionDisplayProperties);
            return this;
        }

        public UserProfileBuilder withUser(User user) {
            userProfile.setUser(user);
            return this;
        }

        public UserProfileBuilder withChannels(String[] channels) {
            userProfile.setChannels(channels);
            return this;
        }

        public UserProfileBuilder withDefaultSettings(DefaultSettings defaultSettings) {
            userProfile.setDefaultSettings(defaultSettings);
            return this;
        }

        public UserProfile build() {
            return this.userProfile;
        }
    }
}

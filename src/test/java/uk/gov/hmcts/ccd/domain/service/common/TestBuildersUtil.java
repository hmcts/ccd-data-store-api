package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseEventTrigger;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseHistoryView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewTab;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewTrigger;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewType;
import uk.gov.hmcts.ccd.domain.model.aggregated.DefaultSettings;
import uk.gov.hmcts.ccd.domain.model.aggregated.JurisdictionDisplayProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.ProfileCaseState;
import uk.gov.hmcts.ccd.domain.model.aggregated.User;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserProfile;
import uk.gov.hmcts.ccd.domain.model.aggregated.WorkbasketDefault;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.Banner;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseState;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTabCollection;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeTab;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeTabField;
import uk.gov.hmcts.ccd.domain.model.definition.ComplexACL;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.domain.model.definition.FixedListItem;
import uk.gov.hmcts.ccd.domain.model.definition.Jurisdiction;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionUiConfig;
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
import static uk.gov.hmcts.ccd.domain.model.definition.FieldType.COMPLEX;

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

        public CaseTypeBuilder withEvents(List<CaseEvent> event) {
            caseType.getEvents().addAll(event);
            return this;
        }

        public CaseTypeBuilder withCaseFields(List<CaseField> fields) {
            caseType.getCaseFields().addAll(fields);
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

    public static class BannerBuilder {

        private final Banner banner;

        private BannerBuilder() {
            this.banner = new Banner();
            this.banner.setJurisdiction(new Jurisdiction());
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

        private final JurisdictionUiConfig jurisdictionUiConfig;

        private JurisdictionUiConfigBuilder() {
            this.jurisdictionUiConfig = new JurisdictionUiConfig();
        }

        public static JurisdictionUiConfigBuilder newJurisdictionUiConfig() {
            return new JurisdictionUiConfigBuilder();
        }

        public JurisdictionUiConfigBuilder withShutteredEnabled(Boolean shuttered) {
            this.jurisdictionUiConfig.setShuttered(shuttered);
            return this;
        }

        public JurisdictionUiConfigBuilder withId(String id) {
            this.jurisdictionUiConfig.setId(id);
            return this;
        }

        public JurisdictionUiConfigBuilder withName(String name) {
            this.jurisdictionUiConfig.setName(name);
            return this;
        }

        public JurisdictionUiConfig build() {
            return jurisdictionUiConfig;
        }
    }

    public static class CaseViewBuilder {
        private final CaseView caseView;
        private final List<CaseViewTrigger> caseViewTriggers = newArrayList();

        private CaseViewBuilder() {
            this.caseView = new CaseView();
            this.caseView.setTabs(new CaseViewTab[0]);
        }

        public static CaseViewBuilder aCaseView() {
            return new CaseViewBuilder();
        }

        public CaseViewBuilder withCaseViewTrigger(CaseViewTrigger caseViewTrigger) {
            this.caseViewTriggers.add(caseViewTrigger);
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
            caseView.setTriggers(caseViewTriggers.toArray(new CaseViewTrigger[]{}));
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
        private final CaseEvent caseEvent;
        private final List<AccessControlList> accessControlLists = newArrayList();

        private CaseEventBuilder() {
            this.caseEvent = new CaseEvent();
        }

        public static CaseEventBuilder newCaseEvent() {
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

        public CaseEventBuilder withDescription(String description) {
            caseEvent.setDescription(description);
            return this;
        }

        public CaseEventBuilder withShowSummary(Boolean showSummary) {
            caseEvent.setShowSummary(showSummary);
            return this;
        }

        public CaseEventBuilder withShowEventNotes(Boolean showEventNotes) {
            caseEvent.setShowEventNotes(showEventNotes);
            return this;
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

        public static CaseEventTriggerBuilder newCaseEventTrigger() {
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

        public CaseEventTriggerBuilder withCaseId(String caseId) {
            this.caseEventTrigger.setCaseId(caseId);
            return this;
        }

        public CaseEventTriggerBuilder withField(CaseViewField caseField) {
            caseFields.add(caseField);
            return this;
        }

        public CaseEventTriggerBuilder withName(String name) {
            this.caseEventTrigger.setName(name);
            return this;
        }

        public CaseEventTriggerBuilder withDescription(String description) {
            this.caseEventTrigger.setDescription(description);
            return this;
        }

        public CaseEventTriggerBuilder withEventToken(String token) {
            this.caseEventTrigger.setEventToken(token);
            return this;
        }

        public CaseEventTriggerBuilder withShowSummary(Boolean isShowSummary) {
            this.caseEventTrigger.setShowSummary(isShowSummary);
            return this;
        }

        public CaseEventTriggerBuilder withShowEventNotes(Boolean isShowEventNotes) {
            this.caseEventTrigger.setShowEventNotes(isShowEventNotes);
            return this;
        }

        public CaseEventTriggerBuilder withEndButtonLabel(String endButtonLabel) {
            this.caseEventTrigger.setEndButtonLabel(endButtonLabel);
            return this;
        }

        public CaseEventTriggerBuilder withCanSaveDraft(boolean isSaveDraft) {
            this.caseEventTrigger.setCanSaveDraft(isSaveDraft);
            return this;
        }

        public CaseEventTrigger build() {
            this.caseEventTrigger.setCaseFields(this.caseFields);
            caseEventTrigger.setWizardPages(this.wizardPages);
            return caseEventTrigger;
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

        public WizardPageComplexFieldOverride build() {
            return this.wizardPageComplexFieldOverride;
        }
    }

    public static class StartEventTriggerBuilder {
        private final StartEventTrigger startEventTrigger;

        private StartEventTriggerBuilder() {
            this.startEventTrigger = new StartEventTrigger();
        }

        public static StartEventTriggerBuilder newStartEventTrigger() {
            return new StartEventTriggerBuilder();
        }

        public StartEventTriggerBuilder withCaseDetails(CaseDetails caseDetails) {
            this.startEventTrigger.setCaseDetails(caseDetails);
            return this;
        }

        public StartEventTriggerBuilder withEventToken(String token) {
            this.startEventTrigger.setToken(token);
            return this;
        }

        public StartEventTrigger build() {
            return startEventTrigger;
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

        public WizardPageBuilder withField(CaseViewField caseField) {
            WizardPageField wizardPageField = new WizardPageField();
            wizardPageField.setCaseFieldId(caseField.getId());
            wizardPageField.setPageColumnNumber(1);
            wizardPageField.setOrder(1);
            wizardPageField.setComplexFieldOverrides(emptyList());
            wizardPageFields.add(wizardPageField);
            return this;
        }

        public WizardPageBuilder withField(CaseViewField caseField, List<WizardPageComplexFieldOverride> complexFieldOverrides) {
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
        private final CaseField caseField;
        private final List<AccessControlList> accessControlLists = newArrayList();
        private final List<ComplexACL> complexACLs = newArrayList();
        private FieldType caseFieldType;

        private CaseFieldBuilder() {
            this.caseField = new CaseField();
        }

        public static CaseFieldBuilder newCaseField() {
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

        public CaseFieldBuilder withFieldLabelText(String label) {
            caseField.setLabel(label);
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
            caseField.setOrder(order);
            return this;
        }

        public CaseFieldBuilder withDisplayContextParameter(final String displayContextParameter) {
            caseField.setDisplayContextParameter(displayContextParameter);
            return this;
        }

        public CaseFieldBuilder withFormattedValue(final String formattedValue) {
            caseField.setFormattedValue(formattedValue);
            return this;
        }

        public CaseField build() {
            caseField.setAccessControlLists(accessControlLists);
            caseField.setComplexACLs(complexACLs);
            caseField.setFieldType(caseFieldType);
            return caseField;
        }
    }

    public static class FixedListItemBuilder {
        private final FixedListItem fixedListItem;

        public FixedListItemBuilder() {
            this.fixedListItem = new FixedListItem();
        }

        public static FixedListItemBuilder aFixedListItem() {
            return new FixedListItemBuilder();
        }

        public FixedListItemBuilder withCode(String code) {
            this.fixedListItem.setCode(code);
            return this;
        }

        public FixedListItemBuilder withOrder(String order) {
            this.fixedListItem.setOrder(order);
            return this;
        }

        public FixedListItem build() {
            return fixedListItem;
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
            fieldType.setCollectionFieldType(aFieldType()
                .withComplexField(complexField)
                .withType(COMPLEX)
                .build());
            return this;
        }

        public FieldTypeBuilder withFixedListItems(final FixedListItem... fixedListItems) {
            fieldType.setFixedListItems(Lists.newArrayList(fixedListItems));
            return this;
        }

        public FieldTypeBuilder withFixedListItems(final List<FixedListItem> fixedListItems) {
            fieldType.setFixedListItems(fixedListItems);
            return this;
        }

        public FieldType build() {
            fieldType.setComplexFields(complexFields);
            return fieldType;
        }
    }

    public static class CaseViewFieldBuilder {
        private final CaseViewField caseViewField;
        private final List<AccessControlList> acls = newArrayList();
        private FieldType caseFieldType;

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

        public CaseViewFieldBuilder withFieldType(FieldType fieldType) {
            this.caseFieldType = fieldType;
            return this;
        }

        public CaseViewFieldBuilder withACL(AccessControlList acl) {
            acls.add(acl);
            return this;
        }

        public CaseViewField build() {
            this.caseViewField.setAccessControlLists(acls);
            this.caseViewField.setFieldType(this.caseFieldType);
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
        private final Jurisdiction jurisdiction;

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

        public WorkbasketInputBuilder withShowCondition(String showCondition) {
            field.setShowCondition(showCondition);
            this.workbasketInput.setField(field);
            return this;
        }

        public WorkbasketInputBuilder withFieldId(String fieldId, String elementPath) {
            field.setId(fieldId);
            field.setElementPath(elementPath);
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

        public SearchInputBuilder withShowCondition(String showCondition) {
            field.setShowCondition(showCondition);
            this.searchInput.setField(field);
            return this;
        }

        public SearchInputBuilder withDisplayContextParameter(String displayContextParameter) {
            this.searchInput.setDisplayContextParameter(displayContextParameter);
            return this;
        }

        public SearchInputBuilder withFieldId(String fieldId, String elementPath) {
            field.setId(fieldId);
            field.setElementPath(elementPath);
            this.searchInput.setField(field);
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

        public CaseTypeTabFieldBuilder withCaseField(CaseField caseField) {
            this.caseTypeTabField.setCaseField(caseField);
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
        private final CaseTypeTab caseTypeTab;
        private final List<CaseTypeTabField> caseTypeTabFields;

        private CaseTypeTabBuilder() {
            this.caseTypeTabFields = newArrayList();
            this.caseTypeTab = new CaseTypeTab();
            this.caseTypeTab.setTabFields(caseTypeTabFields);
        }

        public CaseTypeTabBuilder withTabField(CaseTypeTabField field) {
            this.caseTypeTabFields.add(field);
            return this;
        }

        public CaseTypeTab build() {
            return this.caseTypeTab;
        }

        public static CaseTypeTabBuilder newCaseTab() {
            return new CaseTypeTabBuilder();
        }

    }

    public static class CaseTabCollectionBuilder {
        private final List<CaseTypeTab> tabs;
        private final CaseTabCollection caseTabCollection;

        private CaseTabCollectionBuilder() {
            this.tabs = newArrayList();
            this.caseTabCollection = new CaseTabCollection();
            this.caseTabCollection.setTabs(tabs);
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
                tabField.setDisplayContextParameter("#TABLE(Title, FirstName, MiddleName)");
                tabFields.add(tabField);
            });
            tab.setShowCondition("tabShowCondition");
            tab.setTabFields(tabFields);
            List<CaseTypeTab> tabs = new ArrayList<>();
            tabs.add(tab);
            caseTabCollection.setTabs(tabs);

            return this;
        }

        public CaseTabCollectionBuilder withTab(CaseTypeTab tab) {
            tabs.add(tab);
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

        public UserProfileBuilder withJurisdictionDisplayProperties(JurisdictionDisplayProperties[] jurisdictionDisplayProperties) {
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

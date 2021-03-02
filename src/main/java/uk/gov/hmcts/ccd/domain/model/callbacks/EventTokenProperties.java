package uk.gov.hmcts.ccd.domain.model.callbacks;

public final class EventTokenProperties {

    public static final String EVENT_ID = "event-id";
    public static final String JURISDICTION_ID = "jurisdiction-id";
    public static final String CASE_TYPE_ID = "case-type-id";
    public static final String CASE_ID = "case-id";
    public static final String CASE_VERSION = "case-version";
    public static final String CASE_STATE = "case-state";
    public static final String ENTITY_VERSION = "entity-version";

    private final String uid;
    private final String caseId;
    private final String jurisdictionId;
    private final String eventId;
    private final String caseTypeId;
    private final String version;
    private final String caseState;
    private final String entityVersion;

    public EventTokenProperties(final String uid,
                                final String caseId,
                                final String jurisdictionId,
                                final String eventId,
                                final String caseTypeId,
                                final String version,
                                final String caseState,
                                final String entityVersion) {
        this.uid = uid;
        this.caseId = caseId;
        this.jurisdictionId = jurisdictionId;
        this.eventId = eventId;
        this.caseTypeId = caseTypeId;
        this.version = version;
        this.caseState = caseState;
        this.entityVersion = entityVersion;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getJurisdictionId() {
        return jurisdictionId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getCaseTypeId() {
        return caseTypeId;
    }

    public String getUid() {
        return uid;
    }

    public String getVersion() {
        return version;
    }

    public String getCaseState() {
        return caseState;
    }

    public String getEntityVersion() {
        return entityVersion;
    }
}

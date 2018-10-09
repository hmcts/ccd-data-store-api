package uk.gov.hmcts.ccd.datastore.tests.fixture;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType.CaseData;
import uk.gov.hmcts.ccd.datastore.tests.helper.CCDHelper;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;

import java.util.HashMap;
import java.util.function.Supplier;

public class CCDEventBuilder {
    private static final CCDHelper CCD_HELPER = new CCDHelper();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference STRING_JSON_MAP_TYPE = new TypeReference<HashMap<String, JsonNode>>() {
    };

    private final String jurisdictionId;
    private final String caseTypeId;
    private final String eventId;

    private Supplier<RequestSpecification> asUser;
    private Long caseReference;
    private String summary;
    private String description;
    private CaseData data;

    public CCDEventBuilder(String jurisdictionId, String caseTypeId, String eventId) {
        this.jurisdictionId = jurisdictionId;
        this.caseTypeId = caseTypeId;
        this.eventId = eventId;
    }

    public CCDEventBuilder(String jurisdictionId, String caseTypeId, Long caseReference, String eventId) {
        this.jurisdictionId = jurisdictionId;
        this.caseTypeId = caseTypeId;
        this.caseReference = caseReference;
        this.eventId = eventId;
    }

    public CCDEventBuilder as(Supplier<RequestSpecification> asUser) {
        this.asUser = asUser;
        return this;
    }

    public CCDEventBuilder withSummary(String summary) {
        this.summary = summary;
        return this;
    }

    public CCDEventBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public CCDEventBuilder withData(CaseData data) {
        this.data = data;
        return this;
    }

    public Response submit() {
        final Event event = new Event();
        event.setSummary(summary);
        event.setDescription(description);

        final CaseDataContent caseDataContent = new CaseDataContent();
        caseDataContent.setEvent(event);
        caseDataContent.setData(MAPPER.convertValue(data, STRING_JSON_MAP_TYPE));

        if (isUpdate()) {
            return CCD_HELPER.updateCase(asUser,
                                         jurisdictionId,
                                         caseTypeId,
                                         caseReference,
                                         eventId,
                                         caseDataContent);
        }

        return CCD_HELPER.createCase(asUser,
                                     jurisdictionId,
                                     caseTypeId,
                                     eventId,
                                     caseDataContent);
    }

    public Long submitAndGetReference() {
        return submit().then()
                       .log().ifError()
                       .statusCode(201)
                       .extract()
                       .path("id");
    }

    private Boolean isUpdate() {
        return null != caseReference;
    }
}

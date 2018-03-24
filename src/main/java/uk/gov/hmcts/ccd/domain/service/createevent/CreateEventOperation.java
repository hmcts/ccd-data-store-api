package uk.gov.hmcts.ccd.domain.service.createevent;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.Event;

import java.util.Map;

public interface CreateEventOperation {
    CaseDetails createCaseEvent(String uid,
                                String jurisdictionId,
                                String caseTypeId,
                                String caseReference,
                                Event event,
                                Map<String, JsonNode> data,
                                String token,
                                Boolean ignoreWarning);
}

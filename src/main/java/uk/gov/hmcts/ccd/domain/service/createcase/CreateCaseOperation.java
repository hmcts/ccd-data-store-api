package uk.gov.hmcts.ccd.domain.service.createcase;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.Event;

import java.util.Map;

public interface CreateCaseOperation {
    CaseDetails createCaseDetails(String uid,
                                  String jurisdictionId,
                                  String caseTypeId,
                                  Event event,
                                  Map<String, JsonNode> data,
                                  Boolean ignoreWarning,
                                  String token);
}

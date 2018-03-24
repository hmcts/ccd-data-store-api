package uk.gov.hmcts.ccd.domain.service.validate;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.std.Event;

import java.util.Map;

public interface ValidateCaseFieldsOperation {
    Map<String, JsonNode> validateCaseDetails(String jurisdictionId,
                                              String caseTypeId,
                                              Event event,
                                              final Map<String, JsonNode> data);

    void validateData(final Map<String, JsonNode> data,
                      final CaseType caseType);
}

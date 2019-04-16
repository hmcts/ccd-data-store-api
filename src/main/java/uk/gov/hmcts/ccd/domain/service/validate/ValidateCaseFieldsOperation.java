package uk.gov.hmcts.ccd.domain.service.validate;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

import java.util.Map;

public interface ValidateCaseFieldsOperation {
    Map<String, JsonNode> validateCaseDetails(String caseTypeId,
                                              final CaseDataContent content);
}

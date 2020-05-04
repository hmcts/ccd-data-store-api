package uk.gov.hmcts.ccd.domain.service.validate;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

public interface ValidateCaseFieldsOperation {
    Map<String, JsonNode> validateCaseDetails(String caseTypeId,
                                              final CaseDataContent content);

    void validateData(final Map<String, JsonNode> data,
                      final CaseTypeDefinition caseTypeDefinition);
}

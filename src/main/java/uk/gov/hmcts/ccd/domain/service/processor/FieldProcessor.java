package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;

public interface FieldProcessor {

    JsonNode execute(JsonNode node, CaseField caseField, CaseEventField caseEventField);
}

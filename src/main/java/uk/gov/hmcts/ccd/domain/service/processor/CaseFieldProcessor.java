package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventField;

import java.util.List;
import java.util.Map;

public interface CaseFieldProcessor {

    void process(Map<String, JsonNode> data, List<CaseEventField> list1);
}

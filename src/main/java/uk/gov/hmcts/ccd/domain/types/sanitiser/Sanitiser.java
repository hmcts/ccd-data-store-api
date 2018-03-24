package uk.gov.hmcts.ccd.domain.types.sanitiser;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.domain.types.Typed;

public interface Sanitiser extends Typed {

    JsonNode sanitise(FieldType fieldType, JsonNode fieldData);

}

package uk.gov.hmcts.ccd.domain.types.sanitiser;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.types.Typed;

public interface Sanitiser extends Typed {

    JsonNode sanitise(FieldTypeDefinition fieldTypeDefinition, JsonNode fieldData);

}

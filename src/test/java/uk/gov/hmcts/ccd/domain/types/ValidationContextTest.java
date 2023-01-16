package uk.gov.hmcts.ccd.domain.types;


import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

import java.util.HashMap;
import java.util.Map;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.Assert.assertNull;


class ValidationContextTest {

    private final Map<String, JsonNode> data = new HashMap<>();

    @Test
    void test4ParamConstructor() {
        CaseTypeDefinition caseTypeDefinition = CaseTypeDefinition.builder().id("TEST").build();
        ValidationContext validationContext = new ValidationContext(
            caseTypeDefinition,
            data
        );
        assertNull(validationContext.getFieldDefinition());
        assertNotNull(validationContext.getCaseTypeDefinition());
        assertNotNull(validationContext.getCaseTypeId());
        assertNotNull(validationContext.getData());
        assertNull(validationContext.getFieldValue());
        assertNull(validationContext.getFieldId());
        assertNull(validationContext.getPath());
    }
}

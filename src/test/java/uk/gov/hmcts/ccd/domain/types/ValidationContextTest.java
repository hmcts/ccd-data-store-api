package uk.gov.hmcts.ccd.domain.types;


import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

import java.util.HashMap;
import java.util.Map;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.Assert.assertNull;


class ValidationContextTest {


    private CaseDataContent currentCaseDataContent = new CaseDataContent();
    private String caseTypeId = "test";
    private CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
    private Map<String, JsonNode> data = new HashMap<>();

    @Test
    public void testDefaultConstructor() {
        final ValidationContext validationContext = new ValidationContext();
        assertNull(validationContext.getCaseFieldDefinition());
        assertNull(validationContext.getCaseTypeDefinition());
        assertNull(validationContext.getCaseTypeId());
        assertNull(validationContext.getData());
        assertNull(validationContext.getDataValue());
        assertNull(validationContext.getFieldId());
        assertNull(validationContext.getPath());
    }


    @Test
    public void test4ParamConstructor() {
        ValidationContext validationContext = new ValidationContext(
            caseTypeDefinition,
            data
        );
        assertNull(validationContext.getCaseFieldDefinition());
        assertNotNull(validationContext.getCaseTypeDefinition());
        assertNotNull(validationContext.getCaseTypeId());
        assertNotNull(validationContext.getData());
        assertNull(validationContext.getDataValue());
        assertNull(validationContext.getFieldId());
        assertNull(validationContext.getPath());
    }
}

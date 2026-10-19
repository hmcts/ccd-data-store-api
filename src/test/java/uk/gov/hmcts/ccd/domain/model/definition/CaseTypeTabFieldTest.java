package uk.gov.hmcts.ccd.domain.model.definition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CaseTypeTabFieldTest {

    @Test
    void propertiesCanBeReadAndWritten() {
        CaseTypeTabField tabField = new CaseTypeTabField();
        CaseFieldDefinition caseField = new CaseFieldDefinition();

        tabField.setCaseFieldDefinition(caseField);
        tabField.setDisplayOrder(1);
        tabField.setShowCondition("show condition");
        tabField.setDisplayContextParameter("display context parameter");
        tabField.setDefaultFocus(Boolean.TRUE);

        assertAll(
            () -> assertSame(caseField, tabField.getCaseFieldDefinition()),
            () -> assertEquals(1, tabField.getDisplayOrder()),
            () -> assertEquals("show condition", tabField.getShowCondition()),
            () -> assertEquals("display context parameter", tabField.getDisplayContextParameter()),
            () -> assertEquals(Boolean.TRUE, tabField.getDefaultFocus())
        );
    }
}

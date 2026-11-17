package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaseTypeTabDefinitionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void propertiesCanBeReadAndWritten() {
        CaseTypeTabDefinition tab = new CaseTypeTabDefinition();
        CaseTypeTabField field = new CaseTypeTabField();

        tab.setId("tab-id");
        tab.setLabel("Tab label");
        tab.setDisplayOrder(1);
        tab.setTabFields(List.of(field));
        tab.setShowCondition("show condition");
        tab.setRole("role");
        tab.setDisplayContextParameter("display context parameter");
        tab.setDefaultFocus(Boolean.TRUE);

        assertAll(
            () -> assertEquals("tab-id", tab.getId()),
            () -> assertEquals("Tab label", tab.getLabel()),
            () -> assertEquals(1, tab.getDisplayOrder()),
            () -> assertSame(field, tab.getTabFields().get(0)),
            () -> assertEquals("show condition", tab.getShowCondition()),
            () -> assertEquals("role", tab.getRole()),
            () -> assertEquals("display context parameter", tab.getDisplayContextParameter()),
            () -> assertTrue(tab.getDefaultFocus())
        );

        tab.setDefaultFocus(Boolean.FALSE);
        assertFalse(tab.getDefaultFocus());
    }

    @Test
    void defaultFocusUsesDefaultFocusJsonProperty() throws Exception {
        CaseTypeTabDefinition tab = objectMapper.readValue("{\"defaultFocus\":true}", CaseTypeTabDefinition.class);

        assertTrue(tab.getDefaultFocus());
        JsonNode serializedTab = objectMapper.readTree(objectMapper.writeValueAsString(tab));
        assertTrue(serializedTab.get("defaultFocus").asBoolean());
    }
}

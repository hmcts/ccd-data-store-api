package uk.gov.hmcts.ccd.domain.model.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

class SearchResultViewItemTest {

    private static final String CASE_FIELD_ID = "1";
    private static final String CASE_FIELD_TEXT = "C1";
    private static final String LABEL_FIELD_ID = "fid";
    private static final String LABEL_FIELD_TEXT = "flabel";
    private Map<String, JsonNode> caseFields;
    private List<CaseField> labelFields;
    private JsonNodeFactory jnf = JsonNodeFactory.instance;

    @BeforeEach
    void setUp() {
        caseFields = new HashMap<>();
        caseFields.put(CASE_FIELD_ID, jnf.textNode(CASE_FIELD_TEXT));
        final CaseField caseField = new CaseField();
        caseField.setId(LABEL_FIELD_ID);
        caseField.setLabel(LABEL_FIELD_TEXT);
        labelFields = new ArrayList<>();
        labelFields.add(caseField);
    }

    @DisplayName("should include label fields in the caseFields map")
    @Test
    void checkLabelFieldsAreAddedToCaseFields() {
        final SearchResultViewItem item = new SearchResultViewItem("TestCase", caseFields, labelFields);
        final Map<String, JsonNode> map = item.getCaseFields();
        assertThat(map.size(), is(2));
        assertThat(map.get(CASE_FIELD_ID).asText(), is(CASE_FIELD_TEXT));
        assertThat(map.get(LABEL_FIELD_ID).asText(), is(LABEL_FIELD_TEXT));
    }
}

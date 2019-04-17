package uk.gov.hmcts.ccd.domain.types.sanitiser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;

import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

class TextSanitiserTest {

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final CaseType CASE_TYPE = new CaseType();
    private static final String TYPE_TEXT = "Text";
    private static final FieldType TEXT_FIELD_TYPE = new FieldType();
    private static final String TEXT_FIELD_ID = "TestText";
    private static final CaseField TEXT_FIELD = new CaseField();

    static {
        TEXT_FIELD_TYPE.setId(TYPE_TEXT);
        TEXT_FIELD_TYPE.setType(TYPE_TEXT);
        TEXT_FIELD.setId(TEXT_FIELD_ID);
        TEXT_FIELD.setFieldType(TEXT_FIELD_TYPE);

        CASE_TYPE.setCaseFields(Collections.singletonList(TEXT_FIELD));
    }

    @InjectMocks
    private TextSanitiser textSanitiser;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        textSanitiser = new TextSanitiser();
    }

    @Test
    public void shouldSanitizeValidText() {
        JsonNode data = JSON_NODE_FACTORY.textNode("  Mr Tester  ");
        JsonNode result = JSON_NODE_FACTORY.textNode("Mr Tester");
        JsonNode sanitisedText = textSanitiser.sanitise(TEXT_FIELD_TYPE, data);

        assertThat(sanitisedText, is(result));
    }

    @Test
    public void shouldSanitizeOnlyWhitespace() {
        JsonNode data = JSON_NODE_FACTORY.textNode("       ");
        JsonNode result = JSON_NODE_FACTORY.textNode("");
        JsonNode sanitisedText = textSanitiser.sanitise(TEXT_FIELD_TYPE, data);

        assertThat(sanitisedText, is(result));
    }
}

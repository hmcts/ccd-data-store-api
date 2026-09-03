package listener;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.types.listener.RichTextAreaValidationListener;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("RichTextAreaValidationListener")
class RichTextAreaValidationListenerTest {
    private RichTextAreaValidationListener listener;

    @BeforeEach
    void setUp() {
        listener =
            new RichTextAreaValidationListener();
    }

    @Test
    void testDiscardedTag() {
        listener.discardedTag(null, "RichTextAreaField");
        assertEquals(1, listener.getErrors().size());
        assertEquals("Enter valid tags for RichTextArea field: RichTextAreaField",
            listener.getErrors().getFirst());
    }

    @Test
    void testDiscardedEmptyTag() {
        listener.discardedTag(null, "");
        assertEquals(0, listener.getErrors().size());
    }

    @Test
    void testDiscardedAttributes() {
        listener.discardedAttributes(null, "div", "onclick");
        assertEquals(1, listener.getErrors().size());
        assertEquals("Enter valid attributes for RichTextArea field: div[onclick]",
            listener.getErrors().getFirst());
    }

    @Test
    void testDiscardedEmptyAttributes() {
        listener.discardedAttributes(null, "div", "");
        assertEquals(1, listener.getErrors().size());
        assertEquals("Enter valid attributes for RichTextArea field: div[]",
            listener.getErrors().getFirst());
    }

    @Test
    void testDiscardedAttributesOnClick() {
        listener.discardedAttributes(null, "p", "onclick=\"alert(1)\"");
        assertEquals(1, listener.getErrors().size());
        assertEquals("Enter valid attributes for RichTextArea field: p[onclick=\"alert(1)\"]",
            listener.getErrors().getFirst());
    }

}

package listener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.types.listener.RichTextAreaValidationListener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RichTextAreaValidationListener")
class RichTextAreaValidationListenerTest {

    @Test
    void testDiscardedTag() {
        RichTextAreaValidationListener listener = new RichTextAreaValidationListener();
        listener.discardedTag(null, "RichTextAreaField");
        assertEquals(1, listener.getErrors().size());
        assertEquals("Enter valid tags for RichTextArea field: RichTextAreaField", listener.getErrors().get(0));
    }

    @Test
    void testDiscardedAttributes() {
        RichTextAreaValidationListener listener = new RichTextAreaValidationListener();
        listener.discardedAttributes(null, "div", "onclick");
        // No errors should be added for discarded attributes
        assertTrue(listener.getErrors().isEmpty());
    }


}

package uk.gov.hmcts.ccd.domain.types.listener;

import lombok.Getter;
import org.owasp.html.HtmlChangeListener;

import java.util.ArrayList;
import java.util.List;

public class RichTextAreaValidationListener implements HtmlChangeListener<Void> {

    @Getter
    private final List<String> errors = new ArrayList<>();

    @Override
    public void discardedTag(Void context, String elementName) {
        if (elementName != null) {
            errors.add("Enter valid tags for RichTextArea field: " + elementName);
        }
    }

    @Override
    public void discardedAttributes(
        Void context,
        String tagName,
        String... attributeNames) {

        if (attributeNames == null) {
            return;
        }
        for (String attributeName : attributeNames) {
            if (attributeName != null) {
                errors.add("Enter valid attributes for RichTextArea field: " + tagName + "[" + attributeName + "]");
            }
        }
    }
}


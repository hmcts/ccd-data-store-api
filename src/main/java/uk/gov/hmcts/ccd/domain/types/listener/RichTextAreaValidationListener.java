package uk.gov.hmcts.ccd.domain.types.listener;

import lombok.Getter;
import org.owasp.html.HtmlChangeListener;

import java.util.ArrayList;
import java.util.List;

@Getter
public class RichTextAreaValidationListener implements HtmlChangeListener<Void> {
    private final List<String> errors = new ArrayList<>();

    @Override
    public void discardedTag(Void context, String elementName) {
        errors.add("Enter valid tags for RichTextArea field: " + elementName);
    }

    @Override
    public void discardedAttributes(
        Void context,
        String tagName,
        String... attributeNames) {

        for (String attribute : attributeNames) {
            errors.add(
                "Attribute '" + attribute + "' is not allowed on <" + tagName + ">"
            );
        }
    }

}


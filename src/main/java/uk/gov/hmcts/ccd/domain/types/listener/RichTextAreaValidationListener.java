package uk.gov.hmcts.ccd.domain.types.listener;

import lombok.Getter;
import org.owasp.html.HtmlChangeListener;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RichTextAreaValidationListener implements HtmlChangeListener<Void> {

    @Value("${html.policy.allowed.whitelist.tags}")
    private String[] allowedWhitelistTags;

    @Getter
    private final List<String> errors = new ArrayList<>();

    public RichTextAreaValidationListener(String[] allowedWhitelistTags) {
        this.allowedWhitelistTags = allowedWhitelistTags;
    }

    @Override
    public void discardedTag(Void context, String elementName) {
        if (!Arrays.stream(allowedWhitelistTags).anyMatch(elementName.toLowerCase()::equals)) {
            errors.add("Enter valid tags for RichTextArea field: " + elementName);
        }
    }

    @Override
    public void discardedAttributes(
        Void context,
        String tagName,
        String... attributeNames) {
        if (attributeNames == null || attributeNames.length == 0) {
            return;
        }

        for (String attributeName : attributeNames) {
            errors.add("Enter valid attributes for RichTextArea field: " + tagName + "[" + attributeName + "]");
        }
    }
}


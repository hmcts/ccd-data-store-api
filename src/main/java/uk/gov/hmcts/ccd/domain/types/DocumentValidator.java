package uk.gov.hmcts.ccd.domain.types;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;


@Named("DocumentValidator")
@Singleton
public class DocumentValidator implements BaseTypeValidator {
    public static final String TYPE_ID = "Document";
    static final String DOCUMENT_URL = "document_url";
    private static final String DOCUMENT_BINARY_URL = "document_binary_url";

    private static final Logger LOG = LoggerFactory.getLogger(DocumentValidator.class);

    private final ApplicationParams applicationParams;

    public DocumentValidator(ApplicationParams applicationParams) {
        this.applicationParams = applicationParams;
    }

    @Override
    public BaseType getType() {
        return BaseType.get(TYPE_ID);
    }

    @Override
    public List<ValidationResult> validate(final String dataFieldId,
                                           final JsonNode dataValue,
                                           final CaseField caseFieldDefinition) {

        // Empty text should still check against MIN - MIN may or may not be 0
        if (isNullOrEmpty(dataValue)) {
            return Collections.emptyList();
        }

        if (!dataValue.has(DOCUMENT_URL)) {
            final String nodeType = dataValue.getNodeType().toString().toLowerCase();
            return Collections.singletonList(new ValidationResult(nodeType
                + " does not have document_url key specified", dataFieldId));
        }

        final JsonNode documentUrl = dataValue.get(DOCUMENT_URL);

        if (!documentUrl.isTextual() || documentUrl.isNull()) {
            return Collections.singletonList(new ValidationResult(
                "document_url is not a text value or is null", dataFieldId));
        }

        final String documentUrlValue = documentUrl.textValue();
        final String urlPatternString = applicationParams.getValidDMDomain() + "/documents/[A-Za-z0-9-]+(?:/binary)?";
        final Pattern urlPattern = Pattern.compile(urlPatternString);
        final Matcher documentUrlMatcher = urlPattern.matcher(documentUrlValue);

        if (!documentUrlMatcher.matches()) {
            LOG.error("{} does not match Document Management domain or expected URL path {} {}",
                      documentUrlValue, urlPatternString, dataFieldId);
            return Collections.singletonList(new ValidationResult(documentUrlValue
                + " does not match Document Management domain or expected URL path", dataFieldId));
        }

        if (dataValue.has(DOCUMENT_BINARY_URL)) {
            final JsonNode documentBinaryUrl = dataValue.get(DOCUMENT_BINARY_URL);

            if (!documentBinaryUrl.isTextual() || documentBinaryUrl.isNull()) {
                return Collections.singletonList(new ValidationResult(
                    "document_binary_url is not a text value or is null", dataFieldId));
            }

            final String documentBinaryUrlValue = documentBinaryUrl.textValue();
            final Matcher documentBinaryUrlMatcher = urlPattern.matcher(documentBinaryUrlValue);

            if (!documentBinaryUrlMatcher.matches()) {
                LOG.error("{} does not match Document Management domain or expected URL path {} {}",
                          documentUrlValue, urlPatternString, dataFieldId);
                return Collections.singletonList(new ValidationResult(documentBinaryUrlValue
                    + " does not match Document Management domain or expected URL path", dataFieldId));
            }
        }

        return Collections.emptyList();
    }

}

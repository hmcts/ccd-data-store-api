package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

//todo Cognitive... refactor this class or add file to to sonar exclusions
@SuppressWarnings({"squid:S3776","squid:S1135"})
@Named("DocumentValidator")
@Singleton
public class DocumentValidator implements BaseTypeValidator {
    public static final String TYPE_ID = "Document";
    static final String DOCUMENT_URL = "document_url";
    private static final String DOCUMENT_BINARY_URL = "document_binary_url";
    private static final String CATEGORY_ID = "category_id";
    private static final String UPLOAD_TIMESTAMP = "upload_timestamp";
    private static final String LOG_MESSAGE = "Validation failure for: "; //todo remove
    private static final String NOT_A_VALID_INPUT = " is not a text value or is null";

    private static final Logger LOG = LoggerFactory.getLogger(DocumentValidator.class);

    private final ApplicationParams applicationParams;
    private final TextValidator textValidator;
    private final DateTimeValidator dateTimeValidator;

    public DocumentValidator(ApplicationParams applicationParams,
                             @Qualifier("TextValidator") TextValidator textValidator,
                             @Qualifier("DateTimeValidator") DateTimeValidator dateTimeValidator) {
        this.applicationParams = applicationParams;
        this.textValidator = textValidator;
        this.dateTimeValidator = dateTimeValidator;
    }

    @Override
    public BaseType getType() {
        return BaseType.get(TYPE_ID);
    }

    @Override
    public List<ValidationResult> validate(final String dataFieldId,
                                           final JsonNode dataValue,
                                           final CaseFieldDefinition caseFieldDefinition) {

        // Empty text should still check against MIN - MIN may or may not be 0
        if (Boolean.TRUE.equals(BaseTypeValidator.super.isNullOrEmpty(dataValue))) {
            return Collections.emptyList();
        }

        if (!dataValue.has(DOCUMENT_URL)) {
            final String nodeType = dataValue.getNodeType().toString().toLowerCase();
            return Collections.singletonList(new ValidationResult(nodeType
                + " does not have document_url key specified", dataFieldId));
        }

        final JsonNode documentUrl = dataValue.get(DOCUMENT_URL);

        if (Boolean.TRUE.equals(isNullOrEmpty(documentUrl))) {
            return Collections.singletonList(new ValidationResult(
                DOCUMENT_URL + NOT_A_VALID_INPUT, dataFieldId));
        }

        final String documentUrlValue = documentUrl.textValue();
        final String urlPatternString = applicationParams.getDocumentURLPattern();
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

            if (Boolean.TRUE.equals(isNullOrEmpty(documentBinaryUrl))) {
                return Collections.singletonList(new ValidationResult(
                    DOCUMENT_BINARY_URL + NOT_A_VALID_INPUT, dataFieldId));
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

        if (dataValue.has(CATEGORY_ID)) {

            final JsonNode categoryId = dataValue.get(CATEGORY_ID);

            if (Boolean.TRUE.equals(isNullOrEmpty(categoryId))) {
                LOG.info("{} is not a text value or is null", CATEGORY_ID);
                return Collections.singletonList(new ValidationResult(
                    CATEGORY_ID + NOT_A_VALID_INPUT, dataFieldId));
            }

            final List<ValidationResult> validationResults =
                textValidator.validate(dataFieldId, categoryId, caseFieldDefinition);

            if (!checkValidationResults(validationResults,CATEGORY_ID).isEmpty()) {
                return validationResults;
            }

        }

        if (dataValue.has(UPLOAD_TIMESTAMP)) {
            final JsonNode uploadTimeStamp = dataValue.get(UPLOAD_TIMESTAMP);

            if (Boolean.TRUE.equals(isNullOrEmpty(uploadTimeStamp))) {
                LOG.info("{} is not a text value or is null", UPLOAD_TIMESTAMP);
                return Collections.singletonList(new ValidationResult(
                    UPLOAD_TIMESTAMP + NOT_A_VALID_INPUT, dataFieldId));
            }

            final List<ValidationResult> validationResults =
                dateTimeValidator.validate(dataFieldId, uploadTimeStamp, caseFieldDefinition);

            if (!checkValidationResults(validationResults,UPLOAD_TIMESTAMP).isEmpty()) {
                return validationResults;
            }
        }

        return Collections.emptyList();
    }

    @Override
    public Boolean isNullOrEmpty(JsonNode dataValue) {
        // todo TextValidator does this check as well. Remove?
        return !dataValue.isTextual() || BaseTypeValidator.super.isNullOrEmpty(dataValue);
    }

    private List<ValidationResult> checkValidationResults(final List<ValidationResult> validationResults,
                                                       final String key) {
        if (!validationResults.isEmpty()) {
            LOG.info("{} {}",LOG_MESSAGE,key);
            return validationResults.stream()
                .map(validationResult ->
                    new ValidationResult(
                        key + " " + validationResult.getErrorMessage(),validationResult.getFieldId()))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

}

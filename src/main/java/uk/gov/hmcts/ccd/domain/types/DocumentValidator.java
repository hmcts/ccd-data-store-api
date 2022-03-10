package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Named("DocumentValidator")
@Singleton
public class DocumentValidator implements BaseTypeValidator {
    public static final String TYPE_ID = "Document";
    static final String DOCUMENT_URL = "document_url";
    private static final String DOCUMENT_BINARY_URL = "document_binary_url";
    private static final String CATEGORY_ID = "category_id";
    private static final String UPLOAD_TIMESTAMP = "upload_timestamp";

    private static final Logger LOG = LoggerFactory.getLogger(DocumentValidator.class);

    private final ApplicationParams applicationParams;
    private final TextValidator textValidator;
    private final DateTimeValidator dateTimeValidator;
    private final CachedCaseDefinitionRepository caseDefinitionRepository;

    public DocumentValidator(ApplicationParams applicationParams,
                             @Qualifier("TextValidator") TextValidator textValidator,
                             @Qualifier("DateTimeValidator") DateTimeValidator dateTimeValidator,
                             @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                 CachedCaseDefinitionRepository caseDefinitionRepository) {
        this.applicationParams = applicationParams;
        this.textValidator = textValidator;
        this.dateTimeValidator = dateTimeValidator;
        this.caseDefinitionRepository = caseDefinitionRepository;
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

        if (dataValue.has(CATEGORY_ID)) {
            final JsonNode categoryId = dataValue.get(CATEGORY_ID);

            if (checkIfNullOrEmpty(categoryId)) {
                LOG.info("{} is empty or null", CATEGORY_ID);
                return Collections.singletonList(new ValidationResult(
                    CATEGORY_ID + " is empty or null", dataFieldId));
            }

            final List<ValidationResult> validationResults =
                textValidator.validate(dataFieldId, categoryId, caseFieldDefinition);

            if (!validationResults.isEmpty()) {
                LOG.info("{} validation failure", CATEGORY_ID);
                return validationResults.stream()
                    .map(validationResult ->
                        new ValidationResult(
                            CATEGORY_ID + " " + validationResult.getErrorMessage(),validationResult.getFieldId()))
                    .collect(Collectors.toList());
            }
        }

        if (dataValue.has(UPLOAD_TIMESTAMP)) {
            final JsonNode uploadTimeStamp = dataValue.get(UPLOAD_TIMESTAMP);

            if (checkIfNullOrEmpty(uploadTimeStamp)) {
                LOG.info("{} is empty or null", UPLOAD_TIMESTAMP);
                return Collections.singletonList(new ValidationResult(
                    UPLOAD_TIMESTAMP + " is empty or null", dataFieldId));
            }

            final List<ValidationResult> validationResults =
                dateTimeValidator.validate(dataFieldId, uploadTimeStamp, caseFieldDefinition);

            if (!validationResults.isEmpty()) {
                LOG.info("{} validation failure", UPLOAD_TIMESTAMP);
                return validationResults.stream()
                    .map(validationResult ->
                        new ValidationResult(
                            UPLOAD_TIMESTAMP + " " + validationResult.getErrorMessage(),validationResult.getFieldId()))
                    .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    public Boolean checkIfNullOrEmpty(JsonNode dataValue) {
        return !dataValue.isTextual() || isNullOrEmpty(dataValue);
    }

}

package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

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
    private static final String NOT_TEXT_OR_NULL = " is not a text value or is null";

    private static final Logger LOG = LoggerFactory.getLogger(DocumentValidator.class);

    private final ApplicationParams applicationParams;
    private final DateTimeValidator dateTimeValidator;
    private final CaseDefinitionRepository caseDefinitionRepository;

    public DocumentValidator(ApplicationParams applicationParams,
                             @Qualifier("DateTimeValidator") DateTimeValidator dateTimeValidator,
                             @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                 CaseDefinitionRepository caseDefinitionRepository) {
        this.applicationParams = applicationParams;
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
        List<ValidationResult> validationResults;

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
        if (isNullOrEmpty(documentUrl)) {
            return nullOrEmptyValidationResult(DOCUMENT_URL, dataFieldId);
        }

        final String documentUrlValue = documentUrl.textValue();
        if (documentUrlValue == null) {
            return Collections.singletonList(new ValidationResult(
                DOCUMENT_URL + NOT_TEXT_OR_NULL, dataFieldId));
        }

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
            validationResults = validateDocumentBinaryURL(
                dataFieldId,documentBinaryUrl,urlPattern,documentUrlValue, urlPatternString);
            if (!validationResults.isEmpty()) {
                return validationResults;
            }
        }

        if (dataValue.has(CATEGORY_ID)) {
            validationResults = validateCategoryId(dataFieldId,dataValue,caseFieldDefinition);
            if (!validationResults.isEmpty()) {
                return validationResults;
            }
        }

        if (dataValue.has(UPLOAD_TIMESTAMP)) {
            final JsonNode uploadTimeStamp = dataValue.get(UPLOAD_TIMESTAMP);
            validationResults = validateUploadTimeStamp(dataFieldId, uploadTimeStamp);
            if (!validationResults.isEmpty()) {
                return validationResults;
            }
        }

        return Collections.emptyList();
    }

    private List<ValidationResult> validateDocumentBinaryURL(final String dataFieldId,
                                                             final JsonNode documentBinaryUrl,
                                                             Pattern urlPattern,
                                                             String documentUrlValue,
                                                             String urlPatternString) {

        if (isNullOrEmpty(documentBinaryUrl)) {
            return nullOrEmptyValidationResult(DOCUMENT_BINARY_URL, dataFieldId);
        }

        final String documentBinaryUrlValue = documentBinaryUrl.textValue();
        final Matcher documentBinaryUrlMatcher = urlPattern.matcher(documentBinaryUrlValue);

        if (!documentBinaryUrlMatcher.matches()) {
            LOG.error("{} does not match Document Management domain or expected URL path {} {}",
                documentUrlValue, urlPatternString, dataFieldId);
            return Collections.singletonList(new ValidationResult(documentBinaryUrlValue
                + " does not match Document Management domain or expected URL path", dataFieldId));
        }
        return Collections.emptyList();
    }

    private List<ValidationResult> validateCategoryId(final String dataFieldId,
                                                      final JsonNode dataValue,
                                                      final CaseFieldDefinition caseFieldDefinition) {
        final JsonNode categoryId = dataValue.get(CATEGORY_ID);

        if (isNullOrEmpty(categoryId)) {
            LOG.debug("{}{}",CATEGORY_ID,NOT_TEXT_OR_NULL);
            return Collections.emptyList();
        }

        if (!categoryId.isTextual()) {
            final String nodeType = categoryId.getNodeType().toString().toLowerCase();
            return Collections.singletonList(new ValidationResult(nodeType + " is not a string : " + CATEGORY_ID,
                    dataFieldId));
        }
        return Collections.emptyList();
    }

    private List<ValidationResult> validateUploadTimeStamp(final String dataFieldId,
                                                           final JsonNode uploadTimeStamp) {

        if (isNullOrEmpty(uploadTimeStamp)) {
            LOG.debug("{}{}",UPLOAD_TIMESTAMP,NOT_TEXT_OR_NULL);
            return Collections.emptyList();
        }

        CaseFieldDefinition emptyCaseFieldDefinition = new CaseFieldDefinition();
        emptyCaseFieldDefinition.setFieldTypeDefinition(new FieldTypeDefinition());

        List<ValidationResult> validationResults =
            dateTimeValidator.validate(dataFieldId, uploadTimeStamp, emptyCaseFieldDefinition);
        if (!validationResults.isEmpty()) {
            return validationResult(UPLOAD_TIMESTAMP,validationResults);
        }
        return Collections.emptyList();
    }

    private List<ValidationResult> nullOrEmptyValidationResult(String key, String dataFieldId) {
        LOG.debug("{}{}",key,NOT_TEXT_OR_NULL);
        return Collections.singletonList(new ValidationResult(
            key + NOT_TEXT_OR_NULL, dataFieldId));
    }

    private List<ValidationResult> validationResult(String key, List<ValidationResult> validationResults) {
        LOG.error("Failed to validate {}",key);
        return validationResults.stream()
            .map(validationResult ->
                new ValidationResult(
                    validationResult.getErrorMessage() + " : " + key,validationResult.getFieldId()))
            .collect(Collectors.toList());
    }

}

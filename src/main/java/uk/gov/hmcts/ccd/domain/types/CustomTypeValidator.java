package uk.gov.hmcts.ccd.domain.types;
/**
 * Validator for custom non-base field types, such as CaseLink's custom type TextCaseReference.
 * It allows to execute custom validation specific for the custom type. A custom type extends from a base type.
 * Note that the CCD validation logic will execute only the custom type validator for a field if one is
 * found. It won't execute the base type validation it extends from. The custom type validator needs to
 * execute the base type validator if required
 * If a predefined complex type sub-field can be declared of a base type, prefer using a FieldIdBasedValidator
 * to add additional validation logic
 */

@SuppressWarnings("checkstyle:SummaryJavadoc")
public interface CustomTypeValidator extends FieldValidator {

    /**
     * @return the custom type id to which the validator is associated
     */
    String getCustomTypeId();
}

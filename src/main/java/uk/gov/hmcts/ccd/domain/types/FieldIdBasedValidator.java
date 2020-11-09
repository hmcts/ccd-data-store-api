package uk.gov.hmcts.ccd.domain.types;

/**
 * A validator that is associated to a field id, rather than the field type. Can be used in predefined complex types
 * such as OrganisationPolicy as an easy way to add custom validation for its sub-fields, without having to introduce
 * new ad-hoc custom types.
 * If a custom type needs to be introduced for predefined complex type sub-field, then prefer using
 * a CustomTypeValidator
 * to add additional validation logic
 */

@SuppressWarnings("checkstyle:SummaryJavadoc")
public interface FieldIdBasedValidator extends FieldValidator {

    /**
     * @return the field id to which the validator is associated
     */
    String getFieldId();
}

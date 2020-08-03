package uk.gov.hmcts.ccd.domain.types;

/**
 * Validator for fields of predefined types, such as CaseLink. It allows to add custom validation specific for the predefined type
 */
public interface PredefinedTypeFieldValidator extends FieldValidator {

    String getPredefinedFieldId();
}

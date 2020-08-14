package uk.gov.hmcts.ccd.domain.types;

/**
 * Validator for fields defined on predefined types with a predetermined field ID, such as for CaseLink's text field TextCaseReference.
 * It allows to add custom validation specific for the predefined type's field.
 * Note that the CCD validation logic will execute only the PredefinedTypeFieldValidator for a certain field if one is found. It won't execute
 * any BaseTypeValidator. It's going to be the PredefinedTypeFieldValidator's responsibility to execute the associated BaseTypeValidator if its
 * default validation checks are required
 */
public interface PredefinedTypeFieldValidator extends FieldValidator {

    String getPredefinedFieldId();
}

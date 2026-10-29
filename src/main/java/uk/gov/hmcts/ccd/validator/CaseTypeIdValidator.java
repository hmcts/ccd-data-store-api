package uk.gov.hmcts.ccd.validator;

import uk.gov.hmcts.ccd.validator.annotation.ValidCaseTypeId;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CaseTypeIdValidator implements ConstraintValidator<ValidCaseTypeId, String> {

    @Override
    public boolean isValid(String caseTypeId, ConstraintValidatorContext context) {
        if (caseTypeId == null || caseTypeId.isEmpty() || !isAsciiLetterOrDigit(caseTypeId.charAt(0))) {
            return false;
        }
        return caseTypeId.chars().allMatch(this::isValidCaseTypeIdCharacter);
    }

    private boolean isValidCaseTypeIdCharacter(int character) {
        return isAsciiLetterOrDigit(character)
            || character == '_'
            || character == '-'
            || character == '.';
    }

    private boolean isAsciiLetterOrDigit(int character) {
        return character >= 'a' && character <= 'z'
            || character >= 'A' && character <= 'Z'
            || character >= '0' && character <= '9';
    }
}

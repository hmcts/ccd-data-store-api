package uk.gov.hmcts.ccd.validators;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.validators.annotations.CaseID;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

@Service
public class CaseIDValidator implements ConstraintValidator<CaseID, String> {

    private final UIDService uidService;

    @Autowired
    public CaseIDValidator(final UIDService uidService) {
        this.uidService = uidService;
    }


    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        context.disableDefaultConstraintViolation();
        if (value == null || value.isEmpty()) {
            context.buildConstraintViolationWithTemplate("Case id cannot be empty or null.")
                .addConstraintViolation();
            return false;
        }
        if (!uidService.validateUID(value)) {
            context.buildConstraintViolationWithTemplate("The case id " + value + "is not valid")
                .addConstraintViolation();
            return false;
        }
        return true;
    }
}

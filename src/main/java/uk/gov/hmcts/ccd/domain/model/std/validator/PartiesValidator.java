package uk.gov.hmcts.ccd.domain.model.std.validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.domain.model.search.global.Party;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

public class PartiesValidator implements ConstraintValidator<ValidPartiesItems, List<Party>> {

    private static final Logger LOG = LoggerFactory.getLogger(PartiesValidator.class);


    @Override
    public boolean isValid(final List<Party> partyList, final ConstraintValidatorContext context) {
        if (partyList == null) {
            return true;
        }

        try {
            for (Party party : partyList) {
                if (party.getNumberOfNonNullFields() < 2) {
                    return false;
                }
            }
        } catch (IllegalAccessException e) {
            LOG.error(e.getMessage());
        }
        return true;
    }
}

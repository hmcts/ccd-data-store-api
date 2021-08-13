package uk.gov.hmcts.ccd.domain.model.std.validator;

import uk.gov.hmcts.ccd.domain.model.search.global.Party;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

public class PartiesValidator implements ConstraintValidator<ValidPartiesItems, List<Party>> {

    @Override
    public boolean isValid(final List<Party> partyList, final ConstraintValidatorContext context) {
        if (partyList != null) {
            for (Party party : partyList) {
                int partyFields = 0;
                if (party.getPartyName() != null) {
                    partyFields++;
                }
                if (party.getAddressLine1() != null) {
                    partyFields++;
                }
                if (party.getDateOfBirth() != null) {
                    partyFields++;
                }
                if (party.getEmailAddress() != null) {
                    partyFields++;
                }
                if (party.getPostCode() != null) {
                    partyFields++;
                }
                if (partyFields < 2) {
                    return false;
                }
            }
        }
        return true;
    }
}

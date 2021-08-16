package uk.gov.hmcts.ccd.endpoint.std.validator;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.ccd.domain.model.search.global.Party;
import uk.gov.hmcts.ccd.domain.model.std.validator.PartiesValidator;

import javax.validation.ConstraintValidatorContext;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartiesValidatorTest {
    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Mock
    ConstraintValidatorContext.ConstraintViolationBuilder constraintViolationBuilder;

    private final PartiesValidator validator = new PartiesValidator();
    private final List<Party> partyList = new ArrayList<>();

    @Test
    void returnsTrueWhenPartyListNull() {
        assertTrue(validator.isValid(null, constraintValidatorContext));
    }

    @Test
    void returnsTrueWhenPartyListIsValid() {
        Party party = new Party();
        party.setAddressLine1("address");
        party.setPartyName("name");
        Party secondParty = new Party();
        secondParty.setEmailAddress("someone@cgi.com");
        secondParty.setDateOfBirth("1999-12-01");
        partyList.add(party);
        partyList.add(secondParty);
        assertTrue(validator.isValid(partyList, constraintValidatorContext));
    }

    @Test
    void returnsFalseWhenPartyListIsInvalid() {
        Party party = new Party();
        party.setAddressLine1("address");
        party.setPartyName("name");
        Party secondParty = new Party();
        secondParty.setEmailAddress("someone@cgi.com");
        partyList.add(party);
        partyList.add(secondParty);
        assertFalse(validator.isValid(partyList, constraintValidatorContext));
    }

}

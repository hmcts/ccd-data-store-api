package uk.gov.hmcts.ccd.domain.model.std;

import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.ccd.domain.model.std.validator.ValidationError;

import javax.validation.constraints.Pattern;

@Getter
@Setter
public class Parties {

    public String partyName;

    public String emailAddress;

    public String addressLine1;

    @Pattern(regexp = "(?i)^([A-Z][A-HJ-Y]?\\d[A-Z0-9]? ?\\d[A-Z]{2}|GIR ?0A{2})$", message =
        ValidationError.POSTCODE_INVALID)
    public String postCode;

    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$", message =
        ValidationError.DATE_OF_BIRTH_INVALID)
    public String dateOfBirth;

}

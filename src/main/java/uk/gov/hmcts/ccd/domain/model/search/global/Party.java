package uk.gov.hmcts.ccd.domain.model.search.global;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.ccd.domain.model.std.validator.ValidationError;

import javax.validation.constraints.Pattern;
import java.lang.reflect.Field;

@Getter
@Setter
public class Party {

    private String partyName;

    private String emailAddress;

    private String addressLine1;

    private String postCode;

    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$", message =
        ValidationError.DATE_OF_BIRTH_INVALID)
    private String dateOfBirth;

    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[012])-(0[1-9]|[12][0-9]|3[01])$", message =
        ValidationError.DATE_OF_DEATH_INVALID)
    private String dateOfDeath;

    @JsonIgnore
    public int getNumberOfNonNullFields() throws IllegalAccessException {
        Field[] partyFields = Party.class.getDeclaredFields();
        int nonNullFields = 0;
        for (Field field : partyFields) {
            if (field.get(this) != null) {
                nonNullFields++;
            }
        }
        return nonNullFields;
    }
}

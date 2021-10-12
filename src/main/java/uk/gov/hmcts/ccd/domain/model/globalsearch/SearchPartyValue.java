package uk.gov.hmcts.ccd.domain.model.globalsearch;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.StringUtils;

@JsonNaming(PropertyNamingStrategy.UpperCamelCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class SearchPartyValue {

    private String name;

    private String emailAddress;

    private String addressLine1;

    private String postCode;

    private String dateOfBirth;

    private String dateOfDeath;

    @JsonIgnore
    public boolean isEmpty() {
        return StringUtils.isEmpty(name)
                && StringUtils.isEmpty(emailAddress)
                && StringUtils.isEmpty(addressLine1)
                && StringUtils.isEmpty(postCode)
                && StringUtils.isEmpty(dateOfBirth)
                && StringUtils.isEmpty(dateOfDeath);
    }
}

package uk.gov.hmcts.ccd.domain.model.globalsearch;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonNaming(PropertyNamingStrategy.UpperCamelCaseStrategy.class)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class SearchPartyValue {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String name;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String emailAddress;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String addressLine1;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String postCode;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String dateOfBirth;

    @JsonIgnore
    public boolean isEmpty() {
        return (name == null || name.isEmpty())
                && (emailAddress == null || emailAddress.isEmpty())
                && (addressLine1 == null || addressLine1.isEmpty())
                && (postCode == null || postCode.isEmpty())
                && (dateOfBirth == null || dateOfBirth.isEmpty());
    }
}

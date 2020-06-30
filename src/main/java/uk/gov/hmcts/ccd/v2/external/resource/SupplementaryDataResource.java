package uk.gov.hmcts.ccd.v2.external.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;

@Data
@EqualsAndHashCode
@NoArgsConstructor
public class SupplementaryDataResource {

    @JsonProperty("supplementary_data")
    private SupplementaryData supplementaryData;

    public SupplementaryDataResource(final SupplementaryData supplementaryDataUpdated) {
        this.supplementaryData = supplementaryDataUpdated;
    }
}

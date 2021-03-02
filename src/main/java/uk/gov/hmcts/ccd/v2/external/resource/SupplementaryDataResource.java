package uk.gov.hmcts.ccd.v2.external.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;

@Data
@EqualsAndHashCode
@NoArgsConstructor
public class SupplementaryDataResource {

    @JsonProperty("supplementary_data")
    private Map<String, Object> response;

    public SupplementaryDataResource(final SupplementaryData supplementaryDataUpdated) {
        this.response = supplementaryDataUpdated.getResponse();
    }
}

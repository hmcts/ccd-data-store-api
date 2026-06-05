package uk.gov.hmcts.ccd.v2.external.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@EqualsAndHashCode
@NoArgsConstructor
public class SupplementaryCasesDataResource {

    @JsonProperty("successes")
    private List<SupplementaryCaseSuccessDataResource> successes;

    @JsonProperty("failures")
    private List<SupplementaryCaseFailDataResource> failures;

    public SupplementaryCasesDataResource(final List<SupplementaryCaseSuccessDataResource> successes,
                                            final List<SupplementaryCaseFailDataResource> failures) {
        this.successes = successes;
        this.failures = failures;
    }
}

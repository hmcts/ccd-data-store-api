package uk.gov.hmcts.ccd.v2.internal.resource;

import java.util.List;

import org.springframework.hateoas.ResourceSupport;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionUiConfig;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UIJurisdictionConfigResource extends ResourceSupport {

    private List<JurisdictionUiConfig> configs;

    public UIJurisdictionConfigResource(@NonNull List<JurisdictionUiConfig> listOfConfigs) {
        copyProperties(listOfConfigs);
    }

    private void copyProperties(List<JurisdictionUiConfig> listOfConfigs) {
        this.configs = listOfConfigs;
    }
}

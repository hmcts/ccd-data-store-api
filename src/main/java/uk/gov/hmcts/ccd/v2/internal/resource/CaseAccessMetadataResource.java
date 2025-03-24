package uk.gov.hmcts.ccd.v2.internal.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProcess;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;

import java.util.List;

@Data
@NoArgsConstructor
public class CaseAccessMetadataResource {

    @JsonProperty("accessGrants")
    private List<GrantType> accessGrants;

    @JsonProperty("accessProcess")
    private AccessProcess accessProcess;

}

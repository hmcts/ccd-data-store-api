package uk.gov.hmcts.ccd.v2.external.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.hateoas.RepresentationModel;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryDataRequest;
import uk.gov.hmcts.ccd.v2.external.controller.CaseController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class SupplementaryDataResource extends RepresentationModel<RepresentationModel<?>> {

    @JsonProperty("supplementary_data")
    private SupplementaryData supplementaryData;

    public SupplementaryDataResource(final String caseId,
                                     final SupplementaryDataRequest requestSupplementaryData,
                                     final SupplementaryData supplementaryDataUpdated) {
        this.supplementaryData = supplementaryDataUpdated;
        add(linkTo(methodOn(CaseController.class).updateCaseSupplementaryData(caseId, requestSupplementaryData)).withSelfRel());
    }
}

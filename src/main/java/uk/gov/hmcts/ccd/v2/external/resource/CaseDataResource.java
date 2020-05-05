package uk.gov.hmcts.ccd.v2.external.resource;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.hateoas.RepresentationModel;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.v2.external.controller.CaseDataValidatorController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CaseDataResource extends RepresentationModel {

    private JsonNode data;

    public CaseDataResource(@NonNull CaseDataContent caseData, String caseTypeId, String pageId) {
        copyProperties(caseData);

        add(linkTo(methodOn(CaseDataValidatorController.class).validate(caseTypeId, pageId, caseData)).withSelfRel());
    }

    private void copyProperties(CaseDataContent caseData) {
        this.data = JacksonUtils.convertValueJsonNode(caseData.getData().get("data"));
    }
}

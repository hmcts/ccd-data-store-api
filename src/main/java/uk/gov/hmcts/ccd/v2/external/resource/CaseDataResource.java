package uk.gov.hmcts.ccd.v2.external.resource;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.hateoas.ResourceSupport;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.v2.external.controller.CaseDataValidatorController;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CaseDataResource extends ResourceSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonNode data;

    public CaseDataResource(@NonNull CaseDataContent caseData, String caseTypeId, String pageId) {
        copyProperties(caseData);

        add(linkTo(methodOn(CaseDataValidatorController.class).validate(caseTypeId, pageId, caseData)).withSelfRel());
    }

    private void copyProperties(CaseDataContent caseData) {
        this.data = MAPPER.convertValue(caseData.getData().get("data"), JsonNode.class);
    }
}

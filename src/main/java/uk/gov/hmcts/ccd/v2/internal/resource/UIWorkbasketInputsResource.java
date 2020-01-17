package uk.gov.hmcts.ccd.v2.internal.resource;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.hateoas.ResourceSupport;
import uk.gov.hmcts.ccd.domain.model.search.Field;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;
import uk.gov.hmcts.ccd.v2.internal.controller.UIDefinitionController;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UIWorkbasketInputsResource extends ResourceSupport {

    @Data
    @NoArgsConstructor
    public class UIWorkbasketInput {
        private String label;
        private int order;
        private Field field;
    }

    private UIWorkbasketInput[] workbasketInputs;

    public UIWorkbasketInputsResource(WorkbasketInput[] workbasketInputs, String caseTypeId) {
        copyProperties(workbasketInputs);

        add(linkTo(methodOn(UIDefinitionController.class).getWorkbasketInputsDetails(caseTypeId)).withSelfRel());
    }

    private void copyProperties(WorkbasketInput[] workbasketInputs) {
        this.workbasketInputs = Arrays.stream(workbasketInputs)
            .map(this::buildUIWorkbasketInput)
            .collect(Collectors.toList()).toArray(new UIWorkbasketInput[]{});
    }

    private UIWorkbasketInput buildUIWorkbasketInput(WorkbasketInput workbasketInput) {
        UIWorkbasketInput uiWorkbasketInput = new UIWorkbasketInput();
        uiWorkbasketInput.setField(workbasketInput.getField());
        uiWorkbasketInput.setLabel(workbasketInput.getLabel());
        uiWorkbasketInput.setOrder(workbasketInput.getOrder());
        return uiWorkbasketInput;
    }
}

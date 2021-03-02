package uk.gov.hmcts.ccd.v2.internal.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.hateoas.RepresentationModel;
import uk.gov.hmcts.ccd.domain.model.search.Field;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;
import uk.gov.hmcts.ccd.v2.internal.controller.UIDefinitionController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class WorkbasketInputsViewResource extends RepresentationModel {

    @Data
    @NoArgsConstructor
    public static class WorkbasketInputView {
        private String label;
        private int order;
        private Field field;
        @JsonProperty("display_context_parameter")
        private String displayContextParameter;
    }

    private WorkbasketInputView[] workbasketInputs;

    public WorkbasketInputsViewResource(WorkbasketInput[] workbasketInputs, String caseTypeId) {
        copyProperties(workbasketInputs);

        add(linkTo(methodOn(UIDefinitionController.class).getWorkbasketInputsDetails(caseTypeId)).withSelfRel());
    }

    private void copyProperties(WorkbasketInput[] workbasketInputs) {
        this.workbasketInputs = Arrays.stream(workbasketInputs)
            .map(this::buildUIWorkbasketInput)
            .collect(Collectors.toList()).toArray(new WorkbasketInputView[]{});
    }

    private WorkbasketInputView buildUIWorkbasketInput(WorkbasketInput workbasketInput) {
        WorkbasketInputView workbasketInputView = new WorkbasketInputView();
        workbasketInputView.setField(workbasketInput.getField());
        workbasketInputView.setLabel(workbasketInput.getLabel());
        workbasketInputView.setOrder(workbasketInput.getOrder());
        workbasketInputView.setDisplayContextParameter(workbasketInput.getDisplayContextParameter());
        return workbasketInputView;
    }
}

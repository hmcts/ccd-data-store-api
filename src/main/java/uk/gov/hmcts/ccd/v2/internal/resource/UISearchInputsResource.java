package uk.gov.hmcts.ccd.v2.internal.resource;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.ccd.domain.model.search.Field;
import uk.gov.hmcts.ccd.domain.model.search.SearchInput;
import uk.gov.hmcts.ccd.v2.internal.controller.UIDefinitionController;

import java.util.Arrays;
import java.util.stream.Collectors;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.hateoas.RepresentationModel;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UISearchInputsResource extends RepresentationModel {

    private UISearchInput[] searchInputs;

    @Data
    @NoArgsConstructor
    public static class UISearchInput {
        private String label;
        private int order;
        private Field field;
        @JsonProperty("display_context_parameter")
        private String displayContextParameter;
    }

    public UISearchInputsResource(SearchInput[] searchInputs, String caseTypeId) {
        copyProperties(searchInputs);

        add(linkTo(methodOn(UIDefinitionController.class).getSearchInputsDetails(caseTypeId)).withSelfRel());
    }

    private void copyProperties(SearchInput[] searchInputs) {
        this.searchInputs = Arrays.stream(searchInputs)
            .map(this::buildUISearchInput)
            .collect(Collectors.toList()).toArray(new UISearchInput[]{});
    }

    private UISearchInput buildUISearchInput(SearchInput searchInput) {
        UISearchInput uiSearchInput = new UISearchInput();
        uiSearchInput.setField(searchInput.getField());
        uiSearchInput.setLabel(searchInput.getLabel());
        uiSearchInput.setOrder(searchInput.getOrder());
        uiSearchInput.setDisplayContextParameter(searchInput.getDisplayContextParameter());
        return uiSearchInput;
    }
}

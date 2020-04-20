package uk.gov.hmcts.ccd.v2.internal.resource;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

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
public class SearchInputsViewResource extends RepresentationModel {

    private SearchInputView[] searchInputs;

    @Data
    @NoArgsConstructor
    public static class SearchInputView {
        private String label;
        private int order;
        private Field field;
    }

    public SearchInputsViewResource(SearchInput[] searchInputs, String caseTypeId) {
        copyProperties(searchInputs);

        add(linkTo(methodOn(UIDefinitionController.class).getSearchInputsDetails(caseTypeId)).withSelfRel());
    }

    private void copyProperties(SearchInput[] searchInputs) {
        this.searchInputs = Arrays.stream(searchInputs)
            .map(this::buildUISearchInput)
            .collect(Collectors.toList()).toArray(new SearchInputView[]{});
    }

    private SearchInputView buildUISearchInput(SearchInput searchInput) {
        SearchInputView searchInputView = new SearchInputView();
        searchInputView.setField(searchInput.getField());
        searchInputView.setLabel(searchInput.getLabel());
        searchInputView.setOrder(searchInput.getOrder());
        return searchInputView;
    }
}

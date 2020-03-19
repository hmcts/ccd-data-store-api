package uk.gov.hmcts.ccd.v2.internal.resource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.SearchInputBuilder.aSearchInput;

import org.springframework.hateoas.Link;
import uk.gov.hmcts.ccd.domain.model.search.SearchInput;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UISearchInputsResourceTest {
    private static final String CASE_TYPE_ID = "caseTypeId";
    private static final String LINK_SELF = String.format("/internal/case-types/%s/search-inputs", CASE_TYPE_ID);
    private SearchInput searchInput1 = aSearchInput().withFieldId("field1").build();
    private SearchInput searchInput2 = aSearchInput().withFieldId("field2").build();
    private SearchInput[] searchInputs = new SearchInput[]{searchInput1, searchInput2};

    @Test
    @DisplayName("should copy search inputs")
    void shouldCopySearchInputs() {
        final UISearchInputsResource resource = new UISearchInputsResource(searchInputs, CASE_TYPE_ID);

        List<UISearchInputsResource.UISearchInput> workbasketInputs = Lists.newArrayList(resource.getSearchInputs());
        assertAll(
            () -> assertThat(resource.getSearchInputs(), not(sameInstance(this.searchInputs))),
            () -> assertThat(workbasketInputs, hasItems(hasProperty("field", hasProperty("id", is("field1"))),
                                                        hasProperty("field", hasProperty("id", is("field2")))))
        );
    }

    @Test
    @DisplayName("should link to itself")
    void shouldLinkToSelf() {
        final UISearchInputsResource resource = new UISearchInputsResource(searchInputs, CASE_TYPE_ID);

        Optional<Link> self = resource.getLink("self");
        assertThat(self.get().getHref(), equalTo(LINK_SELF));
    }

}

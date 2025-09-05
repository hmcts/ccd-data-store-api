package uk.gov.hmcts.ccd.v2.internal.resource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COLLECTION;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.SearchInputBuilder.aSearchInput;
import static uk.gov.hmcts.ccd.util.FieldTestUtil.field;
import static uk.gov.hmcts.ccd.util.FieldTestUtil.simpleField;

import org.springframework.hateoas.Link;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.search.Field;
import uk.gov.hmcts.ccd.domain.model.search.SearchInput;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil;
import uk.gov.hmcts.ccd.util.FieldTestUtil;
import uk.gov.hmcts.ccd.util.PathFromUrlUtil;

class SearchInputsViewResourceTest {
    private static final String CASE_TYPE_ID = "caseTypeId";
    private static final String LINK_SELF = String.format("/internal/case-types/%s/search-inputs", CASE_TYPE_ID);
    private SearchInput searchInput1 = aSearchInput().withFieldId("field1").build();
    private SearchInput searchInput2 = aSearchInput().withFieldId("field2").build();
    private SearchInput[] searchInputs = new SearchInput[]{searchInput1, searchInput2};

    @Test
    @DisplayName("should copy search inputs")
    void shouldCopySearchInputs() {
        final SearchInputsViewResource resource = new SearchInputsViewResource(searchInputs, CASE_TYPE_ID);

        List<SearchInputsViewResource.SearchInputView> workbasketInputs =
                Lists.newArrayList(resource.getSearchInputs());
        assertAll(
            () -> assertThat(resource.getSearchInputs(), not(sameInstance(this.searchInputs))),
            () -> assertThat(workbasketInputs, hasItems(hasProperty("field", hasProperty("id", is("field1"))),
                                                        hasProperty("field", hasProperty("id", is("field2")))))
        );
    }

    @Test
    @DisplayName("should link to itself")
    void shouldLinkToSelf() {
        final SearchInputsViewResource resource = new SearchInputsViewResource(searchInputs, CASE_TYPE_ID);

        Optional<Link> self = resource.getLink("self");
        assertThat(PathFromUrlUtil.getActualPath(self.get().getHref()), equalTo(LINK_SELF));
    }

    @Test
    @DisplayName("should workbasket inputs with complex and Collection field")
    void shouldWorkbasketInputsWithComplexCollectionField() {

        TestBuildersUtil.FieldTypeBuilder fieldTypeBuilder = aFieldType();
        fieldTypeBuilder.withType(COLLECTION);
        fieldTypeBuilder.withComplexField(simpleField("Three", 1));
        fieldTypeBuilder.withComplexField(simpleField("One", 2));
        fieldTypeBuilder.withComplexField(simpleField("Two", 3));
        FieldTypeDefinition collectionComplexFieldTypeDefinition = fieldTypeBuilder
            .build();

        Field field = field(FieldTestUtil.COLLECTION_FIELD, aFieldType()
            .withType(COLLECTION)
            .withCollectionFieldType(collectionComplexFieldTypeDefinition)
            .build());
        SearchInput searchInput3 = aSearchInput().withField(field).build();

        Field field1 = field(FieldTestUtil.COMPLEX_FIELD, aFieldType().withId(FieldTestUtil.COMPLEX_FIELD)
            .withType(FieldTypeDefinition.COMPLEX).withComplexField(
                newCaseField().withId("OtherNestedField")
                    .withFieldType(FieldTestUtil.fieldType("Date")).build()).build());
        SearchInput searchInput4 = aSearchInput().withField(field1).build();

        SearchInput[] searchInputs1 = new SearchInput[]{
            searchInput3, searchInput4
        };
        final SearchInputsViewResource resource = new SearchInputsViewResource(searchInputs1, CASE_TYPE_ID);

        List<SearchInputsViewResource.SearchInputView> searchInputs =
            Lists.newArrayList(resource.getSearchInputs());
        assertAll(
            () -> assertThat(resource.getSearchInputs(), not(sameInstance(this.searchInputs))),
            () -> assertThat(searchInputs, hasItems(hasProperty("field",
                    hasProperty("id", is(FieldTestUtil.COLLECTION_FIELD))),
                hasProperty("field", hasProperty("id", is(FieldTestUtil.COMPLEX_FIELD)))))
        );
    }
}

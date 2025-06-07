package uk.gov.hmcts.ccd.domain.model.search.elasticsearch;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@AllArgsConstructor
@Schema(description = "Definition of a case type in the context of the search")
public class SearchResultViewHeaderGroup {

    @NonNull
    @Schema(description = "Metadata for the case type")
    private HeaderGroupMetadata metadata;
    @NonNull
    @Schema(description = "Definition of the fields for the case type")
    private List<SearchResultViewHeader> fields;
    @NonNull
    @Schema(description = "Case references for the case type that are returned in the search")
    private List<String> cases;
}

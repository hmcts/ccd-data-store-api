package uk.gov.hmcts.ccd.domain.model.search.elasticsearch;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NonNull;

import java.util.List;

@Data
@ApiModel(description = "Definition of a case type in the context of the search")
public class UICaseSearchHeader {

    @NonNull
    @ApiModelProperty(value = "Metadata for the case type")
    private UICaseSearchHeaderMetadata metadata;
    @NonNull
    @ApiModelProperty(value = "Definition of the fields for the case type")
    private List<SearchResultViewColumn> fields;
    @NonNull
    @ApiModelProperty(value = "Case references for the case type that are returned in the search")
    private List<String> cases;
}

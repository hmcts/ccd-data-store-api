package uk.gov.hmcts.ccd.domain.model.search.elasticsearch;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;

@Data
public class UICaseSearchHeaderMetadata {

    @NonNull
    private String jurisdiction;
    @NonNull
    @JsonProperty("case_type_id")
    private String caseTypeId;
}

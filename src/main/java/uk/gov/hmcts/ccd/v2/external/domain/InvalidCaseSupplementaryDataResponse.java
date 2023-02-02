package uk.gov.hmcts.ccd.v2.external.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Builder
@Data
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class InvalidCaseSupplementaryDataResponse implements Serializable {

    private static final long serialVersionUID = -3230526610598211439L;

    @JsonProperty("invalidCases")
    private List<InvalidCaseSupplementaryDataItem> dataItems;
}

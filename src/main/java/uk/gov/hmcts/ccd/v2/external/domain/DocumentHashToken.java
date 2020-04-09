package uk.gov.hmcts.ccd.v2.external.domain;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
@Builder
public class DocumentHashToken implements Serializable {

    @JsonProperty("id")
    private String id;

    @JsonProperty("hashToken")
    private String hashToken;

}

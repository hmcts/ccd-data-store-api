package uk.gov.hmcts.ccd.v2.external.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Optional;

@Data
@EqualsAndHashCode
@NoArgsConstructor
public class InvalidCaseSupplementaryDataRequest implements Serializable {
    private static final long serialVersionUID = -4161503964592124374L;

    @JsonProperty("date_from")
    private LocalDateTime dateFrom;

    @JsonProperty("date_to")
    private LocalDateTime dateTo;

    @JsonProperty("limit")
    private Integer limit;

    @JsonProperty("search_ras")
    private Boolean searchRas;

    public Optional<LocalDateTime> getDateTo() {
        return Optional.ofNullable(dateTo);
    }
}

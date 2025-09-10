package uk.gov.hmcts.ccd.data.persistence.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // For forwards compatibility with future fields
public class DecentralisedCaseEvent {

    private CaseDetails caseDetailsBefore;
    private CaseDetails caseDetails;
    private DecentralisedEventDetails eventDetails;
    /**
     * The case_data.id column is needed for decentralised services to perform ElasticSearch indexing since it serves
     * as the unique identifier on ES indexes.
     * We provide it here since the existing CaseDetails type by design omits it.
     */
    private Long internalCaseId;
}

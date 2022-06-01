package uk.gov.hmcts.ccd.domain.model.casefileview;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Document {
    String documentURL;
    String documentFilename;
    String documentBinaryURL;
    String attributePath;
    LocalDateTime uploadTimestamp;
}

package uk.gov.hmcts.ccd.domain.model.casefileview;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "Document definition")
public class Document {
    String documentURL;
    String documentFilename;
    String documentBinaryURL;
    String attributePath;
    LocalDateTime uploadTimestamp;
}

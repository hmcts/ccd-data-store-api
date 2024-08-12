package uk.gov.hmcts.ccd.domain.model.casefileview;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.annotations.ApiModel;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@ApiModel(value = "Document definition")
public class Document {
    String documentURL;
    String documentFilename;
    String documentBinaryURL;
    String attributePath;
    LocalDateTime uploadTimestamp;
}

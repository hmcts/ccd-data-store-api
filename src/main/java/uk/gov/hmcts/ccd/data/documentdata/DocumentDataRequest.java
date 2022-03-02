package uk.gov.hmcts.ccd.data.documentdata;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class DocumentDataRequest {

    String attributePath;

    Integer caseVersion;

    String categoryId;
}

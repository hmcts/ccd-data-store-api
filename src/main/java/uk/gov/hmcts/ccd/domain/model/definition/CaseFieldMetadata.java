package uk.gov.hmcts.ccd.domain.model.definition;

import lombok.Value;

@Value
public class CaseFieldMetadata {
    String path;
    String categoryId;
}

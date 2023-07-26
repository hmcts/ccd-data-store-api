package uk.gov.hmcts.ccd.domain.model.common;

import lombok.Value;

@Value
public class CaseFieldMetadata {
    String path;
    String categoryId;
}

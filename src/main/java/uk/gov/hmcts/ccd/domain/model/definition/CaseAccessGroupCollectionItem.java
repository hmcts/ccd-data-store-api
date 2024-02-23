package uk.gov.hmcts.ccd.domain.model.definition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CaseAccessGroupCollectionItem {

    private String id;
    private CaseAccessGroup value;

}

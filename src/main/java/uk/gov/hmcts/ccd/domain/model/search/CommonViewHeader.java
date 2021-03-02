package uk.gov.hmcts.ccd.domain.model.search;

import uk.gov.hmcts.ccd.domain.model.common.CommonDCPModel;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

public interface CommonViewHeader extends CommonDCPModel {

    String getCaseFieldId();

    FieldTypeDefinition getCaseFieldTypeDefinition();

    boolean isMetadata();
}

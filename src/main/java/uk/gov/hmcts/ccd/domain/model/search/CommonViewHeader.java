package uk.gov.hmcts.ccd.domain.model.search;

import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

public interface CommonViewHeader {

    String getCaseFieldId();

    FieldTypeDefinition getCaseFieldTypeDefinition();

    boolean isMetadata();

    String getDisplayContextParameter();
}

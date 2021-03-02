package uk.gov.hmcts.ccd.domain.model.search;

import java.util.Map;

public interface CommonViewItem {

    String getCaseId();

    Map<String, Object> getFields();

    Map<String, Object> getFieldsFormatted();
}

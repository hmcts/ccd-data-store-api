package uk.gov.hmcts.ccd.domain.types;

import javax.inject.Named;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static java.math.BigDecimal.ONE;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.FixedListItem;

@Named
@Singleton
public class FixedRadioListValidator extends FixedListValidator {
    static final String TYPE_ID = "FixedRadioList";

    @Override
    public BaseType getType() {
        return BaseType.get(TYPE_ID);
    }

}

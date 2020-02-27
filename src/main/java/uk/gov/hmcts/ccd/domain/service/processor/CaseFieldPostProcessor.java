package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventField;
import uk.gov.hmcts.ccd.domain.model.definition.DisplayContext;

import java.util.List;
import java.util.Map;

@Component
public class CaseFieldPostProcessor implements CaseFieldProcessor {

    public static final String DATETIMEDISPLAY_PREFIX = "#DATETIMEDISPLAY(";
    public static final String DATETIMEENTRY_PREFIX = "#DATETIMEENTRY(";

    private final DateTimeFormatParser dateTimeFormatParser;

    @Autowired
    public CaseFieldPostProcessor(DateTimeFormatParser dateTimeFormatParser) {
        this.dateTimeFormatParser = dateTimeFormatParser;
    }

    public void process(Map<String, JsonNode> data, List<CaseEventField> caseEventFields) {
        caseEventFields.forEach(f -> {
            if (f.getDisplayContext() == DisplayContext.COMPLEX.name()) {
                f.getCaseEventFieldComplex();
                // TODO: Recurse
            }

            String displayContextParameter = f.getDisplayContextParameter();
            if (!Strings.isNullOrEmpty(displayContextParameter) && isDateTimeDisplayContextParameter(displayContextParameter)) {
                // TODO: Get the display context parameter value
                JsonNode node = data.get(f.getCaseFieldId());
                String value = node.textValue();

                try {
                    dateTimeFormatParser.parseDateTimeFormat(displayContextParameter, value);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // TODO: Confirm defaults are set/correct
                TextNode newNode = new TextNode(dateTimeFormatParser.convertDateTimeToIso8601(displayContextParameter, value));
                data.replace(f.getCaseFieldId(), newNode);
            }
        });
    }

    private boolean isDateTimeDisplayContextParameter(String displayContextParameter) {
        return displayContextParameter.startsWith(DATETIMEDISPLAY_PREFIX) || displayContextParameter.startsWith(DATETIMEENTRY_PREFIX);
    }



}

package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.model.definition.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class CaseFieldPostProcessor implements CaseFieldProcessor {

    public static final String DATETIMEDISPLAY_PREFIX = "#DATETIMEDISPLAY(";
    public static final String DATETIMEENTRY_PREFIX = "#DATETIMEENTRY(";

    private final DateTimeFormatParser dateTimeFormatParser;

    @Autowired
    public CaseFieldPostProcessor(DateTimeFormatParser dateTimeFormatParser) {
        this.dateTimeFormatParser = dateTimeFormatParser;
    }

    public void process(Map<String, JsonNode> data, List<CaseEventField> caseEventFields, CaseType caseType) {
        List<CaseField> caseFields = caseType.getCaseFields();

        caseEventFields.forEach(f -> {
            if (f.getDisplayContext() == DisplayContext.COMPLEX.name()) {
                f.getCaseEventFieldComplex();
                // TODO: Recurse
            }
            String displayContextParameter = f.getDisplayContextParameter();

            if (!Strings.isNullOrEmpty(displayContextParameter) && isDateTimeDisplayContextParameter(displayContextParameter)) {
                Optional<DisplayContextParameter> param = DisplayContextParameterType.getDisplayContextParameterFor(f.getDisplayContextParameter());

                Optional<CaseField> field = caseType.getCaseField(f.getCaseFieldId());
                field.ifPresent(f2 -> {
                    // TODO: Handle error when parsing - show expected format
                    if (f2.getFieldType().getType().equals(FieldType.DATETIME)) {
                        JsonNode node = data.get(f.getCaseFieldId());
                        String value = node.textValue();

                        try {
                            dateTimeFormatParser.parseDateTimeFormat(param.get().getValue(), value);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        TextNode newNode = new TextNode(dateTimeFormatParser.convertDateTimeToIso8601(param.get().getValue(), value));
                        data.replace(f.getCaseFieldId(), newNode);
                    } else if (f2.getFieldType().getType().equals(FieldType.DATE)) {
                        JsonNode node = data.get(f.getCaseFieldId());
                        String value = node.textValue();

                        try {
                            dateTimeFormatParser.parseDateTimeFormat(param.get().getValue(), value);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        TextNode newNode = new TextNode(dateTimeFormatParser.convertDateToIso8601(param.get().getValue(), value));
                        data.replace(f.getCaseFieldId(), newNode);
                    }
                });
            }
        });
    }

    private boolean isDateTimeDisplayContextParameter(String displayContextParameter) {
        return displayContextParameter.startsWith(DATETIMEDISPLAY_PREFIX) || displayContextParameter.startsWith(DATETIMEENTRY_PREFIX);
    }



}

package uk.gov.hmcts.ccd.domain.model.common;

import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.ccd.domain.model.common.DisplayContextParameterType.getParameterTypeFor;
import static uk.gov.hmcts.ccd.domain.model.common.DisplayContextParameterType.getParameterValueFor;

public class DisplayContextParameter {

    private static final String MULTIPLE_PARAMETERS_STRING = "),";

    private DisplayContextParameterType type;

    private String value;

    public DisplayContextParameter(DisplayContextParameterType type, String value) {
        this.type = type;
        this.value = value;
    }

    public DisplayContextParameterType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public static List<DisplayContextParameter> getDisplayContextParametersFor(String displayContextParameter) {
        List<DisplayContextParameter> displayContextParameterTypeList = new ArrayList<>();
        List<String> displayContextParameters = new ArrayList<>();

        if (Strings.isNullOrEmpty(displayContextParameter)) {
            return Collections.emptyList();
        }
        while (displayContextParameter.contains(MULTIPLE_PARAMETERS_STRING)) {
            displayContextParameters.add(displayContextParameter.substring(0, (displayContextParameter.indexOf(MULTIPLE_PARAMETERS_STRING) + 1)));
            displayContextParameter = displayContextParameter.substring((displayContextParameter.indexOf(MULTIPLE_PARAMETERS_STRING) + 2)).trim();
        }

        displayContextParameters.add(displayContextParameter.trim());

        for (String s : displayContextParameters) {
            Optional<DisplayContextParameterType> type = getParameterTypeFor(s);
            Optional<String> value = getParameterValueFor(s);

            if (type.isPresent() && value.isPresent()) {
                displayContextParameterTypeList.add(new DisplayContextParameter(type.get(), value.get()));
            }

        }
        return displayContextParameterTypeList;
    }
}

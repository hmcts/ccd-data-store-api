package uk.gov.hmcts.ccd.domain.service.processor;

import com.google.common.base.Strings;
import com.hazelcast.util.*;

import java.util.*;

import static uk.gov.hmcts.ccd.domain.service.processor.DisplayContextParameterType.getParameterTypeFor;
import static uk.gov.hmcts.ccd.domain.service.processor.DisplayContextParameterType.getParameterValueFor;

public class DisplayContextParameter {

    private static String MULTIPLE_PARAMETERS_STRING = "),";

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

    public static List<DisplayContextParameter> getDisplayContextParameterFor(String displayContextParameter) {
        List<DisplayContextParameter> displayContextParameterTypeList = new ArrayList<>();
        List<String> displayContextParameters = new ArrayList();

        displayContextParameter = (StringUtil.isNullOrEmpty(displayContextParameter)) ? "" : displayContextParameter;

        while (displayContextParameter.contains(MULTIPLE_PARAMETERS_STRING)) {
            displayContextParameters.add(displayContextParameter.substring(0, (displayContextParameter.indexOf(MULTIPLE_PARAMETERS_STRING) + 1)));
            displayContextParameter = displayContextParameter.substring((displayContextParameter.indexOf(MULTIPLE_PARAMETERS_STRING) + 2)).trim();
        }

        displayContextParameters.add(displayContextParameter.trim());

        for (String s : displayContextParameters) {
            Optional<DisplayContextParameterType> type = getParameterTypeFor(s);
            Optional<String> value = getParameterValueFor(s);

            if (!type.isPresent() || !value.isPresent()) {
                displayContextParameterTypeList.add(new DisplayContextParameter(null, null));
            } else {
                displayContextParameterTypeList.add(new DisplayContextParameter(type.get(), value.get()));
            }

        }
        return displayContextParameterTypeList;
    }

    public static Optional<DisplayContextParameter> getDisplayContextParameterOfType(String displayContextParameter,
                                                                                     DisplayContextParameterType type) {
        return getDisplayContextParameterFor(displayContextParameter).stream()
            .filter(param -> param.getType() == type)
            .findAny();
    }

    public static boolean hasDisplayContextParameterType(String displayContextParameter, DisplayContextParameterType type) {
        return !Strings.isNullOrEmpty(displayContextParameter) &&
            getDisplayContextParameterOfType(displayContextParameter, type)
                .isPresent();
    }
}

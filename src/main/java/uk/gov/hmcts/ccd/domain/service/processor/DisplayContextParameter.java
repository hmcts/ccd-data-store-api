package uk.gov.hmcts.ccd.domain.service.processor;

import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.ccd.domain.service.processor.DisplayContextParameterType.getParameterTypeFor;
import static uk.gov.hmcts.ccd.domain.service.processor.DisplayContextParameterType.getParameterValueFor;

public class DisplayContextParameter {

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

        String[] displayContextParameters = displayContextParameter.split(",");
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
        return Strings.isNullOrEmpty(displayContextParameter) ?
            Optional.empty() :
            getDisplayContextParameterFor(displayContextParameter).stream()
                .filter(param -> param.getType() == type)
                .findAny();
    }

    public static boolean hasDisplayContextParameterType(String displayContextParameter, DisplayContextParameterType type) {
        return !Strings.isNullOrEmpty(displayContextParameter) &&
            getDisplayContextParameterOfType(displayContextParameter, type)
                .isPresent();
    }
}

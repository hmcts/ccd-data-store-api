package uk.gov.hmcts.ccd.domain.service.processor;

import com.google.common.base.Strings;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum DisplayContextParameterType {
    DATETIMEENTRY,
    DATETIMEDISPLAY,
    TABLE,
    LIST;

    private static final Pattern PATTERN = Pattern.compile("#(.+)\\((.+)\\)");
    private static final int TYPE_GROUP = 1;
    private static final int VALUE_GROUP = 2;

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

    public static Optional<DisplayContextParameterType> getParameterTypeFor(String displayContextParameter) {
        Matcher m = PATTERN.matcher(displayContextParameter);
        if (m.matches()) {
            try {
                return Optional.of(valueOf(m.group(TYPE_GROUP)));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public static Optional<String> getParameterValueFor(String displayContextParameter) throws IllegalArgumentException {
        Matcher m = PATTERN.matcher(displayContextParameter);
        if (m.matches() && !Strings.isNullOrEmpty(m.group(VALUE_GROUP))) {
            return Optional.of(m.group(VALUE_GROUP));
        }
        return Optional.empty();
    }
}

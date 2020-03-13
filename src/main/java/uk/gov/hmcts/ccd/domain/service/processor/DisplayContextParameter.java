package uk.gov.hmcts.ccd.domain.service.processor;

import java.util.Optional;

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

    public static Optional<DisplayContextParameter> getDisplayContextParameterOfType(String displayContextParameter,
                                                                                     DisplayContextParameterType type) {
        return DisplayContextParameterType.getDisplayContextParameterFor(displayContextParameter).stream()
            .filter(param -> param.getType() == type)
            .findAny();
    }
}

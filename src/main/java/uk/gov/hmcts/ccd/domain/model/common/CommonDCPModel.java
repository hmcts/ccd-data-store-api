package uk.gov.hmcts.ccd.domain.model.common;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.ccd.domain.model.common.DisplayContextParameter.getDisplayContextParametersFor;

public interface CommonDCPModel {

    String getDisplayContextParameter();

    @JsonIgnore
    default Optional<DisplayContextParameter> getDisplayContextParameter(DisplayContextParameterType type) {
        return getDisplayContextParameters().stream()
            .filter(param -> param.getType() == type)
            .findAny();
    }

    @JsonIgnore
    default List<DisplayContextParameter> getDisplayContextParameters() {
        return getDisplayContextParametersFor(getDisplayContextParameter());
    }

    @JsonIgnore
    default boolean hasDisplayContextParameter(DisplayContextParameterType type) {
        return getDisplayContextParameter(type).isPresent();
    }

    @JsonIgnore
    default Optional<String> getDisplayContextParameterValue(DisplayContextParameterType type) {
        return getDisplayContextParameter(type).map(DisplayContextParameter::getValue);
    }
}

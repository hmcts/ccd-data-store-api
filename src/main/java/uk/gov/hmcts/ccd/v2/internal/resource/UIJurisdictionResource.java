package uk.gov.hmcts.ccd.v2.internal.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.hateoas.RepresentationModel;
import uk.gov.hmcts.ccd.domain.model.aggregated.JurisdictionDisplayProperties;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.v2.internal.controller.UIDefinitionController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UIJurisdictionResource extends RepresentationModel {


    private UIJurisdiction[] jurisdictions;

    public UIJurisdictionResource(JurisdictionDisplayProperties[] displayProperties, String access) {
        copyProperties(displayProperties);

        add(linkTo(methodOn(UIDefinitionController.class).getJurisdictions(access)).withSelfRel());
    }

    public UIJurisdiction[] getJurisdictions() {
        return jurisdictions;
    }

    @Data
    @NoArgsConstructor
    public class UIJurisdiction {
        private String id;
        private String name;
        private String description;
        private List<CaseType> caseTypes = new ArrayList<>();
    }

    private void copyProperties(JurisdictionDisplayProperties[] displayProperties) {
        this.jurisdictions = Arrays.stream(displayProperties)
            .map(this::buildUIJurisdiction)
            .collect(Collectors.toList()).toArray(new UIJurisdiction[]{});
    }

    private UIJurisdiction buildUIJurisdiction(JurisdictionDisplayProperties displayProperties) {
        UIJurisdiction uiJurisdiction = new UIJurisdiction();
        uiJurisdiction.setId(displayProperties.getId());
        uiJurisdiction.setCaseTypes(displayProperties.getCaseTypes());
        uiJurisdiction.setDescription(displayProperties.getDescription());
        uiJurisdiction.setName(displayProperties.getName());
        return uiJurisdiction;
    }

}

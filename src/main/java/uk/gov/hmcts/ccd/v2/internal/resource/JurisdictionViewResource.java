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
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.v2.internal.controller.UIDefinitionController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class JurisdictionViewResource extends RepresentationModel {


    private JurisdictionView[] jurisdictions;

    public JurisdictionViewResource(JurisdictionDisplayProperties[] displayProperties, String access) {
        copyProperties(displayProperties);

        add(linkTo(methodOn(UIDefinitionController.class).getJurisdictions(access)).withSelfRel());
    }

    public JurisdictionView[] getJurisdictions() {
        return jurisdictions;
    }

    @Data
    @NoArgsConstructor
    public class JurisdictionView {
        private String id;
        private String name;
        private String description;
        private List<CaseTypeDefinition> caseTypeDefinitions = new ArrayList<>();
    }

    private void copyProperties(JurisdictionDisplayProperties[] displayProperties) {
        this.jurisdictions = Arrays.stream(displayProperties)
            .map(this::buildUIJurisdiction)
            .collect(Collectors.toList()).toArray(new JurisdictionView[]{});
    }

    private JurisdictionView buildUIJurisdiction(JurisdictionDisplayProperties displayProperties) {
        JurisdictionView jurisdictionView = new JurisdictionView();
        jurisdictionView.setId(displayProperties.getId());
        jurisdictionView.setCaseTypeDefinitions(displayProperties.getCaseTypeDefinitions());
        jurisdictionView.setDescription(displayProperties.getDescription());
        jurisdictionView.setName(displayProperties.getName());
        return jurisdictionView;
    }

}

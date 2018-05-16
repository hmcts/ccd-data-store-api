package uk.gov.hmcts.ccd.domain.model.aggregated;

import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import uk.gov.hmcts.ccd.domain.model.definition.Jurisdiction;

import java.util.ArrayList;
import java.util.List;

public class UserDefault {
    private String id;
    private List<Jurisdiction> jurisdictions;

    @ApiModelProperty(value = "")
    @JsonProperty("work_basket_default_jurisdiction")
    private String workBasketDefaultJurisdiction;

    @ApiModelProperty(value = "")
    @JsonProperty("work_basket_default_case_type")
    private String workBasketDefaultCaseType;

    @ApiModelProperty(value = "")
    @JsonProperty("work_basket_default_state")

    private String workBasketDefaultState;

    public void addJurisdiction(Jurisdiction jurisdiction) {
        if (jurisdictions == null)
            jurisdictions = new ArrayList<>();
        jurisdictions.add(jurisdiction);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Jurisdiction> getJurisdictions() {
        return jurisdictions;
    }

    public void setJurisdictions(List<Jurisdiction> jurisdictions) {
        this.jurisdictions = jurisdictions;
    }

    public String getWorkBasketDefaultJurisdiction() {
        return workBasketDefaultJurisdiction;
    }

    public void setWorkBasketDefaultJurisdiction(final String workBasketDefaultJurisdiction) {
        this.workBasketDefaultJurisdiction = workBasketDefaultJurisdiction;
    }

    public String getWorkBasketDefaultCaseType() {
        return workBasketDefaultCaseType;
    }

    public void setWorkBasketDefaultCaseType(final String workBasketDefaultCaseType) {
        this.workBasketDefaultCaseType = workBasketDefaultCaseType;
    }

    public String getWorkBasketDefaultState() {
        return workBasketDefaultState;
    }

    public void setWorkBasketDefaultState(final String workBasketDefaultState) {
        this.workBasketDefaultState = workBasketDefaultState;
    }

    public List<String> getJurisdictionsId() {
        return this.jurisdictions.stream().map(Jurisdiction::getId).collect(toList());
    }
}
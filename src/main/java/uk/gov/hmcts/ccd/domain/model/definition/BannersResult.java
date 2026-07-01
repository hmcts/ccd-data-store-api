package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;

@Schema
public class BannersResult implements Serializable {

    private List<Banner> banners;

    public BannersResult() {

    }

    public BannersResult(List<Banner> banners) {
        this.banners = banners;
    }

    @Schema
    @JsonProperty("banners")
    public List<Banner> getBanners() {
        return banners;
    }

    public void setBanners(List<Banner> banners) {
        this.banners = banners;
    }
}

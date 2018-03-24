package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

@ApiModel(description = "")
public class Jurisdiction implements Serializable {

    private String id = null;
    private String name = null;
    private String description = null;
    private Date liveFrom = null;
    private Date liveUntil = null;

    /**
     **/
    @ApiModelProperty(required = true, value = "")
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     **/
    @ApiModelProperty(required = true, value = "")
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     **/
    @ApiModelProperty(value = "")
    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     **/
    @ApiModelProperty(value = "")
    @JsonProperty("live_from")
    public Date getLiveFrom() {
        return liveFrom;
    }

    public void setLiveFrom(Date liveFrom) {
        this.liveFrom = liveFrom;
    }

    /**
     **/
    @ApiModelProperty(value = "")
    @JsonProperty("live_until")
    public Date getLiveUntil() {
        return liveUntil;
    }

    public void setLiveUntil(Date liveUntil) {
        this.liveUntil = liveUntil;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Jurisdiction {");

        sb.append("  id: ").append(id).append("");
        sb.append("  name: ").append(name).append("");
        sb.append("  description: ").append(description).append("");
        sb.append("  liveFrom: ").append(liveFrom).append("");
        sb.append("  liveUntil: ").append(liveUntil).append("");
        sb.append("}");
        return sb.toString();
    }
}

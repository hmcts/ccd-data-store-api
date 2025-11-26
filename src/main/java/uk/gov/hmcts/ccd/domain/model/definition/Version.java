package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Date;

@Schema
public class Version implements Serializable, Copyable<Version> {

    private Integer number = null;
    private Date liveFrom = null;
    private Date liveUntil = null;

    @Schema(required = true, description = "Sequantial version number")
    @JsonProperty("number")
    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    /**
     * Date and time from when this version is valid from.
     **/
    @Schema(required = true, description = "Date and time from when this version is valid from")
    @JsonProperty("live_from")
    public Date getLiveFrom() {
        return liveFrom;
    }

    public void setLiveFrom(Date liveFrom) {
        this.liveFrom = liveFrom;
    }

    /**
     * Date and time this version is to be retired.
     **/
    @Schema(description = "Date and time this version is to be retired")
    @JsonProperty("live_until")
    public Date getLiveUntil() {
        return liveUntil;
    }

    public void setLiveUntil(Date liveUntil) {
        this.liveUntil = liveUntil;
    }

    @Override
    public Version createCopy() {
        Version copy = new Version();
        copy.setNumber(this.number);
        copy.setLiveFrom(this.liveFrom != null ? new Date(this.liveFrom.getTime()) : null);
        copy.setLiveUntil(this.liveUntil != null ? new Date(this.liveUntil.getTime()) : null);
        return copy;
    }
}

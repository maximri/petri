package com.wixpress.petri.laboratory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wixpress.petri.experiments.domain.Experiment;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConductibleExperiments {

    private final List<Experiment> experiments;

    public ConductibleExperiments(List<Experiment> experiments){
        this.experiments = experiments;
    }

    @JsonCreator
    public static ConductibleExperiments build(
            @JsonProperty("experiments") List<Experiment> experiments)
    {
        return new ConductibleExperiments(experiments);
    }

    public List<Experiment> getExperiments() {
        return experiments;
    }
}

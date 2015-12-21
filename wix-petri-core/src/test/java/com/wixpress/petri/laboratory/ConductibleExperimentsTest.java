package com.wixpress.petri.laboratory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.wixpress.petri.experiments.domain.Experiment;
import com.wixpress.petri.experiments.jackson.ObjectMapperFactory;
import com.wixpress.petri.laboratory.dsl.ExperimentMakers;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static com.natpryce.makeiteasy.MakeItEasy.an;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;

public class ConductibleExperimentsTest {

    private final ObjectMapper objectMapper = ObjectMapperFactory.makeObjectMapper();

    @Test
    public void canBeSerialized() throws IOException {
        List<Experiment> experiments = ImmutableList.of(an(ExperimentMakers.Experiment).make());
        ConductibleExperiments conductibleExperiments = new ConductibleExperiments(experiments);
        String json = objectMapper.writeValueAsString(conductibleExperiments);
        ConductibleExperiments deSerialized = objectMapper.readValue(json, new TypeReference<ConductibleExperiments>() {
        });
        assertThat(deSerialized.getExperiments(),  is(experiments));
    }

    @Test
    public void canBeDeserializedFromNew() throws IOException {
        String jsonWithUnknownField = "{\"unknown\":0,\"experiments\":[{\"id\":0,\"lastUpdated\":\"2015-12-16T18:21:15.862Z\",\"experimentSnapshot\":{\"key\":\"\",\"fromSpec\":true,\"creationDate\":\"2015-12-16T18:21:15.862Z\",\"description\":\"\",\"startDate\":\"2015-12-16T18:21:15.862Z\",\"endDate\":\"2016-12-16T18:21:15.862Z\",\"groups\":[{\"id\":1,\"chunk\":50,\"value\":\"g1\"},{\"id\":2,\"chunk\":50,\"value\":\"g2\"}],\"scope\":\"\",\"paused\":false,\"name\":\"\",\"creator\":\"\",\"featureToggle\":false,\"originalId\":0,\"linkedId\":0,\"persistent\":true,\"filters\":[],\"onlyForLoggedInUsers\":false,\"comment\":\"\",\"updater\":\"\",\"conductLimit\":0,\"allowedForBots\":false}}]}";
        ConductibleExperiments deSerialized = objectMapper.readValue(jsonWithUnknownField, new TypeReference<ConductibleExperiments>() {
        });
        assertThat(deSerialized.getExperiments(), is(notNullValue()));
        assertThat(deSerialized.getExperiments().get(0).getId(), is(0));
    }

}
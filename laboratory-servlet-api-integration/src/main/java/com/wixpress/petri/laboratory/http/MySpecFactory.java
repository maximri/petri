package com.wixpress.petri.laboratory.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wixpress.framework.cache.registry.GeneratedCollaboratorsRegistrar;
import com.wixpress.framework.cache.registry.RemoteDataFetcherRegistry;
import com.wixpress.framework.cache.report.LoggingRemoteDataFetchingReporter;
import com.wixpress.framework.cache.report.RemoteDataFetchingReporter;
import com.wixpress.framework.cache.spec.AbstractSpecFactory;
import com.wixpress.petri.laboratory.ConductibleExperiments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class MySpecFactory extends AbstractSpecFactory {

    private Logger log = LoggerFactory.getLogger(MySpecFactory.class);


    public MySpecFactory(GeneratedCollaboratorsRegistrar generatedCollaboratorsRegistrar, RemoteDataFetcherRegistry remoteDataFetcherRegistry, ObjectMapper objectMapper, String cacheFolderName) {
        super(generatedCollaboratorsRegistrar, remoteDataFetcherRegistry, objectMapper, cacheFolderName);
    }

    @Override
    public ScheduledExecutorService aScheduler(String namespace) {
        return new ScheduledThreadPoolExecutor(1);
    }

    @Override
    public RemoteDataFetchingReporter aReporter(String namespace) {
        return new LoggingRemoteDataFetchingReporter(log, ConductibleExperiments.class.getSimpleName());
    }

}
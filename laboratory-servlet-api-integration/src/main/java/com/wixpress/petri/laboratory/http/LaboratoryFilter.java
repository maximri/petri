package com.wixpress.petri.laboratory.http;

import com.wixpress.framework.cache.RemoteDataSource;
import com.wixpress.framework.cache.registry.MapBasedRemoteDataFetcherRegistry;
import com.wixpress.framework.cache.spec.SpecFactory;
import com.wixpress.framework.cache.spec.TransientCacheSpec;
import com.wixpress.petri.PetriRPCClient;
import com.wixpress.petri.experiments.domain.FilterTypeIdResolver;
import com.wixpress.petri.laboratory.*;
import com.wixpress.petri.petri.*;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.Properties;
import java.util.concurrent.Executors;

import static com.wixpress.petri.experiments.jackson.ObjectMapperFactory.makeObjectMapper;

/**
 * Created with IntelliJ IDEA.
 * User: sagyr
 * Date: 10/6/14
 * Time: 3:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class LaboratoryFilter implements Filter {

    public static final String PETRI_USER_INFO_STORAGE = "petri_userInfoStorage";
    public static final String PETRI_LABORATORY = "petri_laboratory";
    private final PetriProperties petriProperties = new PetriProperties();

    private TransientCacheSpec<ConductibleExperiments> experimentsTransientCacheSpec;
    private ServerMetricsReporter metricsReporter ;
    private PetriClient petriClient;
    private UserRequestPetriClient userRequestPetriClient;
    private PetriTopology petriTopology;

    public LaboratoryFilter() {
    }

    private static class ByteArrayServletStream extends ServletOutputStream {

        ByteArrayOutputStream baos;

        ByteArrayServletStream(ByteArrayOutputStream baos) {
            this.baos = baos;
        }

        public void write(int param) throws IOException {
            baos.write(param);
        }
    }


    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws ServletException, IOException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) req;
        RequestScopedUserInfoStorage storage = userInfoStorage(httpServletRequest);

        Laboratory laboratory = laboratory(storage);

        httpServletRequest.getSession(true).setAttribute(PETRI_LABORATORY, laboratory);
        httpServletRequest.getSession().setAttribute(PETRI_USER_INFO_STORAGE, storage);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final HttpServletResponseWrapper response = new CachingHttpResponse(resp, new ByteArrayServletStream(baos), new PrintWriter(baos));

        chain.doFilter(req, response);

        final UserInfo userInfo = storage.read();
        final UserInfo originalUserInfo = storage.readOriginal();
        userInfo.saveExperimentState(new CookieExperimentStateStorage(response), originalUserInfo);
        if(petriTopology.isWriteStateToServer()) {
            userInfo.saveExperimentState(new ServerStateExperimentStateStorage(petriClient), originalUserInfo);
        }
        resp.getOutputStream().write(baos.toByteArray());
    }


    private Laboratory laboratory(UserInfoStorage storage) throws MalformedURLException {
        Experiments experiments = new CachedExperiments(new TransientCacheExperimentSource(experimentsTransientCacheSpec.getReadOnlyTransientCache()));
        TestGroupAssignmentTracker tracker = new BILoggingTestGroupAssignmentTracker(new JodaTimeClock());
        ErrorHandler errorHandler = new ErrorHandler() {
            @Override
            public void handle(String message, Throwable cause, ExceptionType exceptionType) {
                cause.printStackTrace();
            }

        };

        return new TrackableLaboratory(experiments, tracker, storage, errorHandler, 50, metricsReporter, userRequestPetriClient, petriTopology);
    }

    private RequestScopedUserInfoStorage userInfoStorage(HttpServletRequest httpServletRequest) {
        return new RequestScopedUserInfoStorage(
                new HttpRequestUserInfoExtractor(
                        httpServletRequest));
    }

    public void destroy() {
        metricsReporter.stopScheduler();
        experimentsTransientCacheSpec.stop();
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        readProperties(filterConfig);

        try {
            petriClient = PetriRPCClient.makeFor(petriTopology.getPetriUrl());
            userRequestPetriClient = PetriRPCClient.makeUserRequestFor(petriTopology.getPetriUrl());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        MySpecFactory mySpecFactory = new MySpecFactory(
                new NopCollaboratorRegistrar(), new MapBasedRemoteDataFetcherRegistry(),
                makeObjectMapper(),
                System.getProperty("java.io.tmpdir")+"gollum/cache/");

        experimentsTransientCacheSpec = mySpecFactory.aSpec(ConductibleExperiments.class, new PetriClientRemoteDataSource());
        experimentsTransientCacheSpec.startRDF();

        startMetricsReporterScheduler(petriTopology.getReportsScheduleTimeInMillis());

        FilterTypeIdResolver.useDynamicFilterClassLoading();
    }



    private class PetriClientRemoteDataSource implements RemoteDataSource<ConductibleExperiments> {
        @Override
        public ConductibleExperiments fetch() throws IOException {
            return new ConductibleExperiments(petriClient.fetchActiveExperiments());
        }
    }

    private void readProperties(FilterConfig filterConfig) {
        String laboratoryConfig = filterConfig.getInitParameter("laboratoryConfig");
        InputStream input = filterConfig.getServletContext().getResourceAsStream(laboratoryConfig);

        Properties p = petriProperties.fromStream(input);

        final String petriUrl = p.getProperty("petri.url");
        //off by default so as not to incur overhead without users being explicitly aware of it
        final Boolean writeStateToServer = Boolean.valueOf(p.getProperty("petri.writeStateToServer", "false"));
        final String reporterInterval = p.getProperty("reporter.interval", "300000");
        petriTopology = new PetriTopology(){

            @Override
            public String getPetriUrl() {
                return petriUrl;
            }

            @Override
            public Long getReportsScheduleTimeInMillis(){
                long scheduleReportInterval = Long.parseLong(reporterInterval);
                return scheduleReportInterval;
            }

            @Override
            public boolean isWriteStateToServer() {return writeStateToServer;}
        };
    }

    private void startMetricsReporterScheduler(Long reportsScheduleTimeInMillis) {
        metricsReporter = new ServerMetricsReporter(petriClient , Executors.newScheduledThreadPool(5), reportsScheduleTimeInMillis);
        metricsReporter.startScheduler();
    }

    private static class CachingHttpResponse extends HttpServletResponseWrapper {
        private final ServletOutputStream sos;
        private final PrintWriter pw;

        public CachingHttpResponse(ServletResponse resp, ServletOutputStream sos, PrintWriter pw) {
            super((HttpServletResponse) resp);
            this.sos = sos;
            this.pw = pw;
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            return sos;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            return pw;
        }
    }

}

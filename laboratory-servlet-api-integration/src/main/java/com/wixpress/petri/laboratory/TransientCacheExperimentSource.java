package com.wixpress.petri.laboratory;

import com.wixpress.framework.cache.DataWithState;
import com.wixpress.framework.cache.ReadOnlyTransientCache;
import com.wixpress.petri.ExperimentsAndState;
import com.wixpress.petri.petri.PetriClient;

import java.net.MalformedURLException;

/**
* Created with IntelliJ IDEA.
* User: sagyr
* Date: 9/2/14
* Time: 12:17 PM
* To change this template use File | Settings | File Templates.
*/
public class TransientCacheExperimentSource implements CachedExperiments.ExperimentsSource {
    private final ReadOnlyTransientCache<ConductibleExperiments> transientCache;

    public TransientCacheExperimentSource(ReadOnlyTransientCache<ConductibleExperiments> transientCache) throws MalformedURLException {
        this.transientCache = transientCache;
    }

    @Override
    public ExperimentsAndState read() {
        DataWithState<ConductibleExperiments> cachedDataWithState = transientCache.readWithState();
        return new ExperimentsAndState(
                cachedDataWithState.data().getExperiments(),
                cachedDataWithState.stale()
        );
    }

}

package org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Default;

@Default
@RequestScoped
public class BSPSampleDataFetcherImpl extends BSPSampleDataFetcher {

    public BSPSampleDataFetcherImpl() {
    }

    /**
     * Container free constructor, need to initialize all dependencies explicitly.
     *
     * @param service   The sample search service to use.
     * @param bspConfig The config object - Thi sis only nullable for tests that don't need to deal with BSP directly.
     */
    public BSPSampleDataFetcherImpl(@Nonnull BSPSampleSearchService service, @Nullable BSPConfig bspConfig) {
        super(service, bspConfig);
    }

    /**
     * New one up using the given service.
     *
     * @param service The search service object.
     */
    public BSPSampleDataFetcherImpl(@Nonnull BSPSampleSearchService service) {
        this(service, null);
    }

}
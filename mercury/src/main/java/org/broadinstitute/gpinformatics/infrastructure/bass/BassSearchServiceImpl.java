/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.bass;

import com.sun.jersey.api.client.Client;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

@Impl
public class BassSearchServiceImpl extends AbstractJerseyClientService implements BassSearchService {
    private final BassConfig bassConfig;

    @Inject
    public BassSearchServiceImpl(BassConfig bassConfig) {
        this.bassConfig = bassConfig;
    }

    @Override
    public List<Map<BassDTO.BassResultColumns, String>> runSearch(Pair<BassDTO.BassResultColumns, String> bassResult) {
        return null;
    }

    @Override
    protected void customizeClient(Client client) {
        specifyHttpAuthCredentials(client, bassConfig);
    }
}

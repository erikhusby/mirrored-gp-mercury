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

package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProject;
import org.broadinstitute.gpinformatics.infrastructure.bioproject.BioProjectList;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.json.JSONException;

import javax.annotation.Nonnull;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

@Dependent
public class BioProjectTokenInput extends TokenInput<BioProject> {
    private BioProjectList bioProjectList;

    public BioProjectTokenInput() {
        this(null);
    }

    @Inject
    public BioProjectTokenInput(BioProjectList bioProjectList) {
        super(DOUBLE_LINE_FORMAT);
        this.bioProjectList = bioProjectList;
    }

    @Override
    protected String getTokenId(BioProject bioProject) {
        return bioProject.getAccession();
    }

    @Override
    protected String getTokenName(BioProject bioProject) {
        return bioProject.getProjectName();
    }

    @Override
    protected String formatMessage(String messageString, BioProject bioProject) {
        return MessageFormat.format(messageString,
                bioProject.getProjectName(),
                String.format("accession: %s alias: %s", bioProject.getAccession(), bioProject.getAlias())
        );
    }

    @Override
    protected BioProject getById(String key) {
        return bioProjectList.getBioProject(key);
    }

    /**
         * Get a list of all BioProjects.
         *
         * @param query the search text string
         * @return list of BioProjects
         * @throws org.json.JSONException
         */
        public String getJsonString(String query) throws JSONException {
            return getJsonString(query, Collections.<BioProject>emptySet());
        }

        /**
         * Get the list of list of BioProjects minus the list of ones to omit.
         *
         * @param query the search text string
         * @param omitProjects list of BioProject to remove from the returned BioProject list
         * @return list of bio projects
         * @throws JSONException
         */
        public String getJsonString(String query, @Nonnull Collection<BioProject> omitProjects) throws JSONException {
            Collection<BioProject> bioProjects = bioProjectList.search(query);
            bioProjects.removeAll(omitProjects);

            return createItemListString(new ArrayList<>(bioProjects));
        }
}

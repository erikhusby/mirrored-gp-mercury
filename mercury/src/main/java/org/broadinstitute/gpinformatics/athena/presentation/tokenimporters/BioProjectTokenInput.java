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

import javax.inject.Inject;
import java.text.MessageFormat;

public class BioProjectTokenInput extends TokenInput<BioProject> {
    private BioProjectList bioProjectList;

    public BioProjectTokenInput() {
        super(SINGLE_LINE_FORMAT);
    }

    @Inject
    public BioProjectTokenInput(BioProjectList bioProjectList) {
        this();
        this.bioProjectList = bioProjectList;
    }

    @Override
    protected String getTokenId(BioProject bioProject) {
        return bioProject.getAccession();
    }

    @Override
    protected String getTokenName(BioProject bioProject) {
        return bioProject.displayName();
    }

    @Override
    protected String formatMessage(String messageString, BioProject bioProject) {
        return MessageFormat.format(messageString, bioProject.displayName());
    }

    @Override
    protected BioProject getById(String key) {
        return bioProjectList.getBioProject(key);
    }
}

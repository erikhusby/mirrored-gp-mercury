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

package org.broadinstitute.gpinformatics.infrastructure.bioproject;

import org.broadinstitute.gpinformatics.infrastructure.jmx.AbstractCache;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionsService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@ApplicationScoped
public class BioProjectList extends AbstractCache implements Serializable {
    private SubmissionsService submissionsService;
    @Inject
    public BioProjectList(SubmissionsService submissionsService) {
        this.submissionsService = submissionsService;
    }

    public BioProjectList() {
    }

    private Map<String, BioProject> bioProjectMap=new HashMap<>();

    @Override
    public synchronized void refreshCache() {
        for (BioProject bioProject : submissionsService.getAllBioProjects()) {
            bioProjectMap.put(bioProject.getAccession(), bioProject);
        }
    }

    public Map<String, BioProject> getBioProjectMap() {
        if (bioProjectMap.isEmpty()){
            refreshCache();
        }
        return bioProjectMap;
    }

    public java.util.Collection<BioProject> getBioProjects() {
        return getBioProjectMap().values();
    }

    public BioProject getBioProject(String testAccessionId) {
        return getBioProjectMap().get(testAccessionId);
    }

    public Collection<BioProject> search(String query) {
        Collection<BioProject> foundList = new HashSet<>();
        for (BioProject bioProject : getBioProjects()) {
            if (bioProject.getAccession().toLowerCase().contains(query.toLowerCase()) ||
                bioProject.getAlias().toLowerCase().contains(query.toLowerCase()) ||
                bioProject.getProjectName().toLowerCase().contains(query.toLowerCase())) {
                foundList.add(bioProject);
            }
        }
        return foundList;
    }
}

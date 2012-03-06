package org.broadinstitute.sequel.entity.bsp;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.broadinstitute.sequel.control.bsp.BSPSampleSearchColumn;
import org.broadinstitute.sequel.control.bsp.BSPSampleSearchService;
import org.broadinstitute.sequel.entity.notice.StatusNote;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.entity.vessel.MolecularState;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.SampleInstanceImpl;
import org.broadinstitute.sequel.entity.analysis.ReadBucket;
import org.broadinstitute.sequel.entity.vessel.MolecularStateImpl;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The basic plan here is to store only the
 * name then
 */
public class BSPSample implements StartingSample {

    private static Log gLog = LogFactory.getLog(BSPSample.class);

    private final String sampleName;

    private Project project;
    
    private String patientId;

    @Inject
    BSPSampleSearchService service;

    /**
     * Is there a distinction in BSP between
     * the name of the sample and the container
     * in which the sample resides?
     * @param sampleName
     */
    public BSPSample(String sampleName,
                     Project p,
                     BSPSampleSearchService bspSearchService) {
        this.sampleName = sampleName;
        this.project = p;
        this.service = bspSearchService;
    }

    /**
     * Fetches all fields live from
     * #service
     */
    private void fetchAllFields() {
        if (service == null) {
            throw new RuntimeException("No BSP service has been declared.");
        }
        else {
            Collection<String> sampleNames = new HashSet<String>();
            sampleNames.add(sampleName);
            // todo query multiple attributes at once for better efficiency.
            // don't just copy paste this!
            List<String[]> results = service.runSampleSearch(sampleNames, BSPSampleSearchColumn.PARTICIPANT_ID);

            if (results == null) {
                throw new RuntimeException("Sample " + sampleName + " not found in BSP");
            }
            if (results.isEmpty()) {
                throw new RuntimeException("Sample " + sampleName + " not found in BSP");
            }
            Set<String> patientIds = new HashSet<String>();
            for (String[] result : results) {
                if (result == null) {
                    throw new RuntimeException("No patient id for sample " + sampleName);
                }
                if (result.length < 1) {
                    throw new RuntimeException("No patient id for sample " + sampleName);
                }
                patientIds.add(result[0]);
            }

            if (patientIds.size() > 1) {
                throw new RuntimeException("Multiple patient ids found for sample " + sampleName);
            }
            patientId = patientIds.iterator().next();
        }
    }

    @Override
    public String getContainerId() {
        return sampleName;
    }

    @Override
    public String getSampleName() {
        return sampleName;
    }

    @Override
    public String getPatientId() {
        if (patientId == null) {
            fetchAllFields();
        }
        return patientId;
    }

    @Override
    public String getOrganism() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void logNote(StatusNote note) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public MolecularState getRootMolecularState() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Project getRootProject() {
        return project;
    }

    @Override
    public Collection<ReadBucket> getRootReadBuckets() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public SampleInstance.GSP_CONTROL_ROLE getRootControlRole() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public SampleInstanceImpl createSampleInstance() {
        return new SampleInstanceImpl(this, SampleInstance.GSP_CONTROL_ROLE.NONE, project, new MolecularStateImpl(), null);
    }

    @Override
    public void setRootProject(Project rootProject) {
        this.project = rootProject;
    }
}

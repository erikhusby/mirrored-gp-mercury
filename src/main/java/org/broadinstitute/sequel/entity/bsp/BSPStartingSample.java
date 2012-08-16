package org.broadinstitute.sequel.entity.bsp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.project.WorkflowDescription;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDataFetcher;
import org.hibernate.envers.Audited;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Transient;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The basic plan here is to store only the
 * name of the sample, and then have
 * a service lookup the real data from bsp.
 */
@Entity
@Audited
@NamedQueries({
        @NamedQuery(
                name = "BSPStartingSample.fetchBySampleName",
                query = "select s from BSPStartingSample s where sampleName = :sampleName"
        )
})
public class BSPStartingSample extends StartingSample {

    private static Log gLog = LogFactory.getLog(BSPStartingSample.class);

    @Transient
    private BSPSampleDTO bspDTO;

    public BSPStartingSample() {}

    /**
     * Is there a distinction in BSP between
     * the name of the sample and the container
     * in which the sample resides?
     * @param sampleName
     * @param  plan
     * @param  bspDTO The DTO fetched from {@link BSPSampleDataFetcher}.
     *                If you're creating lots of these objects, you should probably
     *                do a bulk fetch of all the samples.  Or maybe you don't
     *                want to fetch the BSP data at all unless the user
     *                drills into it.  Either way, you have some decisions to make
     *                about performance.
     */
    public BSPStartingSample(String sampleName,
                             ProjectPlan plan,
                             BSPSampleDTO bspDTO) {
        this(sampleName,plan);
        this.bspDTO = bspDTO;
    }
    
    public BSPStartingSample(String sampleName,
                             ProjectPlan plan) {
        super(sampleName, plan);
    }

    public void setBspDTO(BSPSampleDTO dto) {
        this.bspDTO = dto;
    }

    @Transient
    /**
     * Has the underlying BSP DTO been initialized?
     */
    public boolean hasBSPDTOBeenInitialized() {
        return bspDTO != null;
    }

    @Override
    @Transient
    /**
     * Gets the container id from the underlying
     * BSP DTO.
     */
    public String getContainerId() {
        return getBspDTO().getContainerId();
    }

    @Override
    @Transient
    /**
     * Gets the patient id from the underlying
     * BSP DTO.
     */
    public String getPatientId() {
        return getBspDTO().getPatientId();
    }

    /**
     * Initialize the BSP DTO by calling the BSP web service
     */
    private void initDto() {
        // todo jmt an entity calling a service is ugly, find a better way
        if(bspDTO == null) {
            // todo jmt refactor CDI stuff into a utility
            try {
                InitialContext initialContext = new InitialContext();
                try {
                    BeanManager beanManager = (BeanManager) initialContext.lookup("java:comp/BeanManager");
                    Bean bean = beanManager.getBeans(BSPSampleDataFetcher.class).iterator().next();
                    CreationalContext ctx = beanManager.createCreationalContext(bean);
                    BSPSampleDataFetcher bspSampleSearchService =
                            (BSPSampleDataFetcher) beanManager.getReference(bean, bean.getClass(), ctx);
                    bspDTO = bspSampleSearchService.fetchSingleSampleFromBSP(this.getSampleName());
                } finally {
                    initialContext.close();
                }
            } catch (NamingException e) {
                throw new RuntimeException(e);
            }

        }
    }

    @Override
    @Transient
    /**
     * Gets the organism name from the
     * underlying BSP DTO.
     */
    public String getOrganism() {
        return getBspDTO().getOrganism();
    }

    @Override
    public Set<SampleInstance> getSampleInstances() {
        final Set<SampleInstance> sampleInstances = new LinkedHashSet<SampleInstance>();
        ProjectPlan rootPlan = getRootProjectPlan();
        WorkflowDescription workflow = null;
        if (rootPlan != null) {
            workflow = rootPlan.getWorkflowDescription();
        }
        sampleInstances.add(new SampleInstance(this,null,rootPlan,null,workflow));
        return sampleInstances;
    }

    @Override
    public boolean isAliquotExpected() {
        return true;
    }

    public BSPSampleDTO getBspDTO() {
        initDto();
        return bspDTO;
    }
}

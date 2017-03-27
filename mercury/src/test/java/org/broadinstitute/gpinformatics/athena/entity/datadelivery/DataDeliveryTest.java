package org.broadinstitute.gpinformatics.athena.entity.datadelivery;

import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.search.ConfigurableSearchDao;
import org.broadinstitute.gpinformatics.infrastructure.search.ConfigurableSearchDefinition;
import org.broadinstitute.gpinformatics.infrastructure.search.InfiniumPlateSourceEvaluator;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchDefinitionFactory;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchInstance;
import org.broadinstitute.gpinformatics.infrastructure.search.TraversalEvaluator;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.Criteria;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Prototype entities for Data Delivery
 */
@Test(groups = TestGroups.STANDARD)
public class DataDeliveryTest extends Arquillian {

    @Inject
    private ConfigurableSearchDao configurableSearchDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    public class X extends TraversalEvaluator {

        @Override
        public Set<Object> evaluate(List<? extends Object> rootEntities, SearchInstance searchInstance) {
            return new HashSet<Object>(InfiniumPlateSourceEvaluator.getAllDnaPlateWells(rootEntities));
        }

        @Override
        public List<Object> buildEntityIdList(Set<? extends Object> entities) {
            return null;
        }
    }

    @Test
    public void testX() {
        // Create initial set
        // Search with PDO (UDS? would be valuable to save search values, but traverser would likely be heavily customized ; perhaps not, because of multiple schemas)
        // What are the entities for UDS?  LabVessel?  What if the PDO has just been placed?  What about chip wells, which don't have a LabVessel (only a position)
        // Navigate from PDO to bucket entry, to ponds or chip wells (unlikely to have both for one PDO, but could for RP or SM-ID).  Filter out duplicates as appropriate.
        SearchInstance searchInstance = new SearchInstance();
        String entity = ColumnEntity.LAB_VESSEL.getEntityName();
        ConfigurableSearchDefinition configurableSearchDef = SearchDefinitionFactory.getForEntity(entity);

        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("PDO", configurableSearchDef);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        searchValue.setValues(Collections.singletonList("PDO-9246")); // PDO term alone will give bucket entries, need traverser
        Criteria criteria = configurableSearchDao.buildCriteria(configurableSearchDef, searchInstance);
        List<LabVessel> labVessels = criteria.list();
        Assert.assertEquals(labVessels.size(), 3202);

        X x = new X();
        Set<Object> objectSet = x.evaluate(labVessels, searchInstance);
        Assert.assertEquals(objectSet.size(), 3268); // should be 3302?

        // traverser may need access to PDO or lab batch, but has only lab vessel

        // search yields lab vessels (not metrics?), but these are mapped to DeliveryItems for persistence
        // If there are multiple versions, create all DeliveryItems, and default to not deliver?
        //
        // Yield chip wells and ponds (or specify products in advance?)
        // Need to return placeholder for entries that haven't been analyzed yet
        // Filter duplicates
        // Add to analysis set
        // Fetch metrics (different UDS)
        // Sort? - How to sort duplicates?  Seems most useful to sort them together, but how does that work with paging?
        // Access denormalized data warehouse?
        // Page
        // Choose / exclude from delivery (some may not have metrics yet)
        // Refresh to reflect new duplicates / metrics / versions?

        // Can't search / sort by columns that are in BSP / Metrics.  Could cache / refresh, this is the end of the process.
        // Volume, concentration

        // Traversal + multiple schemas adds huge increase in complexity.
        // Would be much easier to operate on denormalized data, but that's a huge change in philosophy.
        // Set caching / refreshing (or read-only Google Sheet) is a hybrid approach, but there's some risk of delivering incorrect data!
        //   Not clear how to sort duplicates even with Google Sheet!
        // Need to show everything already delivered, and show new versions that could be delivered to same workspace!

        // Difference between creating set, updating set for new vessels (arrays rework), updating set for new verions (topoffs)?
/*
    Dimensions:
        starting point (RP, PDO, sample, vessel)
        metrics point (pond, chip well) more metrics points could appear over time (particularly for arrays)
        versions ()
 */
/*
    Create set
    Persist Search (how to version search?  future release?)
    Traverse to vessels (filter duplicates) (placeholder if no metrics yet) (if filtering duplicates, how to replace an earlier vessel (only if not delivered) that is now filtered?)
    Fetch versions (if any)
    Add new to the set, including placeholder if no metrics yet
    Deselect with reason
    Specify bucket at set or item?
 */
    }
}

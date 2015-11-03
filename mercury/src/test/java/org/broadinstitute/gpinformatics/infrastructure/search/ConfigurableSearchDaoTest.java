package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.Criteria;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Test searches.
 */
@Test(groups = TestGroups.STANDARD)
public class ConfigurableSearchDaoTest extends Arquillian {

    @Inject
    private ConfigurableSearchDao configurableSearchDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    public void testLcset() {
        ConfigurableSearchDefinition configurableSearchDefinition =
                SearchDefinitionFactory.getForEntity(ColumnEntity.LAB_VESSEL.getEntityName());

        SearchInstance searchInstance = new SearchInstance();
        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("LCSET", configurableSearchDefinition);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        searchValue.setValues(Collections.singletonList("LCSET-5332"));
        Criteria criteria = configurableSearchDao.buildCriteria(configurableSearchDefinition, searchInstance);
        @SuppressWarnings("unchecked")
        List<LabVessel> list = criteria.list();
        Assert.assertEquals(list.size(), 15);
    }

    public void testLabel() {
        ConfigurableSearchDefinition configurableSearchDefinition =
                SearchDefinitionFactory.getForEntity( ColumnEntity.LAB_VESSEL.getEntityName());

        SearchInstance searchInstance = new SearchInstance();
        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("Barcode", configurableSearchDefinition);
        searchValue.setOperator(SearchInstance.Operator.IN);
        searchValue.setValues(Arrays.asList("0159877302", "0159877312", "0159877313", "0163049566"));
        Criteria criteria = configurableSearchDao.buildCriteria(configurableSearchDefinition, searchInstance);
        @SuppressWarnings("unchecked")
        List<LabVessel> list = criteria.list();
        Assert.assertEquals(list.size(), 4);
    }

    /**
     * Tests use of tube formations in an event search by vessel (tube) barcode
     */
    private List<LabEvent> getAllEventsForVessels(){
        ConfigurableSearchDefinition configurableSearchDefinition =
                SearchDefinitionFactory.getForEntity( ColumnEntity.LAB_EVENT.getEntityName());

        SearchInstance searchInstance = new SearchInstance();
        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("Event Vessel Barcode"
                , configurableSearchDefinition);
        searchValue.setOperator(SearchInstance.Operator.IN);
        // Two barcoded tubes and two static plates for a mix of in place vessels and container contents
        //      in source and target destinations
        searchValue.setValues(Arrays.asList("0169690767", "AB51755109", "000007959173", "000005686073"));
        Criteria criteria = configurableSearchDao.buildCriteria(configurableSearchDefinition, searchInstance);
        @SuppressWarnings("unchecked")
        List<LabEvent> list = criteria.list();
        Assert.assertEquals(list.size(), 13);
        return list;
    }

    /**
     * Tests use of tube formations in in-place events and transfer events
     */
    public void testEventMixForVessels(){
        ConfigurableSearchDefinition parentConfigurableSearchDefinition =
                SearchDefinitionFactory.getForEntity( ColumnEntity.LAB_EVENT.getEntityName());

        SearchInstance searchInstance = new SearchInstance();
        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("Event Vessel Barcode"
                , parentConfigurableSearchDefinition);
        searchValue.setOperator(SearchInstance.Operator.EQUALS);
        // This barcoded tube has a mixture of in place events and transfers in containers and not in containers
        //   (validates GPLIM-3471)
        searchValue.setValues(Arrays.asList("0175362333"));

        searchInstance.establishRelationships(parentConfigurableSearchDefinition);

        ConfigurableSearchDefinition alternateConfigurableSearchDefinition =
                searchValue.getSearchTerm().getAlternateSearchDefinition();

        Criteria criteria = configurableSearchDao.buildCriteria(alternateConfigurableSearchDefinition, searchInstance);
        List<LabVessel> vesselList = criteria.list();
        Assert.assertEquals(vesselList.size(), 1);

        // Call alternate search definition traversal evaluator to look up all events for the seed vessel(s)
        Set eventList = searchInstance.getAlternateSearchDefinition().getTraversalEvaluators()
                .entrySet().iterator().next().getValue().evaluate(vesselList, searchInstance);
        Assert.assertEquals(eventList.size(), 12);
    }

    public void testEventVessels() {

        ConfigurableSearchDefinition parentConfigurableSearchDefinition =
                SearchDefinitionFactory.getForEntity( ColumnEntity.LAB_EVENT.getEntityName());

        SearchInstance searchInstance = new SearchInstance();
        SearchInstance.SearchValue searchValue = searchInstance.addTopLevelTerm("Event Vessel Barcode"
                , parentConfigurableSearchDefinition);
        searchValue.setOperator(SearchInstance.Operator.IN);
        // Two barcoded tubes and two static plates for a mix of in place vessels and container contents
        //      in source and target destinations
        searchValue.setValues(Arrays.asList("0169690767", "AB51755109", "000007959173", "000005686073"));

        searchInstance.establishRelationships(parentConfigurableSearchDefinition);

        ConfigurableSearchDefinition alternateConfigurableSearchDefinition =
                searchValue.getSearchTerm().getAlternateSearchDefinition();

        Criteria criteria = configurableSearchDao.buildCriteria(alternateConfigurableSearchDefinition, searchInstance);
        List<LabVessel> vesselList = criteria.list();
        Assert.assertEquals(vesselList.size(), 4);

        // Call alternate search definition traversal evaluator to look up all events for the seed vessel(s)
        Set eventList = searchInstance.getAlternateSearchDefinition().getTraversalEvaluators()
                .entrySet().iterator().next().getValue().evaluate(vesselList, searchInstance);

        // Check denature xfer target vessel is AB51755109 and source vessel is 0169690767
        LabEvent targetFoundEvent = null;
        for( LabEvent allEvent : (Set<LabEvent>) eventList ) {
            if( allEvent.getLabEventId().equals( new Long(689215) ) ) {
                targetFoundEvent = allEvent;
                CherryPickTransfer xfer = allEvent.getCherryPickTransfers().iterator().next();
                LabVessel foundVessel = null;
                for( LabVessel vessel : xfer.getTargetVesselContainer().getContainedVessels() ) {
                    if( vessel.getLabel().equals("AB51755109")) {
                        foundVessel = vessel;
                    }
                }
                // Target lab vessels should match
                Assert.assertNotNull(foundVessel, "Did not find expected target vessel in lab event");
                for( LabVessel vessel : xfer.getSourceVesselContainer().getContainedVessels() ) {
                    if( vessel.getLabel().equals("0169690767")) {
                        foundVessel = vessel;
                    }
                }
                // Target lab vessels should match
                Assert.assertNotNull(foundVessel, "Did not find expected source vessel in lab event");
            }
        }
        // Target lab vessels should match
        Assert.assertNotNull( targetFoundEvent, "Did not find expected lab event for vessel barcode" );

    }
}

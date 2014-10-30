package org.broadinstitute.gpinformatics.infrastructure.search;

import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.hibernate.Criteria;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Test searches.
 */
@Test(groups = TestGroups.STANDARD)
public class ConfigurableSearchDaoTest extends ContainerTest {

    @Inject
    private ConfigurableSearchDao configurableSearchDao;

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
        Assert.assertEquals(list.size(), 14);
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
}

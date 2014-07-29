package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.infrastructure.search.ConfigurableSearchDefinition;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchDefinitionFactory;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class ConfigurableListDbFreeTest {

    @Test
    public void testSimplest() {
        ConfigurableSearchDefinition configurableSearchDefinition =
                new SearchDefinitionFactory().buildLabVesselSearchDef();

        BarcodedTube tube1 = new BarcodedTube("tube1");
        List<ColumnTabulation> columnTabulations = new ArrayList<>();
        columnTabulations.add(configurableSearchDefinition.getSearchTerm("Label"));
        ConfigurableList configurableList = new ConfigurableList(columnTabulations, 1, "ASC", ColumnEntity.LAB_VESSEL);

        List<LabVessel> entityList = new ArrayList<>();
        entityList.add(tube1);
        configurableList.addRows(entityList);

        ConfigurableList.ResultList resultList = configurableList.getResultList();
        Assert.assertEquals(resultList.getResultRows().size(), 1);
        List<Comparable<?>> sortableCells = resultList.getResultRows().get(0).getSortableCells();
        Assert.assertEquals(sortableCells.size(), 1);
        Assert.assertEquals(sortableCells.get(0), tube1.getLabel());
    }
}

package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.infrastructure.search.ConfigurableSearchDefinition;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchDefinitionFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.GenericReagent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Date;
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

    @Test
    public void testReagents() {
        ConfigurableSearchDefinition configurableSearchDefinition =
                new SearchDefinitionFactory().buildLabEventSearchDef();
//
//        BarcodedTube tube1 = new BarcodedTube("tube1");


        List<ColumnTabulation> columnTabulations = new ArrayList<>();
        columnTabulations.add(configurableSearchDefinition.getSearchTerm("LabEventId"));
        columnTabulations.add(configurableSearchDefinition.getSearchTerm("Reagents"));
        ConfigurableList configurableList = new ConfigurableList(columnTabulations, 1, "ASC", ColumnEntity.LAB_VESSEL);
//
        List<LabEvent> entityList = new ArrayList<>();
        LabEvent event1 = new LabEvent(LabEventType.A_BASE, new Date(), "SPIDERMAN", 1l, 101l, "Bravo");
        event1.addReagent(new GenericReagent("KAPAAMPKT","14D02A0013"));
        event1.setInPlaceLabVessel(new StaticPlate("000005899473", StaticPlate.PlateType.Eppendorf96 ));
//        <reagent kitType="PEG20NC" barcode="14D16A0009"/>
//        <reagent kitType="ETOH70" barcode="14E07A0029"/>
//        <reagent kitType="10MMTRISH8" barcode="14E16A0017"/>
//        <plate physType="Eppendorf96" barcode="000005899473" section="ALL96" />


        entityList.add(event1);
        configurableList.addRows(entityList);

        ConfigurableList.ResultList resultList = configurableList.getResultList();

        Assert.assertEquals(resultList.getResultRows().size(), 1);
        ConfigurableList.ResultRow row = resultList.getResultRows().get(0);
        // row.getResultLists();   - list of nested table data, etc.

        List<Comparable<?>> sortableCells = resultList.getResultRows().get(0).getSortableCells();
        Assert.assertEquals(sortableCells.size(), 1);
        Assert.assertEquals(sortableCells.get(0), event1.getLabEventId());
    }

}

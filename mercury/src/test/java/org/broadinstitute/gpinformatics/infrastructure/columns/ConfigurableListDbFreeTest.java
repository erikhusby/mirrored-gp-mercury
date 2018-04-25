package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.infrastructure.search.ConfigurableSearchDefinition;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchDefinitionFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.GenericReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Test(groups = TestGroups.DATABASE_FREE)
public class ConfigurableListDbFreeTest {

    @Test(groups= TestGroups.DATABASE_FREE)
    public void testSimplest() {
        ConfigurableSearchDefinition configurableSearchDefinition =
                SearchDefinitionFactory.getForEntity( ColumnEntity.LAB_VESSEL.getEntityName());

        BarcodedTube tube1 = new BarcodedTube("tube1");
        List<ColumnTabulation> columnTabulations = new ArrayList<>();
        columnTabulations.add(configurableSearchDefinition.getSearchTerm("Barcode"));
        ConfigurableList configurableList = new ConfigurableList(columnTabulations, Collections.EMPTY_MAP, 1, "ASC", ColumnEntity.LAB_VESSEL);

        List<LabVessel> entityList = new ArrayList<>();
        entityList.add(tube1);
        // Context not required - BSP user lookup not required
        configurableList.addRows(entityList);

        ConfigurableList.ResultList resultList = configurableList.getResultList();
        Assert.assertEquals(resultList.getResultRows().size(), 1);
        List<Comparable<?>> sortableCells = resultList.getResultRows().get(0).getSortableCells();
        Assert.assertEquals(sortableCells.size(), 1);
        Assert.assertEquals(sortableCells.get(0), tube1.getLabel());
    }

    @Test(groups= TestGroups.DATABASE_FREE)
    public void testReagents() {
        ConfigurableSearchDefinition configurableSearchDefinition =
                SearchDefinitionFactory.getForEntity( ColumnEntity.LAB_EVENT.getEntityName());

        List<ColumnTabulation> columnTabulations = new ArrayList<>();
        columnTabulations.add(configurableSearchDefinition.getSearchTerm("LabEventId"));
        columnTabulations.add(configurableSearchDefinition.getSearchTerm("Lab Event Reagents"));
        ConfigurableList configurableList = new ConfigurableList(columnTabulations, Collections.EMPTY_MAP, 1, "ASC", ColumnEntity.LAB_EVENT);

        List<LabEvent> entityList = new ArrayList<>();
        LabEvent event1 = new LabEvent(LabEventType.A_BASE, new Date(), "SPIDERMAN", 1l, 101l, "Bravo");

        // Have to set PK via reflection!
        injectFakeID ( event1.getClass(), event1, "labEventId",  new Long(666) );

        // Expiration date for all reagents
        Calendar nextMonth = Calendar.getInstance();
        nextMonth.add(Calendar.MONTH, 1);

        Reagent reagent1 = new GenericReagent("KAPAAMPKT","14D02A0013", nextMonth.getTime());
        injectFakeID(reagent1.getClass().getSuperclass(), reagent1, "reagentId", new Long(999));
        event1.addReagent(reagent1);

        event1.setInPlaceLabVessel(new StaticPlate("000005899473", StaticPlate.PlateType.Eppendorf96 ));
//        <reagent kitType="PEG20NC" barcode="14D16A0009"/>
//        <reagent kitType="ETOH70" barcode="14E07A0029"/>
//        <reagent kitType="10MMTRISH8" barcode="14E16A0017"/>
//        <plate physType="Eppendorf96" barcode="000005899473" section="ALL96" />


        entityList.add(event1);
        // Context not required - BSP user lookup not required
        configurableList.addRows(entityList);

        // Verify parent row
        ConfigurableList.ResultList resultList = configurableList.getResultList();
        Assert.assertEquals(resultList.getResultRows().size(), 1);
        List<String> cellValues = resultList.getResultRows().get(0).getRenderableCells();
        Assert.assertEquals(cellValues.size(), 1);
        Assert.assertEquals(cellValues.get(0), event1.getLabEventId().toString());

        // Lab event parent row should have sortable eventID value
        List<Comparable<?>> cellSortValues = resultList.getResultRows().get(0).getSortableCells();
        Assert.assertEquals(cellSortValues.size(), 1);
        Long value = (Long) cellSortValues.get(0);
        Assert.assertEquals( value.compareTo(new Long(666)), 0);

        // Verify nested table
        ConfigurableList.ResultRow row = resultList.getResultRows().get(0);
        Map<String, ConfigurableList.ResultList> nestedTables = row.getNestedTables();

        // A single nested table
        Assert.assertEquals( nestedTables.size(), 1 );
        // Hardcoded in SearchDefinitionFactory
        Assert.assertTrue(nestedTables.containsKey("Lab Event Reagents"));

        ConfigurableList.ResultList nestedTable = nestedTables.get("Lab Event Reagents");
        Assert.assertEquals(nestedTable.getResultRows().size(), 1 );
        // Hardcoded in SearchDefinitionFactory
        Assert.assertEquals(nestedTable.getHeaders().get(0).getViewHeader(), "Reagent Type" );
        Assert.assertEquals(nestedTable.getHeaders().get(1).getViewHeader(), "Reagent Lot" );
        Assert.assertEquals(nestedTable.getHeaders().get(2).getViewHeader(), "Reagent Expiration" );

        Assert.assertEquals(nestedTable.getResultRows().get(0).getRenderableCells().get(0), "KAPAAMPKT" );
        Assert.assertEquals(nestedTable.getResultRows().get(0).getRenderableCells().get(1), "14D02A0013" );
        Assert.assertEquals(nestedTable.getResultRows().get(0)
                .getRenderableCells().get(2), ColumnValueType.DATE.format(nextMonth.getTime(), null ) );

        // **** Test multiple rows ****
        Reagent reagent2 = new GenericReagent("PEG20NC","14D16A0009", nextMonth.getTime());
        //injectFakeID ( reagent2.getClass().getSuperclass(), reagent2, "reagentId",  new Long(998) );
        event1.addReagent(reagent2);

        Reagent reagent3 = new GenericReagent("ETOH70","14E07A0029", nextMonth.getTime());
        //injectFakeID ( reagent3.getClass().getSuperclass(), reagent3, "reagentId",  new Long(999) );
        event1.addReagent(reagent3);

        // Rebuild ConfigurableList
        configurableList = new ConfigurableList(columnTabulations, Collections.EMPTY_MAP, 1, "ASC", ColumnEntity.LAB_EVENT);
        configurableList.addRows(entityList);
        resultList = configurableList.getResultList();
        // Verify nested table
        row = resultList.getResultRows().get(0);
        nestedTables = row.getNestedTables();
        nestedTable = nestedTables.get("Lab Event Reagents");
        Assert.assertEquals(nestedTable.getResultRows().size(), 3 );

    }

    private void injectFakeID ( Class clazz, Object target, String field, Long idVal ) {
        try {
            Field idf = clazz.getDeclaredField(field);
            idf.setAccessible(true);
            idf.set(target, idVal);
        } catch (Exception ex ) {throw new RuntimeException( "Reflection failure", ex); }
    }

}

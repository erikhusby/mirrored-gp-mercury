package org.broadinstitute.sequel.control.pass;


import org.apache.commons.beanutils.BeanUtils;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleSearchServiceImpl;
import org.broadinstitute.sequel.infrastructure.bsp.QABSPConnectionParameters;
import org.broadinstitute.sequel.presentation.pass.PassSample;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PassBSPSampleSearchService {


    // hardwiring this to the production impl for now given the questionable @Default on a Mock impl in src/main
    // plus the explicit specification of a QA instance in the integration test.  we really need to figure out how to
    // plumb this stuff
    BSPSampleSearchServiceImpl bspSampleSearchService = new BSPSampleSearchServiceImpl(new QABSPConnectionParameters());


    public void lookupSampleDataInBSP(List<PassSample> samples) {

        List<BSPSampleSearchColumn> searchColumns = new ArrayList<BSPSampleSearchColumn>();
        for (PassBSPColumn passColumn : PassBSPColumn.values()) {
            searchColumns.add(passColumn.getBspColumn());
        }


        Map<String, PassSample> sampleIDToSampleMap = new HashMap<String, PassSample>();
        for (PassSample sample : samples) {
            sampleIDToSampleMap.put(sample.getSampleId(), sample);
        }


        List<String[]> resultSetList;


        try {

            resultSetList = bspSampleSearchService.runSampleSearch(sampleIDToSampleMap.keySet(), searchColumns);

            for (String[] resultSet : resultSetList) {

                String sampleID = resultSet[0];
                PassSample sample = sampleIDToSampleMap.get(sampleID);
                if (sample != null) {

                    sample.setLookupError(false);

                    PassBSPColumn[] gwtCols = PassBSPColumn.values();

                    // skip 0, that's the sample id
                    for (int i = 1; i < gwtCols.length; i++) {

                        PassBSPColumn gwtCol = gwtCols[i];

                        BeanUtils.setProperty(sample,
                                gwtCol.getPropertyName(), gwtCol.format(resultSet[i]));

                    }
                }
            }

        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (RuntimeException re) {
            if (re.getCause().getClass() == IOException.class) {
                // ignore these; may be caused
            } else throw re;
        }

    }

}

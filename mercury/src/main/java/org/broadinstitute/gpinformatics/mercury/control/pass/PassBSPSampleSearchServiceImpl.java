package org.broadinstitute.gpinformatics.mercury.control.pass;


import org.apache.commons.beanutils.BeanUtils;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.mercury.presentation.pass.PassSample;

import javax.inject.Inject;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


// Do not annotate as @Impl, that is a @Stereotype synonym for @Alternative and will cause this implementation
// not to be seen by CDI since there currently isn't a producer or stub for this service
// @Impl
public class PassBSPSampleSearchServiceImpl implements PassBSPSampleSearchService {


    @Inject
    private BSPSampleSearchService bspSampleSearchService;


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

        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        catch (RuntimeException re) {
            if (re.getCause().getClass() == IOException.class) {
                // ignore these; may be caused
            } else throw re;
        }

    }

}

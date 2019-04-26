package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPGetExportedSamplesFromAliquots;
import org.broadinstitute.gpinformatics.infrastructure.bsp.exports.IsExported;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class PassingFingerprintPlugin implements ListPlugin {

    // todo jmt passing FP plugin - include technology, date, aliquot ID? (tree?)
    // Call BSP web service, find passing FPs, show most recent
    @Override
    public List<ConfigurableList.Row> getData(List<?> entityList, ConfigurableList.HeaderGroup headerGroup, @Nonnull SearchContext context) {
        List<MercurySample> sampleList = (List<MercurySample>) entityList;
        BSPGetExportedSamplesFromAliquots samplesFromAliquots = ServiceAccessUtility.getBean(
                BSPGetExportedSamplesFromAliquots.class);
        for( MercurySample sample : sampleList ) {
            samplesFromAliquots.getExportedSamplesFromAliquots(Collections.singleton(sample.getSampleKey()), IsExported.ExternalSystem.GAP);
        }
        return null;
    }

    @Override
    public ConfigurableList.ResultList getNestedTableData(Object entity, ColumnTabulation columnTabulation, @Nonnull SearchContext context) {
        throw new UnsupportedOperationException("Method getNestedTableData not implemented in "
                + getClass().getSimpleName() );
    }
}

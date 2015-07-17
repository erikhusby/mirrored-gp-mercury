/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2015 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

public class SampleDataTestFactory {
    public static MercurySample getTestMercurySample(LabVessel.MaterialType sampleMaterialType,
                                                      MercurySample.MetadataSource metadataSource) {
        if (metadataSource == MercurySample.MetadataSource.MERCURY) {
            return new MercurySample("SM-1", ImmutableSet.of(new Metadata(Metadata.Key.MATERIAL_TYPE,
                    sampleMaterialType.getDisplayName())));
        } else {
            return new MercurySample("SM-1", new BspSampleData(
                    ImmutableMap.of(BSPSampleSearchColumn.MATERIAL_TYPE, sampleMaterialType.getDisplayName())));
        }

    }
}

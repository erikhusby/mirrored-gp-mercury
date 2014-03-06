/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.test.dbfree;

import edu.mit.broad.bsp.core.datavo.workrequest.items.kit.PostReceiveOption;
import org.broadinstitute.bsp.client.sample.MaterialInfoDto;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKit;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKitDetail;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.KitType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ProdOrderKitTestFactory {

    public static final KitType kitType = KitType.DNA_MATRIX;
    public static final String bspName = "adsfasdf";
    public static final MaterialInfoDto materialInfoDto = new MaterialInfoDto(kitType.getKitName(), bspName);

    public static ProductOrderKit createDummyProductOrderKit(int maxSamples) {
        Set<ProductOrderKitDetail> originalKitDetailSet = new HashSet<>();
        MaterialInfoDto materialInfoDto = new MaterialInfoDto(kitType.getKitName(), bspName);

        ProductOrderKitDetail kitDetail =
                new ProductOrderKitDetail((long) maxSamples, KitType.DNA_MATRIX, 87L, materialInfoDto);
        kitDetail.getPostReceiveOptions().addAll(Collections.singleton(PostReceiveOption.PICO_RECEIVED));
        kitDetail.setProductOrderKitDetailId(4243L);

        ProductOrderKit orderKit = new ProductOrderKit(33L, 44L);

        originalKitDetailSet.add(kitDetail);

        kitDetail.getPostReceiveOptions().addAll(Collections.singleton(PostReceiveOption.FLUIDIGM_FINGERPRINTING));
        kitDetail.getPostReceiveOptions().addAll(Collections.singleton(PostReceiveOption.BIOANALYZER));
        orderKit.setKitOrderDetails(originalKitDetailSet);
        return orderKit;
    }


}

package org.broadinstitute.gpinformatics.athena.presentation.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderCompletionStatus;
import org.jetbrains.annotations.NotNull;

/**
 */
public class SampleProgressBar {

    public enum Size {
        SMALL, LARGE
    }

    private ProductOrderCompletionStatus status;

    @NotNull
    public static String formatPercentageString(double percentage) {
        String percentageString;
        if (percentage == 0) {
            percentageString = "0";
        } else if (percentage == 1) {
            percentageString = "100";
        } else if (percentage < .01) {
            percentageString = "<1";
        } else if (percentage > .99) {
            percentageString = ">99";
        } else {
            percentageString = String.valueOf(Math.round(percentage * 100));
        }
        return percentageString;
    }

    public static long getPercentForProgressBarWidth(double percentage) {
        if (percentage == 0) {
            return 0;
        } else if (percentage == 1) {
            return 100;
        } else if (percentage < .01) {
            return 1;
        } else if (percentage > .99) {
            return 99;
        }
        return Math.round(percentage * 100);
    }

    public ProductOrderCompletionStatus getStatus() {
        return status;
    }

    public void setStatus(ProductOrderCompletionStatus status) {
        this.status = status;
    }
}

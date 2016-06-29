/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2016 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.deployment;

/**
 * This enum is used to help prevent test-only methods or constructors from being used in Production.
 * <p/>
 * For an example;
 * <br/>
 * actionBean = new ResearchProjectActionBean(NotForProductionUse.I_PROMISE, bspUserList, userBean, broadPiList);
 */
public enum NotForProductionUse {
        I_PROMISE;

        @Override
        public String toString() {
            return "Do Not Use In Pro...duc...tion!!";
        }

        public static void doAgree(NotForProductionUse iAttest) {
            if (iAttest==null) {
                throw new IllegalArgumentException(String.format("Really: %s", NotForProductionUse.I_PROMISE));
            }
        }
}

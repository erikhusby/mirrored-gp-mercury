/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2018 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@Test(groups = TestGroups.DATABASE_FREE)
public class PipelineDataTypeDbFreeTest {
    public void testContains() {
        List<PipelineDataType> pipelineDataTypes =
            Arrays.asList(new PipelineDataType("foo"), new PipelineDataType("bar"));
        assertThat(PipelineDataType.contains(pipelineDataTypes, "bar"), is(true));
    }

    public void testDoesntContain() {
        List<PipelineDataType> pipelineDataTypes =
            Arrays.asList(new PipelineDataType("foo"), new PipelineDataType("bar"));
        assertThat(PipelineDataType.contains(pipelineDataTypes, "baz"), is(false));
    }
}

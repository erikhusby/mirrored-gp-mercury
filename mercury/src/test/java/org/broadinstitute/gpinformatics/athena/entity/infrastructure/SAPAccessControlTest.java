package org.broadinstitute.gpinformatics.athena.entity.infrastructure;

import org.hamcrest.Matchers;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.testng.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class SAPAccessControlTest {

    @Test
    public void testAddingFeatures() throws Exception {

        SAPAccessControl control = new SAPAccessControl();

        Set<AccessItem> featureSet = new HashSet<>() ;
        featureSet.add(new AccessItem("feature1"));
        featureSet.add(new AccessItem("feature2"));
        featureSet.add(new AccessItem("feature3"));



        assertThat(control.getAccessStatus(), is(AccessStatus.ENABLED));
        control.setAccessStatus(AccessStatus.DISABLED);
        assertThat(control.getAccessStatus(), is(control.getAccessStatus()));

        assertThat(control.getDisabledFeatures(), is(Matchers.<AccessItem>empty()));

        control.setDisabledFeatures(featureSet);

        assertThat(control.getDisabledFeatures(), is(equalTo(featureSet)));
        control.addDisabledFeatures("feature3");
        assertThat(control.getDisabledFeatures(), is(equalTo(featureSet)));
        control.addDisabledFeatures("feature4");
        assertThat(control.getDisabledFeatures().size(), is(equalTo(4)));

    }
}
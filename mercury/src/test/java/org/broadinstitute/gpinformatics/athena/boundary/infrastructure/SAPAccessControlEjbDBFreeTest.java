package org.broadinstitute.gpinformatics.athena.boundary.infrastructure;

import org.broadinstitute.gpinformatics.athena.control.dao.infrastructure.SAPAccessControlDao;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.AccessStatus;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.SAPAccessControl;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class SAPAccessControlEjbDBFreeTest {

    private SAPAccessControlDao accessControlDao = Mockito.mock(SAPAccessControlDao.class);
    private SAPAccessControlEjb accessControlEjb = new SAPAccessControlEjb(accessControlDao);

    @BeforeMethod
    public void setUp() throws Exception {

        SAPAccessControl accessControl = new SAPAccessControl();
        accessControl.setAccessStatus(AccessStatus.ENABLED);
        accessControl.setDisabledFeatures(new HashSet<String>(Arrays.<String>asList("Materials", "Other Materials")));

        Mockito.when(accessControlDao.getAccessControl()).thenReturn(accessControl);
    }

    @Test
    public void testRetrieveAccessControl() throws Exception {

        SAPAccessControl accessControl = accessControlEjb.getCurrentControlDefinitions();

        assertThat(accessControl, is(notNullValue()));
        assertThat(accessControl.getAccessStatus(), is(AccessStatus.ENABLED));
        assertThat(accessControl.getDisabledFeatures().size(), is(equalTo(2)));
        assertThat(accessControl.getDisabledFeatures(),containsInAnyOrder("Other Materials","Materials"));
    }

    @Test
    public void testResetControl() throws Exception {


        SAPAccessControl accessControl = new SAPAccessControl();
        accessControl.setAccessStatus(AccessStatus.DISABLED);
        accessControl.setDisabledFeatures(new HashSet<String>(Arrays.<String>asList("Test Materials", "Other Stuff")));

        Mockito.when(accessControlDao.getAccessControl()).thenReturn(accessControl);

        SAPAccessControl control = accessControlEjb.getCurrentControlDefinitions();

        assertThat(control, is(notNullValue()));
        assertThat(control.getAccessStatus(), is(AccessStatus.DISABLED));
        assertThat(control.getDisabledFeatures().size(), is(equalTo(2)));
        assertThat(control.getDisabledFeatures(),containsInAnyOrder("Test Materials","Other Stuff"));

        control = accessControlEjb.resetControlDefinitions();

        assertThat(control, is(notNullValue()));
        assertThat(control.getAccessStatus(), is(AccessStatus.ENABLED));
        assertThat(control.getDisabledFeatures(), is(emptyCollectionOf(String.class)));

    }
}

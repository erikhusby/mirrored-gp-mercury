package org.broadinstitute.gpinformatics.athena.boundary.infrastructure;

import org.broadinstitute.gpinformatics.athena.control.dao.infrastructure.SAPAccessControlDao;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.AccessItem;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.AccessStatus;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.SAPAccessControl;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
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

@Test(groups = TestGroups.DATABASE_FREE)
public class SAPAccessControlEjbDBFreeTest {

    private SAPAccessControlDao accessControlDao = Mockito.mock(SAPAccessControlDao.class);
    private SAPAccessControlEjb accessControlEjb = new SAPAccessControlEjb(accessControlDao);

    @BeforeMethod
    public void setUp() throws Exception {

        SAPAccessControl accessControl = new SAPAccessControl();
        accessControl.setAccessStatus(AccessStatus.ENABLED);
        accessControl.setDisabledItems(new HashSet<AccessItem>(Arrays.<AccessItem>asList(new AccessItem("Materials"), new AccessItem("Other Materials"))));

        Mockito.when(accessControlDao.getAccessControl()).thenReturn(accessControl);
    }

    @Test
    public void testRetrieveAccessControl() throws Exception {

        SAPAccessControl accessControl = accessControlEjb.getCurrentControlDefinitions();

        assertThat(accessControl, is(notNullValue()));
        assertThat(accessControl.getAccessStatus(), is(AccessStatus.ENABLED));
        assertThat(accessControl.getDisabledItems().size(), is(equalTo(2)));
        assertThat(accessControl.getDisabledItems(),containsInAnyOrder(new AccessItem("Other Materials"),new AccessItem("Materials")));
    }

    @Test
    public void testResetControl() throws Exception {


        SAPAccessControl accessControl = new SAPAccessControl();
        accessControl.setAccessStatus(AccessStatus.DISABLED);
        accessControl.setDisabledItems(new HashSet<AccessItem>(Arrays.<AccessItem>asList(new AccessItem("Test Materials"), new AccessItem("Other Stuff"))));

        Mockito.when(accessControlDao.getAccessControl()).thenReturn(accessControl);

        SAPAccessControl control = accessControlEjb.getCurrentControlDefinitions();

        assertThat(control, is(notNullValue()));
        assertThat(control.getAccessStatus(), is(AccessStatus.DISABLED));
        assertThat(control.getDisabledItems().size(), is(equalTo(2)));
        assertThat(control.getDisabledItems(),containsInAnyOrder(new AccessItem("Test Materials"),new AccessItem("Other Stuff")));

        control = accessControlEjb.resetControlDefinitionItems();

        assertThat(control, is(notNullValue()));
        assertThat(control.getAccessStatus(), is(AccessStatus.ENABLED));
        assertThat(control.getDisabledItems(), is(emptyCollectionOf(AccessItem.class)));

    }
}

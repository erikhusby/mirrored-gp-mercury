package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.broadinstitute.gpinformatics.athena.entity.products.GenotypingProductOrderMapping;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.ArchetypeAttribute;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/*
 * Database test for the Infinium Run Resource
 */
public class InfiniumRunResourceContainerTest extends Arquillian {

    @Inject
    private InfiniumRunResource infiniumRunResource;

    @Inject
    private AttributeArchetypeDao attributeArchetypeDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Test
    public void testPdoOverrideDefaultChipValue() throws Exception {
        GenotypingProductOrderMapping mockGenotypingProductOrderMapping1 = mock(GenotypingProductOrderMapping.class);
        final ArchetypeAttribute archetypeAttribute1 = mock(ArchetypeAttribute.class);
        final ArchetypeAttribute archetypeAttribute2 = mock(ArchetypeAttribute.class);

        Set<ArchetypeAttribute> archetypeAttributes = new HashSet<ArchetypeAttribute>() {{
            add(archetypeAttribute1); add(archetypeAttribute2);
        }};

        when(mockGenotypingProductOrderMapping1.getAttributes()).thenReturn(archetypeAttributes);

        when(archetypeAttribute1.getAttributeName()).thenReturn("zcall_threshold_unix");
        when(archetypeAttribute1.getAttributeValue()).thenReturn("zcalloverride");

        when(archetypeAttribute2.getAttributeName()).thenReturn("cluster_location_unix");
        when(archetypeAttribute2.getAttributeValue()).thenReturn("clusteroverride");

        AttributeArchetypeDao spyAttributeArchetypeDao = spy(attributeArchetypeDao);
        doReturn(mockGenotypingProductOrderMapping1).when(mockAttributeArchetypeDao).findGenotypingProductOrderMapping(anyString());
        infiniumRunResource.setAttributeArchetypeDao(mockAttributeArchetypeDao);
        InfiniumRunBean run = infiniumRunResource.getRun("3999582166_R01C01");
        Assert.assertEquals(run.getClusterFilePath(), "clusteroverride");
        Assert.assertEquals(run.getzCallThresholdsPath(), "zcalloverride");
    }
}

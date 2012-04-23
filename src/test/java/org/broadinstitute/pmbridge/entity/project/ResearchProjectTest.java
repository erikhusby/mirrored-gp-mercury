package org.broadinstitute.pmbridge.entity.project;

import org.apache.log4j.Logger;
import org.broad.squid.services.TopicService.*;
import org.broadinstitute.pmbridge.WeldBooter;
import org.broadinstitute.pmbridge.entity.bsp.BSPCollection;
import org.broadinstitute.pmbridge.entity.bsp.BSPCollectionID;
import org.broadinstitute.pmbridge.entity.common.Name;
import org.broadinstitute.pmbridge.entity.common.QuoteId;
import org.broadinstitute.pmbridge.entity.experiments.*;
import org.broadinstitute.pmbridge.entity.experiments.gap.GapExperimentRequest;
import org.broadinstitute.pmbridge.entity.experiments.seq.SeqExperimentRequest;
import org.broadinstitute.pmbridge.entity.person.Person;
import org.broadinstitute.pmbridge.entity.person.RoleType;
import org.broadinstitute.pmbridge.infrastructure.gap.ExperimentPlan;
import org.broadinstitute.pmbridge.infrastructure.squid.SeqConnectionParameters;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/12/12
 * Time: 11:26 AM
 */
public class ResearchProjectTest  extends WeldBooter {

    private static final Logger LOG = Logger.getLogger(ResearchProjectTest.class);

    private SeqConnectionParameters seqConnectionParameters;
    private SquidTopicPortype squidServicePort;


    @BeforeClass
    private void init() throws MalformedURLException {
        seqConnectionParameters = weldUtil.getFromContainer(SeqConnectionParameters.class);
        QName serviceName = new QName(SeqConnectionParameters.SQUID_NAMESPACE, SeqConnectionParameters.SQUID_TOPIC);
        String wsdlURL = seqConnectionParameters.getSquidRoot() + SeqConnectionParameters.SQUID_WSDL;
        URL url = new URL(wsdlURL);
        Service service = Service.create(url, serviceName);
        squidServicePort = service.getPort(serviceName, SquidTopicPortype.class);
    }

    @Test
    public void testResearchProject() throws Exception {
        Date start = new Date();

        Person programMgr = new Person("Erica", "Shefler", "shefler@broad", RoleType.PROGRAM_PM );
        ResearchProject researchProject = new ResearchProject( programMgr,
                new Name("MyResearchProject"), new ResearchProjectId("1"), "To study stuff."  );

        ResearchProject researchProject2 = new ResearchProject( programMgr,
                new Name("MyResearchProject"), new ResearchProjectId("1"), "To study stuff."  );


        //assertReflectionEquals( researchProject, researchProject2);

        //Equal
        Assert.assertTrue(researchProject.equals(researchProject2));
        Assert.assertEquals(researchProject.hashCode(), researchProject2.hashCode());

        // test modification is same as creation
        Assert.assertTrue(researchProject.creation.equals(researchProject.getModification()));

        //Add a collection
        BSPCollection collection1 = new BSPCollection(new BSPCollectionID("12345"), "AlxCollection1");
        researchProject.addBSPCollection(collection1);

        //No longer equal
        Assert.assertFalse(researchProject.equals(researchProject2));
        Assert.assertNotEquals(researchProject.hashCode(), researchProject2.hashCode());

        Date stop = new Date();
        FundingSource fundingSource1 = new FundingSource(new GrantId("100"), new Name("SmallGrant"), start, stop,
                new Name("NIH") );
        FundingSource fundingSource2 = new FundingSource(new GrantId("200"), new Name("OtherGrant"), start, stop,
                new Name("NHGRI") );
        researchProject.addFundingSource(fundingSource1);
        researchProject.addFundingSource(fundingSource2);

        researchProject.addIrbNumber("irb123");
        researchProject.addIrbNumber("irb456");

        Person scientist1 = new Person("Adam", "Bass", "bass@broadinstitute.org", RoleType.BROAD_SCIENTIST );
        Person scientist2 = new Person("Noel", "Burtt", "bass@broadinstitute.org", RoleType.BROAD_SCIENTIST );
        researchProject.addSponsoringScientist( scientist1 );
        researchProject.addSponsoringScientist( scientist2 );


        // Create a GAP Experiment Request
        Person platformManager1 = new Person("Rob", "Onofrio", "onofrio@broad", RoleType.PLATFORM_PM );
        Person platformManager2 = new Person("Maeghan", "harden", "harden@broad", RoleType.PLATFORM_PM );
        List platformManagers =  new ArrayList<Person>();
        platformManagers.add(platformManager1);
        platformManagers.add(platformManager2);

        List programManagers =  new ArrayList<Person>();
        programManagers.add(programMgr);

        //TODO
//        ExperimentPlan expPlan = new ExperimentPlan();
//        expPlan.setProjectName();

        GapExperimentRequest gapExperimentRequest = new GapExperimentRequest(programMgr, new ExperimentId("GXP-123"),
                new Name("aGapTitle"),
                new PlatformId("GAP"), platformManagers, programManagers, null, new QuoteId("BSP-123"), new QuoteId("GAN-123") );


        researchProject.addExperimentRequest(gapExperimentRequest);

        // Retrieve a Sequencing Experiment Request
        final String PASS_NUMBER = "PASS-5447";
        final String PASS_TITLE = "1000 Genomes Seq Based Validation Using Custom Bait (Plate 2)";


        //Get a PASS from Squid.
        AbstractPass pass = squidServicePort.loadPassByNumber(PASS_NUMBER);


        // Verify that the the tile and number of the retrieved Pass is what we expect.
        Assert.assertEquals(pass.getProjectInformation().getTitle(), PASS_TITLE );
        Assert.assertEquals(pass.getProjectInformation().getPassNumber(), PASS_NUMBER);

                // Create a GAP Experiment Request
        Person platformManager3 = new Person("Steve", "Ferriera", "onofrio@broad", RoleType.PLATFORM_PM );
        List platformManagers2 =  new ArrayList<Person>();
        platformManagers2.add(platformManager3);

        List programManagers2 =  new ArrayList<Person>();
        programManagers.add(programMgr);

        SeqExperimentRequest passExperimentRequest = new SeqExperimentRequest(programMgr, new ExperimentId("PASS-5555"),
                new Name("aPassTitle"),
                new PlatformId("SEQ"), platformManagers, programManagers, null, new QuoteId("BSP-123"), new QuoteId("GAN-123") );

        passExperimentRequest.setPass(pass);

        researchProject.addExperimentRequest(passExperimentRequest);

        Assert.assertEquals(researchProject.getExperimentRequests().size(), 2 );

        System.out.println(researchProject.toString());

    }

    @Test
    public void testGetPasses() throws Exception {
        SummarizedPassListResult myExpRequests = squidServicePort.searchPassesByCreator("mccrory");
        for (SummarizedPass summary : myExpRequests.getSummarizedPassList()) {
            System.out.println(summary.getResearchProject() + " "
                    + summary.getPassNumber() + " "
                    + summary.getStatus() + " "
                    + summary.getUpdatedBy() + " "
                    + summary.getTitle() + "\t"
                    + summary.getLastModified().getTime().toLocaleString()
            );
        }
    }


    @Test
    public void testGettersSetters() throws Exception {

    }

}

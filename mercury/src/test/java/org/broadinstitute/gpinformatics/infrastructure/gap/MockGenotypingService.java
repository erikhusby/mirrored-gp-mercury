package org.broadinstitute.gpinformatics.infrastructure.gap;

import org.broadinstitute.gpinformatics.infrastructure.experiments.ExperimentRequestSummary;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;

import javax.enterprise.inject.Alternative;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/29/12
 * Time: 1:20 PM
 */
@Alternative
public class MockGenotypingService implements GenotypingService {


    public MockGenotypingService() {
    }

    @Override
    public List<ExperimentRequestSummary> getRequestSummariesByCreator(final Person creator) {
        //TODO
        throw new IllegalStateException("Not Yet Implemented");
        //return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    // Snapshot of web service output on 7/3/2012, using URL:
    // http://gap/ws/project_management/get_experiment_platforms
    private static final String PLATFORM_XML_SOURCE = "<platforms>\n"
            +"    <platform>\n"
            +"        <name>HT Genotyping</name>\n"
            +"        <vendor>Illumina</vendor>\n"
            +"        <products>\n"
            +"            <product name=\"Omni1M\" active=\"true\"/>\n"
            +"            <product name=\"Omni Express\" active=\"true\"/>\n"
            +"            <product name=\"Omni5M\" active=\"true\"/>\n"
            +"            <product name=\"Methylation 450k\" active=\"true\"/>\n"
            +"            <product name=\"Metabochip\" active=\"true\"/>\n"
            +"            <product name=\"1KG Human Exome\" active=\"true\"/>\n"
            +"            <product name=\"Cyto-12\" active=\"true\"/>\n"
            +"            <product name=\"Omni Express + Exome\" active=\"true\"/>\n"
            +"            <product name=\"Omni2.5M-8\" active=\"true\"/>\n"
            +"            <product name=\"Human Exome\" active=\"false\"/>\n"
            +"            <product name=\"Omni 1M Quad\" active=\"true\"/>\n"
            +"            <product name=\"Canine Array\" active=\"true\"/>\n"
            +"            <product name=\"Immunochip\" active=\"true\"/>\n"
            +"        </products>\n"
            +"    </platform>\n"
            +"</platforms>";

    @Override
    public Platforms getPlatforms() {
        return ObjectMarshaller.unmarshall(Platforms.class, PLATFORM_XML_SOURCE);
    }




}

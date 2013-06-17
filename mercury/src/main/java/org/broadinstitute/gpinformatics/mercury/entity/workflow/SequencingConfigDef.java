/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlID;
import java.io.Serializable;

/**
 * Defaults for different workflow processes such as chemistry and read structure
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class SequencingConfigDef implements Serializable {
    private static final long serialVersionUID = 2013061401L;

    @XmlID
    private String name=null;
    private InstrumentWorkflow instrumentWorkflow=null;
    private ReadStructure readStructure=null;
    private Chemistry chemistry=null;


    /** For JAXB */
    @SuppressWarnings("UnusedDeclaration")
    SequencingConfigDef() {
    }

    public SequencingConfigDef(String name) {
        this.name = name;
    }

    public enum InstrumentWorkflow {
        Resequencing("Resequencing"),
        Amplicon("Amplicon");
        private String value;

        InstrumentWorkflow(String value) {
            this.value = value;
        }
        public String getValue() {
            return value;
        }
    }

    public enum ReadStructure {
        POOL_TEST("8B8B"),
        PRODUCTION("76T8B8B76T");

        private String value;
        ReadStructure(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum Chemistry {
        Default, LongRead, Amplicon;
    }

    public ReadStructure getReadStructure() {
        return readStructure;
    }

    public void setReadStructure(ReadStructure readStructure) {
        this.readStructure = readStructure;
    }

    public Chemistry getChemistry() {
        return chemistry;
    }

    public void setChemistry(Chemistry chemistry) {
        this.chemistry = chemistry;
    }

    public InstrumentWorkflow getInstrumentWorkflow() {
        return instrumentWorkflow;
    }

    public void setInstrumentWorkflow(InstrumentWorkflow instrumentWorkflow) {
        this.instrumentWorkflow = instrumentWorkflow;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

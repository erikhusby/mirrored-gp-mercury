package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import org.apache.commons.io.FileUtils;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.Transient;
import java.io.File;
import java.io.IOException;

@Entity
@Audited
public abstract class AligntmentTaskBase extends ProcessTask {

    @Transient
    public File reference;

    @Transient
    public File fastQList;

    @Transient
    public String fastQSampleId;

    @Transient
    public File outputDir;

    @Transient
    public File intermediateResultsDir;

    @Transient
    public String outputFilePrefix;

    @Transient
    public String vcSampleName;

    @Transient
    public File qcContaminationFile;

    @Transient
    public File qcCoverageBedFile;

    @Transient
    public File configFile;

    @Transient
    public Boolean enableVariantCaller;

    public AligntmentTaskBase() {
    }

    public AligntmentTaskBase(String partitiion) {
        super(partitiion);
    }

    public File getReference() {
        if (reference == null) {
            reference = new File(DragenTaskBuilder.parseCommandFromArgument(
                    DragenTaskBuilder.REFERENCE, getCommandLineArgument(), true));
        }
        return reference;
    }

    public File getFastQList() {
        if (fastQList == null) {
            fastQList = new File(DragenTaskBuilder.parseCommandFromArgument(
                    DragenTaskBuilder.FASTQ_LIST, getCommandLineArgument()));
        }
        return fastQList;
    }

    public String getFastQSampleId() {
        if (fastQSampleId == null) {
            fastQSampleId = DragenTaskBuilder.parseCommandFromArgument(
                    DragenTaskBuilder.FASTQ_LIST_SAMPLE_ID, getCommandLineArgument());
        }
        return fastQSampleId;
    }

    public File getOutputDir() {
        if (outputDir == null) {
            outputDir = new File(DragenTaskBuilder.parseCommandFromArgument(
                    DragenTaskBuilder.OUTPUT_DIRECTORY, getCommandLineArgument()));
        }
        return outputDir;
    }

    public File getIntermediateResultsDir() {
        return intermediateResultsDir;
    }

    public String getOutputFilePrefix() {
        if (outputFilePrefix == null) {
            outputFilePrefix = DragenTaskBuilder.parseCommandFromArgument(
                    DragenTaskBuilder.OUTPUT_FILE_PREFIX, getCommandLineArgument());
        }
        return outputFilePrefix;
    }

    public String getVcSampleName() {
        if (vcSampleName == null) {
            vcSampleName = DragenTaskBuilder.parseCommandFromArgument(
                    DragenTaskBuilder.VC_SAMPLE_NAME, getCommandLineArgument());
        }
        return vcSampleName;
    }

    public boolean isEnableVariantCaller() {
        if (enableVariantCaller == null) {
            String arg = DragenTaskBuilder.parseCommandFromArgument(
                    DragenTaskBuilder.ENABLE_VARIANT_CALLER, getCommandLineArgument());
            if (arg != null) {
                enableVariantCaller = Boolean.valueOf(arg);

            } else {
                // TODO JW hanlde config file params better
                File configFile = getConfigFile();
                if (configFile != null) {
                    try {
                        String fileContents = FileUtils.readFileToString(configFile);
                        enableVariantCaller = fileContents.contains("enable-map-align-output = true");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    enableVariantCaller = false;
                }
            }
        }
        return enableVariantCaller;
    }

    public File getQcContaminationFile() {
        if (qcContaminationFile == null) {
            String contamFilePath = DragenTaskBuilder.parseCommandFromArgument(
                    DragenTaskBuilder.QC_CROSS_CONT_VCF, getCommandLineArgument());
            if (contamFilePath == null) {
                return null;
            }
            qcContaminationFile = new File(contamFilePath);
        }
        return qcContaminationFile;
    }

    public File getConfigFile() {
        if (configFile == null) {
            String configFilePath = DragenTaskBuilder.parseCommandFromArgument(
                    DragenTaskBuilder.CONFIG_FILE, getCommandLineArgument());
            if (configFilePath == null) {
                return null;
            }
            configFile = new File(configFilePath);
        }
        return configFile;
    }
}

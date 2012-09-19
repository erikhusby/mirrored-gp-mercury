package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.athena.Namespaces;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.math.BigDecimal;


/**
 * <p>Java class for CoverageAndAnalysisInformation complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="CoverageAndAnalysisInformation">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice>
 *         &lt;element name="attemptedLanesCoverageModel" type="{urn:Topic}AttemptedLanesCoverageModel"/>
 *         &lt;element name="pfAlignedBasesCoverageModel" type="{urn:Topic}PFAlignedBasesCoverageModel"/>
 *         &lt;element name="pfBasesCoverageModel" type="{urn:Topic}PFBasesCoverageModel"/>
 *         &lt;element name="targetCoverageModel" type="{urn:Topic}TargetCoverageModel"/>
 *         &lt;element name="pfReadsCoverageModel" type="{urn:Topic}PFReadsCoverageModel"/>
 *         &lt;element name="meanTargetCoverageModel" type="{urn:Topic}MeanTargetCoverageModel"/>
 *         &lt;element name="acceptedBasesCoverageModel" type="{urn:Topic}AcceptedBasesCoverageModel"/>
 *         &lt;element name="attemptedRegionsCoverageModel" type="{urn:Topic}AttemptedRegionsCoverageModel"/>
 *         &lt;element name="physicalCoverageModel" type="{urn:Topic}PhysicalCoverageModel"/>
 *         &lt;element name="q20BasesCoverageModel" type="{urn:Topic}Q20BasesCoverageModel"/>
 *         &lt;element name="programPseudoDepthCoverageModel" type="{urn:Topic}ProgramPseudoDepthCoverageModel"/>
 *       &lt;/choice>
 *       &lt;attGroup ref="{urn:Topic}coverageAndAnalysisInformationAttributeGroup"/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(namespace = Namespaces.PRODUCT_NS, propOrder = {
        "attemptedLanesCoverageModel",
        "targetCoverageModel",
        "pfReadsCoverageModel",
        "meanTargetCoverageModel",
        "programPseudoDepthCoverageModel"
})
public class CoverageAndAnalysisInformation
        implements Serializable {

    private final static long serialVersionUID = 12343L;
    @XmlElement
    protected AttemptedLanesCoverageModel attemptedLanesCoverageModel;
    @XmlElement
    protected TargetCoverageModel targetCoverageModel;
    @XmlElement
    protected PFReadsCoverageModel pfReadsCoverageModel;
    @XmlElement
    protected MeanTargetCoverageModel meanTargetCoverageModel;
    @XmlElement
    protected ProgramPseudoDepthCoverageModel programPseudoDepthCoverageModel;
    @XmlAttribute(required = true)
    protected AnalysisPipelineType analysisPipeline;
    @XmlAttribute(required = true)
    protected AlignerType aligner;
    @XmlAttribute(required = true)
    protected long referenceSequenceId;
    @XmlAttribute
    protected BigDecimal plex;
    @XmlAttribute(required = true)
    protected boolean keepFastQs;

    /**
     * Gets the value of the attemptedLanesCoverageModel property.
     *
     * @return possible object is
     *         {@link AttemptedLanesCoverageModel }
     */
    public AttemptedLanesCoverageModel getAttemptedLanesCoverageModel() {
        return attemptedLanesCoverageModel;
    }

    /**
     * Sets the value of the attemptedLanesCoverageModel property.
     *
     * @param value allowed object is
     *              {@link AttemptedLanesCoverageModel }
     */
    public void setAttemptedLanesCoverageModel(AttemptedLanesCoverageModel value) {
        this.attemptedLanesCoverageModel = value;
    }


    /**
     * Gets the value of the targetCoverageModel property.
     *
     * @return possible object is
     *         {@link TargetCoverageModel }
     */
    public TargetCoverageModel getTargetCoverageModel() {
        return targetCoverageModel;
    }

    /**
     * Sets the value of the targetCoverageModel property.
     *
     * @param value allowed object is
     *              {@link TargetCoverageModel }
     */
    public void setTargetCoverageModel(TargetCoverageModel value) {
        this.targetCoverageModel = value;
    }

    /**
     * Gets the value of the pfReadsCoverageModel property.
     *
     * @return possible object is
     *         {@link PFReadsCoverageModel }
     */
    public PFReadsCoverageModel getPfReadsCoverageModel() {
        return pfReadsCoverageModel;
    }

    /**
     * Sets the value of the pfReadsCoverageModel property.
     *
     * @param value allowed object is
     *              {@link PFReadsCoverageModel }
     */
    public void setPfReadsCoverageModel(PFReadsCoverageModel value) {
        this.pfReadsCoverageModel = value;
    }

    /**
     * Gets the value of the meanTargetCoverageModel property.
     *
     * @return possible object is
     *         {@link MeanTargetCoverageModel }
     */
    public MeanTargetCoverageModel getMeanTargetCoverageModel() {
        return meanTargetCoverageModel;
    }

    /**
     * Sets the value of the meanTargetCoverageModel property.
     *
     * @param value allowed object is
     *              {@link MeanTargetCoverageModel }
     */
    public void setMeanTargetCoverageModel(MeanTargetCoverageModel value) {
        this.meanTargetCoverageModel = value;
    }

    /**
     * Gets the value of the programPseudoDepthCoverageModel property.
     *
     * @return possible object is
     *         {@link ProgramPseudoDepthCoverageModel }
     */
    public ProgramPseudoDepthCoverageModel getProgramPseudoDepthCoverageModel() {
        return programPseudoDepthCoverageModel;
    }

    /**
     * Sets the value of the programPseudoDepthCoverageModel property.
     *
     * @param value allowed object is
     *              {@link ProgramPseudoDepthCoverageModel }
     */
    public void setProgramPseudoDepthCoverageModel(ProgramPseudoDepthCoverageModel value) {
        this.programPseudoDepthCoverageModel = value;
    }

    /**
     * Gets the value of the analysisPipeline property.
     *
     * @return possible object is
     *         {@link AnalysisPipelineType }
     */
    public AnalysisPipelineType getAnalysisPipeline() {
        return analysisPipeline;
    }

    /**
     * Sets the value of the analysisPipeline property.
     *
     * @param value allowed object is
     *              {@link AnalysisPipelineType }
     */
    public void setAnalysisPipeline(AnalysisPipelineType value) {
        this.analysisPipeline = value;
    }

    /**
     * Gets the value of the aligner property.
     *
     * @return possible object is
     *         {@link org.broad.squid.services.TopicService.AlignerType }
     */
    public AlignerType getAligner() {
        return aligner;
    }

    /**
     * Sets the value of the aligner property.
     *
     * @param value allowed object is
     *              {@link org.broad.squid.services.TopicService.AlignerType }
     */
    public void setAligner(AlignerType value) {
        this.aligner = value;
    }

    /**
     * Gets the value of the referenceSequenceId property.
     */
    public long getReferenceSequenceId() {
        return referenceSequenceId;
    }

    /**
     * Sets the value of the referenceSequenceId property.
     */
    public void setReferenceSequenceId(long value) {
        this.referenceSequenceId = value;
    }

    /**
     * Gets the value of the plex property.
     *
     * @return possible object is
     *         {@link java.math.BigDecimal }
     */
    public BigDecimal getPlex() {
        return plex;
    }

    /**
     * Sets the value of the plex property.
     *
     * @param value allowed object is
     *              {@link java.math.BigDecimal }
     */
    public void setPlex(BigDecimal value) {
        this.plex = value;
    }

    /**
     * Gets the value of the keepFastQs property.
     */
    public boolean isKeepFastQs() {
        return keepFastQs;
    }

    /**
     * Sets the value of the keepFastQs property.
     */
    public void setKeepFastQs(boolean value) {
        this.keepFastQs = value;
    }

}

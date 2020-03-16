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

package org.broadinstitute.gpinformatics.mercury.presentation.receiving;

import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.entity.products.BillingTriggerMapping;
import org.broadinstitute.gpinformatics.athena.entity.products.BillingTriggerMapping_;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.MayoManifestEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.entity.infrastructure.KeyValueMapping;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This handles viewing and updating the PDO parameters used for AoU PDO creation.
 */
@UrlBinding("/receiving/aou_pdo_config.action")
public class AouPdoConfigActionBean extends CoreActionBean {
    private static final String LIST_PAGE = "/receiving/aou_pdo_config.jsp";
    private MessageCollection messages = new MessageCollection();
    private List<Dto> dtos;
    private boolean saveDespiteErrors;

    @Inject
    private AttributeArchetypeDao attributeArchetypeDao;

    @Inject
    private MayoManifestEjb mayoManifestEjb;
    private BillingTriggerMapping billingTrigger;
    private BillingTriggerMapping wgsBillingTrigger;

    @Before(stages = LifecycleStage.BindingAndValidation)
    public void postConstruct() {
        // Populates parameters from AttributeArchetypes.
        Map<String, String> wgsParams = attributeArchetypeDao.findKeyValueMap(KeyValueMapping.AOU_PDO_WGS);
        Map<String, String> arrayParams = attributeArchetypeDao.findKeyValueMap(KeyValueMapping.AOU_PDO_ARRAY);
        dtos = Stream.concat(wgsParams.keySet().stream(), arrayParams.keySet().stream()).
            distinct().
            sorted(Comparator.reverseOrder()).
            map(param -> new Dto(param, wgsParams.get(param), arrayParams.get(param))).
            collect(Collectors.toList());

        billingTrigger = attributeArchetypeDao.findSingle(BillingTriggerMapping.class, BillingTriggerMapping_.group,
            BillingTriggerMapping.AOU_PDO_BILLING);
    }

    @DefaultHandler
    @HandlesEvent(LIST_ACTION)
    public Resolution list() {
        return new ForwardResolution(LIST_PAGE);
    }

    @HandlesEvent("validate")
    public Resolution validate() {
        mayoManifestEjb.validateAouPdoParams(dtos, messages);
        addMessages(messages);
        return new ForwardResolution(LIST_PAGE);
    }

    @HandlesEvent(SAVE_ACTION)
    public Resolution save() {
        mayoManifestEjb.validateAouPdoParams(dtos, messages);
        if (messages.hasErrors() && !saveDespiteErrors) {
            messages.addWarning("The changes were not saved.");
            addMessages(messages);
            return new ForwardResolution(LIST_PAGE);
        } else {
            mayoManifestEjb.saveAouPdoParams(dtos);
            messages.addInfo("The changes were saved.");
            addMessages(messages);
            return new RedirectResolution(AouPdoConfigActionBean.class, LIST_ACTION);
        }
    }

    public BillingTriggerMapping getBillingTrigger() {
        return billingTrigger;
    }

    public void setBillingTrigger(BillingTriggerMapping billingTrigger) {
        this.billingTrigger = billingTrigger;
    }

    public List<Dto> getDtos() {
        return dtos;
    }

    public void setDtos(List<Dto> dtos) {
        this.dtos = dtos;
    }

    public boolean isSaveDespiteErrors() {
        return saveDespiteErrors;
    }

    public void setSaveDespiteErrors(boolean saveDespiteErrors) {
        this.saveDespiteErrors = saveDespiteErrors;
    }

    public class Dto {
        private String paramName;
        private String wgsValue;
        private String arrayValue;
        private Set<ResearchProject.BillingTrigger> billingTrigger = new HashSet<>();

        public Dto(String paramName, String wgsValue, String arrayValue) {
            this.paramName = paramName;
            this.wgsValue = wgsValue;
            this.arrayValue = arrayValue;
        }

        public String getParamName() {
            return paramName;
        }

        public void setParamName(String paramName) {
            this.paramName = paramName;
        }

        public String getWgsValue() {
            return wgsValue;
        }

        public void setWgsValue(String wgsValue) {
            this.wgsValue = wgsValue;
        }

        public String getArrayValue() {
            return arrayValue;
        }

        public void setArrayValue(String arrayValue) {
            this.arrayValue = arrayValue;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Dto dto = (Dto) o;
            return Objects.equals(paramName, dto.paramName) &&
                    Objects.equals(wgsValue, dto.wgsValue) &&
                    Objects.equals(arrayValue, dto.arrayValue);
        }

        @Override
        public int hashCode() {
            return Objects.hash(paramName, wgsValue, arrayValue);
        }
    }
}

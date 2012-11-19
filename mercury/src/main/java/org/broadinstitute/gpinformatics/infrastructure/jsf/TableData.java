package org.broadinstitute.gpinformatics.infrastructure.jsf;

import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * @author breilly
 */
//@ManagedBean
//@ViewScoped
//@Named
//@ConversationScoped
public class TableData implements Serializable {

//    @Produces @ConversationScoped
    public static TableData produceTableData(InjectionPoint injectionPoint, BeanManager beanManager) {
        Bean bean = beanManager.resolve(beanManager.getBeans(injectionPoint.getType(), (Annotation[]) injectionPoint.getQualifiers().toArray()));
        if (bean != null) {
            CreationalContext creationalContext = beanManager.createCreationalContext(bean);
            if (creationalContext != null) {
                Object instance = bean.create(creationalContext);
                return (TableData) instance;
            }
        }
        return null;
    }

//    @Produces @ConversationScoped
    public static TableData produceTableData(InjectionPoint injectionPoint) {
        return new TableData();
    }

    @Produces @ConversationScoped
    public static TableData produceTableData(BeanManager beanManager) {
        return new TableData();
    }

    private List values = new ArrayList();

    private List filteredValues = new ArrayList();

    public List getValues() {
        return values;
    }

    public void setValues(List values) {
        this.values = values;
    }

    public List getFilteredValues() {
        return filteredValues;
    }

    public void setFilteredValues(List filteredValues) {
        this.filteredValues = filteredValues;
    }
}

package org.broadinstitute.gpinformatics.athena;

import org.jboss.weld.environment.se.WeldContainer;

public class WeldUtil {
    
    private WeldContainer weld;
    
    public WeldUtil(WeldContainer weld) {
        if (weld == null) {
             throw new NullPointerException("weld cannot be null."); 
        }
        this.weld = weld;
    }
    
    public <T> T getFromContainer(Class<T> clazz) {
        return weld.instance().select(clazz).get();
    }
    
    
}

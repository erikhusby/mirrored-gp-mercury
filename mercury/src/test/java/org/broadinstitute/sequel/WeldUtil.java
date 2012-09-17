package org.broadinstitute.sequel;

//import org.jboss.weld.environment.se.Weld;
//import org.jboss.weld.environment.se.WeldContainer;

@Deprecated
public class WeldUtil {
    
//    private WeldContainer weld;
    
//    public WeldUtil(WeldContainer weld) {
    public WeldUtil() {
//        if (weld == null) {
//             throw new NullPointerException("weld cannot be null.");
//        }
//        this.weld = weld;
    }
    
    public <T> T getFromContainer(Class<T> clazz) {
//        return weld.instance().select(clazz).get();
        return null;
    }
    
    
}

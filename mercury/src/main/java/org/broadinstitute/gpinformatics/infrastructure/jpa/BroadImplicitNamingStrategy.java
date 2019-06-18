package org.broadinstitute.gpinformatics.infrastructure.jpa;

import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.internal.util.StringHelper;

import java.util.Locale;

/**
 * Replaces deprecated Hibernate configuration setting: <br/>
 * &lt;property name="hibernate.ejb.naming_strategy" value="org.hibernate.cfg.ImprovedNamingStrategy" /&gt; <br/>
 * Mercury uses deprecated improved naming strategy that prefers embedded underscores to mixed case names
 *  but Hibernate 5+ does not provide a compatible replacement. <br/>
 * Rather than refactor every entity to explicitly define column names, layer on top of Hibernate's JPA implementation.
 */

public class BroadImplicitNamingStrategy extends org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl {

    public BroadImplicitNamingStrategy() throws Exception {
        super();
    }

    @Override
    protected String transformEntityName(EntityNaming entityNaming) {

        // prefer the JPA entity name, if specified...
        if ( StringHelper.isNotEmpty( entityNaming.getJpaEntityName() ) ) {
            return addUnderscores( entityNaming.getJpaEntityName() );
        } else {
            // otherwise, use the Hibernate entity name
            return addUnderscores( StringHelper.unqualify( entityNaming.getEntityName() ) );
        }
    }

    @Override
    protected String transformAttributePath(AttributePath attributePath) {
        return addUnderscores( super.transformAttributePath(attributePath) );
    }

    private static String addUnderscores(String name) {
        StringBuilder buf = new StringBuilder( name.replace('.', '_') );
        for (int i=1; i<buf.length()-1; i++) {
            if (
                    Character.isLowerCase( buf.charAt(i-1) ) &&
                            Character.isUpperCase( buf.charAt(i) ) &&
                            Character.isLowerCase( buf.charAt(i+1) )
                    ) {
                buf.insert(i++, '_');
            }
        }
        return buf.toString().toUpperCase(Locale.ROOT);
    }
}

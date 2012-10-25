package org.broadinstitute.gpinformatics.infrastructure.jpa;

import org.hibernate.dialect.PostgresPlusDialect;

import java.sql.Types;

/**
 * @author Scott Matthews
 *         Date: 10/24/12
 *         Time: 2:31 PM
 */
public class AthenaPostgresPlusDialect extends PostgresPlusDialect {

    public AthenaPostgresPlusDialect () {
        super();

        registerColumnType( Types.BIT, "number(1,0)" );

    }
}

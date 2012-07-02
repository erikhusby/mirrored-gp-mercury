package org.broadinstitute.sequel.infrastructure.thrift;

import edu.mit.broad.prodinfo.thrift.lims.TZIMSException;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRun;
import org.apache.thrift.TException;

import java.io.Serializable;

/**
 * Wrapper around thrift that helps us
 * test things without having to connect
 * to (slow) deployed services
 */
public interface ThriftService extends Serializable {

    public TZamboniRun fetchRun(String runName) throws TZIMSException, TException;

}

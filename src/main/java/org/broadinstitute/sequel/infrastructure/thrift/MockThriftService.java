package org.broadinstitute.sequel.infrastructure.thrift;

import edu.mit.broad.prodinfo.thrift.lims.TZIMSException;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRun;
import org.apache.thrift.TException;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import java.io.IOException;

@Default
public class MockThriftService implements ThriftService {

    public MockThriftService() {}

    @Override
    public TZamboniRun fetchRun(String runName) throws TZIMSException, TException {
        TZamboniRun run = null;

        try {
            run = ThriftFileAccessor.deserializeRun();
        }
        catch(IOException e) {
            throw new RuntimeException("Failed to deserialize run from local file",e);
        }
        return run;
    }
}
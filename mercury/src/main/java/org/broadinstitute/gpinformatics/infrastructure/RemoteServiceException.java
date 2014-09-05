package org.broadinstitute.gpinformatics.infrastructure;

/**
 * Provides a way to allow a webservice consumer to gracefully handle web service failures.
 * Driving use-case is to provide a user friendly web error message (e.g. "Remove BSP columns or try again later")
 *    when a user-defined search is executed with BSP columns in the results and the BSP application is down.
 */
public class RemoteServiceException extends RuntimeException {
    public RemoteServiceException(){
        super();
    }

    public RemoteServiceException(String message){
        super(message);
    }

    public RemoteServiceException(String message, Throwable cause){
        super(message, cause);
    }

    public RemoteServiceException(Throwable cause){
        super(cause);
    }

}

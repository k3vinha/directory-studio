/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */

package org.apache.directory.studio.connection.core.jobs;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.directory.studio.connection.core.ConnectionCoreConstants;
import org.apache.directory.studio.connection.core.Messages;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.ProgressMonitorWrapper;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;


/**
 * The StudioProgressMonitor extends the the Eclipse
 * Progress Monitor with active cancellation capabilities.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class StudioProgressMonitor extends ProgressMonitorWrapper
{

    /** The job reports progress and checks for cancellation. */
    private static Job reportProgressAndCheckCancellationJob = new Job(
        Messages.jobs__progressmonitor_check_cancellation )
    {
        protected IStatus run( IProgressMonitor monitor )
        {
            while ( true )
            {
                synchronized ( monitors )
                {
                    for ( Iterator<StudioProgressMonitor> it = monitors.iterator(); it.hasNext(); )
                    {
                        StudioProgressMonitor next = it.next();
                        StudioProgressMonitor spm = next;

                        do
                        {
                            // check report progress message
                            if ( !spm.isCanceled() && !spm.done && spm.reportProgressMessage != null )
                            {
                                spm.subTask( spm.reportProgressMessage );
                                spm.reportProgressMessage = null;
                            }

                            // check if canceled
                            if ( spm.isCanceled() )
                            {
                                spm.fireCancelRequested();
                            }
                            if ( spm == next && ( spm.isCanceled() || spm.done ) )
                            {
                                it.remove();
                            }

                            if ( spm.getWrappedProgressMonitor() != null
                                && spm.getWrappedProgressMonitor() instanceof StudioProgressMonitor )
                            {
                                spm = ( StudioProgressMonitor ) spm.getWrappedProgressMonitor();
                            }
                            else
                            {
                                spm = null;
                            }
                        }
                        while ( spm != null );
                    }
                }

                try
                {
                    Thread.sleep( 1000 );
                }
                catch ( InterruptedException e )
                {
                }
            }
        }
    };
    static
    {
        reportProgressAndCheckCancellationJob.setSystem( true );
        reportProgressAndCheckCancellationJob.schedule();
    }

    private static List<StudioProgressMonitor> monitors = Collections
        .synchronizedList( new ArrayList<StudioProgressMonitor>() );

    private boolean done;

    private List<Status> errorStatusList;

    private List<CancelListener> cancelListenerList;

    private String reportProgressMessage = null;


    /**
     * Creates a new instance of ExtendedProgressMonitor.
     * 
     * @param monitor the progress monitor to forward to
     */
    public StudioProgressMonitor( IProgressMonitor monitor )
    {
        super( monitor );
        this.done = false;
        monitors.add( this );
    }


    /**
     * @see org.eclipse.core.runtime.ProgressMonitorWrapper#setCanceled(boolean)
     */
    public void setCanceled( boolean b )
    {
        super.setCanceled( b );
        if ( b )
        {
            fireCancelRequested();
        }
    }


    /**
     * @see org.eclipse.core.runtime.ProgressMonitorWrapper#done()
     */
    public void done()
    {
        synchronized ( this )
        {
            done = true;
            super.done();
        }
    }


    /**
     * Adds the cancel listener.
     * 
     * @param listener the listener
     */
    public void addCancelListener( CancelListener listener )
    {
        if ( cancelListenerList == null )
        {
            cancelListenerList = new ArrayList<CancelListener>();
        }
        if ( !cancelListenerList.contains( listener ) )
        {
            cancelListenerList.add( listener );
        }
    }


    /**
     * Removes the cancel listener.
     * 
     * @param listener the listener
     */
    public void removeCancelListener( CancelListener listener )
    {
        if ( cancelListenerList != null && cancelListenerList.contains( listener ) )
        {
            cancelListenerList.remove( listener );
        }
    }


    private void fireCancelRequested()
    {
        CancelEvent event = new CancelEvent( this );
        if ( cancelListenerList != null )
        {
            for ( int i = 0; i < cancelListenerList.size(); i++ )
            {
                CancelListener listener = cancelListenerList.get( i );
                listener.cancelRequested( event );
            }
        }
    }


    /**
     * Report progress.
     * 
     * @param message the message
     */
    public void reportProgress( String message )
    {
        reportProgressMessage = message;
    }


    /**
     * Report error.
     * 
     * @param message the message
     */
    public void reportError( String message )
    {
        this.reportError( message, null );
    }


    /**
     * Report error.
     * 
     * @param exception the exception
     */
    public void reportError( Exception exception )
    {
        reportError( null, exception );
    }


    /**
     * Report error.
     * 
     * @param message the message
     * @param exception the exception
     */
    public void reportError( String message, Exception exception )
    {
        if ( errorStatusList == null )
        {
            errorStatusList = new ArrayList<Status>( 3 );
        }

        if ( message == null )
        {
            message = ""; //$NON-NLS-1$
        }

        Status errorStatus = new Status( IStatus.ERROR, ConnectionCoreConstants.PLUGIN_ID, IStatus.ERROR, message,
            exception );
        errorStatusList.add( errorStatus );
    }


    /**
     * Errors reported.
     * 
     * @return true, if errors reported
     */
    public boolean errorsReported()
    {
        return errorStatusList != null;
    }


    /**
     * Gets the error status.
     * 
     * @param message the message
     * @param TODO: context message, e.g. search parameters or mod-ldif
     * 
     * @return the error status
     */
    public IStatus getErrorStatus( String message )
    {
        // multi: message + für jeden errorStatus (\n + message)
        // children: alle errorStatus + stacktrace->message

        if ( errorStatusList != null && !errorStatusList.isEmpty() )
        {
            // append status messages to message
            for ( Status status : errorStatusList )
            {
                String statusMessage = status.getMessage();
                Throwable exception = status.getException();
                String exceptionMessage = exception != null ? exception.getMessage() : null;

                // Tweak exception message for some well-know exceptions
                Throwable e = exception;
                while ( e != null )
                {
                    if ( e instanceof UnknownHostException )
                    {
                        exceptionMessage = "Unknown Host: " + e.getMessage(); //$NON-NLS-1$
                    }
                    else if ( e instanceof SocketException )
                    {
                        exceptionMessage = e.getMessage() + " (" + exceptionMessage + ")";; //$NON-NLS-1$ //$NON-NLS-2$
                    }

                    // next cause
                    e = e.getCause();
                }

                // append explicit status message
                if ( !StringUtils.isEmpty( statusMessage ) )
                {
                    message += "\n - " + statusMessage;
                }
                // append exception message if different to status message
                if ( exception != null && exceptionMessage != null && !exceptionMessage.equals( statusMessage ) )
                {
                    // strip control characters
                    int indexOfAny = StringUtils.indexOfAny( exceptionMessage, "\n\r\t" );
                    if ( indexOfAny > -1 )
                    {
                        exceptionMessage = exceptionMessage.substring( 0, indexOfAny - 1 );
                    }
                    message += "\n - " + exceptionMessage;
                }
            }

            // create main status
            MultiStatus multiStatus = new MultiStatus( ConnectionCoreConstants.PLUGIN_ID, IStatus.ERROR, message, null );

            // append child status
            for ( Status status : errorStatusList )
            {
                String statusMessage = status.getMessage();
                if ( status.getException() != null )
                {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter( sw );
                    status.getException().printStackTrace( pw );
                    statusMessage = sw.toString();
                }
                multiStatus.add( new Status( status.getSeverity(), status.getPlugin(), status.getCode(), statusMessage,
                    status.getException() ) );
            }

            return multiStatus;
        }
        else
        {
            return Status.OK_STATUS;
        }
    }


    /**
     * Gets the exception.
     * 
     * @return the exception
     */
    public Exception getException()
    {
        if ( errorStatusList != null )
        {
            return ( Exception ) errorStatusList.get( 0 ).getException();
        }
        return null;
    }


    /**
     * Resets this status.
     */
    public void reset()
    {
        this.done = false;
        this.errorStatusList = null;
    }

    /**
     * CancelEvent.
     *
     * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
     * @version $Rev$, $Date$
     */
    public static class CancelEvent
    {
        private IProgressMonitor monitor;


        /**
         * Creates a new instance of CancelEvent.
         * 
         * @param monitor the progress monitor
         */
        public CancelEvent( IProgressMonitor monitor )
        {
            this.monitor = monitor;
        }


        /**
         * Gets the monitor.
         * 
         * @return the progress monitor
         */
        public IProgressMonitor getMonitor()
        {
            return monitor;
        }
    }

    /**
     * CancelListener.
     *
     * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
     * @version $Rev$, $Date$
     */
    public interface CancelListener
    {

        /**
         * Cancel requested.
         * 
         * @param event the event
         */
        public void cancelRequested( CancelEvent event );
    }

}

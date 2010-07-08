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

package org.apache.directory.studio.ldapbrowser.common.actions;


import org.apache.directory.studio.connection.core.Utils;
import org.apache.directory.studio.ldapbrowser.common.BrowserCommonActivator;
import org.apache.directory.studio.ldapbrowser.common.BrowserCommonConstants;
import org.apache.directory.studio.ldapbrowser.core.model.IBrowserConnection;
import org.apache.directory.studio.ldapbrowser.core.model.IQuickSearch;
import org.apache.directory.studio.ldapbrowser.core.model.impl.QuickSearch;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.dialogs.PreferencesUtil;


/**
 * This class implements the Open Quick Seach Action.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class OpenQuickSearchAction extends BrowserAction
{

    /**
     * Creates a new instance of OpenQuickSearchAction.
     */
    public OpenQuickSearchAction()
    {
    }


    /**
     * {@inheritDoc}
     */
    public void run()
    {
        IBrowserConnection browserConnection = getBrowserConnection();
        if ( browserConnection != null )
        {
            IQuickSearch quickSearch = browserConnection.getSearchManager().getQuickSearch();
            if ( quickSearch == null )
            {
                quickSearch = new QuickSearch( browserConnection.getRootDSE(), browserConnection );
                browserConnection.getSearchManager().setQuickSearch( quickSearch );
            }

            String pageId = BrowserCommonConstants.PROP_SEARCH;
            PreferenceDialog dialog = PreferencesUtil.createPropertyDialogOn( getShell(), quickSearch, pageId, null,
                null );
            String title = quickSearch.getName();
            if ( dialog != null )
            {
                title = Utils.shorten( title, 30 );
            }
            dialog.getShell().setText( NLS.bind( Messages.getString( "PropertiesAction.PropertiesForX" ), title ) ); //$NON-NLS-1$
            dialog.open();
        }
    }


    /**
     * {@inheritDoc}
     */
    public String getText()
    {
        return Messages.getString( "OpenQuickSearchAction.OpenQuickSearch" ); //$NON-NLS-1$
    }


    /**
     * {@inheritDoc}
     */
    public ImageDescriptor getImageDescriptor()
    {
        return BrowserCommonActivator.getDefault().getImageDescriptor( BrowserCommonConstants.IMG_QUICKSEARCH );
    }


    /**
     * {@inheritDoc}
     */
    public String getCommandId()
    {
        return null;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isEnabled()
    {
        return getBrowserConnection() != null;
    }


    private IBrowserConnection getBrowserConnection()
    {
        if ( getInput() instanceof IBrowserConnection )
        {
            return ( IBrowserConnection ) getInput();
        }
        if ( getSelectedSearchResults().length > 0 )
        {
            return getSelectedSearchResults()[0].getEntry().getBrowserConnection();
        }
        if ( getSelectedEntries().length > 0 )
        {
            return getSelectedEntries()[0].getBrowserConnection();
        }
        if ( getSelectedSearches().length > 0 )
        {
            return getSelectedSearches()[0].getBrowserConnection();
        }
        return null;
    }
}

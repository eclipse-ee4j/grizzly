/*
 * Copyright (c) 2008, 2020 Oracle and/or its affiliates. All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.glassfish.grizzly.servlet;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import org.glassfish.grizzly.http.server.util.Enumerator;


/**
 * {@link FilterConfig} implementation.
 * 
 * @author Jeanfrancois Arcand
 */
@SuppressWarnings("unchecked")
public class FilterConfigImpl implements FilterConfig {

    
    /**
     * The Context with which we are associated.
     */
    private WebappContext servletContext = null;
    
    
    /**
     * The application Filter we are configured for.
     */
    private Filter filter = null;
    
    
    /**
     * Filter's initParameters.
     */
    private Map<String, String> initParameters = null;
    
    
    /**
     * Filter name
     */
    private String filterName;

    
    // ------------------------------------------------------------------ //
    

    
    public FilterConfigImpl(WebappContext servletContext) {
        this.servletContext = servletContext;
    }

    
    /**
     * {@inheritDoc}
     */    
    @Override
    public String getInitParameter(String name) {
        if (initParameters == null) {
            return null;
        }
        return initParameters.get(name);
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getFilterName() {
        return filterName;
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Enumeration<String> getInitParameterNames() {
        Map map = initParameters;
        if (map == null) {
            return (new Enumerator<String>(new ArrayList<String>()));
        } else {
            return (new Enumerator<String>(map.keySet()));
        }
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    
    /**
     * Return the application Filter we are configured for.
     */
    public Filter getFilter(){
       return filter;
    }

    
    /**
     * Release the Filter instance associated with this FilterConfig,
     * if there is one.
     */
    protected void recycle() {
        if (this.filter != null) {
            filter.destroy();
        }
        this.filter = null;
    }


    /**
     * Set the {@link Filter} associated with this object.
     * @param filter
     */
    protected void setFilter(Filter filter) {
        this.filter = filter;
    }

    
    /**
     * Set the {@link Filter}'s name associated with this object.
     * @param filterName the name of this {@link Filter}.
     */    
    protected void setFilterName(String filterName) {
        this.filterName = filterName;
    }

    
    /**
     * Set the init parameters associated with this associated {@link Filter}.
     * @param initParameters the configuration parameters for this {@link Filter}
     */    
    protected void setInitParameters(Map<String, String> initParameters) {
        if (initParameters != null && !initParameters.isEmpty()) {
            this.initParameters = initParameters;
        }
    }
}

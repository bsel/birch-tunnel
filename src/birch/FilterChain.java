/*
 * FilterChain.java
 *
 * Copyright (C) 2009 Beselius
 *
 * This file is part of Birch.
 *
 * Birch is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Birch is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Birch.  If not, see <http://www.gnu.org/licenses/>.
 */
package birch;

import java.util.*;
import java.util.logging.*;

/**
 *
 * @author Beselius
 */
public class FilterChain {

   private ServiceLoader<FilterFactory>[] filterFactoryLoaders;
   private String[] filterNames;
   private Map<String, FilterFactory> factoryMap;

   public FilterChain(ServiceLoader<FilterFactory>[] filterFactoryLoaders, String[] filterNames)
           throws ClassNotFoundException {
      this.filterFactoryLoaders = filterFactoryLoaders;
      this.filterNames = filterNames;

      factoryMap = new HashMap<String, FilterFactory>();
      fillFactoryMap();
   }

   public Filter getNewList() {
      List<Filter> filters;
      Filter lastFilter;

      filters = new ArrayList<Filter>(filterNames.length);

      for (String filterName : filterNames) {
         filters.add( factoryMap.get(filterName).getFilterInstance(Filter.nullObject) );
      }

      lastFilter = Filter.nullObject;
      for (Filter filter : filters) {
         lastFilter.setNext(filter);
         lastFilter = filter;
      }

      if (filters.isEmpty()) {
         return Filter.nullObject;
      } else {
         return filters.get(0);
      }
   }

   private void fillFactoryMap() throws ClassNotFoundException {
      FilterFactory factory;

      for (String filterName : filterNames) {
         factory = getFilterFactory(filterName);
         if (factory == null) {
            throw new ClassNotFoundException("FilterFactory for filter '" + filterName + "' not found.");
         }
         factoryMap.put(filterName, factory);
      }
   }

   private FilterFactory getFilterFactory(String filterName) {
      for (ServiceLoader<FilterFactory> loader : filterFactoryLoaders) {
         Logger.getLogger(FilterChain.class.getName())
                 .log(Level.FINER, "search in ServiceLoader '" + loader.toString() + "'");

         for (FilterFactory factory : loader) {
            Logger.getLogger(FilterChain.class.getName())
                    .log(Level.FINEST, "factory " + factory.getFitlerName() + "'");

            if (filterName.equals(factory.getFitlerName())) {
               return factory;
            }
         }
      }
      return null;
   }
}

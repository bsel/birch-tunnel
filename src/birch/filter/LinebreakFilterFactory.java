/*
 * LinebreakFilterFactory.java
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
package birch.filter;

import birch.Filter;
import birch.FilterFactory;

import java.util.ServiceConfigurationError;

/**
 *
 * @author Beselius
 */
public class LinebreakFilterFactory implements FilterFactory {

   public Filter getFilterInstance(Filter next) throws ServiceConfigurationError {
      return new LinebreakFilter(next);
   }

   public String getFitlerName() {
      return LinebreakFilter.class.getName();
   }

}

/*
 * TestFilter.java
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

/**
 *
 * @author Beselius
 */
public class TestFilter implements Filter {

   private Filter filter;

   public TestFilter(Filter filter) {
      this.filter = filter;
   }

   public String send(String message) {
      String result;
      String[] splited;

      result = message;

      splited = message.split(" ", 3);
      if ("PRIVMSG".equals(splited[0])) {
         result = message + "\nPRIVMSG #bsel :test";
      } else {
         result = message;
      }

      return filter.send(result);
   }

   public String receive(String message) {
      return filter.receive(message);
   }

   public Filter getNext() {
      return filter;
   }

   public void setNext(Filter filter) {
      this.filter = filter;
   }
}

/*
 * Filter.java
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

/**
 *
 * @author Beselius
 */
public interface Filter {

   /**
    * Messages from client to server.
    * @param message
    * @return
    */
   String send(String message);

   /**
    * Messages from server to client.
    * @param message
    * @return
    */
   String receive(String message);

   /**
    *
    * @return the next filter in the chain.
    */
   Filter getNext();

   /**
    * Set a new filter.
    * @param filter filter to set.
    */
   void setNext(Filter filter);

   /** The Filter Null Object. */
   public static final Filter nullObject = new Filter() {

      public String send(String message) {
         return message;
      }

      public String receive(String message) {
         return message;
      }

      public Filter getNext() {
         return this;
      }

      public void setNext(Filter filter) {
         // Nothing to to
      }
   };
}

/*
 * AbstractFilter.java
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
public abstract class AbstractFilter implements Filter {

   private Filter next;

   public AbstractFilter(Filter filter) {
      this.next = filter;
   }

   public String send(String message) {
      return next.send(doSend(message));
   }

   public String receive(String message) {
      return doReceive(next.receive(message));
   }

   protected abstract String doSend(String message);

   protected abstract String doReceive(String message);

   public Filter getNext() {
      return next;
   }

   public void setNext(Filter filter) {
      this.next = filter;
   }

}

/*
 * LinebreakFilter.java
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

import birch.AbstractFilter;
import birch.Filter;

import java.util.*;

/**
 *
 * @author Beselius
 */
public class LinebreakFilter extends AbstractFilter {

   public static final String LINEBREAK_INDICATOR = "<birchLB>";
   public static final int DEFAULT_MAX_LINE_LENGTH = 400;

   private final int maxLineLength;
   private Map<String, StringBuilder> buffers;

   public LinebreakFilter(Filter filter) {
      this(filter, DEFAULT_MAX_LINE_LENGTH);
   }

   public LinebreakFilter(Filter filter, int maxLineLength) {
      super(filter);

      this.maxLineLength = maxLineLength;
      buffers = new HashMap<String, StringBuilder>();
   }

   protected String doSend(String message) {
      String result;
      String prefix;
      String[] parts;

      result = message;

      if (result.startsWith("PRIVMSG") && result.length() > maxLineLength) {
         parts = result.split(" ", 3);
         if (parts.length == 3) {
            prefix = parts[0] + " " + parts[1] + " :";
            result = chopMessage(prefix, parts[2].substring(1));
         }
      }

      return result;
   }

   protected String doReceive(String message) {
      String result;
      StringBuilder buffer;
      String[] parts;
      String sender;

      result = message;

      parts = result.split(" ", 4);
      if (parts.length == 4 && parts[1].startsWith("PRIVMSG")) {
         sender = extractSender(message);
         buffer = buffers.get(sender);
         if (buffer == null) {
            buffer = new StringBuilder();
         }
         if (parts[3].startsWith(":" + LINEBREAK_INDICATOR)) {
            buffers.put(sender, buffer);
            if (buffer.length() == 0) {
               buffer.append(parts[0]);
               buffer.append(' ');
               buffer.append(parts[1]);
               buffer.append(' ');
               buffer.append(parts[2]);
               buffer.append(" :");
            }
            buffer.append(parts[3].substring(LINEBREAK_INDICATOR.length() + 1));
            result = "";
         } else {
            if (buffer.length() > 0) {
               buffer.append(parts[3].substring(1));
               result = buffer.toString();
               buffers.remove(sender);
            }
         }
      }

      return result;
   }

   private String chopMessage(String prefix, String substring) {
      StringBuilder result;
      int max_length;
      int position;
      int end;
      int length;

      result = new StringBuilder();

      length = substring.length();
      max_length = maxLineLength - prefix.length();
      position = 0;
      while (position < length) {
         if (position != 0) {
            result.append("\n");
         }
         result.append(prefix);
         if (length - position > max_length) {
            end =  position + max_length - LINEBREAK_INDICATOR.length();
            result.append(LINEBREAK_INDICATOR);
         } else {
            end = length;
         }
         result.append(substring.substring(position, end));
         position = end;
      }

      return result.toString();
   }

   private String extractSender(String message) {
      return message.substring(1, message.indexOf(':', 1));
   }
}

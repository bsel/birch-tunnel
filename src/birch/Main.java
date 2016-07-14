/*
 * Main.java
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

import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 *
 * @author Beselius
 */
public class Main {

   public static void main(String[] args) {
      if (args.length < 2) {
         System.out.println("Usage: " + Main.class.getName() + " <server_name> <server_port> [proxy_listen_address] [proxy_listen_port]");
         return;
      }

      Proxy proxy;

      try {

         if (args.length > 2) {
            if (args.length > 3) {
               proxy = new Proxy(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]));
            } else {
               if (args[2].matches("[0-9]*")) {
                  proxy = new Proxy(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]));
               } else {
                  proxy = new Proxy(args[0], Integer.parseInt(args[1]), args[2]);
               }
            }
         } else {
            proxy = new Proxy(args[0], Integer.parseInt(args[1]));
         }
         proxy.start();

         System.out.println("Press CTRL+C to stop and exit.");
      } catch (ClassNotFoundException ex) {
         Logger.getLogger(Main.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
      } catch (FileNotFoundException ex) {
         Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
      } catch (IOException ex) {
         Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
      }
   }
}

/*
 * ServerConnection.java
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

import java.io.IOException;

import java.net.Socket;
import java.net.UnknownHostException;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author Beselius
 */
public class ServerConnection extends AbstractFilteredConnection {

   private ClientConnection clientConnection;
   private AtomicBoolean stopping;

   public ServerConnection(ClientConnection clientConnection, String host, int port, String charset)
           throws UnknownHostException, IOException {
      super(new Socket(host, port), charset);

      this.clientConnection = clientConnection;
      this.stopping = new AtomicBoolean(false);

      Logger.getLogger(ServerConnection.class.getName()).log(Level.INFO, "ServerConnection created");
   }

   @Override
   public void close() {
      super.close();

      if (stopping.getAndSet(true) == false) {
         clientConnection.close();
      }
   }

   @Override
   protected void parse(String line) {
      Logger.getLogger(ServerConnection.class.getName())
              .log(Level.FINEST, "parse line from server: " + line);

      clientConnection.receive(line);
   }
}

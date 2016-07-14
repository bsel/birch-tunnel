/*
 * ClientConnection.java
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
import java.util.List;
import java.util.ArrayList;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author Beselius
 */
public class ClientConnection extends AbstractFilteredConnection implements Filter {

   private Proxy proxy;
   private Filter filter;
   private AbstractFilteredConnection serverConnection = null;
   private volatile List<String> buffer = new ArrayList<String>();
   private AtomicBoolean stopping;
   private String charset;

   public ClientConnection(Filter filter, Proxy proxy, Socket socket, String serverHost, int serverPort, String charset)
           throws UnknownHostException, IOException {
      super(socket, charset);

      this.filter = filter;
      this.proxy = proxy;
      this.charset = charset;
      stopping = new AtomicBoolean(false);

      try {
         connect(serverHost, serverPort);
      } catch (IOException ex) {
         close();
         throw ex;
      }

      Logger.getLogger(ClientConnection.class.getName()).log(Level.INFO, "ClientConnection created");
   }

   @Override
   public void close() {
      super.close();
      if (stopping.getAndSet(true) == false) {
         if (serverConnection != null) {
            serverConnection.close();
         }
         proxy.removeClientConnection(this);
      }
   }

   protected void parse(String line) {
      Logger.getLogger(ClientConnection.class.getName()).log(Level.FINEST, "parse line: " + line);

      if (serverConnection != null && serverConnection.getSocket().isConnected()) {
         String lineToSend;

         lineToSend = send(line);

         Logger.getLogger(ClientConnection.class.getName()).log(Level.FINEST, "filtered line: " + line);
         serverConnection.getOutput().println(lineToSend);
      } else {
         buffer.add(line);
      }
   }

   private void connect(String host, int port) throws UnknownHostException, IOException {
      Logger.getLogger(ClientConnection.class.getName()).log(Level.INFO, "connect to server: " + host + ":" + port);

      serverConnection = new ServerConnection(this, host, port, charset);
      proxy.execute(serverConnection);
      for (String line : buffer) {
         serverConnection.getOutput().println(line);
      }
      buffer.clear();
   }

   public String send(String message) {
      try {
         return filter.send(message);
      } catch (RuntimeException ex) {
         Logger.getLogger(ClientConnection.class.getName()).log(Level.SEVERE, "Error while processing the filter chain.", ex);

         return message;
      }
   }

   public String receive(String message) {
      try {
         getOutput().println(filter.receive(message));
      } catch (RuntimeException ex) {
         Logger.getLogger(ClientConnection.class.getName()).log(Level.SEVERE, "Error while processing the filter chain.", ex);

         getOutput().println(message);
      }
      return null;
   }

   public Filter getNext() {
      return filter;
   }

   public void setNext(Filter filter) {
      this.filter = filter;
   }
}

/*
 * AbstractFilteredConnection.java
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

import java.net.Socket;
import java.net.SocketException;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import java.nio.charset.Charset;

/**
 *
 * @author Beselius
 */
public abstract class AbstractFilteredConnection extends Thread {

   private volatile boolean running;

   private String charset;
   private Socket socket;
   private PrintWriter output;

   public AbstractFilteredConnection(Socket socket) throws IOException {
      this(socket, Charset.defaultCharset().displayName());
   }

   public AbstractFilteredConnection(Socket socket, String charset) throws IOException {
      this.socket = socket;
      this.charset = charset;
      output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), charset), true);
   }

   @Override
   public void run() {
      try {
         BufferedReader input;
         String line;

         input = new BufferedReader(new InputStreamReader(socket.getInputStream(), charset));
         running = true;
         while (running && !isInterrupted()) {
            line = input.readLine();
            if (line == null) {
               running = false;
            } else {
               parse(line);
            }
         }
         close();

      } catch (SocketException ex) {
         Logger.getLogger(ClientConnection.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
      } catch (IOException ex) {
         Logger.getLogger(ClientConnection.class.getName()).log(Level.SEVERE, null, ex);
      }
   }

   public void close() {
      running = false;

      if (!socket.isInputShutdown()) {
         try {
            socket.shutdownInput();
         } catch (IOException ex) {
            Logger.getLogger(AbstractFilteredConnection.class.getName()).log(Level.WARNING, ex.getLocalizedMessage(), ex);
         }
      }
      if (!socket.isOutputShutdown()) {
         try {
            socket.shutdownOutput();
         } catch (IOException ex) {
            Logger.getLogger(AbstractFilteredConnection.class.getName()).log(Level.WARNING, ex.getLocalizedMessage(), ex);
         }
      }
      if (!socket.isClosed()) {
         try {
            try {
               sleep(100);
            } catch (InterruptedException ex) {
               Logger.getLogger(AbstractFilteredConnection.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
            }
            socket.close();
         } catch (IOException ex) {
            Logger.getLogger(ClientConnection.class.getName()).log(Level.WARNING, ex.getLocalizedMessage(), ex);
         }
      }
   }

   protected abstract void parse(String line);

   protected PrintWriter getOutput() {
      return output;
   }

   protected Socket getSocket() {
      return socket;
   }

   @Override
   public String toString() {
      return socket.toString();
   }
}

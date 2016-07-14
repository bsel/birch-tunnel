/*
 * Proxy.java
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

import birch.util.PluginLoader;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import java.util.*;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.locks.*;

import java.io.*;

import java.nio.charset.Charset;

/**
 *
 * @author Beselius
 */
public class Proxy extends Thread {

   public static interface Config {

      int getPort();

      String getAddress();

      String[] getFilterChain();

      String[] getPluginDirectories();

      String getDefaultServer();

      int getDefaultServerPort();

      String getCharset();
   }

   public static class PropertiesConfig implements Config {

      private String filename;

      private int port;
      private String address;
      private int defaultServerPort;
      private String defaultServer;
      private String[] filterChain;
      private String[] pluginDirectories;
      private String charset;

      public PropertiesConfig(String filename)
              throws FileNotFoundException, IOException {
         this.filename = filename;

         loadFromFile();
      }

      public int getPort() {
         return port;
      }

      public String getAddress() {
         return address;
      }

      public String getDefaultServer() {
         return defaultServer;
      }

      public int getDefaultServerPort() {
         return defaultServerPort;
      }

      public String[] getFilterChain() {
         return filterChain;
      }

      public String[] getPluginDirectories() {
         return pluginDirectories;
      }

      public String getCharset() {
         return charset;
      }

      private void loadFromFile() throws FileNotFoundException, IOException {
         Properties properties;
         InputStream inputStream;

         inputStream = new FileInputStream(filename);
         properties = new Properties();
         properties.load(inputStream);
         inputStream.close();

         port = Integer.parseInt(properties.getProperty("port", String.valueOf(DEFAULT_PORT)));
         address = properties.getProperty("address", DEFAULT_ADDRESS);
         defaultServerPort = Integer.parseInt(properties.getProperty("defaultServerPort", String.valueOf(DEFAULT_PORT)));
         defaultServer = properties.getProperty("defaultServer", "");
         filterChain = properties.getProperty("filterchain", "").split(" ");
         pluginDirectories = properties.getProperty("plugindirs", "").split(" ");
         charset = properties.getProperty("charset", Charset.defaultCharset().displayName());
      }
   }

   public final static int BACKLOG = 5;
   public final static String DEFAULT_CONFIG = "birch.config";
   public final static int DEFAULT_PORT = 6667;
   public final static String DEFAULT_ADDRESS = "0.0.0.0";

   private volatile boolean running;

   private Config config;
   private PluginLoader<FilterFactory> pluginLoader;
   private ReentrantLock filterChainLock;
   private volatile FilterChain filterChain;
   private ServerSocket socket;
   private String serverHost;
   private int serverPort;
   private int listenPort;
   private String listenAddress;
   private String charset;

   private final ExecutorService executor;
   private final Set<ClientConnection> clientConnections;

   public Proxy(String serverHost, int serverPort)
           throws ClassNotFoundException, IOException {
      this(serverHost, serverPort, DEFAULT_PORT);
   }

   public Proxy(String serverHost, int serverPort, int port)
           throws ClassNotFoundException, IOException {
      this(serverHost, serverPort, port, new PropertiesConfig(DEFAULT_CONFIG));
   }

   public Proxy(String serverHost, int serverPort, String address)
           throws ClassNotFoundException, IOException {
      this(serverHost, serverPort, address, new PropertiesConfig(DEFAULT_CONFIG));
   }

   public Proxy(String serverHost, int serverPort, String address, int port)
           throws ClassNotFoundException, IOException {
      this(serverHost, serverPort, address, port, new PropertiesConfig(DEFAULT_CONFIG));
   }

   public Proxy(String serverHost, int serverPort, int port, Config config)
           throws ClassNotFoundException, IOException {
      this(serverHost, serverPort, config);

      this.listenPort = port;
   }

   public Proxy(String serverHost, int serverPort, String address, Config config)
           throws ClassNotFoundException, IOException {
      this(serverHost, serverPort, config);

      this.listenAddress = address;
   }

   public Proxy(String serverHost, int serverPort, String address, int port, Config config)
           throws ClassNotFoundException, IOException {
      this(serverHost, serverPort, config);

      this.listenPort = port;
      this.listenAddress = address;
   }

   public Proxy(String serverHost, int serverPort, Config config)
           throws ClassNotFoundException, IOException {
      this.serverHost = serverHost;
      this.serverPort = serverPort;

      filterChainLock = new ReentrantLock();
      setConfig(config);
      clientConnections = Collections.synchronizedSet(new HashSet<ClientConnection>());
      executor = Executors.newFixedThreadPool(5);
   }

   private void setConfig(Config config) throws ClassNotFoundException, IOException {
      this.config = config;

      listenPort = config.getPort();
      listenAddress = config.getAddress();
      charset = config.getCharset();

      List<ServiceLoader<FilterFactory>> loaders;

      loaders = new ArrayList<ServiceLoader<FilterFactory>>();
      loaders.add(ServiceLoader.load(FilterFactory.class));

      pluginLoader = new PluginLoader<FilterFactory>(FilterFactory.class, config.getPluginDirectories());
      for (ServiceLoader<FilterFactory> loader : pluginLoader.getLoaders()) {
         loaders.add(loader);
      }

      filterChain = new FilterChain(loaders.toArray(new ServiceLoader[0]), config.getFilterChain());
   }

   @Override
   public void interrupt() {
      running = false;
      super.interrupt();
   }

   @Override
   public void run() {
      try {
         InetAddress address;

         address = InetAddress.getByName(listenAddress);
         socket = new ServerSocket(listenPort, BACKLOG, address);

         try {
            socket.setSoTimeout(3000);

            running = true;
            while (running) {
               Socket clientSocket;
               try {
                  clientSocket = socket.accept();
                  Logger.getLogger(Proxy.class.getName()).log(Level.INFO, "client connected");

                  addClientConnection(clientSocket);
               } catch (SocketTimeoutException ex) {
                  Logger.getLogger(Proxy.class.getName()).log(Level.FINER, "timeout exception to check for interrupt");
               }
            }
            Logger.getLogger(Proxy.class.getName()).log(Level.INFO, "Interrupted");
         } catch (IOException ex) {
            Logger.getLogger(Proxy.class.getName()).log(Level.SEVERE, null, ex);
         } finally {
            executor.shutdown();
            try {
               socket.close();
            } catch (IOException ex) {
               Logger.getLogger(Proxy.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            ClientConnection[] clients;
            synchronized (clientConnections) {
               clients = clientConnections.toArray(new ClientConnection[0]);
            }
            for (ClientConnection client : clients) {
               Logger.getLogger(Proxy.class.getName()).log(Level.INFO, "stopping client " + client);
               client.close();
            }
            try {
               interrupted();
               executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
               Logger.getLogger(Proxy.class.getName()).log(Level.SEVERE, null, ex);
            }
         }

      } catch (IOException ex) {
         Logger.getLogger(Proxy.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
      }
   }

   protected ClientConnection addClientConnection(Socket clientSocket) {
      ClientConnection result = null;

      filterChainLock.lock();
      try {
         result = new ClientConnection(
                 filterChain.getNewList(),
                 this,
                 clientSocket,
                 serverHost,
                 serverPort,
                 charset
              );

         executor.execute(result);
         clientConnections.add(result);

         Logger.getLogger(Proxy.class.getName()).log(Level.INFO, "client added");
      } catch (RejectedExecutionException ex) {
         Logger.getLogger(Proxy.class.getName()).log(Level.SEVERE, null, ex);
         result = null;
      } catch (IOException ex) {
         Logger.getLogger(Proxy.class.getName()).log(Level.SEVERE, null, ex);
         result = null;
      } finally {
         filterChainLock.unlock();
      }

      return result;
   }

   public void removeClientConnection(AbstractFilteredConnection clientConnection) {
      clientConnections.remove(clientConnection);
      Logger.getLogger(Proxy.class.getName()).log(Level.INFO, "client removed");
   }

   public void execute(AbstractFilteredConnection connection) {
      executor.execute(connection);
   }

   public ClientConnection[] getClientConnections() {
      return clientConnections.toArray(new ClientConnection[0]);
   }

   public FilterChain getFilterChain() {
      return filterChain;
   }

   public void setFilterChain(FilterChain filterChain) {
      filterChainLock.lock();
      try {
         this.filterChain = filterChain;
         for (ClientConnection client : clientConnections) {
            client.setNext(filterChain.getNewList());
         }
      } finally {
         filterChainLock.unlock();
      }
   }
}

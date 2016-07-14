/*
 * GUI.java
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
package birch.gui;

import birch.AbstractFilteredConnection;
import birch.ClientConnection;
import birch.FilterChain;
import birch.FilterFactory;
import birch.Proxy;
import birch.util.PluginLoader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.File;

import java.net.SocketException;
import java.net.Socket;

import java.util.*;
import java.util.ResourceBundle;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.LogManager;

import java.util.concurrent.*;

import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;

import javax.swing.DefaultListModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import java.text.MessageFormat;

/**
 *
 * @author Beselius
 */
public class GUI {

   private class ProxyConfig implements Proxy.Config {

      private String host;
      private int port;
      private String address;
      private int serverPort;
      private String[] filterChain;
      private String[] pluginDirectories;
      private String charset;

      public ProxyConfig(String address, int port, String host, int serverPort, String[] filterChain,
              String[] pluginDirectories, String charset) {
         this.host = host;
         this.port = port;
         this.address = address;
         this.serverPort = serverPort;
         this.filterChain = filterChain;
         this.pluginDirectories = pluginDirectories;
         this.charset = charset;
      }

      public int getPort() {
         return port;
      }

      public String getAddress() {
         return address;
      }

      public String[] getFilterChain() {
         return filterChain;
      }

      public String[] getPluginDirectories() {
         return pluginDirectories;
      }

      public String getDefaultServer() {
         return host;
      }

      public int getDefaultServerPort() {
         return serverPort;
      }

      public String getCharset() {
         return charset;
      }
   }

   private class ServerHandler {

      private class ModelProxy extends Proxy {

         private DefaultListModel listModel;
         private ServerHandler serverHandler;

         public ModelProxy(String serverHost, int serverPort, Config config, ServerHandler serverHandler)
                 throws ClassNotFoundException, IOException {
            super(serverHost, serverPort, config);

            this.serverHandler = serverHandler;
            listModel = new DefaultListModel();
         }

         @Override
         public void run() {
            super.run();

            java.awt.EventQueue.invokeLater(new Runnable() {

               public void run() {
                  servers.remove(serverHandler);
                  serverListModel.removeElement(serverHandler);
                  mainFrame.getTabbedPane().remove(serverTab);
               }
            });
         }

         private ClientConnection _addClientConnection(Socket clientSocket) {
             return super.addClientConnection(clientSocket);
         }

         @Override
         protected ClientConnection addClientConnection(final Socket clientSocket) {
            FutureTask<ClientConnection> result;

            result = new FutureTask<ClientConnection>(new Callable<ClientConnection>() {

               final DefaultListModel finalListModel = listModel;

               public ClientConnection call() throws Exception {
                  ClientConnection clientConnection;

                  synchronized (finalListModel) {
                     clientConnection = _addClientConnection(clientSocket);
                     if (clientConnection != null) {
                        finalListModel.addElement(clientConnection);
                     }
                     return clientConnection;
                  }
               }
            });

            java.awt.EventQueue.invokeLater(result);

            try {
               return result.get();
            } catch (InterruptedException ex) {
               Logger.getLogger(GUI.class.getName()).log(Level.WARNING, MessageFormat.format(
                       logBundle.getString("waiting_for_client_add"), ex.getLocalizedMessage()));
            } catch (ExecutionException ex) {
               Logger.getLogger(GUI.class.getName()).log(Level.WARNING, MessageFormat.format(
                       logBundle.getString("waiting_for_client_add"), ex.getLocalizedMessage()));
            }
            return null;
         }

         @Override
         public void removeClientConnection(final AbstractFilteredConnection clientConnection) {
            Logger.getLogger(ModelProxy.class.getName()).log(Level.FINER,
                    MessageFormat.format(logBundle.getString("Remove_client"), clientConnection));
            super.removeClientConnection(clientConnection);

            java.awt.EventQueue.invokeLater(new Runnable() {
               public void run() {
                  boolean inList;
                  final DefaultListModel finalListModel;

                  finalListModel = listModel;
                  synchronized (finalListModel) {
                     inList = finalListModel.removeElement(clientConnection);
                  }
                  if (inList) {
                     Logger.getLogger(ModelProxy.class.getName()).log(Level.FINEST,
                             MessageFormat.format(logBundle.getString("Removed_client"), clientConnection));
                  } else {
                     Logger.getLogger(ModelProxy.class.getName()).log(Level.FINEST,
                             MessageFormat.format(logBundle.getString("Client_not_in_List"), clientConnection));
                  }
               }
            });
         }

         public DefaultListModel getListModel() {
            return listModel;
         }
      }

      private ServerStatusPanel serverTab;
      private ModelProxy proxy;
      private ProxyConfig config;
      private DefaultListModel filterChainModel;

      private String name;

      public ServerHandler(ProxyConfig config) throws ClassNotFoundException, IOException {
         this.config = config;
         this.proxy = new ModelProxy(
                 config.getDefaultServer(),
                 config.getDefaultServerPort(),
                 config,
                 this
               );

         name = MessageFormat.format(bundle.getString("server_string"),
                 config.getDefaultServer(), String.valueOf(config.getDefaultServerPort()), String.valueOf(config.getPort()));

         serverTab = new ServerStatusPanel();
         serverTab.setName(name);
         prepareServerTab(serverTab);

         serverListModel.addElement(this);
         mainFrame.getTabbedPane().add(serverTab);

         executor.execute(proxy);
      }

      public void stopServer() {
         proxy.interrupt();
      }

      @Override
      public String toString() {
         return name;
      }

      private void prepareServerTab(final ServerStatusPanel serverTab) {
         serverTab.getClientsList().setModel(proxy.getListModel());

         serverTab.getDisconnectClientButton().addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
               Object[] selected;

               selected = serverTab.getClientsList().getSelectedValues();
               for (Object element : selected) {
                  if (element instanceof ClientConnection) {
                     ((ClientConnection) element).close();
                  }
               }
            }
         });

         filterChainModel = new DefaultListModel();
         String[] filters = config.getFilterChain();
         for (String filter : filters) {
            filterChainModel.addElement(filter);
         }
         filterChainModel.addListDataListener(new ListDataListener() {

            public void intervalAdded(ListDataEvent e) {
               updateFitlerChain();
            }

            public void intervalRemoved(ListDataEvent e) {
               updateFitlerChain();
            }

            public void contentsChanged(ListDataEvent e) {
               updateFitlerChain();
            }
            
            private void updateFitlerChain() {
               String[] filters;

               try {
                  filters = new String[filterChainModel.size()];
                  for (int i = 0; i < filters.length; ++i) {
                     filters[i] = filterChainModel.get(i).toString();
                  }
                  proxy.setFilterChain(new FilterChain(serviceLoaders, filters));
               } catch (ClassNotFoundException ex) {
                  Logger.getLogger(GUI.class.getName()).log(Level.WARNING, ex.getLocalizedMessage());
               }
            }
         });

         serverTab.getFilterConfigurationPanel().getFilterChainList().setModel(filterChainModel);

         serverTab.getFilterConfigurationPanel().getAvailableFiltersComboBox()
                 .setModel(availableFilterModel);

         serverTab.getFilterConfigurationPanel().getInsertFilterButton()
                 .addActionListener(new FilterChainAdd(
                     serverTab.getFilterConfigurationPanel().getAvailableFiltersComboBox(),
                     serverTab.getFilterConfigurationPanel().getFilterChainList()));

         serverTab.getFilterConfigurationPanel().getRemoveFiltersButton()
                 .addActionListener(new FilterChainRemove(
                     serverTab.getFilterConfigurationPanel().getFilterChainList()));
      }
   }

   private class StartServer implements ActionListener {

      public void actionPerformed(ActionEvent e) {
         try {
            ProxyConfig newConfig;
            String[] filterChain;

            filterChain = new String[filterChainModel.size()];
            for (int i = 0; i < filterChain.length; ++i) {
               filterChain[i] = filterChainModel.get(i).toString();
            }

            newConfig = new ProxyConfig(
                    mainFrame.getServerConfigurationPanel().getProxyAddressTextField().getText(),
                    Integer.parseInt(mainFrame.getServerConfigurationPanel().getProxyPortTextField().getText()),
                    mainFrame.getServerConfigurationPanel().getHostTextField().getText(),
                    Integer.parseInt(mainFrame.getServerConfigurationPanel().getPortTextField().getText()),
                    filterChain,
                    config.getPluginDirectories(),
                    config.getCharset()
                 );

            servers.add(new ServerHandler(newConfig));

         } catch (NumberFormatException ex) {
            Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage());
         } catch (RejectedExecutionException ex) {
            Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage());
         } catch (ClassNotFoundException ex) {
            Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage());
         } catch (FileNotFoundException ex) {
            Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage());
         } catch (SocketException ex) {
            Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage());
         } catch (IOException ex) {
            Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage());
         }
      }
   }

   private class StopServer implements ActionListener {

      public void actionPerformed(ActionEvent e) {
         Object[] values;

         values = mainFrame.getServerListPanel().getServerList().getSelectedValues();
         for (Object value : values) {
            if (value instanceof ServerHandler) {
               ((ServerHandler) value).stopServer();
            } else {
               serverListModel.removeElement(value);
            }
         }
      }
   }

   private class FilterChainAdd implements ActionListener {

      private JComboBox comboBox;
      private JList list;

      public FilterChainAdd(JComboBox comboBox, JList listModel) {
         this.comboBox = comboBox;
         this.list = listModel;
      }
      
      public void actionPerformed(ActionEvent e) {
         Object elementToAdd;
         int index;

         elementToAdd = comboBox.getSelectedItem();
         index = list.getMaxSelectionIndex();
         if (index == -1) {
            ((DefaultListModel) list.getModel()).addElement(elementToAdd);
         } else {
            ((DefaultListModel) list.getModel()).add(index, elementToAdd);
         }
      }
   }

   private class FilterChainRemove implements ActionListener {

      private JList list;

      public FilterChainRemove(JList list) {
         this.list = list;
      }

      public void actionPerformed(ActionEvent e) {
         int[] selected;

         selected = list.getSelectedIndices();
         for (int index = selected.length - 1; index >= 0; --index) {
            ((DefaultListModel) list.getModel()).remove(index);
         }
      }
   }

   private static final ResourceBundle bundle = java.util.ResourceBundle.getBundle("birch/gui/Bundle");
   private static final ResourceBundle logBundle = java.util.ResourceBundle.getBundle("birch/gui/LogBundle");

   private final ExecutorService executor;

   private MainFrame mainFrame;
   private AboutDialog aboutDialog;

   private final List<ServerHandler> servers;
   private DefaultListModel serverListModel;
   private DefaultComboBoxModel availableFilterModel;
   private DefaultListModel filterChainModel;

   private Proxy.Config config;
   private ServiceLoader<FilterFactory>[] serviceLoaders;
   private String[] filters;

   public GUI() throws IOException {
      executor = Executors.newFixedThreadPool(3);
      servers = Collections.synchronizedList(new ArrayList<ServerHandler>());
      config = new Proxy.PropertiesConfig("birch.config");

      mainFrame = new MainFrame();

      serverListModel = new DefaultListModel();
      mainFrame.getServerListPanel().getServerList().setModel(serverListModel);

      // Filters
      serviceLoaders = getServiceLoaders();
      filters = getFilters(serviceLoaders);
      availableFilterModel = new DefaultComboBoxModel(filters);
      mainFrame.getFilterConfigurationPanel().getAvailableFiltersComboBox().setModel(availableFilterModel);

      filterChainModel = new DefaultListModel();
      fillFilterChainModel();
      mainFrame.getFilterConfigurationPanel().getFilterChainList().setModel(filterChainModel);

      // ServerConfigurationPanel
      mainFrame.getServerConfigurationPanel().getStartServerButton()
              .addActionListener(new StartServer());
      mainFrame.getFilterConfigurationPanel().getInsertFilterButton()
              .addActionListener(new FilterChainAdd(
                    mainFrame.getFilterConfigurationPanel().getAvailableFiltersComboBox(),
                    mainFrame.getFilterConfigurationPanel().getFilterChainList()));
      mainFrame.getFilterConfigurationPanel().getRemoveFiltersButton()
              .addActionListener(new FilterChainRemove(
                    mainFrame.getFilterConfigurationPanel().getFilterChainList()));
      // Fill ServerConfigurationPanel with default data
      mainFrame.getServerConfigurationPanel()
              .getHostTextField().setText(config.getDefaultServer());
      mainFrame.getServerConfigurationPanel()
              .getPortTextField().setText(String.valueOf(config.getDefaultServerPort()));
      mainFrame.getServerConfigurationPanel()
              .getProxyPortTextField().setText(String.valueOf(config.getPort()));
      mainFrame.getServerConfigurationPanel()
              .getProxyAddressTextField().setText(config.getAddress());

      // ServerListPanel
      mainFrame.getServerListPanel().getStopButton()
              .addActionListener(new StopServer());

      // MainFrame
      mainFrame.addWindowListener(new WindowAdapter() {

         @Override
         public void windowClosed(WindowEvent e) {
            Logger.getLogger(GUI.class.getName()).log(Level.INFO, logBundle.getString("Window_closed"));
            exiting();
         }

         private void exiting() {
            executor.shutdown();
            synchronized (servers) {
               for (ServerHandler server : servers) {
                  Logger.getLogger(GUI.class.getName()).log(Level.INFO,
                          MessageFormat.format(logBundle.getString("stopping_server"), server));
                  server.stopServer();
               }
            }
            executor.shutdownNow();
         }
      });

      // Exit menue item
      mainFrame.getExitMenuItem().addActionListener(new ActionListener() {

         public void actionPerformed(ActionEvent e) {
            mainFrame.setVisible(false);
            mainFrame.dispose();
         }
      });

      // Help menue
      aboutDialog = new AboutDialog(mainFrame, true);
      mainFrame.getAboutMenuItem().addActionListener(new ActionListener() {

         public void actionPerformed(ActionEvent e) {
            aboutDialog.setVisible(true);
         }
      });

      mainFrame.setLocationByPlatform(true);
      mainFrame.setVisible(true);
   }

   private ServiceLoader<FilterFactory>[] getServiceLoaders() {
      PluginLoader<FilterFactory> pluginLoader;
      List<ServiceLoader<FilterFactory>> loaders;

      loaders = new ArrayList<ServiceLoader<FilterFactory>>();
      loaders.add(ServiceLoader.load(FilterFactory.class));

      pluginLoader = new PluginLoader<FilterFactory>(FilterFactory.class, config.getPluginDirectories());
      for (ServiceLoader<FilterFactory> loader : pluginLoader.getLoaders()) {
         loaders.add(loader);
      }

      return loaders.toArray(new ServiceLoader[0]);
   }

   private String[] getFilters(ServiceLoader<FilterFactory>[] serviceLoaders) throws FileNotFoundException, IOException {
      Set<String> result;
      
      result = new HashSet<String>();
      
      for (ServiceLoader<FilterFactory> loader : serviceLoaders) {
         for (FilterFactory factory : loader) {
            result.add(factory.getFitlerName());
         }
      }

      return result.toArray(new String[0]);
   }

   private void fillFilterChainModel() {
      List<String> availableFilters = Arrays.asList(this.filters);

      for (String filterName : config.getFilterChain()) {
         if (availableFilters.contains(filterName)) {
            filterChainModel.addElement(filterName);
         } else {
            Logger.getLogger(GUI.class.getName()).log(Level.WARNING,
                    MessageFormat.format(logBundle.getString("filter_not_found"), filterName));
         }
      }
   }

   /**
    * @param args the command line arguments
    */
   public static void main(String args[]) {
      java.awt.EventQueue.invokeLater(new Runnable() {

         public void run() {
            try {

               reloadLoggingConfiguration();
               new GUI();

            } catch (IOException ex) {
               Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, ex.getLocalizedMessage(), ex);
            }
         }
      });
   }

   private static void reloadLoggingConfiguration() throws IOException {
      File file;

      file = new File("logging.properties");
      if (file.exists()) {
         LogManager.getLogManager().readConfiguration(new FileInputStream(file));
         Logger.getLogger(GUI.class.getName())
                 .info(MessageFormat.format(logBundle.getString("load_log_properties"), file.getName()));
      }
   }
}

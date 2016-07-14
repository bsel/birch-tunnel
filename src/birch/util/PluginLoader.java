/*
 * PluginLoader.java
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
package birch.util;

import java.io.File;
import java.io.FilenameFilter;

import java.util.ServiceLoader;
import java.util.List;
import java.util.ArrayList;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 *
 * @author Beselius
 */
public class PluginLoader<T> {

   private static final FilenameFilter filenameFilter = new FilenameFilter() {

      public boolean accept(File dir, String name) {
         return name.endsWith(".jar");
      }
   };

   private String[] directories;
   private Class<T> service;
   private ServiceLoader<T>[] loaders;

   public PluginLoader(Class<T> service, String[] directories) {
      this.directories = directories;
      this.service = service;

      loaders = createLoaders(directories);
   }

   public ServiceLoader<T>[] getLoaders() {
      return loaders;
   }

   private ServiceLoader<T>[] createLoaders(String[] directories) {
      List<ServiceLoader<T>> result;
      List<URL> urls;
      ClassLoader classLoader;

      result = new ArrayList<ServiceLoader<T>>();

      urls = new ArrayList<URL>();
      for (String directory : directories) {
         Logger.getLogger(PluginLoader.class.getName()).log(Level.FINER, "search for jars in directory '" + directory + "'");

         File dir;
         File[] files;

         dir = new File(directory);
         if (!dir.exists() || !dir.isDirectory()) {
            continue;
         }

         urls.clear();
         files = dir.listFiles(filenameFilter);
         for (File file : files) {
            Logger.getLogger(PluginLoader.class.getName()).log(Level.FINEST, "add file '" + file.getName() + "'");
            try {
               urls.add(new URL("file", "", file.getAbsolutePath()));
            } catch (MalformedURLException ex) {
               Logger.getLogger(PluginLoader.class.getName()).log(Level.SEVERE, null, ex);
            }
         }
         classLoader = URLClassLoader.newInstance(urls.toArray(new URL[0]), ClassLoader.getSystemClassLoader());
         result.add(ServiceLoader.load(service, classLoader));
      }

      return result.toArray(new ServiceLoader[0]);
   }
}

/*
 * FilterConfigurationPanel.java
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

/*
 * FilterConfigurationPanel.java
 *
 * Created on 24.05.2009, 18:13:41
 */
package birch.gui;

import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JList;

/**
 *
 * @author Beselius
 */
public class FilterConfigurationPanel extends javax.swing.JPanel {

   /** Creates new form FilterConfigurationPanel */
   public FilterConfigurationPanel() {
      initComponents();
   }

   /** This method is called from within the constructor to
    * initialize the form.
    * WARNING: Do NOT modify this code. The content of this method is
    * always regenerated by the Form Editor.
    */
   @SuppressWarnings("unchecked")
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      availableFiltersLabel = new javax.swing.JLabel();
      availableFiltersComboBox = new javax.swing.JComboBox();
      filterChainLabel = new javax.swing.JLabel();
      insertFilterButton = new javax.swing.JButton();
      removeFiltersButton = new javax.swing.JButton();
      jScrollPane1 = new javax.swing.JScrollPane();
      filterChainList = new javax.swing.JList();

      java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("birch/gui/Bundle"); // NOI18N
      availableFiltersLabel.setText(bundle.getString("available_filters")); // NOI18N

      filterChainLabel.setText(bundle.getString("filter_chain")); // NOI18N

      insertFilterButton.setText(bundle.getString("insert")); // NOI18N

      removeFiltersButton.setText(bundle.getString("remove")); // NOI18N

      jScrollPane1.setViewportView(filterChainList);

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
      this.setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(availableFiltersLabel)
               .addComponent(filterChainLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(removeFiltersButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 236, Short.MAX_VALUE)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(availableFiltersComboBox, 0, 182, Short.MAX_VALUE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(insertFilterButton))
               .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 236, Short.MAX_VALUE))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(availableFiltersComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(availableFiltersLabel)
               .addComponent(insertFilterButton))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(filterChainLabel)
               .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 159, Short.MAX_VALUE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(removeFiltersButton)
            .addContainerGap())
      );
   }// </editor-fold>//GEN-END:initComponents
   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JComboBox availableFiltersComboBox;
   private javax.swing.JLabel availableFiltersLabel;
   private javax.swing.JLabel filterChainLabel;
   private javax.swing.JList filterChainList;
   private javax.swing.JButton insertFilterButton;
   private javax.swing.JScrollPane jScrollPane1;
   private javax.swing.JButton removeFiltersButton;
   // End of variables declaration//GEN-END:variables

   public JComboBox getAvailableFiltersComboBox() {
      return availableFiltersComboBox;
   }

   public JList getFilterChainList() {
      return filterChainList;
   }

   public JButton getInsertFilterButton() {
      return insertFilterButton;
   }

   public JButton getRemoveFiltersButton() {
      return removeFiltersButton;
   }
}

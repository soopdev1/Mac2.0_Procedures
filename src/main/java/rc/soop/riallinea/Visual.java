/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rc.soop.riallinea;

import static rc.soop.riallinea.AllineaRealOsp.allineaReport;
import static rc.soop.riallinea.Util.fd;
import static rc.soop.riallinea.Util.patternsql;
import static rc.soop.riallinea.Util.roundDoubleandFormat;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import org.joda.time.DateTime;

/**
 *
 * @author rcosco
 */
public class Visual extends javax.swing.JFrame {

    /**
     * Creates new form Visual
     */
    public Visual() {
        initComponents();
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dim.width / 2 - this.getSize().width / 2, dim.height / 2 - this.getSize().height / 2);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        nazione = new javax.swing.JComboBox<>();
        filials = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        solocentr = new javax.swing.JCheckBox();
        jButton3 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        jLabel1.setText("Mac2.0 Allinea");

        nazione.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "ITA", "CZ", "UK", "ITA TEST", "CZ TEST", "UK TEST" }));

        jLabel2.setText("NAZIONE");

        jLabel3.setText("FILIALE");

        jButton1.setText("ALLINEA STOCK");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setText("ALLINEA OSP PRECEDENTI");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        solocentr.setText("SOLO CENTRALE");

        jButton3.setText("APRI MAC");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 77, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel3))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(filials, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(nazione, javax.swing.GroupLayout.PREFERRED_SIZE, 124, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jSeparator1))
                        .addGap(40, 40, 40)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(solocentr, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(63, 63, 63)
                        .addComponent(jLabel1)))
                .addContainerGap(25, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(nazione, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(solocentr))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(filials, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jButton3)))
                .addGap(18, 18, 18)
                .addComponent(jButton1)
                .addGap(18, 18, 18)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton2)
                .addGap(26, 26, 26))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        String naz = this.nazione.getSelectedItem().toString();
        String fil_cod = this.filials.getText().trim();
        Db_Master db = null;
        if (naz.equals("ITA")) {
            db = new Db_Master();
        } else if (naz.equals("CZ")) {
            db = new Db_Master(true, false);
        } else if (naz.equals("UK")) {
            db = new Db_Master(false, true);
        } else if (naz.equals("ITA TEST")) {
            db = new Db_Master(false, false, true);
        } else if (naz.equals("CZ TEST")) {
            db = new Db_Master(true, false, true);
        } else if (naz.equals("UK TEST")) {
            db = new Db_Master(false, true, true);
        }

        if (db == null) {
            JOptionPane.showMessageDialog(this, "CODICE NAZIONE ERRATO", "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            //          CENTRALE
            List<IpFiliale> fi1ial_list_ip = db.getIpFiliale();
            List<String> filial_list = fi1ial_list_ip.stream().map(f1 -> f1.getFiliale()).collect(Collectors.toList());
            if (filial_list.contains(fil_cod)) {
                String fil[] = {fil_cod, fil_cod};
                boolean central_resp = allineaReport(db, fil);
                if (!central_resp) {
                    JOptionPane.showMessageDialog(this, "ALLINEAMENTO CENTRALE FALLITO. RIPROVARE,", "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    if (this.solocentr.isSelected()) {
                        JOptionPane.showMessageDialog(this, "Operazione completata correttamente", "Success", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        String ip = fi1ial_list_ip.stream().filter(f1 -> f1.getFiliale().equals(fil[0])).map(f1 -> f1.getIp()).collect(Collectors.toList()).get(0);
                        Db_Master filialdb = new Db_Master(true, ip);
                        boolean filial_resp = allineaReport(filialdb, fil);
                        filialdb.closeDB();
                        if (!filial_resp) {
                            JOptionPane.showMessageDialog(this, "ALLINEAMENTO FILIALE FALLITO. RIPROVARE,", "Error", JOptionPane.ERROR_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(this, "Operazione completata correttamente", "Success", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "CODICE FILIALE ERRATO", "Error", JOptionPane.ERROR_MESSAGE);
            }
            db.closeDB();
        }


    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed

// TODO add your handling code here:
        String naz = this.nazione.getSelectedItem().toString();
        String fil_cod = this.filials.getText().trim();
        Db_Master db = null;
        if (naz.equals("ITA")) {
            db = new Db_Master();
        } else if (naz.equals("CZ")) {
            db = new Db_Master(true, false);
        } else if (naz.equals("UK")) {
            db = new Db_Master(false, true);
        } else if (naz.equals("ITA TEST")) {
            db = new Db_Master(false, false, true);
        } else if (naz.equals("CZ TEST")) {
            db = new Db_Master(true, false, true);
        } else if (naz.equals("UK TEST")) {
            db = new Db_Master(false, true, true);
        }

        if (db == null) {
            JOptionPane.showMessageDialog(this, "CODICE NAZIONE ERRATO", "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            DateTime start = new DateTime().minusDays(20).withMillisOfDay(0);
            DateTime end = new DateTime().withMillisOfDay(0);
            //CENTRALE
            List<IpFiliale> fi1ial_list_ip = db.getIpFiliale();
            List<String> filial_list = fi1ial_list_ip.stream().map(f1 -> f1.getFiliale()).collect(Collectors.toList());
            if (filial_list.contains(fil_cod)) {
                while (start.isBefore(end)) {
                    String stdate = start.toString(patternsql) + " 23:59:59";
                    String fil[] = {fil_cod, fil_cod};
                    ArrayList<BranchStockInquiry_value> dati = db.list_BranchStockInquiry_value(fil, stdate, "CH");
                    if (!dati.isEmpty()) {
                        Office_sp sp = db.list_query_officesp2(fil[0], stdate.substring(0, 10)).get(0);
                        ArrayList<OfficeStockPrice_value> last = db.list_OfficeStockPrice_value(
                                sp.getCodice(), fil[0]);
                        for (int x = 0; x < last.size(); x++) {
                            OfficeStockPrice_value od = last.get(x);

                            for (int i = 0; i < dati.size(); i++) {

                                if (dati.get(i).getCurrency().equalsIgnoreCase(od.getCurrency()) && !od.getQta().equals(dati.get(i).getDati().get(0).toString())) {

                                    double nc = fd(dati.get(i).getDati().get(0).toString()) * fd(od.getMedioacq());
                                    String upd = "UPDATE office_sp_valori SET quantity = '" + dati.get(i).getDati().get(0).toString() + "', controv = '" + roundDoubleandFormat(nc, 2) + "' "
                                            + "WHERE cod ='" + sp.getCodice() + "' AND currency ='" + od.getCurrency() + "' AND kind ='01'";

                                    if (this.solocentr.isSelected()) {
                                        try {
                                            db.getC().createStatement().executeUpdate(upd);
                                        } catch (SQLException ex) {
                                            ex.printStackTrace();
                                            JOptionPane.showMessageDialog(this, "ALLINEAMENTO CENTRALE FALLITO. RIPROVARE,", "Error", JOptionPane.ERROR_MESSAGE);
                                        }
                                    } else {
                                        String ip = fi1ial_list_ip.stream().filter(f1 -> f1.getFiliale().equals(fil[0])).map(f1 -> f1.getIp()).collect(Collectors.toList()).get(0);
                                        Db_Master filialdb = new Db_Master(true, ip);
                                        if (filialdb.getC() == null) {
                                            JOptionPane.showMessageDialog(this, "ALLINEAMENTO FILIALE FALLITO. IMPOSSIBILE CONNETTERSI A FILIALE,", "Error", JOptionPane.ERROR_MESSAGE);
                                        } else {
                                            try {
                                                int upd_FILIAL = filialdb.getC().createStatement().executeUpdate(upd);
                                                if (upd_FILIAL > 0) {
                                                    db.getC().createStatement().executeUpdate(upd);
                                                }
                                            } catch (SQLException ex) {
                                                JOptionPane.showMessageDialog(this, "ALLINEAMENTO FILIALE FALLITO. RIPROVARE,", "Error", JOptionPane.ERROR_MESSAGE);
                                                ex.printStackTrace();
                                            }
                                            filialdb.closeDB();
                                        }
                                    }

                                }
                            }

                        }
                    }
                    start = start.plusDays(1);
                }
                JOptionPane.showMessageDialog(this, "Operazione completata correttamente", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "CODICE FILIALE ERRATO", "Error", JOptionPane.ERROR_MESSAGE);
            }

            db.closeDB();

        }


    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        // TODO add your handling code here:

        String naz = this.nazione.getSelectedItem().toString();
        String fil_cod = this.filials.getText().trim();
        Db_Master db = null;
        if (naz.equals("ITA")) {
            db = new Db_Master();
        } else if (naz.equals("CZ")) {
            db = new Db_Master(true, false);
        } else if (naz.equals("UK")) {
            db = new Db_Master(false, true);
        } else if (naz.equals("ITA TEST")) {
            db = new Db_Master(false, false, true);
        } else if (naz.equals("CZ TEST")) {
            db = new Db_Master(true, false, true);
        } else if (naz.equals("UK TEST")) {
            db = new Db_Master(false, true, true);
        }

        if (db == null) {
            JOptionPane.showMessageDialog(this, "CODICE NAZIONE ERRATO", "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            //          CENTRALE
            List<IpFiliale> fi1ial_list_ip = db.getIpFiliale();
            List<String> filial_list = fi1ial_list_ip.stream().map(f1 -> f1.getFiliale()).collect(Collectors.toList());
            if (filial_list.contains(fil_cod)) {

                String ip = fi1ial_list_ip.stream().filter(f1 -> f1.getFiliale().equals(fil_cod)).map(f1 -> f1.getIp()).collect(Collectors.toList()).get(0);
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    try {
                        Desktop.getDesktop().browse(new URI("http://" + ip + ":8080/Mac2.0/Login?type=1&username=utest0000&password=123456789b"));
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(this, "ERRORE APERTURA LINK, IP " + ip, "Error", JOptionPane.ERROR_MESSAGE);
                    }

                } else {
                    JOptionPane.showMessageDialog(this, "ERRORE APERTURA LINK, IP " + ip, "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "CODICE FILIALE ERRATO", "Error", JOptionPane.ERROR_MESSAGE);
            }
            db.closeDB();
        }


    }//GEN-LAST:event_jButton3ActionPerformed
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        }
        //</editor-fold>

        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            new Visual().setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField filials;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JComboBox<String> nazione;
    private javax.swing.JCheckBox solocentr;
    // End of variables declaration//GEN-END:variables
}

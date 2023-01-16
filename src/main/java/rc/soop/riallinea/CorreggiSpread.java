/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package rc.soop.riallinea;

import static rc.soop.riallinea.Util.createLog;
import static rc.soop.riallinea.Util.estraiEccezione;
import static rc.soop.riallinea.Util.fd;
import static rc.soop.riallinea.Util.roundDouble;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.joda.time.DateTime;
import rc.soop.maintenance.Monitor;

/**
 *
 * @author rcosco
 */
public class CorreggiSpread {

    private static final Logger log = createLog("Mac2.0_CorreggiSpreadCZ_", Monitor.rb.getString("path.log"), "yyyyMMdd");

    public static void enginecz() {
        try {
            Db_Master db1 = new Db_Master(true, false);
            List<IpFiliale> lf = db1.getIpFiliale();
            try (Statement st1 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_UPDATABLE); 
                    ResultSet rs1 = st1.executeQuery("SELECT c.cod,c.data,c.filiale,c.tipotr,c.id FROM ch_transaction c WHERE c.data > '2021-10-01 00:00:00' AND CAST(c.spread_total AS DECIMAL(12,8)) = 0")) {
                while (rs1.next()) {
                    String codtr = rs1.getString(1);
                    String data = new DateTime(rs1.getDate("c.data").getTime()).toString("yyyy-MM-dd");
                    String filiale = rs1.getString("c.filiale");
                    String tipotr = rs1.getString("c.tipotr");
                    String id = rs1.getString("c.id");
                    String ip = lf.stream().filter(d1 -> d1.getFiliale().equals(filiale)).findAny().get().getIp();

                    try (Statement st2 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                            ResultSet.CONCUR_UPDATABLE); 
                            ResultSet rs2 = st2.executeQuery("SELECT c.valuta,c.quantita,c.rate,c.spread FROM ch_transaction_valori c WHERE c.cod_tr='" + codtr + "'")) {
                        if (rs2.next()) {
                            String valuta = rs2.getString(1);
                            String quantita = rs2.getString(2);
                            String rate = rs2.getString(3);
                            String spread = rs2.getString(4);
                            if (fd(spread) == 0.00) {

                                double cv = roundDouble(fd(quantita) * fd(rate), 2);

                                try (Statement st3 = db1.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                                        ResultSet.CONCUR_UPDATABLE); 
                                        ResultSet rs3 = st3.executeQuery("SELECT rif_bce FROM rate WHERE valuta='" + valuta + "' AND data='" + data + "'")) {
                                    if (rs3.next()) {
                                        String bce = rs3.getString(1);
                                        double cv_bce = roundDouble(fd(quantita) * fd(bce), 2);

                                        double spread_res = roundDouble(cv - cv_bce, 2);
                                        if (tipotr.equals("B")) {
                                            spread_res = roundDouble(cv_bce - cv, 2);
                                        }

                                        Db_Master db2 = new Db_Master(true, ip);
                                        if (db2.getC() != null) {
                                            //  CENTRALE
                                            try (Statement st3A = db2.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                                                    ResultSet.CONCUR_UPDATABLE)) {
                                                st3A.executeUpdate("UPDATE ch_transaction SET spread_total='" + spread_res + "' WHERE cod = '" + codtr + "'");
                                                st3A.executeUpdate("UPDATE ch_transaction_valori SET spread='" + spread_res + "' WHERE cod_tr = '" + codtr + "'");
                                                log.log(Level.INFO, "OK CENTRALE {0} - {1} - {2} - {3} - {4} - {5} - {6} - {7} - {8} - {9} - {10}", new Object[]{filiale, data, id, ip, spread, valuta, quantita, rate, cv, cv_bce, spread_res});
                                            }
                                            //  FILIALE
                                            try (Statement st4 = db2.getC().createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                                                    ResultSet.CONCUR_UPDATABLE)) {
                                                st4.executeUpdate("UPDATE ch_transaction SET spread_total='" + spread_res + "' WHERE cod = '" + codtr + "'");
                                                st4.executeUpdate("UPDATE ch_transaction_valori SET spread='" + spread_res + "' WHERE cod_tr = '" + codtr + "'");
                                                log.log(Level.INFO, "OK FILIALE {0} - {1} - {2} - {3} - {4} - {5} - {6} - {7} - {8} - {9} - {10}", new Object[]{filiale, data, id, ip, spread, valuta, quantita, rate, cv, cv_bce, spread_res});
                                            }
                                            
                                            db2.closeDB();
                                        } else {
                                            log.log(Level.SEVERE, "KO FILIALE NON RAGGIUNGIBILE {0} {1} {2}", new Object[]{filiale, data, id});
                                        }

                                    }
                                }
                            } else {
                                log.log(Level.SEVERE, "VERIFICARE {0}", codtr);
                            }
                        }
                    }
                }
            }

            db1.closeDB();

        } catch (Exception ex) {
            ex.printStackTrace();
            log.severe(estraiEccezione(ex));
        }

    }
//
    public static void main(String[] args) {
        enginecz();
    }

}

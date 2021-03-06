/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.refill.start;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import static it.refill.start.Central_Branch.pwd_f;
import static it.refill.start.Central_Branch.pwd_h;
import static it.refill.start.Central_Branch.user_f;
import static it.refill.start.Central_Branch.user_h;
import static it.refill.start.Utility.createLog;
import static it.refill.start.Utility.pattern4;
import static it.refill.start.Utility.rb;

/**
 *
 * @author rcosco
 */
public class ReadTracking {

    private static final Logger log = createLog("Mac2.0_ReadTracking_", rb.getString("path.log"), pattern4);
    
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private static final String sql = "SELECT id,user,descr,timestamp FROM tr WHERE descr LIKE '%PENDING%' AND TIMESTAMP LIKE CONCAT(CURDATE(),'%')";
    private static final String sql_test = "SELECT id,user,descr,timestamp FROM tr WHERE descr LIKE '%PENDING%' AND TIMESTAMP > '2019-01-01 00:00:00'";
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void test() {
        DBHost dbm0 = new DBHost("//172.18.17.41:3306/maccorp", user_h, pwd_h, log);
        ArrayList<String> list = dbm0.list_branch_enabled();
        list.forEach(bra -> {
            if (!bra.equals("000")) {
                String myip = dbm0.getIpFiliale(bra);
                if (myip != null && !myip.equals("")) {
                    DBFiliale dbfiliale = new DBFiliale(myip, bra, user_f, pwd_f, log);
                    if (dbfiliale.getConnectionDB() == null) {
                        log.log(Level.SEVERE, "BRANCH: {0} - IP: {1} non raggiungibile.", new Object[]{bra, myip});
                    } else {
                        try {
                            ResultSet rs = dbfiliale.getConnectionDB().createStatement().executeQuery(sql_test);

                            while (rs.next()) {
                                String ins = "INSERT INTO tr_sblocco VALUES (?,?,?,?,?)";
                                PreparedStatement ps = dbm0.getConnectionDB().prepareStatement(ins);
                                ps.setString(1, rs.getString(1));
                                ps.setString(2, bra);
                                ps.setString(3, rs.getString(2));
                                ps.setString(4, rs.getString(3));
                                ps.setString(5, rs.getString(4));
                                try {
                                    ps.execute();
                                    log.log(Level.INFO, "INSERITO: {0}", ps.toString());
                                } catch (Exception ex) {
                                    if (!ex.getMessage().toLowerCase().contains("duplicate")) {
                                        log.log(Level.SEVERE, "FILIALE: {0} ERROR: {1}", new Object[]{bra, ex.getMessage()});
                                    }
                                }
                            }
                            rs.close();
                        } catch (SQLException ex) {
                            log.log(Level.SEVERE, "FILIALE: {0} ERROR: {1}", new Object[]{bra, ex.getMessage()});
                        }
                        dbfiliale.closeDB();
                    }
                }
            }
        });
        dbm0.closeDB();
        
        
    }
    
    public static void prod() {
        DBHost dbm0 = new DBHost("//172.18.17.41:3306/maccorpita", user_h, pwd_h, log);
        ArrayList<String> list = dbm0.list_branch_enabled();
        list.forEach(bra -> {
            if (!bra.equals("000")) {
                String myip = dbm0.getIpFiliale(bra);
                if (myip != null && !myip.equals("")) {
                    DBFiliale dbfiliale = new DBFiliale(myip, bra, user_f, pwd_f, log);
                    if (dbfiliale.getConnectionDB() == null) {
                        log.log(Level.SEVERE, "BRANCH: {0} - IP: {1} non raggiungibile.", new Object[]{bra, myip});
                    } else {
                        try {
                            ResultSet rs = dbfiliale.getConnectionDB().createStatement().executeQuery(sql);

                            while (rs.next()) {
                                String ins = "INSERT INTO tr_sblocco VALUES (?,?,?,?,?)";
                                PreparedStatement ps = dbm0.getConnectionDB().prepareStatement(ins);
                                ps.setString(1, rs.getString(1));
                                ps.setString(2, bra);
                                ps.setString(3, rs.getString(2));
                                ps.setString(4, rs.getString(3));
                                ps.setString(5, rs.getString(4));
                                try {
                                    ps.execute();
                                    log.log(Level.INFO, "INSERITO: {0}", ps.toString());
                                } catch (Exception ex) {
                                    if (!ex.getMessage().toLowerCase().contains("duplicate")) {
                                        log.log(Level.SEVERE, "FILIALE: {0} ERROR: {1}", new Object[]{bra, ex.getMessage()});
                                    }
                                }
                            }
                            rs.close();
                        } catch (SQLException ex) {
                            log.log(Level.SEVERE, "FILIALE: {0} ERROR: {1}", new Object[]{bra, ex.getMessage()});
                        }
                        dbfiliale.closeDB();
                    }
                }
            }
        });
        dbm0.closeDB();
    }
    
//    public static void main(String[] args) {
////        test();
////        prod();
//    }

}

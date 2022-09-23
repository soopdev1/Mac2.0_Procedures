/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.refill.esolver;

import static it.refill.esolver.Util.log;
import static it.refill.esolver.Util.test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 *
 * @author rcosco
 */
public class Db_ATL {

    public Connection c = null;

    public Connection getC() {
        return c;
    }

    public void setC(Connection c) {
        this.c = c;
    }

    public Db_ATL() {
        String drivername = "org.mariadb.jdbc.Driver";
        String typedb = "mariadb";
        String user = "maccorp";
        String pwd = "M4cc0Rp";

        String host = "//macdb01.mactwo.loc:3306/macatl";
        if (test) {
            host = "//macdb01.mactwo.loc:3306/macatl_test";
        }

        try {
            Class.forName(drivername).newInstance();
            Properties p = new Properties();
            p.put("user", user);
            p.put("password", pwd);
            p.put("useUnicode", "true");
            p.put("characterEncoding", "UTF-8");
            p.put("useSSL", "false");
            this.c = DriverManager.getConnection("jdbc:" + typedb + ":" + host, p);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SQLException ex) {
            log.log(Level.SEVERE, "{0}: {1}", new Object[]{ex.getStackTrace()[0].getMethodName(), ex.getMessage()});
        }
    }

    public void closeDB() {
        try {
            if (this.c != null) {
                this.c.close();
            }
        } catch (SQLException ex) {
            log.log(Level.SEVERE, "{0}: {1}", new Object[]{ex.getStackTrace()[0].getMethodName(), ex.getMessage()});
        }
    }

    public int setDateCorrect() {
        int x;
        try {
            try (Statement st1 = this.c.createStatement()) {
                x = st1.executeUpdate("UPDATE dati_fatture SET dateoper=datereg WHERE dateoper<>datereg");
            }
        } catch (SQLException ex) {
            log.log(Level.SEVERE, "{0}: {1}", new Object[]{ex.getStackTrace()[0].getMethodName(), ex.getMessage()});
            x = -1;
        }
        return x;
    }

    public List<Atl_dati_clienti> atl_c1(String data, String filiale) {
        List<Atl_dati_clienti> out = new ArrayList<>();
        try {
            String sql2 = "SELECT * FROM dati_clienti WHERE clientcode IN (SELECT contoreg FROM details_dati_fatture "
                    + "WHERE cod IN (SELECT cod FROM dati_fatture WHERE dateoper like '" + data
                    + "%' AND cod LIKE '" + filiale + "%')) AND clientcode NOT IN (SELECT DISTINCT(contoreg) from details_dati_fatture WHERE category = 'TUR' "
                    + "AND cod like'" + filiale + "%') GROUP BY clientcode;";
            try (ResultSet rs1 = this.c.createStatement().executeQuery(sql2)) {
                while (rs1.next()) {
                    Atl_dati_clienti df1 = new Atl_dati_clienti(
                            rs1.getString(1), rs1.getString(2),
                            rs1.getString(3),
                            rs1.getString(3),
                            //                        rs1.getString(4), 
                            rs1.getString(5), rs1.getString(6),
                            rs1.getString(7), rs1.getString(8), rs1.getString(9),
                            rs1.getString(10), rs1.getString(11)
                    );
                    out.add(df1);
                }
            }
        } catch (SQLException ex) {
            log.log(Level.SEVERE, "{0}: {1}", new Object[]{ex.getStackTrace()[0].getMethodName(), ex.getMessage()});
        }
        return out;

    }

    public List<Atl_dati_fatture> atl_f1_P2(String data, String filiale) {
        List<Atl_dati_fatture> out = new ArrayList<>();
        List<Atl_details_dati_fatture> df = new ArrayList<>();
        List<Atl_detailsiva_dati_fatture> di = new ArrayList<>();
        try {
            String sql3 = "SELECT * FROM details_dati_fatture WHERE cod IN "
                    + "(SELECT cod FROM dati_fatture WHERE datereg like '" + data + "%' AND tipomov='C' AND cod LIKE '" + filiale + "%') ORDER by cod,numriga";
            try (ResultSet rs3 = this.c.createStatement().executeQuery(sql3)) {
                while (rs3.next()) {
                    df.add(new Atl_details_dati_fatture(rs3.getString(1), rs3.getString(2), rs3.getString(3), rs3.getString(4),
                            rs3.getString(5), rs3.getString(6), rs3.getString(7), rs3.getString(8), rs3.getString(9),
                            rs3.getString(10), rs3.getString(11), rs3.getString(12), rs3.getString(13)));
                }
            }

            String sql1 = "SELECT * FROM dati_fatture WHERE datereg like '" + data + "%' AND tipomov='C' AND cod LIKE '" + filiale + "%' ORDER BY datereg";
            try (ResultSet rs1 = this.c.createStatement().executeQuery(sql1)) {
                while (rs1.next()) {
                    String cod = rs1.getString(1);

                    Atl_dati_fatture df1 = new Atl_dati_fatture(rs1.getString(1),
                            rs1.getString(2), rs1.getString(3), rs1.getString(4),
                            rs1.getString(5), rs1.getString(6),
                            rs1.getString(7), rs1.getString(8),
                            rs1.getString(10), rs1.getString(9),
                            df.stream().filter(d1 -> d1.getCod().equals(cod)).collect(Collectors.toList()),
                            di.stream().filter(d1 -> d1.getCod().equals(cod)).collect(Collectors.toList())
                    );
                    out.add(df1);
                }
            }
        } catch (SQLException ex) {
            log.log(Level.SEVERE, "{0}: {1}", new Object[]{ex.getStackTrace()[0].getMethodName(), ex.getMessage()});
        }
        return out;
    }

    public List<Atl_dati_fatture> atl_f1_P4(String data, String filiale) {
        List<Atl_dati_fatture> out = new ArrayList<>();
        List<Atl_details_dati_fatture> df = new ArrayList<>();
        try {

            String sql3 = "SELECT * FROM details_dati_fatture WHERE cod IN "
                    + "(SELECT cod FROM dati_fatture WHERE datereg like '" + data + "%' "
                    + "AND tipomov='G' AND cod LIKE '" + filiale + "%') ORDER by cod,numriga";
            try (ResultSet rs3 = this.c.createStatement().executeQuery(sql3)) {
                while (rs3.next()) {
                    df.add(new Atl_details_dati_fatture(rs3.getString(1), rs3.getString(2),
                            rs3.getString(3), rs3.getString(4),
                            rs3.getString(5), rs3.getString(6), rs3.getString(7),
                            rs3.getString(8), rs3.getString(9),
                            rs3.getString(10), rs3.getString(11),
                            rs3.getString(12), rs3.getString(13)));
                }
            }

            String sql1 = "SELECT * FROM dati_fatture WHERE datereg like '" + data + "%' AND tipomov='G' AND cod LIKE '" + filiale + "%' ORDER BY datereg";
            try (ResultSet rs1 = this.c.createStatement().executeQuery(sql1)) {
                while (rs1.next()) {
                    String cod = rs1.getString(1);
                    Atl_dati_fatture df1 = new Atl_dati_fatture(rs1.getString(1), rs1.getString(2),
                            rs1.getString(3), rs1.getString(4), rs1.getString(5), rs1.getString(6),
                            rs1.getString(7), rs1.getString(8), rs1.getString(10), rs1.getString(9),
                            df.stream().filter(d1 -> d1.getCod().equals(cod)).collect(Collectors.toList()),
                            new ArrayList<>()
                    );
                    out.add(df1);
                }
            }
        } catch (SQLException ex) {
            log.log(Level.SEVERE, "{0}: {1}", new Object[]{ex.getStackTrace()[0].getMethodName(), ex.getMessage()});
        }
        return out;
    }

    public List<Atl_dati_fatture> atl_f1_P6(String data, String filiale) {
        List<Atl_dati_fatture> out = new ArrayList<>();
        List<Atl_details_dati_fatture> df = new ArrayList<>();
        List<Atl_detailsiva_dati_fatture> di = new ArrayList<>();
        try {
            String sql2 = "SELECT * FROM details_dativa_fatture WHERE cod IN "
                    + "(SELECT cod FROM dati_fatture WHERE datereg like '" + data
                    + "%' AND cod LIKE '" + filiale + "%' AND (tipomov='F' OR tipomov='N')) ORDER by cod,nrigaiva";

            try (ResultSet rs2 = this.c.createStatement().executeQuery(sql2)) {
                while (rs2.next()) {
                    di.add(new Atl_detailsiva_dati_fatture(rs2.getString(1), rs2.getString(2), rs2.getString(3),
                            rs2.getString(4), rs2.getString(5), rs2.getString(6), rs2.getString(7)));
                }
            }

            String sql3 = "SELECT * FROM details_dati_fatture WHERE cod IN "
                    + "(SELECT cod FROM dati_fatture WHERE datereg like '" + data
                    + "%' AND cod LIKE '" + filiale + "%' AND (tipomov='F' OR tipomov='N'))"
                    + " AND tipoconto <> 'I' ORDER by cod,numriga";

            try (ResultSet rs3 = this.c.createStatement().executeQuery(sql3)) {
                while (rs3.next()) {
                    df.add(new Atl_details_dati_fatture(rs3.getString(1), rs3.getString(2), rs3.getString(3), rs3.getString(4),
                            rs3.getString(5), rs3.getString(6), rs3.getString(7), rs3.getString(8), rs3.getString(9),
                            rs3.getString(10), rs3.getString(11), rs3.getString(12), rs3.getString(13)));
                }
            }

            String sql1 = "SELECT * FROM dati_fatture WHERE datereg like '" + data + "%' AND (tipomov='F' OR tipomov='N') AND cod LIKE '" + filiale + "%' ORDER BY datereg";
            try (ResultSet rs1 = this.c.createStatement().executeQuery(sql1)) {
                while (rs1.next()) {
                    String cod = rs1.getString(1);
                    Atl_dati_fatture df1 = new Atl_dati_fatture(rs1.getString(1), rs1.getString(2), rs1.getString(3), rs1.getString(4), rs1.getString(5), rs1.getString(6),
                            rs1.getString(7), rs1.getString(8),
                            rs1.getString(10), rs1.getString(9),
                            df.stream().filter(d1 -> d1.getCod().equals(cod)).collect(Collectors.toList()),
                            di.stream().filter(d1 -> d1.getCod().equals(cod)).collect(Collectors.toList())
                    );
                    out.add(df1);
                }
            }
        } catch (SQLException ex) {
            log.log(Level.SEVERE, "{0}: {1}", new Object[]{ex.getStackTrace()[0].getMethodName(), ex.getMessage()});
        }
        return out;
    }

}

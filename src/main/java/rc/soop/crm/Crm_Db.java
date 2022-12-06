/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rc.soop.crm;

import static rc.soop.crm.Action.log;
import static rc.soop.crm.CRM_batch.test;
import static rc.soop.crm.MailObject.sendMailHtml;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import org.apache.commons.validator.routines.EmailValidator;

/**
 *
 * @author agodino
 */
public class Crm_Db {

    Connection c = null;

    public Crm_Db() {
        try {
            String drivername = "org.mariadb.jdbc.Driver";
            String typedb = "mariadb";
            String user = "maccorp";
            String pwd = "M4cc0Rp";
            String host = "//172.18.17.41:3306/mac_crm_prod";
            if (test) {
                host = "//172.18.17.41:3306/mac_crm";
            }
            Class.forName(drivername).newInstance();
            Properties p = new Properties();
            p.put("user", user);
            p.put("password", pwd);
            p.put("useUnicode", "true");
            p.put("characterEncoding", "UTF-8");
            p.put("useSSL", "false");
            this.c = DriverManager.getConnection("jdbc:" + typedb + ":" + host, p);
            this.c.createStatement().execute("SET GLOBAL max_allowed_packet=1024*1024*1024;");
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SQLException ex) {
            this.c = null;
            log.log(Level.SEVERE, "connection CRM_DB: {0}", ex.getMessage());
            sendMailHtml("mac2.0@setacom.it", "ERROR PREAUTH", "CONTROLLARE ERRORE CONNESSIONE DB PROCEDURA NOTTURNA CRM/CRV PREAUTH " + rc.soop.crm.CRM_batch.test + " :" + ex.getMessage());
        }
    }

    public Connection getC() {
        return c;
    }

    public void setC(Connection c) {
        this.c = c;
    }

    public void closeDB() {
        try {
            if (this.c != null) {
                this.c.close();
            }
        } catch (SQLException ex) {
            log.log(Level.SEVERE, "closeDB: {0}", ex.getMessage());
        }
    }

    public void insertTracking(String code, String action) {
        try {
            String sql = "INSERT INTO tracking SET idoperatore = ?, azione = ?";
            PreparedStatement ps = this.c.prepareStatement(sql);
            ps.setString(1, code);
            ps.setString(2, action);
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.log(Level.SEVERE, "insertTracking: {0}", ex.getMessage());
        }
    }

    public String getUserPermission(String name) {//fatto
        try {
            String sql = "SELECT permessi FROM pagina WHERE nome = ?";
            PreparedStatement ps = this.c.prepareStatement(sql);
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("permessi");
            }
        } catch (SQLException ex) {
            log.log(Level.SEVERE, "getUserPermission: {0}", ex.getMessage());
        }
        return "";
    }

    public int get_Day_NOSHOW() {
        try {
            String sql = "SELECT value FROM settings WHERE id = 'NOS'";
            PreparedStatement ps = this.c.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int x = Integer.parseInt(rs.getString(1));
                return x;
            }
        } catch (NumberFormatException | SQLException ex) {
            log.log(Level.SEVERE, "get_day_NOSHOW: {0}", ex.getMessage());
        }
        return -1;
    }

    public int get_Day_RECAP() {
        try {
            String sql = "SELECT value FROM settings WHERE id = 'REC'";
            PreparedStatement ps = this.c.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int x = Integer.parseInt(rs.getString(1));
                return x;
            }
        } catch (NumberFormatException | SQLException ex) {
            log.log(Level.SEVERE, "get_day_RECAP: {0}", ex.getMessage());
        }
        return -1;
    }

    public Settings get_Settings(String id) {
        try {
            String sql = "SELECT * FROM settings WHERE id = ?";
            PreparedStatement ps = this.c.prepareStatement(sql);
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Settings(rs.getString(1), rs.getString(2), rs.getString(3));
            }
        } catch (SQLException ex) {
            log.log(Level.SEVERE, "getSettings: {0}", ex.getMessage());
        }
        return null;
    }

    public void insertpreauth(Preauth pr) {
        try {
            String sql = "INSERT INTO preauth VALUES (?,?,?,?,?,?,?,?,?)";
            PreparedStatement ps = this.c.prepareStatement(sql);
            ps.setString(1, pr.getCodice());
            ps.setString(2, pr.getPrenotazione());
            ps.setString(3, pr.getRequest());
            ps.setString(4, pr.getRequest_date());
            ps.setString(5, pr.getResponse_code());
            ps.setString(6, pr.getResponse_message());
            ps.setString(7, pr.getResponse());
            ps.setString(8, pr.getResponse_date());
            ps.setString(9, pr.getStato());
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.log(Level.SEVERE, "insertpreauth: {0}", ex.getMessage());
        }
    }

    public String getEmail(String filiale) {
        try {
            String sql = "SELECT email FROM email WHERE filiale = ?";
            PreparedStatement ps = this.c.prepareStatement(sql);
            ps.setString(1, filiale);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String mail = rs.getString(1);
                if (EmailValidator.getInstance().isValid(mail)) {
                    return mail;
                }
            }
        } catch (SQLException ex) {
            log.log(Level.SEVERE, "getEmail: {0}", ex.getMessage());
        }
        return null;
    }

    public List<Items> list_Branch_Website() {
        List<Items> output = new ArrayList<>();
        try {
            String s1 = "SELECT * FROM branch ORDER BY de_branch";
            PreparedStatement ps = this.c.prepareStatement(s1);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                output.add(new Items(rs.getString(1), (rs.getString(2)), (rs.getString(3))));
            }
        } catch (SQLException ex) {
            log.log(Level.SEVERE, "list_Branch_Website: {0}", ex.getMessage());
        }
        return output;
    }

    public boolean insert_Branch(String cod, String de_branch) {
        try {
            String s1 = "INSERT INTO branch VALUES (?,?,?)";
            PreparedStatement ps = this.c.prepareStatement(s1);
            ps.setString(1, cod);
            ps.setString(2, de_branch);
            ps.setString(3, "0");
            ps.execute();
            return true;
        } catch (SQLException ex) {
            log.log(Level.SEVERE, "insert_Branch: {0}", ex.getMessage());
        }
        return false;
    }

}

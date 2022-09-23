package it.refill.testarea;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import static it.refill.testarea.Utility.patternsqldate;
import org.joda.time.DateTime;

/**
 *
 * @author rcosco
 */
public class Db_Loy {

    public Connection c = null;

    public Connection getC() {
        return c;
    }

    public void setC(Connection c) {
        this.c = c;
    }

    public Db_Loy() {
        try {
            String drivername = "org.mariadb.jdbc.Driver";
            String typedb = "mariadb";
            String user = "loyalty";
            String pwd = "loyalty";
            String host = "//machaproxy01.mactwo.loc:3306/loyalty";
            Class.forName(drivername).newInstance();
            Properties p = new Properties();
            p.put("user", user);
            p.put("password", pwd);
            p.put("characterEncoding", "UTF-8");
            p.put("useSSL", "false");
            p.put("connectTimeout", "1000");
            this.c = DriverManager.getConnection("jdbc:" + typedb + ":" + host, p);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SQLException ex) {
            this.c = null;
        }
    }

    public void closeDB() {
        try {
            if (this.c != null) {
                this.c.close();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public String getCodiceCliente(String codloya) {
        try {
            String sql = "SELECT codcliente FROM mac_associate WHERE codloya = '" + codloya + "'";
            try (PreparedStatement ps = this.c.prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;

    }

    public String getCodiceCompleto(String codloya, String stato) {
        try {
            String sql = "SELECT codice FROM codici WHERE pubblico = '" + codloya + "' AND stato='" + stato + "'";
            try (PreparedStatement ps = this.c.prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public String[] getCodiceCompleto(String codloya) {
        try {
            String sql = "SELECT codice,stato FROM codici WHERE pubblico = '" + codloya + "'";
            try (PreparedStatement ps = this.c.prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String[] o = {rs.getString(1), rs.getString(2)};
                    return o;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public boolean update_mac_associate(String codcliente, String codloya, String stato) {
        try {
            String upd = "UPDATE mac_associate SET stato = ? WHERE codcliente = ? AND codloya = ?";
            try (PreparedStatement ps = this.c.prepareStatement(upd)) {
                ps.setString(1, stato);
                ps.setString(2, codcliente);
                ps.setString(3, codloya);
                ps.executeUpdate();
                return true;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public boolean update_stato_codici(String codloya, String stato) {
        try {
            String upd = "UPDATE codici SET stato = ? WHERE codice = ?";
            try (PreparedStatement ps = this.c.prepareStatement(upd)) {
                ps.setString(1, stato);
                ps.setString(2, codloya);
                ps.executeUpdate();
                return true;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public boolean ins_mac_associate(String codcliente, String codloya) {
        try {
            String ins = "INSERT INTO mac_associate (codcliente,codloya,data) VALUES (?,?,?)";
            try (PreparedStatement ps = this.c.prepareStatement(ins)) {
                ps.setString(1, codcliente);
                ps.setString(2, codloya);
                ps.setString(3, new DateTime().toString(patternsqldate));
                ps.execute();
                return true;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            if (ex.getMessage().contains("Duplicate")) {
                return update_mac_associate(codcliente, codloya, "1");
            }
        }
        return false;
    }

    public String getCodiceClienteAttivo(String codcliente) {
        try {
            String sql = "SELECT RIGHT(codloya,8) FROM mac_associate WHERE codcliente = ? AND stato = ? ";
            try (PreparedStatement ps = this.c.prepareStatement(sql)) {
                ps.setString(1, codcliente);
                ps.setString(2, "1");
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString(1);
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

}

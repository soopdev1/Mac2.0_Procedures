/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.refill.qlik;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 *
 * @author rcosco
 */
public class Db {

    public static final String path_log = "/mnt/logdb/";

// SVILUPPO
//    private static final String user = "root";
//    private static final String host = "//172.31.224.159:3306/MAC_sftp";
//    private static final String pwd = "fertilizza";
// PRODUZIONE
    private static final String user = "maccorp";
    private static final String host = "//machaproxy01.mactwo.loc:3306/macsftp";
    private static final String pwd = "M4cc0Rp";

    private Connection c = null;
    private LoggerNew logger = new LoggerNew("SFTP_MAC", path_log);

    public Db(boolean mac) {
        try {
            Class.forName("org.mariadb.jdbc.Driver").newInstance();
            if (mac) {
                String prodhost = "//machaproxy01.mactwo.loc:3306/maccorpita";
                this.c = DriverManager.getConnection("jdbc:mariadb:" + prodhost + "?user=" + user + "&password=" + pwd);
            } else {
                this.c = DriverManager.getConnection("jdbc:mariadb:" + host + "?user=" + user + "&password=" + pwd);
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SQLException ex) {
            logger.log.log(Level.SEVERE, "Errore CONNESSIONE DB : {0}", ex.getMessage());
            this.c = null;
        }
    }

    public Db(String linkhost) {
        try {
            Class.forName("org.mariadb.jdbc.Driver").newInstance();
            this.c = DriverManager.getConnection("jdbc:mariadb:" + linkhost + "?user=" + user + "&password=" + pwd);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SQLException ex) {
            logger.log.log(Level.SEVERE, "Errore CONNESSIONE DB : {0}", ex.getMessage());
            this.c = null;
        }
    }

    public void closeDB() {
        try {
            if (c != null) {
                this.c.close();
            }
        } catch (SQLException ex) {
            logger.log.log(Level.SEVERE, "Errore CONNESSIONE DB : {0}", ex.getMessage());
        }
    }

    public Connection getConnectionDB() {
        return c;
    }

    public Connection getConnection() {
        return c;
    }

    public String getUtente() {
        return user;
    }

    public String getPassword() {
        return pwd;
    }

    public String getHost() {
        return host;
    }

    public String getPath(String path) {
        try {
            String sql = "SELECT url FROM path WHERE id=?";
            try (PreparedStatement ps = this.c.prepareStatement(sql)) {
                ps.setString(1, path);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("url");
                    }
                }
            }
        } catch (SQLException e) {
            logger.log.log(Level.SEVERE, "Errore DB getPath: {0}", e.getMessage());
        }
        return "";
    }

}

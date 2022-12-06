/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rc.soop.sftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import rc.soop.qlik.LoggerNew;
import static rc.soop.rilasciofile.Action.insertFile;
import static rc.soop.rilasciofile.Utility.getHASH;
import static rc.soop.rilasciofile.Utility.getStringBase64_IO;
import static rc.soop.rilasciofile.Utility.isDirectory;
import static rc.soop.rilasciofile.Utility.isFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import org.joda.time.DateTime;
import rc.soop.esolver.Util;
import rc.soop.rilasciofile.Fileinfo;
import static rc.soop.sftp.SftpMaccorp.rb;
import static rc.soop.sftp.SftpMaccorp.se_ip;
import static rc.soop.sftp.SftpMaccorp.se_port;
import static rc.soop.sftp.SftpMaccorp.se_pwd;
import static rc.soop.sftp.SftpMaccorp.se_user;

/**
 *
 * @author rcosco
 */
public class SftpSIA {

//  SFTP CLIENTE - SIA
    private static final String SIA = "SIA".toLowerCase();
    private static final String SIA_user = rb.getString("sia.user");

    private ArrayList<Fileinfo> allfile_SIA;
    private String download_sftp_SIA, upload_SIA, download_locale, Pathlog;
    public LoggerNew logger;

    public SftpSIA() {
        String dtnow = new DateTime().toString("yyyyMMdd");
        Db db = new Db(false);
        this.download_sftp_SIA = db.getPath("download_sftp_sia");
        this.upload_SIA = db.getPath("upload_sia") + dtnow + "/";
        this.download_locale = db.getPath("download_locale") + File.separator + dtnow + File.separator;
        this.allfile_SIA = db.getFile(SIA);
        this.Pathlog = db.getPath("Pathlog");
        db.closeDB();
    }

    public void sftpsia(boolean prod) {
        this.Pathlog = rb.getString("sia.path.log");
        this.logger = new LoggerNew("SFTP_SIA", this.Pathlog);
        this.logger.log.log(Level.WARNING, "STARTING DOWNLOAD SFTP {0}", SIA.toUpperCase());

        String SIA_ip = rb.getString("sia.preprod.host");
        String privateKey = rb.getString("sia.preprod.key");
        String SIA_port = rb.getString("sia.preprod.port");
        if (prod) {
            SIA_ip = rb.getString("sia.prod.host"); //  PROD1
            privateKey = rb.getString("sia.prod.key");
            SIA_port = rb.getString("sia.prod.port");
        }
        ChannelSftp sftpsia = SftpConnection.connect(
                SIA_user,
                SIA_ip,
                Util.parseIntR(SIA_port),
                privateKey,
                this.logger
        );
        if (prod) {
            if (sftpsia == null || !sftpsia.isConnected()) {
                SIA_ip = rb.getString("sia.prod.host2");//PROD2
                sftpsia = SftpConnection.connect(
                        SIA_user,
                        SIA_ip,
                        Util.parseIntR(SIA_port),
                        privateKey,
                        this.logger);
            }
        }
        this.download_locale = rb.getString("sia.path.out") + new DateTime().toString("yyyyMMdd") + "/"; //SIA
        new File(this.download_locale).mkdirs();
        if (sftpsia != null && sftpsia.isConnected()) {
            this.logger.log.log(Level.WARNING, "SFTP CONNECTED: {0}", SIA_ip);
            try {
                sftpsia.cd(this.download_sftp_SIA);
                ArrayList<LsEntry> v = new ArrayList<>(sftpsia.ls(this.download_sftp_SIA));

                for (int i = 0; i < v.size(); i++) {
                    if (!v.get(i).getAttrs().isDir()) {
                        if (!isFile(v.get(i).getFilename(), this.allfile_SIA)) {//se il file non esiste lo scarica 
                            long size = v.get(i).getAttrs().getSize();
                            if (size > 0) {
                                try {
                                    File download = new File(this.download_locale + v.get(i).getFilename());
                                    try ( OutputStream out = new BufferedOutputStream(new FileOutputStream(download))) {
                                        this.logger.log.log(Level.INFO, "GET: {0}", new Object[]{v.get(i).getFilename()});
                                        sftpsia.get(v.get(i).getFilename(), out);
                                        out.flush();
                                    }
                                    String hash = getHASH(download);
                                    if (download.length() > 0) {
                                        this.logger.log.log(Level.INFO, "{3}: FILE SCARICATO: {0} - SIZE:{1} - HASH: {2}", new Object[]{v.get(i).getFilename(), size, hash, SIA.toUpperCase()});
                                        insertFile(new Fileinfo(
                                                v.get(i).getFilename(),
                                                hash,
                                                size,
                                                getStringBase64_IO(download)), SIA);
                                    } else {
                                        download.delete();
                                    }
                                } catch (Exception e1) {
                                    e1.printStackTrace();
                                    this.logger.log.log(Level.SEVERE, "ERRORE {1} FILE: {0}", new Object[]{e1.getMessage(), SIA.toUpperCase() + " - " + v.get(i).getFilename()});
                                }
                            } else {
                                logger.log.log(Level.SEVERE, "ERRORE {0} : Il file {1} ha dimensione pari a 0.", new Object[]{SIA.toUpperCase(), v.get(i).getFilename()});
                            }
                        }
                    }
                }
                SftpConnection.closeConnection(sftpsia, this.logger);
            } catch (Exception e) {
                this.logger.log.log(Level.SEVERE, "ERRORE {1} SFTP LS: {0}", new Object[]{e.getMessage(), SIA.toUpperCase()});
            }
        }

        File download_dir = new File(this.download_locale);
        File[] ls = download_dir.listFiles();
        this.logger.log.log(Level.WARNING, "FILES DOWNLOADED: {0}", ls.length);

        if (ls.length > 0) {
            ChannelSftp sftpseta = SftpConnection.connect(se_user, se_pwd, se_ip, se_port, this.logger);//inizio dell'upload dei file.
            if (sftpseta.isConnected()) {
                if (!isDirectory(sftpseta, this.upload_SIA)) {
                    try {
                        sftpseta.mkdir(this.upload_SIA);
                    } catch (Exception ex) {
                        this.logger.log.log(Level.SEVERE, "ERRORE CREAZIONE CARTELLA {0}: {1}", new Object[]{SIA.toUpperCase(), ex.getMessage()});
                    }
                }
                for (File file : ls) {
                    try {
                        if (file.isFile() && file.length() > 0) {
                            sftpseta.put(new FileInputStream(file), this.upload_SIA + file.getName());
                            this.logger.log.log(Level.INFO, "{3}: FILE CARICATO: {0} - SIZE: {1} - HASH: {2}",
                                    new Object[]{file.getName(), file.length(), getHASH(file), SIA.toUpperCase()});
                        }
                    } catch (Exception e) {
                        this.logger.log.log(Level.SEVERE, "ERRORE FILE: {0}", e.getMessage());
                    }
                }
                SftpConnection.closeConnection(sftpseta, this.logger);
            } else {
                this.logger.log.severe("SFTP SETA NON CONNESSO");
            }
        }

    }

    public void rilasciaFIle(File fileupload, boolean prod) {
        this.logger = new LoggerNew("SFTP_SIA", this.Pathlog);
        this.logger.log.log(Level.WARNING, "STARTING UPLOAD ON DEMAND SFTP {0}", SIA.toUpperCase());

        String SIA_ip = rb.getString("sia.preprod.host");
        String privateKey = rb.getString("sia.preprod.key");
        String SIA_port = rb.getString("sia.preprod.port");
        if (prod) {
            SIA_ip = rb.getString("sia.prod.host"); //  PROD1
            privateKey = rb.getString("sia.prod.key");
            SIA_port = rb.getString("sia.prod.port");
        }
        ChannelSftp sftpsia = SftpConnection.connect(
                SIA_user,
                SIA_ip,
                Util.parseIntR(SIA_port),
                privateKey,
                this.logger
        );
        if (prod) {
            if (sftpsia == null || !sftpsia.isConnected()) {
                SIA_ip = rb.getString("sia.prod.host2");//PROD2
                sftpsia = SftpConnection.connect(
                        SIA_user,
                        SIA_ip,
                        Util.parseIntR(SIA_port),
                        privateKey,
                        this.logger);
            }
        }

        if (sftpsia != null && sftpsia.isConnected()) {

            this.logger.log.log(Level.WARNING, "SFTP CONNECTED: {0}", SIA_ip);
            try {
                sftpsia.cd("/input/");
                try ( InputStream is = new FileInputStream(fileupload)) {
                    sftpsia.put(is, fileupload.getName());

                }
            } catch (Exception ex1) {
                this.logger.log.log(Level.SEVERE, "ERROR: {0}", ex1.getMessage());
            }
            SftpConnection.closeConnection(sftpsia, this.logger);
        } else {
            this.logger.log.severe("SFTP SETA NON CONNESSO");
        }

    }

}

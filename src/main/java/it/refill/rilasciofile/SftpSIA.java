/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.refill.rilasciofile;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;
import it.refill.esolver.Util;
import it.refill.qlik.LoggerNew;
import static it.refill.rilasciofile.Action.insertFile;
import static it.refill.rilasciofile.Utility.getHASH;
import static it.refill.rilasciofile.Utility.getStringBase64_IO;
import static it.refill.rilasciofile.Utility.isDirectory;
import static it.refill.rilasciofile.Utility.isFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import org.joda.time.DateTime;

/**
 *
 * @author rcosco
 */
public class SftpSIA {

//  SFTP CLIENTE - SIA
    private static final String SIA = "SIA".toLowerCase();
    private static final String SIA_user = "36072";

//  SFTP SETA - PROD
    private static final String se_user = "macsftp";
    private static final String se_pwd = "SpTT25$$cr7";

//  SFTP SETA - TEST
//  private static final String se_user = "macsftp_test";
//  private static final String se_pwd = "Dop77$$R.";
    private static final String se_ip = "172.18.17.38";
    private static final int se_port = 22;

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

        this.Pathlog = "/mnt/sia/log/"; //LINUX

        this.logger = new LoggerNew("SFTP_SIA", this.Pathlog);
        this.logger.log.log(Level.WARNING, "STARTING DOWNLOAD SFTP {0}", SIA.toUpperCase());

        String SIA_ip = "193.178.204.95";
        String privateKey = "/mnt/run/private_key_SIATEST.ppk";
        int SIA_port = 20022;

        if (prod) {
            SIA_ip = "193.178.207.171"; //PROD1
            privateKey = "/mnt/run/id_rsa";
//            privateKey = "private_macrun02.ppk";
            SIA_port = 8022;
        }

        ChannelSftp sftpsia = SftpConnection.connect(
                SIA_user,
                SIA_ip,
                SIA_port,
                privateKey,
                this.logger
        );

        if (prod) {
            if (sftpsia == null || !sftpsia.isConnected()) {
                SIA_ip = "193.178.207.172";//PROD2
                sftpsia = SftpConnection.connect(
                        SIA_user,
                        SIA_ip,
                        SIA_port,
                        privateKey,
                        this.logger);
            }
        }

        this.download_locale = "/mnt/sia/" + new DateTime().toString("yyyyMMdd") + "/"; //SIA

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
//                                    PipedOutputStream out = new PipedOutputStream();
                                    File download = new File(this.download_locale + v.get(i).getFilename());
                                    OutputStream out = new BufferedOutputStream(new FileOutputStream(download));
                                    this.logger.log.log(Level.INFO, "GET: {0}", new Object[]{v.get(i).getFilename()});
                                    sftpsia.get(v.get(i).getFilename(), out);
//                                    sftpsia.get(v.get(i).getFilename(), this.download_locale + v.get(i).getFilename(), new SystemOutProgressMonitor());
//                                    InputStream in = sftpsia.get(v.get(i).getFilename(), new SystemOutProgressMonitor());
//                                    byte[] inBytes = toByteArray(in);
//                                    System.out.println("com.seta.sftpmaccorp.SftpSIA.sftpsia() " + inBytes.length);
                                    out.flush();
                                    out.close();

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
                                } catch (SftpException | IOException e1) {
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
            } catch (SftpException e) {
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
                    } catch (SftpException ex) {
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
                    } catch (SftpException e) {
                        this.logger.log.log(Level.SEVERE, "ERRORE SFTP UPLOAD: {0}", e.getMessage());
                    } catch (FileNotFoundException e) {
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

        String SIA_ip = "193.178.204.95";
        String privateKey = "/mnt/run/private_key_SIATEST.ppk";
        int SIA_port = 20022;

        if (prod) {
            SIA_ip = "193.178.207.171"; //PROD1
            privateKey = "/mnt/run/id_rsa";
            SIA_port = 8022;
        }

        ChannelSftp sftpsia = SftpConnection.connect(
                SIA_user,
                SIA_ip,
                SIA_port,
                privateKey,
                this.logger
        );

        if (prod && (sftpsia == null || !sftpsia.isConnected())) {
            SIA_ip = "193.178.207.172";//PROD2
            sftpsia = SftpConnection.connect(
                    SIA_user,
                    SIA_ip,
                    SIA_port,
                    privateKey,
                    this.logger);
        }

        if (sftpsia != null && sftpsia.isConnected()) {

            this.logger.log.log(Level.WARNING, "SFTP CONNECTED: {0}", SIA_ip);
            try {
                sftpsia.cd("/input/");
                try (InputStream is = new FileInputStream(fileupload)) {
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

//    public static void main(String[] args) {
//        new SftpSIA().sftpsia(true);
//    }
}

class SystemOutProgressMonitor implements SftpProgressMonitor {

    public SystemOutProgressMonitor() {
        Util.log.fine("");
    }

    @Override
    public void init(int op, java.lang.String src, java.lang.String dest, long max) {
        Util.log.info("STARTING: " + op + " " + src + " -> " + dest + " total: " + max);
    }

    @Override
    public boolean count(long bytes) {
        for (int x = 0; x < bytes; x++) {
            Util.log.finer("#");
        }
        return (true);
    }

    @Override
    public void end() {
        Util.log.info("\nFINISHED!");
    }
}

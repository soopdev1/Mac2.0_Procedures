/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.refill.rilasciofile;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpException;
import it.refill.qlik.LoggerNew;
import static it.refill.rilasciofile.Action.estraiNumber;
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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.joda.time.DateTime;

public class SftpMaccorp {

    ///////////////////////////////////////////////////////////////
    //SFTP CLIENTE - MERCURY
    private static final String MER = "MERCURY".toLowerCase();
    private static final String MER_user = "maccorp";
    private static final String MER_pwd = "mccrp_2k9_STF";
    private static final String MER_ip = "filegate.monetaonline.it";
    private static final int MER_port = 22;

    //SFTP CLIENTE - BASSILICHI
    public static final String BAS = "BASSILICHI".toLowerCase();
    public static final String BAS_user = "setacom";
    public static final int BAS_port = 22;

    //SFTP SETA - PROD
    public static final String se_user = "macsftp";
    public static final String se_pwd = "SpTT25$$cr7";
    //SFTP SETA - TEST
//    public static final String se_user = "macsftp_test";
//    public static final String se_pwd = "Dop77$$R.";

    public static final String se_ip = "172.18.17.38";
    public static final int se_port = 22;
    ///////////////////////////////////////////////////////////////

    private ArrayList<Fileinfo> allfile_MER, allfile_BAS;
    private String download_sftp_MER, download_sftp_BAS, upload_mercury, upload_bassilichi, download_locale;
    public LoggerNew logger;

    public SftpMaccorp() {
        String dtnow = new DateTime().toString("yyyyMMdd");
        Db db = new Db(false);
        this.download_sftp_MER = db.getPath("download_sftp");
        this.download_sftp_BAS = db.getPath("download_sftp_bass");
        this.upload_mercury = db.getPath("upload_mercury") + dtnow + "/";
        this.upload_bassilichi = db.getPath("upload_bassilichi") + dtnow + "/";
        this.download_locale = db.getPath("download_locale") + File.separator + dtnow + File.separator;
        this.allfile_MER = db.getFile(MER);
        this.allfile_BAS = db.getFile(BAS);
        this.logger = new LoggerNew("SFTP_MAC", db.getPath("Pathlog"));
        db.closeDB();
    }

    public void sftpmercury() {
        this.logger.log.log(Level.WARNING, "STARTING DOWNLOAD SFTP {0}", MER.toUpperCase());
        //MERCURY
        ChannelSftp sftpmercury = SftpConnection.connect(
                MER_user,
                MER_pwd,
                MER_ip,
                MER_port,
                this.logger);

        new File(this.download_locale + MER).mkdirs();
        //inizio download da SFTP MERCURY
        if (sftpmercury != null && sftpmercury.isConnected()) {
            try {
                ArrayList<LsEntry> v = new ArrayList<>(sftpmercury.ls(this.download_sftp_MER));
                for (int i = 0; i < v.size(); i++) {
                    if (!v.get(i).getAttrs().isDir()) {
                        if (!isFile(v.get(i).getFilename(), this.allfile_MER)) {
                            long size = v.get(i).getAttrs().getSize();
                            if (size > 0) {
                                File download = new File(this.download_locale + MER + File.separator + v.get(i).getFilename());
                                OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(download));
                                sftpmercury.get(this.download_sftp_MER + v.get(i).getFilename(), outputStream);
                                outputStream.flush();
                                outputStream.close();
                                String hash = getHASH(download);
                                this.logger.log.log(Level.INFO, "{3} FILE SCARICATO: {0} SIZE:{1} HASH: {2}", new Object[]{v.get(i).getFilename(), size, hash, MER.toUpperCase()});
                                insertFile(new Fileinfo(
                                        v.get(i).getFilename(),
                                        hash,
                                        size,
                                        getStringBase64_IO(download)), MER);
                            } else {
                                logger.log.log(Level.SEVERE, "ERRORE {0} : Il file {1} ha dimensione pari a 0.", new Object[]{MER.toUpperCase(), v.get(i).getFilename()});
                            }
                        }
                    }
                }
                SftpConnection.closeConnection(sftpmercury, this.logger);
            } catch (SftpException e) {
                this.logger.log.log(Level.SEVERE, "ERRORE {1} SFTP LS: {0}", new Object[]{e.getMessage(), MER.toUpperCase()});
            } catch (IOException e) {
                this.logger.log.log(Level.SEVERE, "ERRORE {1} FILE: {0}", new Object[]{e.getMessage(), MER.toUpperCase()});
            }
        } else {
            this.logger.log.log(Level.SEVERE, "CLIENT {0} NON CONNESSO", MER.toUpperCase());
        }
        this.logger.log.log(Level.WARNING, "END DOWNLOAD SFTP {0}", MER.toUpperCase());
        File download_dir = new File(this.download_locale + MER);
        File[] ls = download_dir.listFiles();
        this.logger.log.log(Level.WARNING, "FILES DOWNLOADED: {0}", ls.length);
        if (ls.length > 0) {
            ChannelSftp sftpseta = SftpConnection.connect(se_user, se_pwd, se_ip, se_port, this.logger);//inizio dell'upload dei file.
            if (sftpseta.isConnected()) {
                if (!isDirectory(sftpseta, this.upload_mercury)) {
                    try {
                        sftpseta.mkdir(this.upload_mercury);
                    } catch (SftpException ex) {
                        this.logger.log.log(Level.SEVERE, "ERRORE CREAZIONE CARTELLA {0}: {1}", new Object[]{MER.toUpperCase(), ex.getMessage()});
                    }
                }
                for (File file : ls) {
                    try {
                        if (file.isFile()) {
                            sftpseta.put(new FileInputStream(file), this.upload_mercury + file.getName());
                            this.logger.log.log(Level.INFO, "{3}: FILE CARICATO: {0} - SIZE: {1} - HASH: {2}", new Object[]{file.getName(), file.length(), getHASH(file), MER.toUpperCase()});
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

    public static void listFiles(ArrayList<LsEntry> originale, ChannelSftp sftpbassilichi, String path, List<Fileinfo> listafinale) {

        try {
            ArrayList<LsEntry> v = new ArrayList<>(sftpbassilichi.ls(path));
            for (int x = 0; x < v.size(); x++) {
                LsEntry valoreinterno = v.get(x);
                if (valoreinterno.getAttrs().isDir()) {
                    if (valoreinterno.getFilename().equals(".") || valoreinterno.getFilename().equals("..")) {

                    } else {
//                        System.out.println("com.seta.sftpmaccorp.SftpMaccorp.sftpbassilichi(1) " + path + "/" + valoreinterno.getFilename());
                        listFiles(v, sftpbassilichi, path + "/" + valoreinterno.getFilename(), listafinale);
                        //                        originale.add(v);
                    }
                } else {
//                    System.out.println("com.seta.sftpmaccorp.SftpMaccorp.sftpbassilichi(2) " + valoreinterno.getFilename());
//                    System.out.println("com.seta.sftpmaccorp.SftpMaccorp.listFiles() "+ path + "/" + valoreinterno.getFilename());
                    listafinale.add(new Fileinfo(estraiNumber(path) + "_" + valoreinterno.getFilename(), valoreinterno.getAttrs().getSize(), path + "/" + valoreinterno.getFilename(), true));
//                    originale.add(valoreinterno);
                }
            }

        } catch (SftpException ex) {
        }
    }

    public void sftpnexi(boolean prod) {
        this.logger.log.log(Level.WARNING, "STARTING DOWNLOAD SFTP {0}", BAS.toUpperCase());
        String BAS_ip = "stgtransfer.nexi.it";
        String privateKey = "private_key_PREPROD.ppk";
        if (prod) {
            BAS_ip =  "transfer.nexi.it";//PROD
            privateKey = "private_key_PROD.ppk";
        }
        
        //BASSILICHI - NEXI
//        ChannelSftp sftpbassilichi = SftpConnection.connect(
//                BAS_user,
//                BAS_ip,
//                BAS_port,
//                privateKey, this.logger);
        

    //PREPROD
//        ChannelSftp sftpnexi_preprod = SftpConnection.connect(
//                            BAS_user, //USER
//                            "stgtransfer.nexi.it", //   HOST
//                            115, //PORTA
//                            "private_key_PREPROD.ppk",
//                            this.logger);
        ChannelSftp sftpnexi = SftpConnection.connect(
                            BAS_user, //USER
                            BAS_ip, //HOST
                            115, //PORTA
                            privateKey,
                            this.logger);
        

//        if (sftpbassilichi == null || !sftpbassilichi.isConnected()) {
//            BAS_ip = "194.184.20.164";//PROD2
//            sftpbassilichi = SftpConnection.connect(
//                    BAS_user,
//                    BAS_ip,
//                    BAS_port,
//                    privateKey, this.logger);
//        }

        //inizio download da SFTP BASSILICHI - NEXI
        if (sftpnexi != null && sftpnexi.isConnected()) {
            new File(this.download_locale + BAS).mkdirs();
            this.logger.log.log(Level.WARNING, "SFTP CONNECTED: {0}", BAS_ip);
            try {
                sftpnexi.cd(this.download_sftp_BAS);
                ArrayList<LsEntry> v = new ArrayList<>(sftpnexi.ls(sftpnexi.pwd()));
                List<Fileinfo> listafinale = new ArrayList<>();
                String start = sftpnexi.pwd();
                for (int i = 0; i < v.size(); i++) {

                    if (v.get(i).getFilename().equals(".") || v.get(i).getFilename().equals("..")) {
                    } else {
                        listFiles(v, sftpnexi, start + "/" + v.get(i).getFilename(), listafinale);
                    }
                }

                for (int x = 0; x < listafinale.size(); x++) {
                    Fileinfo file1 = listafinale.get(x);
                    if (file1.isIsfile()) {
                        if (!isFile(file1.getName(), this.allfile_BAS)) {
                            if (file1.getSize() > 0) {
                                try {
                                    File download = new File(this.download_locale + BAS + File.separator + file1.getName());
                                    OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(download));
                                    sftpnexi.get(file1.getPath(), outputStream);
                                    outputStream.flush();
                                    outputStream.close();
                                    String hash = getHASH(download);
                                    this.logger.log.log(Level.INFO, "{3}: FILE SCARICATO: {0} - SIZE:{1} - HASH: {2}",
                                            new Object[]{file1.getName(), file1.getSize(), hash, BAS.toUpperCase()});
                                    insertFile(new Fileinfo(
                                            file1.getName(),
                                            hash,
                                            file1.getSize(),
                                            getStringBase64_IO(download)),
                                            BAS);
                                } catch (SftpException e) {
                                    this.logger.log.log(Level.SEVERE, "ERRORE {1} SFTP LS: {0}", new Object[]{e.getMessage(), BAS.toUpperCase()});
                                } catch (IOException e) {
                                    this.logger.log.log(Level.SEVERE, "ERRORE {1} FILE: {0}", new Object[]{e.getMessage(), BAS.toUpperCase()});
                                }
                            } else {
                                logger.log.log(Level.SEVERE, "ERRORE {0} : Il file {1} ha dimensione pari a 0.", new Object[]{BAS.toUpperCase(), file1.getName()});
                            }
                        }
                    }
                }
                SftpConnection.closeConnection(sftpnexi, this.logger);
            } catch (SftpException e) {
                this.logger.log.log(Level.SEVERE, "ERRORE {1} SFTP LS: {0}", new Object[]{e.getMessage(), BAS.toUpperCase()});
            }
        } else {
            this.logger.log.log(Level.SEVERE, "CLIENT {0} NON CONNESSO", BAS.toUpperCase());
        }
        this.logger.log.log(Level.WARNING, "END DOWNLOAD SFTP {0}", BAS.toUpperCase());

        File download_dir = new File(download_locale + BAS);
        File[] ls = download_dir.listFiles();

        this.logger.log.log(Level.WARNING, "FILES DOWNLOADED: {0}", ls.length);

        if (ls.length > 0) {
            ChannelSftp sftpseta = SftpConnection.connect(se_user, se_pwd, se_ip, se_port, this.logger);//inizio dell'upload dei file.
            if (sftpseta.isConnected()) {
                if (!isDirectory(sftpseta, this.upload_bassilichi)) {
                    try {
                        System.out.println("com.seta.sftpmaccorp.SftpMaccorp.sftpbassilichi() "+this.upload_bassilichi);
                        sftpseta.mkdir(this.upload_bassilichi);
                    } catch (SftpException ex) {
                        ex.printStackTrace();
                        this.logger.log.log(Level.SEVERE, "ERRORE CREAZIONE CARTELLA {0}: {1}", new Object[]{BAS.toUpperCase(), ex.getMessage()});
                    }
                }
                for (File file : ls) {
                    try {
                        if (file.isFile()) {
                            sftpseta.put(new FileInputStream(file), this.upload_bassilichi + file.getName());
                            this.logger.log.log(Level.INFO, "{3}: FILE CARICATO: {0} - SIZE: {1} - HASH: {2}", new Object[]{file.getName(), file.length(), getHASH(file), BAS.toUpperCase()});
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

    public static void main(String[] args) {
        new SftpMaccorp().sftpnexi(true);
//        new SftpMaccorp().sftpmercury();
    }

}

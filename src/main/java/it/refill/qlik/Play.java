/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.refill.qlik;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.security.jce.JceSecurityProvider;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.share.DiskShare;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.joda.time.DateTime;

/**
 *
 * @author rcosco
 */
public class Play {

    private static final String hostsiap = "ftp.siapcn.it";
    private static final String usersiap = "ftpf5z";
    private static final String passsiap = "gbgt1019";

//    private static final String usersiap = "ftpf6i";
//    private static final String passsiap = "gbgt1132";
    private static final String sambaDomain = "setaloc";
    private static final String sambaUsername = "usrqlik";
    private static final String sambaPass = "FnC018$!!";
    private static final String sambaIP = "172.18.17.48";
    private static final String sambaSharedPath = "Dati_Atlante";

    public static void main(String[] args) {
        Db db = new Db(false);
        LoggerNew logger = new LoggerNew("FTP_QLIK", db.getPath("Pathlog"));
        db.closeDB();
        try {
            logger.log.info("FTP ATLANTE BIGLIETTI");

            DateTime now = new DateTime();
            String date = now.toString("yyyy-MM-dd");
            FTPClient ftpClient = new FTPClient();
            ftpClient.connect(hostsiap, 21);
            ftpClient.login(usersiap, passsiap);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            FTPFileFilter ff = (FTPFile ftpf)
                    -> ftpf.getName().toUpperCase().contains("BIGLIETTI")
                    || ftpf.getName().toUpperCase().contains("PRATICHE")
                    || ftpf.getName().toUpperCase().contains("VOUCHER");

            FTPFile[] files = ftpClient.listFiles("/ExportBI/" + date + "/", ff);
            new File(date).mkdirs();

            if (files != null) {
                logger.log.log(Level.INFO, "NUMERO FILES DISPONIBILI: {0}", files.length);

                SmbConfig cfg = SmbConfig.builder().
                        withMultiProtocolNegotiate(true).
                        withSecurityProvider(new JceSecurityProvider(new BouncyCastleProvider())).
                        build();

                SMBClient client = new SMBClient(cfg);
                Connection connection = client.connect(sambaIP);
                com.hierynomus.smbj.session.Session session = connection.authenticate(new AuthenticationContext(sambaUsername, sambaPass.toCharArray(), sambaDomain));
                DiskShare share = (DiskShare) session.connectShare(sambaSharedPath);

                for (FTPFile aFile : files) {
                    if (!aFile.isDirectory()) {
                        File downloadFile1 = new File(date + "/" + aFile.getName());
                        boolean success;
                        try (OutputStream outputStream1 = new BufferedOutputStream(new FileOutputStream(downloadFile1))) {
                            success = ftpClient.retrieveFile("/ExportBI/" + date + "/" + aFile.getName(), outputStream1);
                        }

                        if (success) {

                            String name = downloadFile1.getName();
                            if (name.toUpperCase().contains("BIGLIETTI")) {
                                name = "BIGLIETTI.xls";
                            } else if (name.toUpperCase().contains("PRATICHE")) {
                                name = "PRATICHE.xls";
                            } else if (name.toUpperCase().contains("VOUCHERS")) {
                                name = "VOUCHERS.xls";
                            } else {
                                continue;
                            }

                            File out = convertXls(downloadFile1);
                            System.out.println(out.getPath());
                            boolean up = copy_SMB(share, name, FileUtils.readFileToByteArray(out), logger);
                            if (up) {
                                logger.log.log(Level.INFO, "{0} SCARICATO CORRETTAMENTE E CARICATO NELLA CARTELLA DI QLIK", name);
                            } else {
                                logger.log.log(Level.SEVERE, "{0}ERRORE NUMERO FILES DISPONIBILI: ", name);
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            logger.log.severe(ex.getMessage());
        }

    }

    public static File convertXls(File ing) {
        try {
            DateTime inizioanno = new DateTime().withMonthOfYear(1).withDayOfMonth(1).withMillisOfDay(0);
            DateTime oggi = new DateTime().withMillisOfDay(0);
//            int numerofogli = oggi.monthOfYear().get();

            List<String> elencofogliOK = new ArrayList<>();
            while (inizioanno.isBefore(oggi)) {
                elencofogliOK.add(inizioanno.toString("MM") + "_" + inizioanno.monthOfYear().getAsText(Locale.ITALY).toUpperCase());
                inizioanno = inizioanno.plusMonths(1);
            }

            List<String> elencofogli = new ArrayList<>();

            HSSFWorkbook wb = new HSSFWorkbook(new FileInputStream(ing));
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                elencofogli.add(wb.getSheetAt(i).getSheetName());
            }

            String nomefoglio = oggi.toString("MM") + "_" + oggi.monthOfYear().getAsText(Locale.ITALY).toUpperCase();
            if (!elencofogli.contains(nomefoglio)) {
                System.out.println("KO");
                elencofogliOK.forEach(f1 -> {
                    if (!elencofogli.contains(f1)) {
                        HSSFSheet nuovofoglio = wb.cloneSheet(elencofogli.size() - 1);
                        wb.setSheetName(wb.getSheetIndex(nuovofoglio), f1);

                        nuovofoglio.getLastRowNum();
//                        nuovofoglio.getLastRowNum();
                        System.out.println(f1 + " DA CREARE " + nuovofoglio.getLastRowNum());
                        if (nuovofoglio.getLastRowNum() > 6) {
                            for (int x = 7; x <= nuovofoglio.getLastRowNum(); x++) {
                                HSSFRow r = nuovofoglio.getRow(x);
                                for (int y = 0; y <= r.getLastCellNum(); y++) {
                                    if (r.getCell(y) != null) {
                                        r.getCell(y).setCellValue("");
                                    }
                                }
                            }
                            wb.setForceFormulaRecalculation(true);
                            System.out.println(f1 + " RIGHE DA SVUOTARE ");
                        }

                    }
                });
                elencofogli.forEach(f1 -> {
                    if (!elencofogliOK.contains(f1)) {
                        wb.removeSheetAt(wb.getSheetIndex(f1));
                        System.out.println(f1 + " DA ELIMINARE");
                    }
                });
                File file2 = new File(ing.getPath() + "_v2.xls");
                FileOutputStream outputStream = new FileOutputStream(file2);
                wb.write(outputStream);
                wb.close();
                return file2;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ing;
    }

    private static boolean copy_SMB(DiskShare share, String filename, byte[] bytes, LoggerNew logger) {
        // this is com.hierynomus.smbj.share.File !
        int idx = filename.lastIndexOf("/");
        // if file is in folder(s), create them first
        if (idx > -1) {
            String folder = filename.substring(0, idx);
            try {
                if (!share.folderExists(folder)) {
                    share.mkdir(folder);
                }
            } catch (SMBApiException ex) {
                logger.log.severe(ex.getMessage());
                return false;
            }
        }
        // I am creating file with flag FILE_CREATE, which will throw if file exists already
        if (share.fileExists(filename)) {
            share.rm(filename);
        }
        com.hierynomus.smbj.share.File f = share.openFile(filename,
                new HashSet<>(Arrays.asList(AccessMask.GENERIC_ALL)),
                new HashSet<>(Arrays.asList(FileAttributes.FILE_ATTRIBUTE_NORMAL)),
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_CREATE,
                new HashSet<>(Arrays.asList(SMB2CreateOptions.FILE_DIRECTORY_FILE))
        );
        if (f == null) {
            return false;
        }
        try {
            OutputStream os = f.getOutputStream();
            os.write(bytes);
            os.close();
            return true;
        } catch (IOException ex) {
            logger.log.severe(ex.getMessage());
        }
        return false;
    }

}

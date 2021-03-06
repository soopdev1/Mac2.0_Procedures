/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.refill.rilasciofile;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import it.refill.esolver.Branch;
import it.refill.esolver.NC_category;
import static it.refill.esolver.Util.patternnormdate_filter;
import static it.refill.esolver.Util.patternsql;
import it.refill.qlik.LoggerNew;
import static it.refill.rilasciofile.SftpMaccorp.se_ip;
import static it.refill.rilasciofile.SftpMaccorp.se_port;
import static it.refill.rilasciofile.SftpMaccorp.se_pwd;
import static it.refill.rilasciofile.SftpMaccorp.se_user;
import static it.refill.rilasciofile.Utility.isDirectory;
import static it.refill.rilasciofile.Utility.patternmonthsql;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.logging.Level;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;

/**
 *
 * @author rcosco
 */
public class GeneraFile {

    private static void ondemand(String anno) {
        GeneraFile gf = new GeneraFile();
        gf.setIs_IT(true);
        gf.setIs_UK(false);
        gf.setIs_CZ(false);
        DatabaseCons db = new DatabaseCons(gf);
        String path = db.getPath("temp");
//        ArrayList<String> br1 = db.list_branchcode_completeAFTER311217();
        ArrayList<NC_transaction> result = db.query_NC_ondemand(anno);
        if (result.size() > 0) {
            String nomereport = "LIST TRANSACTION NOCHANGE " + anno + "_2.xlsx";
            File Output = new File(path + nomereport);
            Excel.excel_transactionnc_list(gf, Output, result);
            System.out.println("com.fl.upload.GeneraFile.ondemand() " + path + nomereport);
        }
        db.closeDB();
    }

    public void rilasciafile(GeneraFile gf, String tipofile) {
        gf.logger.log.warning("START");

        gf.setIs_IT(true);
        gf.setIs_UK(false);
        gf.setIs_CZ(false);

        DatabaseCons db = new DatabaseCons(gf);

//        DateTime iniziomese = new DateTime().minusDays(1).dayOfMonth().withMinimumValue();
        DateTime iniziomese = new DateTime().minusDays(1).dayOfMonth().withMinimumValue().withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);

//        System.out.println("com.fl.upload.GeneraFile.rilasciafile() "+);
        String mesemysql = iniziomese.toString(patternmonthsql);
        DateTime ieri = new DateTime().minusDays(1);

        String meseriferimento = iniziomese.monthOfYear().getAsText(Locale.ITALY).toUpperCase();
        String annoriferimento = iniziomese.year().getAsText(Locale.ITALY).toUpperCase();

        String data1 = iniziomese.toString(patternsql);
        String data2 = ieri.toString(patternsql);

        String d3 = iniziomese.toString(patternnormdate_filter);
        String d4 = ieri.toString(patternnormdate_filter);
        String meseanno_prec = iniziomese.minusMonths(1).toString(patternmonthsql);
        String anno_rif = iniziomese.minusMonths(1).year().getAsText();
        String meseriferimento_prec = iniziomese.minusMonths(1).monthOfYear().getAsText(Locale.ITALY).toUpperCase();
        String path = db.getPath("temp");
        ArrayList<String> br1 = db.list_branchcode_completeAFTER311217();
        ArrayList<String> filiali_soloROMA = db.list_branch_RM();
        ArrayList<Branch> allenabledbr = db.list_branch();

//        data1 = "2019-10-01";
//        data2 = "2019-10-31";
        if (tipofile.equals("TCH")) { // LIST TRANSACTION CHANGE - DA INIZIO MESE A IERI      
            ArrayList<Ch_transaction> result = db.query_transaction_ch_new(data1, data2, br1);
            String base64;
            if (!result.isEmpty()) {
                String nomereport = "LIST TRANSACTION CHANGE DA " + data1 + " A " + data2 + ".xlsx";
                File Output = new File(path + nomereport);
                base64 = Excel.excel_transaction_listEVO(gf, Output, result);
                if (base64 != null) {
                    boolean es = rilasciasftp(Output, meseriferimento + "/LIST TRANSACTION CHANGE", annoriferimento);
                    if (es) {
                        gf.logger.log.log(Level.WARNING, "FILE RILASCIATO CON SUCCESSO: {0}", Output.getPath());
                    } else {
                        gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
                    }
                } else {
                    gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
                }
            } else {
                gf.logger.log.log(Level.WARNING, "NESSUN DATO TROVATO. {0}", tipofile);
            }
        } else if (tipofile.equals("TNC")) { // LIST TRANSACTION NOCHANGE - DA INIZIO MESE A IERI      
            ArrayList<NC_transaction> result = db.query_NC_transaction_NEW(data1, data2, br1, "NO");
            String base64;
            if (!result.isEmpty()) {
                String nomereport = "LIST TRANSACTION NOCHANGE DA " + data1 + " A " + data2 + ".xlsx";
                File Output = new File(path + nomereport);
                base64 = Excel.excel_transactionnc_list(gf, Output, result);
                if (base64 != null) {
                    boolean es = rilasciasftp(Output, meseriferimento + "/LIST TRANSACTION NOCHANGE", annoriferimento);
                    if (es) {
                        gf.logger.log.log(Level.WARNING, "FILE RILASCIATO CON SUCCESSO: {0}", Output.getPath());
                    } else {
                        gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
                    }
                } else {
                    gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
                }
            } else {
                gf.logger.log.log(Level.WARNING, "NESSUN DATO TROVATO. {0}", tipofile);
            }
        } else if (tipofile.equals("SB1")) { // SB TRANSACTION - DA INIZIO MESE A IERI    
            TillTransactionListBB_value pdfsell = db.list_SBTransactionList(br1, data1, data2, allenabledbr);

            if (pdfsell != null) {
                ArrayList<String> datifooter = new ArrayList<>();
                datifooter.add(pdfsell.getTransactionnumberresidentbuy());
                datifooter.add(pdfsell.getTransactionnumbernonresidentbuy());
                datifooter.add(pdfsell.getInternetbookingamountyes());
                datifooter.add(pdfsell.getInternetbookingnumberyes());
                datifooter.add(pdfsell.getPosbuyamount());
                datifooter.add(pdfsell.getPosbuynumber());
                datifooter.add(pdfsell.getBankbuyamount());
                datifooter.add(pdfsell.getBankbuynumber());
                datifooter.add(pdfsell.getTransactionnumberresidentsell());
                datifooter.add(pdfsell.getTransactionnumbernonresidentsell());
                datifooter.add(pdfsell.getInternetbookingamountno());
                datifooter.add(pdfsell.getInternetbookingnumberno());
                datifooter.add(pdfsell.getPossellamount());
                datifooter.add(pdfsell.getPossellnumber());
                datifooter.add(pdfsell.getBanksellamount());
                datifooter.add(pdfsell.getBanksellnumber());
                pdfsell.setFooterdati(datifooter);

                String nomereport = "SB LIST TRANSACTION DA " + data1 + " A " + data2 + ".xlsx";
                File Output = new File(path + nomereport);
                String base64 = Excel.BB_receiptexcel(Output, pdfsell, d3, d4, "SellBack Transaction List - Group By Sell-Buy");
                if (base64 != null) {
                    boolean es = rilasciasftp(Output, meseriferimento + "/SB LIST TRANSACTION", annoriferimento);
                    if (es) {
                        gf.logger.log.log(Level.WARNING, "FILE RILASCIATO CON SUCCESSO: {0}", Output.getPath());
                    } else {
                        gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
                    }
                } else {
                    gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
                }
            } else {
                gf.logger.log.log(Level.WARNING, "NESSUN DATO TROVATO. {0}", tipofile);
            }

        } else if (tipofile.equals("BB1")) { // BB TRANSACTION - DA INIZIO MESE A IERI    

            TillTransactionListBB_value pdfsell = db.list_BBTransactionList_mod(br1, data1, data2, allenabledbr);
            if (pdfsell != null) {
                ArrayList<String> datifooter = new ArrayList<>();
                datifooter.add(pdfsell.getTransactionnumberresidentbuy());
                datifooter.add(pdfsell.getTransactionnumbernonresidentbuy());
                datifooter.add(pdfsell.getInternetbookingamountyes());
                datifooter.add(pdfsell.getInternetbookingnumberyes());
                datifooter.add(pdfsell.getPosbuyamount());
                datifooter.add(pdfsell.getPosbuynumber());
                datifooter.add(pdfsell.getBankbuyamount());
                datifooter.add(pdfsell.getBankbuynumber());
                datifooter.add(pdfsell.getTransactionnumberresidentsell());
                datifooter.add(pdfsell.getTransactionnumbernonresidentsell());
                datifooter.add(pdfsell.getInternetbookingamountno());
                datifooter.add(pdfsell.getInternetbookingnumberno());
                datifooter.add(pdfsell.getPossellamount());
                datifooter.add(pdfsell.getPossellnumber());
                datifooter.add(pdfsell.getBanksellamount());
                datifooter.add(pdfsell.getBanksellnumber());
                pdfsell.setFooterdati(datifooter);

                String nomereport = "BB LIST TRANSACTION DA " + data1 + " A " + data2 + ".xlsx";
                File Output = new File(path + nomereport);
                String base64 = Excel.BB_receiptexcel(Output, pdfsell, d3, d4, "BuyBack Transaction List - Group By Buy-Sell ");
                if (base64 != null) {
                    boolean es = rilasciasftp(Output, meseriferimento + "/BB LIST TRANSACTION", annoriferimento);
                    if (es) {
                        gf.logger.log.log(Level.WARNING, "FILE RILASCIATO CON SUCCESSO: {0}", Output.getPath());
                    } else {
                        gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
                    }
                } else {
                    gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
                }
            } else {
                gf.logger.log.log(Level.WARNING, "NESSUN DATO TROVATO. {0}", tipofile);
            }

        } else if (tipofile.equals("AML1")) { // AML MASTER - DA INIZIO MESE A IERI    

            String nomereport = "MONEY LAUNDERING - MASTER DATA DA " + data1 + " A " + data2 + ".xls";
            File Output = new File(path + nomereport);

            String base64 = Excel.AML_anagrafica(gf, Output, data1, data2);
            if (base64 != null) {
                boolean es = rilasciasftp(Output, meseriferimento + "/MONEY LAUNDERING", annoriferimento);
                if (es) {
                    gf.logger.log.log(Level.WARNING, "FILE RILASCIATO CON SUCCESSO: {0}", Output.getPath());
                } else {
                    gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
                }
            } else {
                gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
            }

        } else if (tipofile.equals("AML2")) { //AML REGISTRATION - DA INIZIO MESE A IERI    

            String nomereport = "MONEY LAUNDERING - REGISTRATION DA " + data1 + " A " + data2 + ".xls";
            File Output = new File(path + nomereport);
            String base64 = Excel.AML_registrazione(gf, Output, data1, data2);
            if (base64 != null) {
                boolean es = rilasciasftp(Output, meseriferimento + "/MONEY LAUNDERING", annoriferimento);
                if (es) {
                    gf.logger.log.log(Level.WARNING, "FILE RILASCIATO CON SUCCESSO: {0}", Output.getPath());
                } else {
                    gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
                }
            } else {
                gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
            }

        } else if (tipofile.equals("COR")) { // CORA MESE PRECEDENTE   
            boolean es = false;
            String nomereport = "CORA MENSILE " + meseanno_prec + ".zip";
            File Output = new File(path + nomereport);
            try {
                String base64 = db.getCORA(meseanno_prec, "0");
                FileUtils.writeByteArrayToFile(Output, Base64.decodeBase64(base64.getBytes()));
                if (Output.exists() && Output.length() > 0) {
                    es = true;
                }
            } catch (Exception ex) {
                gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0} -- {1}", new Object[]{Output.getPath(), ex.getMessage()});
                es = false;
            }
            if (es) {
                boolean es1 = rilasciasftp(Output, meseriferimento_prec + "/CORA", anno_rif);
                if (es1) {
                    gf.logger.log.log(Level.WARNING, "FILE RILASCIATO CON SUCCESSO: {0}", Output.getPath());
                } else {
                    gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
                }
            } else {
                gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
            }

        } else if (tipofile.equals("OAM")) { // OAM MESE PRECEDENTE   
            boolean es = false;
            String nomereport = "OAM ORDINARY " + meseanno_prec + ".zip";
            File Output = new File(path + nomereport);
            try {
                String base64 = db.getOAM(meseanno_prec, "0");
                FileUtils.writeByteArrayToFile(Output, Base64.decodeBase64(base64.getBytes()));
                if (Output.exists() && Output.length() > 0) {
                    es = true;
                }
            } catch (Exception ex) {
                gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0} -- {1}", new Object[]{Output.getPath(), ex.getMessage()});
                es = false;
            }
            if (es) {
                boolean es1 = rilasciasftp(Output, meseriferimento_prec + "/OAM", anno_rif);
                if (es1) {
                    gf.logger.log.log(Level.WARNING, "FILE RILASCIATO CON SUCCESSO: {0}", Output.getPath());
                } else {
                    gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
                }
            } else {
                gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
            }
        } else if (tipofile.equals("CP1")) { // CASHIER PERFRMANCE - DA INIZIO MESE A IERI    

            String nomereport = "CASHIER PERFORMANCE DA " + data1 + " A " + data2 + ".xls";
            File Output = new File(path + nomereport);

            ArrayList<String[]> fasce = db.list_fasce_cashier_perf("BS", null);
            ArrayList<C_CashierPerformance_value> dati = db.list_C_CashierPerformance_value(data1, data2, "BS", br1, fasce);
            ArrayList<String> alcolonne = new ArrayList<>();
            alcolonne.add("User");
            alcolonne.add("0%");
            alcolonne.add("NEG");
            alcolonne.add("Full");
            alcolonne.add("#Trans.");
            alcolonne.add("NFF");
            alcolonne.add("DEL");
            alcolonne.add("Volume");
            alcolonne.add("Com+Fix");
            alcolonne.add("%Media");
            alcolonne.add("Val.Medio");
            alcolonne.add("Com.Media");
            alcolonne.add("#ERR");
            alcolonne.add("tot. ERR");

            String base64 = Excel.CP_mainexcel(Output, d3, d4, data1, data2, "BS", dati, alcolonne, br1, allenabledbr);
            if (base64 != null) {
                boolean es = rilasciasftp(Output, meseriferimento + "/CASHIER PERFORMANCE", annoriferimento);
                if (es) {
                    gf.logger.log.log(Level.WARNING, "FILE RILASCIATO CON SUCCESSO: {0}", Output.getPath());
                } else {
                    gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
                }
            } else {
                gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
            }

        } else if (tipofile.equals("DA1")) { // MANAGEMENT CONTROL - DAILY REPORT - DA INIZIO MESE A IERI     //COMPLETO
            String nomereport = "MANAGEMENT CONTROL - DAILY REPORT DA " + data1 + " A " + data2 + ".xlsx";
            File Output = new File(path + nomereport);

            ArrayList<NC_category> listnccat = db.list_ALL_nc_category("000");

            String base64 = ControlloGestione.daily_report(Output, br1, mesemysql, meseriferimento, allenabledbr, iniziomese, db, listnccat);
            if (base64 != null) {
                boolean es = rilasciasftp(Output, meseriferimento + "/MANAGEMENT CONTROL", annoriferimento);
                if (es) {
                    gf.logger.log.log(Level.WARNING, "FILE RILASCIATO CON SUCCESSO: {0}", Output.getPath());
                } else {
                    gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
                }
            } else {
                gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
            }
        } else if (tipofile.equals("DAR")) { // MANAGEMENT CONTROL - DAILY REPORT - DA INIZIO MESE A IERI     //ROMA
            String nomereport = "MANAGEMENT CONTROL - DAILY REPORT - SOLO ROMA DA " + data1 + " A " + data2 + ".xlsx";
            File Output = new File(path + nomereport);
            ArrayList<NC_category> listnccat = db.list_ALL_nc_category("000");

            String base64 = ControlloGestione.daily_report(Output, filiali_soloROMA, mesemysql, meseriferimento, allenabledbr, iniziomese, db, listnccat);
            if (base64 != null) {
                boolean es = rilasciasftp(Output, meseriferimento + "/MANAGEMENT CONTROL", annoriferimento);
                if (es) {
                    gf.logger.log.log(Level.WARNING, "FILE RILASCIATO CON SUCCESSO: {0}", Output.getPath());
                } else {
                    gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
                }
            } else {
                gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
            }

        } else if (tipofile.equals("DAUK")) { // MANAGEMENT CONTROL - DAILY REPORT UK - DA INIZIO MESE A IERI     //COMPLETO
            gf.setIs_IT(false);
            gf.setIs_UK(true);
            db.closeDB();
            db = new DatabaseCons(gf);
            ArrayList<NC_category> listnccat = db.list_ALL_nc_category("000");
            br1 = db.list_branchcode_completeAFTER311217();
            allenabledbr = db.list_branch();
            String nomereport = "MANAGEMENT CONTROL - DAILY REPORT UK - DA " + data1 + " A " + data2 + ".xlsx";
            File Output = new File(path + nomereport);

            String base64 = ControlloGestione.daily_report(Output, br1, mesemysql, meseriferimento, allenabledbr, iniziomese, db, listnccat);
            if (base64 != null) {
                boolean es = rilasciasftp(Output, meseriferimento + "/MANAGEMENT CONTROL UK", annoriferimento);
                if (es) {
                    gf.logger.log.log(Level.WARNING, "FILE RILASCIATO CON SUCCESSO: {0}", Output.getPath());
                } else {
                    gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
                }
            } else {
                gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
            }

        } else if (tipofile.equals("DACZ")) { // MANAGEMENT CONTROL - DAILY REPORT CZ - DA INIZIO MESE A IERI     //COMPLETO
            gf.setIs_IT(false);
            gf.setIs_UK(false);
            gf.setIs_CZ(true);
            db.closeDB();

            db = new DatabaseCons(gf);
            ArrayList<NC_category> listnccat = db.list_ALL_nc_category("000");
            br1 = db.list_branchcode_completeAFTER311217();
            allenabledbr = db.list_branch();
            String nomereport = "MANAGEMENT CONTROL - DAILY REPORT CZ - DA " + data1 + " A " + data2 + ".xlsx";
            File Output = new File(path + nomereport);

            String base64 = ControlloGestione.daily_report(Output, br1, mesemysql, meseriferimento, allenabledbr, iniziomese, db, listnccat);
            if (base64 != null) {
                boolean es = rilasciasftp(Output, meseriferimento + "/MANAGEMENT CONTROL CZ", annoriferimento);
                if (es) {
                    gf.logger.log.log(Level.WARNING, "FILE RILASCIATO CON SUCCESSO: {0}", Output.getPath());
                } else {
                    gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
                }
            } else {
                gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
            }

        } else if (tipofile.equals("MCO1")) { // MANAGEMENT CONTROL - REPORT MANAGEMENT CONTROL - DA INIZIO MESE A IERI     //COMPLETO

            String nomereport = "MANAGEMENT CONTROL - REPORT MANAGEMENT CONTROL N1 DA " + data1 + " A " + data2 + ".xlsx";
            File Output = new File(path + nomereport);
            String base64 = ControlloGestione.management_change_n1(Output, br1, data1, data2, true, allenabledbr, db);
            if (base64 != null) {
                boolean es = rilasciasftp(Output, meseriferimento + "/MANAGEMENT CONTROL", annoriferimento);
                if (es) {
                    gf.logger.log.log(Level.WARNING, "FILE RILASCIATO CON SUCCESSO: {0}", Output.getPath());
                } else {
                    gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
                }
            } else {
                gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
            }

        } else if (tipofile.equals("MCO2")) { // MANAGEMENT CONTROL - REPORT MANAGEMENT CONTROL - DA INIZIO MESE A IERI     // NO DELETE
            String nomereport = "MANAGEMENT CONTROL - REPORT MANAGEMENT CONTROL N1 - NO DELETE OPERATIONS DA " + data1 + " A " + data2 + ".xlsx";
            File Output = new File(path + nomereport);
            String base64 = ControlloGestione.management_change_n1(Output, br1, data1, data2, false, allenabledbr, db);
            if (base64 != null) {
                boolean es = rilasciasftp(Output, meseriferimento + "/MANAGEMENT CONTROL", annoriferimento);
                if (es) {
                    gf.logger.log.log(Level.WARNING, "FILE RILASCIATO CON SUCCESSO: {0}", Output.getPath());
                } else {
                    gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
                }
            } else {
                gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
            }

        } else if (tipofile.equals("INS")) { // MANAGEMENT CONTROL - REPORT MANAGEMENT CONTROL - DA INIZIO MESE A IERI     // NO DELETE
            String nomereport = "MANAGEMENT CONTROL - REPORT LIMIT INSURANCE BRANCH DA " + data1 + " A " + data2 + ".xlsx";
            File Output = new File(path + nomereport);
            String base64 = ControlloGestione.limit_insurance(Output, br1, iniziomese, ieri, allenabledbr, db);
            if (base64 != null) {
                boolean es = rilasciasftp(Output, meseriferimento + "/MANAGEMENT CONTROL", annoriferimento);
                if (es) {
                    gf.logger.log.log(Level.WARNING, "FILE RILASCIATO CON SUCCESSO: {0}", Output.getPath());
                } else {
                    gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
                }
            } else {
                gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
            }

        } else if (tipofile.equals("CA1")) { // MANAGEMENT CONTROL - REPORT CHANGE ACCOUNTING N1 - DA INIZIO MESE A IERI     //COMPLETO
            String nomereport = "MANAGEMENT CONTROL - REPORT CHANGE ACCOUNTING N1 DA " + data1 + " A " + data2 + ".xlsx";
            File Output = new File(path + nomereport);
            String base64 = ControlloGestione.management_change_accounting1(Output, br1, iniziomese, ieri, allenabledbr, db);
            if (base64 != null) {
                boolean es = rilasciasftp(Output, meseriferimento + "/MANAGEMENT CONTROL", annoriferimento);
                if (es) {
                    gf.logger.log.log(Level.WARNING, "FILE RILASCIATO CON SUCCESSO: {0}", Output.getPath());
                } else {
                    gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
                }
            } else {
                gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
            }
        } else if (tipofile.equals("CAER")) { // 
            String nomereport = "CASHIER OPENCLOSE ERRORS DA " + data1 + " A " + data2 + ".xlsx";
            File Output = new File(path + nomereport);
            String base64 = ControlloGestione.C_OpenCloseError(Output, br1, data1, data2, allenabledbr, db);
            if (base64 != null) {
                boolean es = rilasciasftp(Output, meseriferimento + "/CASHIER PERFORMANCE", annoriferimento);
                if (es) {
                    gf.logger.log.log(Level.WARNING, "FILE RILASCIATO CON SUCCESSO: {0}", Output.getPath());
                } else {
                    gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
                }
            } else {
                gf.logger.log.log(Level.SEVERE, "ERRORE RILASCIO FILE: {0}", Output.getPath());
            }
        }

        db.closeDB();
        gf.logger.log.warning("END");
    }

//    public static void main(String[] args) {
////        GeneraFile gf = new GeneraFile();
////        gf.rilasciafile(gf, args[0]);
//
//        ondemand("2019");
//
//    }

    private boolean rilasciasftp(File file, String meseriferimento, String annoriferimento) {
        boolean ok = true;
        String folder_dest = "/mnt/MasterVolume/macsftp/Files/" + annoriferimento + "/" + meseriferimento + "/";
        ChannelSftp sftpseta = SftpConnection.connect(se_user, se_pwd, se_ip, se_port, this.logger);//inizio dell'upload dei file.
        if (sftpseta.isConnected()) {
            if (!isDirectory(sftpseta, folder_dest)) {
                logger.log.log(Level.INFO, "CREO CARTELLA {0}", folder_dest);
                try {
                    sftpseta.mkdir(folder_dest);
                } catch (SftpException ex) {
                    ok = false;
                    logger.log.log(Level.SEVERE, "ERRORE CREAZIONE CARTELLA {0}: {1}", new Object[]{folder_dest, ex.getMessage()});
                }
            }
            try {
                sftpseta.put(new FileInputStream(file), folder_dest + file.getName());
                logger.log.log(Level.INFO, "{3}: FILE CARICATO: {0} - SIZE: {1}", new Object[]{file.getName(), file.length(), "SFTP_MAC_FILES"});
            } catch (SftpException | FileNotFoundException ex) {
                ok = false;
                logger.log.log(Level.SEVERE, "ERRORE UPLOAD FILE {0}: {1}", new Object[]{file.getName(), ex.getMessage()});
            }
            SftpConnection.closeConnection(sftpseta, logger);
        } else {
            ok = false;
        }
        return ok;
    }

    public boolean is_IT = false;
    public boolean is_CZ = false;
    public boolean is_UK = true;

    public LoggerNew logger = new LoggerNew("SFTP_MAC_FILES", "/mnt/temp/");

    public boolean isIs_IT() {
        return is_IT;
    }

    public void setIs_IT(boolean is_IT) {
        this.is_IT = is_IT;
    }

    public boolean isIs_CZ() {
        return is_CZ;
    }

    public void setIs_CZ(boolean is_CZ) {
        this.is_CZ = is_CZ;
    }

    public boolean isIs_UK() {
        return is_UK;
    }

    public void setIs_UK(boolean is_UK) {
        this.is_UK = is_UK;
    }

    public String getThousand() {
        if (isIs_UK()) {
            return ",";
        } else {
            return ".";
        }
    }

    public String getDecimal() {
        if (isIs_UK()) {
            return ".";
        } else {
            return ",";
        }
    }

    public String getCODNAZ() {
        if (isIs_UK()) {
            return "031";
        } else if (isIs_CZ()) {
            return "275";
        } else {
            return "086";
        }
    }

//ITA
//    public static final String thousand = ".";
//    public static final String decimal = ",";
//    public static final String codnaz = "086";
//CZ
//    public static final String thousand = ".";
//    public static final String decimal = ",";
//    public static final String codnaz = "275";
//UK
//    public static final String thousand = ",";
//    public static final String decimal = ".";
//    public static final String codnaz = "031";
}

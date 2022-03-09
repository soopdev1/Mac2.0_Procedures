/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.refill.qlik;


import it.refill.esolver.NC_category;
import static it.refill.esolver.Util.fd;
import static it.refill.esolver.Util.patternnormdate_filter;
import static it.refill.esolver.Util.patternsql;
import static it.refill.esolver.Util.roundDoubleandFormat;
import it.refill.rilasciofile.Ch_transaction;
import it.refill.rilasciofile.DatabaseCons;
import it.refill.rilasciofile.GeneraFile;
import it.refill.rilasciofile.NC_causal;
import it.refill.rilasciofile.NC_transaction;
import static it.refill.rilasciofile.Utility.formatALNC_causal_ncde;
import static it.refill.rilasciofile.Utility.parseDoubleR;
import java.util.ArrayList;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 *
 * @author rcosco
 */
public class VAT_Ref {

    public static String insert_Qlik_VAT(GeneraFile gf, ArrayList<NC_transaction> result) {
        DatabaseCons db = new DatabaseCons(gf);
        try {
            
            ArrayList<NC_causal> array_nc_caus = db.list_nc_causal_all("000");
            ArrayList<String[]> array_nc_descr = db.list_nc_descr();

            for (int i = 0; i < result.size(); i++) {
                NC_transaction res = (NC_transaction) result.get(i);

                    String q1 = (roundDoubleandFormat(fd(res.getQuantita()), 0));
                    String p1 = (res.getPrezzo());
                    String f1 = ("0.00");
                    if (res.getFg_tipo_transazione_nc().equals("1")) {
                        q1 = "1";
                        p1 = (res.getNetto());
                        f1 = (res.getCommissione());
                    } else if (res.getFg_tipo_transazione_nc().equals("3")) {
                        q1 = (roundDoubleandFormat(fd(res.getRicevuta()), 0));
                        p1 = (res.getQuantita());
                    } else if (res.getFg_tipo_transazione_nc().equals("21")) {
                        String comm = "0.00";
                        if (fd(res.getCommissione()) > 0) {
                            comm = res.getCommissione();
                        } else {
                            comm = res.getTi_ticket_fee();
                        }
                        if (res.getTotal().contains("-")) {
                            comm = "-" + comm;
                        }
                        f1 = (comm);
                    }


                    Ch_transaction cht = null;
                    if (!res.getCh_transaction().equals("-")) {
                        cht = db.query_transaction_ch_reportNC(res.getCh_transaction());
                    }
                    
                    Qlik_ref nch;
                    
                    if (cht != null) {
                        double GM = fd(cht.getCommission()) + parseDoubleR(gf, cht.getRound()) + fd(cht.getSpread_total());
                        double stimaCO = parseDoubleR(gf, res.getTotal()) * (parseDoubleR(gf, cht.getCommission()) + parseDoubleR(gf, cht.getRound())) / parseDoubleR(gf, cht.getPay());
                        double stimaGM = (GM * parseDoubleR(gf, res.getTotal())) / parseDoubleR(gf, cht.getPay());
                        String volume =  roundDoubleandFormat((fd(p1)-stimaCO),2);
                        nch = new Qlik_ref(res.getCod(), res.getId(), res.getData(), res.getFiliale(),
                                res.getDel_fg(), res.getUser(), 
                                q1, p1, f1, res.getGruppo_nc(), res.getCausale_nc(),
                                res.getCl_cognome() + " " + res.getCl_nome(), formatALNC_causal_ncde(res.getCausale_nc(), array_nc_caus, array_nc_descr),
                                cht.getFix(), cht.getCom(), cht.getRound(), cht.getCommission(),
                                volume, roundDoubleandFormat(stimaCO,2), roundDoubleandFormat(stimaGM,2));
                    } else {
                        String volume = p1;
                        nch = new Qlik_ref(res.getCod(), res.getId(), res.getData(), res.getFiliale(),
                                res.getDel_fg(), res.getUser(), 
                                q1, p1, f1, res.getGruppo_nc(), res.getCausale_nc(),
                                res.getCl_cognome() + " " + res.getCl_nome(), 
                                formatALNC_causal_ncde(res.getCausale_nc(), array_nc_caus, array_nc_descr),
                                "0.00", "0.00", "0.00", "0.00",
                                volume, "0.00", "0.00");
                    }
                    
                    
                    db.insert_Qlik_VAT(nch);
               
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        db.closeDB();
        
        
        return null;
    }
    
    public static DateTime getDateRif(String from) {
        try {
            DateTimeFormatter formatter = DateTimeFormat.forPattern(patternnormdate_filter);
            DateTime dt = formatter.parseDateTime(from);
            return dt;
        } catch (Exception ex) {
            return null;
        }
    }
    
    
    public static void main(String[] args) {
        //RIMBORSI IVA
        GeneraFile gf = new GeneraFile();
        gf.setIs_IT(true);
        gf.setIs_UK(false);
        gf.setIs_CZ(false);

        DatabaseCons db = new DatabaseCons(gf);

//        DateTime iniziomese = new DateTime().minusDays(1).dayOfMonth().withMinimumValue().withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);;
//
//        DateTime ieri = new DateTime().minusDays(1);
//
//        String data1 = iniziomese.toString(patternsql);
//        String data2 = ieri.toString(patternsql);
//        
        
        String data1 = new DateTime().minusDays(1).toString(patternsql);
//        String data2 = "2020-02-02";
        
        ArrayList<String> br1 = db.list_branchcode_completeAFTER311217();
        ArrayList<NC_transaction> result = db.query_NC_transaction_NEW(data1, data1, br1, "SI");

        insert_Qlik_VAT(gf, result);
        
        db.verifica_Delete();
        
        db.closeDB();
    }
}
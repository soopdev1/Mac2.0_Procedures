/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.refill.rilasciofile;


import it.refill.esolver.Branch;
import static it.refill.esolver.Util.patternyear;
import static it.refill.rilasciofile.Utility.patternnormdate;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.joda.time.DateTime;

/**
 *
 * @author rcosco
 */
public class GeneraStockPriceHistorical {

    public static void main(String[] args) {

        String year = new DateTime().toString(patternyear);
        String datareport = new DateTime().toString(patternnormdate);
        GeneraFile gf = new GeneraFile();
        gf.setIs_IT(true);
        gf.setIs_UK(false);
        gf.setIs_CZ(false);

        Db dbm = new Db(true);
        String path = "E:\\temp\\HSP\\";
        ArrayList<Branch> allenabledbr = dbm.list_branch_completeAFTER311217();
        ArrayList<StockPrice_value> complete = new ArrayList<>();
        List<String> br1 = allenabledbr.stream().map(valore -> valore.getCod()).distinct().collect(Collectors.toList());

        for (int i = 0; i < br1.size(); i++) {
            try {
                String cod1 = br1.get(i);
                String desc1 = allenabledbr.stream().filter(c1 -> c1.getCod().equals(cod1)).findAny().get().getDe_branch();
                String[] fil = {br1.get(i), desc1};
                ArrayList<StockPrice_value> dati = dbm.list_StockPrice_value(fil, true, "EUR", gf);
                if (!dati.isEmpty()) {
                    StockPrice_value pdf = new StockPrice_value();
                    pdf.setId_filiale(fil[0]);
                    pdf.setDe_filiale(fil[1]);
                    pdf.setDati(dati);
                    complete.add(pdf);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        dbm.closeDB();
        StockPrice sp = new StockPrice();

        String base64_e = sp.receiptexcel_multi(path, complete, datareport);
        String base64 = sp.receipt_multi(path, complete, datareport);

        try {
//            Db dbm1 = new Db("//machaproxy01.mactwo.loc:3306/maccorp");
            Db dbm1 = new Db(true);
            
            Statement st = dbm1.getConnection().createStatement();
            st.executeUpdate("UPDATE conf SET des = '" + base64 + "' WHERE id = 'stockprice." + year + ".pdf'");
            st.close();
            Statement st2 = dbm1.getConnection().createStatement();
            st2.executeUpdate("UPDATE conf SET des = '" + base64_e + "' WHERE id = 'stockprice." + year + ".excel'");
            st2.close();
            dbm1.closeDB();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

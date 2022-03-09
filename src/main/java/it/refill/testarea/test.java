/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.refill.testarea;

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author rcosco
 */
public class test {

    public static final String host_PROD_CZ = "//machaproxy01.mactwo.loc:3306/maccorpczprod";
    public static final String host_TEST_CZ = "//machaproxy01.mactwo.loc:3306/maccorpcz";

    public static void main(String[] args) {
        
        try {
            
            
            
//            File txt = new File("C:\\Users\\rcosco\\Desktop\\content.txt");
//            
//            String bs4 = Files.toString(txt, Charset.defaultCharset());
//                
//            FileUtils.writeByteArrayToFile(new File("C:\\Users\\rcosco\\Desktop\\content.pdf"), Base64.decodeBase64(bs4)); 
            
//        try {
//            Db db = new Db(host_TEST_CZ, false);
//            ResultSet rs = db.getC().createStatement().executeQuery("SELECT gruppo_nc,causale_nc,fg_in_out,fg_tipo_transazione_nc,nc_de,de_causale_nc FROM maccorpcz.nc_causali WHERE filiale='000'");
//
////            AtomicInteger i0 = new AtomicInteger(0);
//            while (rs.next()) {
//                String ncde = "00";
//                if (rs.getString(4).equals("1") && rs.getString(3).equals("2")) {
//                    ncde = "01";
//                } else if (rs.getString(4).equals("1") && rs.getString(3).equals("1")) {
//                    ncde = "02";
//                } else if (rs.getString(4).equals("2") && rs.getString(3).equals("3")) {   //STOCK
//                    ncde = "09";
//                } else if (rs.getString(4).equals("2") && rs.getString(3).equals("2")) {   //STOCK
//                    ncde = "10";
//                } else if (rs.getString(4).equals("2") && rs.getString(3).equals("4")) {   //STOCK
//                    ncde = "14";
//                } else if (rs.getString(4).equals("4") && rs.getString(3).equals("1")) {   //NO STOCK
//                    ncde = "15";
//                } else if (rs.getString(4).equals("4") && rs.getString(3).equals("2")) {   //NO STOCK 
//                    ncde = "16";
//                } else {
//                    System.out.println(rs.getString(6) + " : " + rs.getString(4) + ";" + rs.getString(3) + " = " + rs.getString(5) + " ----------------------------");
//                }
//
////                System.out.println(rs.getString(1)+" (-) "+rs.getString(2)+" (-) "+rs.getString(3)+" (-) "+rs.getString(4));
////                String causale_nc = StringUtils.leftPad(String.valueOf(i0.addAndGet(1)), 6, "0");
//                String upd = "UPDATE nc_causali SET nc_de = '" + ncde + "' WHERE filiale = '000' AND gruppo_nc = '"
//                        + rs.getString(1) + "' AND causale_nc = '" + rs.getString(2) + "'";
//                boolean es = db.getC().createStatement().executeUpdate(upd) > 0;
//                System.out.println(upd + " --> " + es);
//            }
//
//            db.closeDB();
//        } catch (SQLException ex) {
//            ex.printStackTrace();
//        }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

;

}

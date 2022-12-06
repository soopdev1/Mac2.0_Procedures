
import static rc.soop.esolver.Util.patternsql;
import rc.soop.rilasciofile.Ch_transaction;
import rc.soop.rilasciofile.DatabaseCons;
import rc.soop.rilasciofile.Excel;
import rc.soop.rilasciofile.GeneraFile;
import java.io.File;
import java.util.ArrayList;
import org.joda.time.DateTime;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
/**
 *
 * @author rcosco
 */
public class ReportOndemand {

    public static void main(String[] args) {
        GeneraFile gf = new GeneraFile();
        gf.setIs_IT(true);
        gf.setIs_UK(false);
        gf.setIs_CZ(false);
        DatabaseCons db = new DatabaseCons(gf);
        String path = db.getPath("temp");
        ArrayList<String> br1 = db.list_branchcode_completeAFTER311217();
//        ArrayList<Branch> allenabledbr = db.list_branch();

        String data1 = new DateTime(2022, 9, 1, 0, 0).toString(patternsql);
        String data2 = new DateTime(2022, 9, 30, 23, 59).toString(patternsql);

        ArrayList<Ch_transaction> result = db.query_transaction_ch_new(data1, data2, br1);
        String nomereport = "LIST TRANSACTION CHANGE DA " + data1 + " A " + data2 + ".xlsx";
        File Output = new File(path + nomereport);
        String base64 = Excel.excel_transaction_listEVO(gf, Output, result);
//        ArrayList<NC_transaction> result = db.query_NC_transaction_NEW(data1, data2, br1, "NO");
//        String nomereport = "LIST TRANSACTION NOCHANGE DA " + data1 + " A " + data2 + ".xlsx";
//        File Output = new File(path + nomereport);
//        String base64 = Excel.excel_transactionnc_list(gf, Output, result);
        db.closeDB();

        System.out.println(path + nomereport);
    }
}

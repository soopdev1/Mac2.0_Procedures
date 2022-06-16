/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.refill.main;

import static it.refill.cora.MacCORA.generaannuale;
import static it.refill.cora.MacCORA.generamensile;
import it.refill.esolver.Atlante;
import it.refill.esolver.ESolver;
import static it.refill.gs.Client.getInputList;
import static it.refill.gs.Client.invia;
import it.refill.gs.DatiInvio;
import it.refill.gs.Db_Master;
import it.refill.gs.Filiale;
import it.refill.rilasciofile.GeneraFile;
import it.refill.rilasciofile.SftpSIA;
import java.io.File;
import java.util.List;
import org.joda.time.DateTime;

/**
 *
 * @author raf
 */
public class MainSelector {

    public static void main(String[] args) {

        int scelta;
        String repvalue;
        try {
            scelta = Integer.parseInt(args[0]);
            repvalue = args[1];
        } catch (Exception e) {
            scelta = 0;
            repvalue = "TCH";
        }

        switch (scelta) {
            case 1: //  ESOLVER
                ESolver.main(args);
                break;
            case 2: //  ATLANTE
                Atlante.main(args);
                break;
            case 3: //  NEXI - FILE TRIMESTRALE ESOLVER
                new SftpSIA().rilasciaFIle(new File(args[1]), true);
                break;
            case 4: //  GRANDI STAZIONI
                List<Filiale> input = getInputList();
                DateTime dt = new DateTime();
                Db_Master db1 = new Db_Master();
                List<DatiInvio> dati = db1.query_datiinvio(input, dt);
                db1.closeDB();
                invia(dati);
                break;
            case 5: //  RILASCIO FILE
                GeneraFile gf = new GeneraFile();
                gf.rilasciafile(gf, repvalue);
                break;
            case 6: //  CORA MENSILE
                generamensile();
                break;
            case 7: //  CORA ANNUALE
                generaannuale();
                break;
            default:
                break;
        }

    }
}

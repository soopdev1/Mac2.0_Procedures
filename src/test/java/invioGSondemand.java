
import static it.refill.gs.Client.invia2022;
import it.refill.gs.DatiInvio;
import it.refill.gs.Db_Master;
import it.refill.gs.Filiale;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
/**
 *
 * @author Raffaele
 */
public class invioGSondemand {

    public static void main(String[] args) {
        Db_Master db1 = new Db_Master();
        List<Filiale> input = new ArrayList<>();
        input.add(new Filiale("189", "MIC_A0199"));
        List<DatiInvio> dati = db1.query_datiinvio(input, new DateTime(2019, 2, 1, 0, 0), new DateTime());
        db1.closeDB();
        invia2022(dati);
    }
}

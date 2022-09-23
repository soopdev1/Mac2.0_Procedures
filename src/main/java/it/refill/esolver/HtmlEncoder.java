package it.refill.esolver;


import static it.refill.esolver.Util.generaId;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;

public final class HtmlEncoder {

    public static <T extends Appendable> T escapeNonLatin(CharSequence sequence, T out)
            throws IOException {
        for (int i = 0; i < sequence.length(); i++) {
            char ch = sequence.charAt(i);
            if (Character.UnicodeBlock.of(ch) == Character.UnicodeBlock.BASIC_LATIN) {
                out.append(ch);
            } else {
                int codepoint = Character.codePointAt(sequence, i);

                i += Character.charCount(codepoint) - 1;

                out.append("&#x");
                out.append(Integer.toHexString(codepoint));
                out.append(";");
            }
        }
        return out;
    }

    public static String base64HTML(String path, String ing) {
        try {
            File f1 = new File(path + generaId(150) + ".html");
            FileOutputStream is = new FileOutputStream(f1);
            OutputStreamWriter osw = new OutputStreamWriter(is);
            BufferedWriter w = new BufferedWriter(osw);
            w.write(ing);
            w.close();
            osw.close();
            is.close();
            String base64 = new String(Base64.encodeBase64(FileUtils.readFileToByteArray(f1)));
            f1.delete();
            return base64;
        } catch (FileNotFoundException ex) {
        } catch (IOException ex) {
        }
        return null;
    }

    public static String getBase64HTML(String base64) {
        byte[] ing = Base64.decodeBase64(base64.getBytes());
        String s = new String(ing);
        return s;
    }

}

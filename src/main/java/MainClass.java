import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.Connection;
import org.jsoup.Connection.Response;

import java.io.IOException;

public class MainClass {
    public static void main(String[] args) {
        System.out.println("Message from MainClass");
        runParser();
    }

    public static void runParser(){
        final String url = "http://www.cse.ust.hk";
        Connection conn = Jsoup.connect(url);
        try {
            Response res = conn.execute();
            Boolean redirectFlag = res.hasHeader("location");
            String redirectAddr = res.header("location");
            String lastModified = res.header("last-modified");
            System.out.println(redirectFlag);
            System.out.println(redirectAddr);
            System.out.println(lastModified);
        } catch (HttpStatusException e) {
        } catch (IOException e) {
        }
    }

}

package crawler;

import org.rocksdb.RocksDBException;
import repository.Repository;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class ResultWriter {

    public static void write_spider_result() {
        try (var writer = new BufferedWriter(new FileWriter("spider_result.txt"))) {
            writer.write("");
            var map = Repository.Page.getMap_url_pageId();
            for(Map.Entry<String, String> entry : map.entrySet()){
                var pageInfo = Repository.PageInfo.getPageInfo(entry.getValue());
                if (pageInfo == null) continue;

                writer.append(pageInfo.pgTitle).append(String.valueOf('\n'));
                writer.append(new String(entry.getKey())).append(String.valueOf('\n'));
                writer.append(pageInfo.lastModifiedDate).append(", ")
                        .append(pageInfo.pageSize).append('\n');

                var wordPosting = Repository.ForwardFrequency
                        .getMap_WordId_Freq(entry.getValue());

//                System.out.println(wordPosting);

                for(var e : wordPosting.entrySet()){
//                    System.out.println(e);
                    String termId = e.getKey();
                    writer.append(Repository.Word.getWord(termId) + " " + e.getValue() + ";");
                }
                writer.append('\n');

                for(var link: pageInfo.childLinks){
                    writer.append(link).append('\n');
                }

                writer.append("---------------------------------------------").append("\n\n");

            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RocksDBException e) {
            e.printStackTrace();
        }

    }
}

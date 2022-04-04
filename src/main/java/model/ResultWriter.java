package model;

import org.rocksdb.RocksDBException;
import repository.Repository;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class ResultWriter {

    public static void write_spider_result() {
        try (var writer = new BufferedWriter(new FileWriter("spider_result.txt"))) {
            writer.write("");
            var map = Repository.Page.getMap_url_pageId();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                var pageInfo = Repository.PageInfo.getPageInfo(entry.getValue());
                if (pageInfo == null) continue;

                writer.append(pageInfo.pgTitle).append(String.valueOf('\n'));
                writer.append(new String(entry.getKey())).append(String.valueOf('\n'));
                writer.append(pageInfo.lastModifiedDate).append(", ")
                        .append(pageInfo.pageSize).append('\n');

                var wordPosting = Repository.ForwardIndex
                        .getMap_WordId_Positions(entry.getValue());

//                System.out.println(wordPosting);

                for (var e : wordPosting.entrySet()) {
//                    System.out.println(e);
                    String termId = e.getKey();
                    writer.append(Repository.Word.getWord(termId) + " " + e.getValue() + ";");
                }
                writer.append('\n');

                for (var link : pageInfo.childLinks) {
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

    public static void write_inverted_file() {
        try (var writer = new BufferedWriter(new FileWriter("inverted_file.txt"))) {
            var inverted = Repository.InvertedIndex.getAll_InvertedIndex();

            for (Map.Entry<String, HashMap<String, List<Integer>>> entry : inverted.entrySet()) {
                var wordId = entry.getKey();
                var map_pageId_posList = entry.getValue();

                writer.append(wordId)
                        .append(": ")
                        .append(map_pageId_posList.toString())
                        .append('\n');
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void write_page_children(HashMap<String, HashSet<String>> map) {
        try (var writer = new BufferedWriter(new FileWriter("page_children.txt"))) {
            // sort in asc length
            var sortedKeyList = map.keySet().stream().sorted().collect(Collectors.toList());
            // for (Map.Entry<String, HashSet<String>> entry : map.entrySet()) {
            for (String page: sortedKeyList) {
                
                var childrenList = map.get(page);

                writer.append(page)
                        .append("> \n\n");
                for (String child: childrenList) {
                    writer.append(child).append("\n");
                }

                writer.append("==================================================================\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

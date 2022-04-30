package model;

import org.rocksdb.RocksDBException;
import repository.Repository;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Class for writing the results of queries  */
public class ResultWriter {

    /** Writes the result of the crawled and indexed pages into spider_result.txt */
    public static void write_spider_result() {
        try (var writer = new BufferedWriter(new FileWriter("spider_result.txt"))) {
            writer.write("");
            var map_url_pageId = Repository.Page.getMap_url_pageId();
            var map_pageId_pageInfo = Repository.PageInfo.getMap_pageId_pageInfo();

            for (Map.Entry<String, String> entry : map_url_pageId.entrySet()) {
                String url = entry.getKey();
                String pageId = entry.getValue();

                var pageInfo = map_pageId_pageInfo.get(pageId);
                if (pageInfo == null) continue;

                writer.append(pageInfo.pageTitle).append(String.valueOf('\n'));
                writer.append(new String(url)).append(String.valueOf('\n'));
                writer.append(pageInfo.lastModifiedDate).append(", ")
                        .append(pageInfo.pageSize).append('\n');

                var wordPosting = Repository.ForwardIndex
                        .getMap_WordId_Positions(pageId);

                for (var e : wordPosting.entrySet()) {
                    String termId = e.getKey();
                    writer.append(Repository.Word.getWord(termId) + " " + e.getValue() + ";");
                }
                writer.append('\n');

                writer.append("Parent Links\n");
                for (var link : pageInfo.parentLinks) {
                    writer.append(Repository.Page.getPageUrl(link)).append('\n');
                }

                writer.append("Child Links\n");
                for (var link : pageInfo.childLinks) {
                    writer.append(Repository.Page.getPageUrl(link)).append('\n');
                }

                writer.append("---------------------------------------------").append("\n\n");

            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    /** Writes the inverted Index to inverted_file.txt */
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

    /** Writes the forward index title to forwardTitle.txt */
    public static void write_forwardTitle_File() {
        try (var writer = new BufferedWriter(new FileWriter("forwardTitle.txt"))) {
            var forwardIndexFile = Repository.ForwardIndex_Title.getAll_ForwardIndex();

            for (Map.Entry<String, HashMap<String, List<Integer>>> entry : forwardIndexFile.entrySet()) {
                var pageId = entry.getKey();
                var pageTitle = entry.getValue();

                writer.append(Repository.Page.getPageUrl(pageId))
                        .append(": {");
                for (var wordId : pageTitle.keySet()) {
                    var word = Repository.Word.getWord(wordId);
                    writer.append(word).append(" :").append(pageTitle.get(wordId).toString());
                }
                writer.append("}").append('\n');
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    /** Writes the invertedIndex title to invertedTitle_file.txt */
    public static void write_invertedTitle_file() {
        try (var writer = new BufferedWriter(new FileWriter("invertedTitle_file.txt"))) {
            var inverted = Repository.InvertedIndex_Title.getAll_InvertedIndex();

            for (Map.Entry<String, HashMap<String, List<Integer>>> entry : inverted.entrySet()) {
                var wordId = entry.getKey();
                var map_pageId_posList = entry.getValue();


                writer.append(Repository.Word.getWord(wordId))
                        .append(": ")
                        .append(map_pageId_posList.toString())
                        .append('\n');

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    /** Writes the result of the query to query_result.txt
     *
     * @param resultList  The list of pages/results to be written to query_result.txt
     */
    public static void write_queryResult(List<RetrievedDocument> resultList) {
        try (var writer = new BufferedWriter(new FileWriter("query_result.txt"))) {
//            for (int i = 0; i < resultList.size(); i++) {
//                var page = resultList.get(i);
//
//            }
            writer.append(resultList.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

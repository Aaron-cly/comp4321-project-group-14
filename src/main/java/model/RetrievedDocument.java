package model;

import java.util.HashMap;
import java.util.HashSet;

public class RetrievedDocument extends PageInfo {
    public double score;
    public HashSet<String> parentLinks;
    public HashMap<String, Integer> keyword_freq;

    private RetrievedDocument(String pageTitle, String url, String lastModifiedData, HashSet<String> childLinks, String pageSize, int max_termFreq) {
        super(pageTitle, url, lastModifiedData, childLinks, pageSize, max_termFreq);
    }

    public RetrievedDocument(PageInfo pageInfo, double score, HashSet<String> parentLinks, HashMap<String, Integer> keyword_freq) {
        this(pageInfo.pageTitle, pageInfo.url, pageInfo.lastModifiedDate, pageInfo.childLinks, pageInfo.pageSize, pageInfo.max_termFreq);
        this.score = score;
        this.parentLinks = parentLinks;
        this.keyword_freq = keyword_freq;
    }

    @Override
    public String toString() {
        return String.format("%.2f", score) + "\t" + pageTitle+
                "\t\t" + url + '\n' +
                "\t\t" + lastModifiedDate + ", " + pageSize + '\n' +
                "\t\t" + keyword_freq + '\n' +
                "\tParent Links" + '\n' +
                "\t\t" + parentLinks.stream().map(url -> url + '\n') +
                "\tChild Links" + '\n' +
                "\t\t" + childLinks.stream().map(url -> url + '\n') +
                "=======================================================\n" ;
    }

}

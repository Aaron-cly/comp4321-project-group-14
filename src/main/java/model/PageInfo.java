package model;

import java.io.Serializable;
import java.util.HashSet;

public class PageInfo implements Serializable {
    public String pgTitle;
    public String lastModifiedDate;
    public HashSet<String> childLinks;
    public String pageSize;
    public int max_termFreq;

    public PageInfo(String pgTitle, String lastModifiedData, HashSet<String> childLinks, String pageSize, int max_termFreq) {
        this.pgTitle = pgTitle;
        this.lastModifiedDate = lastModifiedData;
        this.childLinks = childLinks;
        this.pageSize = pageSize;
        this.max_termFreq = max_termFreq;
    }

    public static byte[] convertToByteArray(PageInfo data) {
        return SerializeUtil.serialize(data);
    }

    public static PageInfo deserialize(byte[] data) {
        return SerializeUtil.deserialize(data);
    }

    @Override
    public String toString() {
        return "PageInfo {" +
                "pgTitle='" + pgTitle + '\'' +
                ", lastModifiedData=" + lastModifiedDate +
                ", childLinks= " + childLinks +
                ", max_termFreq= " + max_termFreq +
                '}';
    }

}

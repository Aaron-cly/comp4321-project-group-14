package model;

import java.io.*;
import java.util.HashSet;

public class PageInfo implements Serializable {
    public String pgTitle;
    public String lastModifiedDate;
    public HashSet<String> childLinks;

    public PageInfo(String pgTitle, String lastModifiedData, HashSet<String> childLinks) {
        this.pgTitle = pgTitle;
        this.lastModifiedDate = lastModifiedData;
        this.childLinks = childLinks;
    }

    public static byte[] convertToByteArray(PageInfo data) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(data);
            oos.flush();
            return bos.toByteArray();
        }  catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static PageInfo deserialize(byte[] data){
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            PageInfo deserializedData = (PageInfo) ois.readObject();
            return deserializedData;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String toString() {
        return "PageInfo {" +
                "pgTitle='" + pgTitle + '\'' +
                ", lastModifiedData=" + lastModifiedDate +
                ", childLinks=" + childLinks +
                '}';
    }

}

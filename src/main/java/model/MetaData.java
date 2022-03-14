package model;

import java.io.*;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class MetaData implements Serializable {
    public String pgTitle;
    public String url;
    public HashMap<String, Integer> frequencies = new HashMap<>();
    public Date lastModifiedDate;
    public long pgSize;
    public List<String> childLinks;

    public MetaData(String pgTitle, String url, HashMap<String, Integer> frequencies, Date lastModifiedData, long pgSize, List<String> childLinks) {
        this.frequencies = frequencies;
        this.pgTitle = pgTitle;
        this.url = url;
        this.lastModifiedDate = lastModifiedData;
        this.pgSize = pgSize;
        this.childLinks = childLinks;
    }

    public static byte[] convertToByteArray(MetaData data) {
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

    public static MetaData deserialize(byte[] data){
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            MetaData deserializedData = (MetaData) ois.readObject();
            return deserializedData;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String toString() {
        return "MetaData{" +
                "frequencies=" + frequencies +
                ", pgTitle='" + pgTitle + '\'' +
                ", lastModifiedData=" + lastModifiedDate +
                ", pgSize=" + pgSize +
                ", childLinks=" + childLinks +
                '}';
    }
}

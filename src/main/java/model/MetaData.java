package model;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MetaData implements Serializable {
    String pgTitle;
    Date lastModifiedData;
    long pgSize;
    List<String> childLinks;

    public MetaData(String pgTitle, Date lastModifiedData, long pgSize, List<String> childLinks) {
        this.pgTitle = pgTitle;
        this.lastModifiedData = lastModifiedData;
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
                "pgTitle='" + pgTitle + '\'' +
                ", lastModifiedData=" + lastModifiedData +
                ", pgSize=" + pgSize +
                ", childLinks=" + childLinks +
                '}';
    }
}

package model;

import java.io.*;

public class SerializeUtil {
    public static byte[] serialize(Serializable obj) {
        byte[] dataBytes = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            dataBytes = bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dataBytes;
    }

    public static <E extends Serializable> E deserialize(byte[] dataBytes) {
        E obj = null;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(dataBytes);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            obj = (E) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return obj;
    }
}

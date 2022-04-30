package model;

import java.io.*;

/** Helper class for serializing and deserializing objects. Used in saving objects or retrieving objects in rocksdb */
public class SerializeUtil {
    /** Serializes objects into byte[]
     *
     * @param obj  Object to be serialized
     * @return  The byte[] equivalent of the given obj parameter
     */
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

    /** Deserialized objects from byte[] back to the object
     *
     * @param dataBytes  The byte[], to be deserialized
     * @param <E>
     * @return  The deserialized object
     */
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

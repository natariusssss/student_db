import java.io.*;
import java.util.*;

public class StudentIndex {
    private String indexPath;
    private Map<Integer, Long> index;

    public StudentIndex(String indexPath) {
        this.indexPath = indexPath;
        this.index = new HashMap<>();
        loadIndex();
    }

    private void loadIndex() {
        index.clear();
        File file = new File(indexPath);
        if (!file.exists()) return;
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            while (in.available() > 0) {
                int id = in.readInt();
                long offset = in.readLong();
                index.put(id, offset);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveIndex() {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(indexPath))) {
            for (Map.Entry<Integer, Long> entry : index.entrySet()) {
                out.writeInt(entry.getKey());
                out.writeLong(entry.getValue());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public long findOffset(int studentId) {
        return index.getOrDefault(studentId, -1L);
    }

    public boolean contains(int studentId) {
        return index.containsKey(studentId);
    }

    public void addEntry(int studentId, long offset) {
        index.put(studentId, offset);
        saveIndex();
    }

    public void removeEntry(int studentId) {
        index.remove(studentId);
        saveIndex();
    }

    public void clear() {
        index.clear();
        new File(indexPath).delete();
    }
}

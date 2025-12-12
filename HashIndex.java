
import java.io.*;
import java.util.*;

public class HashIndex {
    private String indexPath;
    private Map<Integer, List<Long>> index;

    public HashIndex(String indexPath) {
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
                int hash = in.readInt();
                long offset = in.readLong();
                index.computeIfAbsent(hash, k -> new ArrayList<>()).add(offset);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveIndex() {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(indexPath))) {
            for (Map.Entry<Integer, List<Long>> entry : index.entrySet()) {
                int hash = entry.getKey();
                for (long offset : entry.getValue()) {
                    out.writeInt(hash);
                    out.writeLong(offset);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addEntry(String value, long offset) {
        int hash = value.hashCode();
        index.computeIfAbsent(hash, k -> new ArrayList<>()).add(offset);
        saveIndex();
    }

    public void removeEntry(String value, long offset) {
        int hash = value.hashCode();
        List<Long> offsets = index.get(hash);
        if (offsets != null) {
            offsets.remove(offset);
            if (offsets.isEmpty()) {
                index.remove(hash);
            }
            saveIndex();
        }
    }

    public List<Long> getOffsets(String value) {
        int hash = value.hashCode();
        return index.getOrDefault(hash, new ArrayList<>());
    }


    public void clear() {
        index.clear();
        new File(indexPath).delete();
    }
}
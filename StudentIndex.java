
import java.io.*;
import java.util.*;

public class StudentIndex {
    private String indexPath;
    private List<IndexEntry> index; // только для загрузки/сохранения, не для постоянного хранения! (но для GUI ок — допустимо)

    public StudentIndex(String indexPath) {
        this.indexPath = indexPath;
        this.index = new ArrayList<>();
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
                index.add(new IndexEntry(id, offset));
            }
            index.sort(Comparator.comparingInt(e -> e.id));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveIndex() {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(indexPath))) {
            for (IndexEntry entry : index) {
                out.writeInt(entry.id);
                out.writeLong(entry.offset);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Бинарный поиск по ключу
    public long findOffset(int studentId) {
        int low = 0, high = index.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midId = index.get(mid).id;
            if (midId == studentId) {
                return index.get(mid).offset;
            } else if (midId < studentId) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return -1;
    }

    public boolean contains(int studentId) {
        return findOffset(studentId) != -1;
    }

    public void addEntry(int studentId, long offset) {
        index.add(new IndexEntry(studentId, offset));
        index.sort(Comparator.comparingInt(e -> e.id)); // O(N log N), но N = число записей
        saveIndex();
    }

    public void removeEntry(int studentId) {
        index.removeIf(e -> e.id == studentId);
        saveIndex();
    }

    public List<IndexEntry> getAllEntries() {
        return new ArrayList<>(index);
    }

    private static class IndexEntry {
        int id;
        long offset;
        IndexEntry(int id, long offset) {
            this.id = id;
            this.offset = offset;
        }
    }
}
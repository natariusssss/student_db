import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class StudentDatabase {
    private String dataFilePath;
    private String indexPath;
    private StudentIndex index;
    private HashIndex nameIndex;
    private RandomAccessFile dataFile;

    public StudentDatabase(String dbName) {
        this.dataFilePath = dbName + ".dat";
        this.indexPath = dbName + ".idx";
        this.index = new StudentIndex(indexPath);
        this.nameIndex = new HashIndex(dbName + "_name.idx");

        try {
            this.dataFile = new RandomAccessFile(dataFilePath, "rw");
        } catch (IOException e) {
            throw new RuntimeException("Не удалось открыть файл БД", e);
        }
    }

    public boolean addStudent(Student student) throws IOException {
        if (index.contains(student.getStudentId())) {
            return false;
        }
        dataFile.seek(dataFile.length());
        long offset = dataFile.getFilePointer();
        student.writeTo(dataFile);
        index.addEntry(student.getStudentId(), offset);
        nameIndex.addEntry(student.getName(), offset);
        return true;
    }

    public boolean deleteStudentById(int studentId) throws IOException {
        long offset = index.findOffset(studentId);
        if (offset == -1) return false;
        dataFile.seek(offset);
        Student s = Student.readFrom(dataFile);
        index.removeEntry(studentId);
        nameIndex.removeEntry(s.getName(), offset);
        return true;
    }

    public int deleteStudentsByField(String field, Object value) throws IOException {
        List<Integer> idsToDelete = new ArrayList<>();
        dataFile.seek(0);
        while (dataFile.getFilePointer() < dataFile.length()) {
            long pos = dataFile.getFilePointer();
            Student s = Student.readFrom(dataFile);
            if (matches(s, field, value)) {
                idsToDelete.add(s.getStudentId());
            }
        }
        int count = 0;
        for (int id : idsToDelete) {
            if (deleteStudentById(id)) count++;
        }
        return count;
    }

    public Student findStudentById(int studentId) throws IOException {
        long offset = index.findOffset(studentId);
        if (offset == -1) return null;
        dataFile.seek(offset);
        return Student.readFrom(dataFile);
    }

    public List<Student> findStudentsByField(String field, Object value) throws IOException {
        if ("name".equals(field)) {
            return findStudentsByName((String) value);
        }
        List<Student> results = new ArrayList<>();
        dataFile.seek(0);
        while (dataFile.getFilePointer() < dataFile.length()) {
            Student s = Student.readFrom(dataFile);
            if (matches(s, field, value)) {
                results.add(s);
            }
        }
        return results;
    }

    private List<Student> findStudentsByName(String name) throws IOException {
        List<Long> offsets = nameIndex.getOffsets(name);
        List<Student> results = new ArrayList<>();
        for (long offset : offsets) {
            dataFile.seek(offset);
            Student s = Student.readFrom(dataFile);
            if (s.getName().equals(name)) {
                results.add(s);
            }
        }
        return results;
    }

    private boolean matches(Student s, String field, Object value) {
        switch (field) {
            case "name": return s.getName().equals(value);
            case "gpa": return Math.abs(s.getGpa() - (Double) value) < 1e-6;
            case "enrollmentYear": return s.getEnrollmentYear() == (Integer) value;
            default: return false;
        }
    }

    public void updateStudent(Student updated) throws IOException {
        long offset = index.findOffset(updated.getStudentId());
        if (offset == -1) throw new IllegalArgumentException("Студент не найден");
        dataFile.seek(offset);
        Student old = Student.readFrom(dataFile);
        if (!old.getName().equals(updated.getName())) {
            nameIndex.removeEntry(old.getName(), offset);
            nameIndex.addEntry(updated.getName(), offset);
        }
        dataFile.seek(offset);
        updated.writeTo(dataFile);
    }

    public void clear() throws IOException {
        dataFile.setLength(0);
        index = new StudentIndex(indexPath);
        nameIndex.clear();
    }

    public void close() throws IOException {
        dataFile.close();
        index.saveIndex();
        nameIndex.saveIndex();
    }

    public void backup(String backupName) throws IOException {
        Files.copy(Paths.get(dataFilePath), Paths.get(backupName + ".dat"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get(indexPath), Paths.get(backupName + ".idx"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get(dataFilePath.replace(".dat", "_name.idx")),
                Paths.get(backupName + "_name.idx"), StandardCopyOption.REPLACE_EXISTING);
    }

    public void restoreFromBackup(String backupName) throws IOException {
        close();
        Files.copy(Paths.get(backupName + ".dat"), Paths.get(dataFilePath), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get(backupName + ".idx"), Paths.get(indexPath), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get(backupName + "_name.idx"),
                Paths.get(dataFilePath.replace(".dat", "_name.idx")), StandardCopyOption.REPLACE_EXISTING);
        dataFile = new RandomAccessFile(dataFilePath, "rw");
        index = new StudentIndex(indexPath);
        nameIndex = new HashIndex(dataFilePath.replace(".dat", "_name.idx"));
    }

    public List<Student> getAllStudents() throws IOException {
        List<Student> list = new ArrayList<>();
        dataFile.seek(0);
        while (dataFile.getFilePointer() < dataFile.length()) {
            list.add(Student.readFrom(dataFile));
        }
        return list;
    }
}
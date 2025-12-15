import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class StudentDatabase {
    private String dataFilePath;
    private String indexPath;
    private StudentIndex idIndex;
    private ValueIndex nameIndex;
    private ValueIndex gpaIndex;
    private ValueIndex yearIndex;
    private RandomAccessFile dataFile;

    public StudentDatabase(String dbName) {
        this.dataFilePath = dbName + ".dat";
        this.indexPath = dbName + ".idx";
        this.idIndex = new StudentIndex(indexPath);
        this.nameIndex = new ValueIndex(dbName + "_name.idx");
        this.gpaIndex = new ValueIndex(dbName + "_gpa.idx");
        this.yearIndex = new ValueIndex(dbName + "_year.idx");

        try {
            this.dataFile = new RandomAccessFile(dataFilePath, "rw");
        } catch (IOException e) {
            throw new RuntimeException("Не удалось открыть файл БД", e);
        }
    }

    private long encodeGpa(double gpa) {
        return (long) Math.round(gpa * 100);
    }

    private long encodeName(String name) {
        return name.hashCode();
    }

    public boolean addStudent(Student student) throws IOException {
        if (idIndex.contains(student.getStudentId())) {
            return false;
        }
        dataFile.seek(dataFile.length());
        long offset = dataFile.getFilePointer();
        student.writeTo(dataFile);

        idIndex.addEntry(student.getStudentId(), offset);
        nameIndex.addEntry(encodeName(student.getName()), offset);
        gpaIndex.addEntry(encodeGpa(student.getGpa()), offset);
        yearIndex.addEntry(student.getEnrollmentYear(), offset);
        return true;
    }

    public boolean deleteStudentById(int studentId) throws IOException {
        long offset = idIndex.findOffset(studentId);
        if (offset == -1) return false;

        dataFile.seek(offset);
        Student s = Student.readFrom(dataFile);

        idIndex.removeEntry(studentId);
        nameIndex.removeEntry(encodeName(s.getName()), offset);
        gpaIndex.removeEntry(encodeGpa(s.getGpa()), offset);
        yearIndex.removeEntry(s.getEnrollmentYear(), offset);
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

    private boolean matches(Student s, String field, Object value) {
        switch (field) {
            case "name": return s.getName().equals(value);
            case "gpa": return Math.abs(s.getGpa() - (Double) value) < 1e-6;
            case "enrollmentYear": return s.getEnrollmentYear() == (Integer) value;
            default: return false;
        }
    }

    public Student findStudentById(int studentId) throws IOException {
        long offset = idIndex.findOffset(studentId);
        if (offset == -1) return null;
        dataFile.seek(offset);
        return Student.readFrom(dataFile);
    }

    public List<Student> findStudentsByField(String field, Object value) throws IOException {
        switch (field) {
            case "name": return findStudentsByName((String) value);
            case "gpa": return findStudentsByGpa((Double) value);
            case "enrollmentYear": return findStudentsByYear((Integer) value);
            default: throw new IllegalArgumentException("Unknown field: " + field);
        }
    }

    private List<Student> findStudentsByName(String name) throws IOException {
        return readStudentsByOffset(nameIndex.getOffsets(encodeName(name)), name, null, null);
    }

    private List<Student> findStudentsByGpa(double gpa) throws IOException {
        return readStudentsByOffset(gpaIndex.getOffsets(encodeGpa(gpa)), null, gpa, null);
    }

    private List<Student> findStudentsByYear(int year) throws IOException {
        return readStudentsByOffset(yearIndex.getOffsets(year), null, null, year);
    }

    private List<Student> readStudentsByOffset(List<Long> offsets, String name, Double gpa, Integer year) throws IOException {
        List<Student> results = new ArrayList<>();
        for (long offset : offsets) {
            dataFile.seek(offset);
            Student s = Student.readFrom(dataFile);
            boolean match = true;
            if (name != null) match = s.getName().equals(name);
            if (gpa != null) match = Math.abs(s.getGpa() - gpa) < 1e-6;
            if (year != null) match = s.getEnrollmentYear() == year;
            if (match) results.add(s);
        }
        return results;
    }

    public void updateStudent(Student updated) throws IOException {
        long offset = idIndex.findOffset(updated.getStudentId());
        if (offset == -1) throw new IllegalArgumentException("Student not found");

        dataFile.seek(offset);
        Student old = Student.readFrom(dataFile);

        nameIndex.removeEntry(encodeName(old.getName()), offset);
        gpaIndex.removeEntry(encodeGpa(old.getGpa()), offset);
        yearIndex.removeEntry(old.getEnrollmentYear(), offset);

        dataFile.seek(offset);
        updated.writeTo(dataFile);

        nameIndex.addEntry(encodeName(updated.getName()), offset);
        gpaIndex.addEntry(encodeGpa(updated.getGpa()), offset);
        yearIndex.addEntry(updated.getEnrollmentYear(), offset);
    }

    public void clear() throws IOException {
        dataFile.setLength(0);
        idIndex = new StudentIndex(indexPath);
        nameIndex.clear();
        gpaIndex.clear();
        yearIndex.clear();
    }

    public void close() throws IOException {
        dataFile.close();
        idIndex.saveIndex();
        nameIndex.saveIndex();
        gpaIndex.saveIndex();
        yearIndex.saveIndex();
    }

    public void backup(String backupName) throws IOException {
        Files.copy(Paths.get(dataFilePath), Paths.get(backupName + ".dat"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get(indexPath), Paths.get(backupName + ".idx"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get(dataFilePath.replace(".dat", "_name.idx")), Paths.get(backupName + "_name.idx"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get(dataFilePath.replace(".dat", "_gpa.idx")), Paths.get(backupName + "_gpa.idx"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get(dataFilePath.replace(".dat", "_year.idx")), Paths.get(backupName + "_year.idx"), StandardCopyOption.REPLACE_EXISTING);
    }

    public void restoreFromBackup(String backupName) throws IOException {
        close();
        Files.copy(Paths.get(backupName + ".dat"), Paths.get(dataFilePath), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get(backupName + ".idx"), Paths.get(indexPath), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get(backupName + "_name.idx"), Paths.get(dataFilePath.replace(".dat", "_name.idx")), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get(backupName + "_gpa.idx"), Paths.get(dataFilePath.replace(".dat", "_gpa.idx")), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Paths.get(backupName + "_year.idx"), Paths.get(dataFilePath.replace(".dat", "_year.idx")), StandardCopyOption.REPLACE_EXISTING);

        dataFile = new RandomAccessFile(dataFilePath, "rw");
        idIndex = new StudentIndex(indexPath);
        nameIndex = new ValueIndex(dataFilePath.replace(".dat", "_name.idx"));
        gpaIndex = new ValueIndex(dataFilePath.replace(".dat", "_gpa.idx"));
        yearIndex = new ValueIndex(dataFilePath.replace(".dat", "_year.idx"));
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

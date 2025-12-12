
import java.io.*;

public class Student implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int NAME_LENGTH = 32; // фиксированная длина имени
    public static final int RECORD_SIZE = 4 + NAME_LENGTH + 8 + 4; // int + char[32] + double + int

    private int studentId;
    private String name;
    private double gpa;
    private int enrollmentYear;

    // Конструктор
    public Student(int studentId, String name, double gpa, int enrollmentYear) {
        this.studentId = studentId;
        this.name = padString(name, NAME_LENGTH);
        this.gpa = gpa;
        this.enrollmentYear = enrollmentYear;
    }

    // Getters
    public int getStudentId() { return studentId; }
    public String getName() { return trimString(name); }
    public double getGpa() { return gpa; }
    public int getEnrollmentYear() { return enrollmentYear; }

    // Setters (для редактирования)
    public void setName(String name) { this.name = padString(name, NAME_LENGTH); }
    public void setGpa(double gpa) { this.gpa = gpa; }
    public void setEnrollmentYear(int year) { this.enrollmentYear = year; }

    // Вспомогательные методы
    private static String padString(String str, int length) {
        if (str == null) str = "";
        if (str.length() > length) return str.substring(0, length);
        return String.format("%-" + length + "s", str);
    }

    private static String trimString(String str) {
        return str.trim();
    }

    // Запись в RandomAccessFile
    public void writeTo(RandomAccessFile file) throws IOException {
        file.writeInt(studentId);
        file.writeChars(padString(name, NAME_LENGTH));
        file.writeDouble(gpa);
        file.writeInt(enrollmentYear);
    }

    // Чтение из RandomAccessFile
    public static Student readFrom(RandomAccessFile file) throws IOException {
        int id = file.readInt();
        char[] nameChars = new char[NAME_LENGTH];
        for (int i = 0; i < NAME_LENGTH; i++) {
            nameChars[i] = file.readChar();
        }
        String name = new String(nameChars);
        double gpa = file.readDouble();
        int year = file.readInt();
        return new Student(id, trimString(name), gpa, year);
    }

    @Override
    public String toString() {
        return String.format("ID: %d, Name: %s, GPA: %.2f, Year: %d",
                studentId, getName(), gpa, enrollmentYear);
    }
}

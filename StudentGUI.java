
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;

public class StudentGUI extends JFrame {
    private StudentDatabase db;
    private JTable table;
    private DefaultTableModel tableModel;

    public StudentGUI() {
        setTitle("Student File Database");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        tableModel = new DefaultTableModel(new Object[]{"ID", "Name", "GPA", "Year"}, 0);
        table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel panel = new JPanel(new GridLayout(1, 6));
        JButton openBtn = new JButton("Open DB");
        JButton addBtn = new JButton("Add");
        JButton deleteBtn = new JButton("Delete by ID");
        JButton searchBtn = new JButton("Search");
        JButton editBtn = new JButton("Edit");
        JButton backupBtn = new JButton("Backup");

        panel.add(openBtn);
        panel.add(addBtn);
        panel.add(deleteBtn);
        panel.add(searchBtn);
        panel.add(editBtn);
        panel.add(backupBtn);

        add(panel, BorderLayout.SOUTH);

        openBtn.addActionListener(e -> openDB());
        addBtn.addActionListener(e -> addStudent());
        deleteBtn.addActionListener(e -> deleteStudent());
        searchBtn.addActionListener(e -> searchStudent());
        editBtn.addActionListener(e -> editStudent());
        backupBtn.addActionListener(e -> backupDB());

        setVisible(true);
    }

    private void openDB() {
        String name = JOptionPane.showInputDialog(this, "DB Name:");
        if (name == null || name.trim().isEmpty()) return;
        try {
            if (db != null) db.close();
            db = new StudentDatabase(name.trim());
            refreshTable();
            JOptionPane.showMessageDialog(this, "DB opened!");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        if (db == null) return;
        try {
            List<Student> students = db.getAllStudents();
            for (Student s : students) {
                tableModel.addRow(new Object[]{
                        s.getStudentId(),
                        s.getName(),
                        s.getGpa(),
                        s.getEnrollmentYear()
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addStudent() {
        try {
            int id = Integer.parseInt(JOptionPane.showInputDialog("ID:"));
            String name = JOptionPane.showInputDialog("Name:");
            double gpa = Double.parseDouble(JOptionPane.showInputDialog("GPA:"));
            int year = Integer.parseInt(JOptionPane.showInputDialog("Year:"));
            if (db.addStudent(new Student(id, name, gpa, year))) {
                refreshTable();
                JOptionPane.showMessageDialog(this, "Added!");
            } else {
                JOptionPane.showMessageDialog(this, "ID already exists!");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid input");
        }
    }

    private void deleteStudent() {
        try {
            int id = Integer.parseInt(JOptionPane.showInputDialog("ID to delete:"));
            if (db.deleteStudentById(id)) {
                refreshTable();
                JOptionPane.showMessageDialog(this, "Deleted!");
            } else {
                JOptionPane.showMessageDialog(this, "Not found!");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error");
        }
    }

    private void searchStudent() {
        String field = JOptionPane.showInputDialog("Field (name/gpa/year):");
        if (field == null) return;
        try {
            Object value;
            if ("gpa".equals(field)) {
                value = Double.parseDouble(JOptionPane.showInputDialog("Value:"));
            } else if ("year".equals(field)) {
                value = Integer.parseInt(JOptionPane.showInputDialog("Value:"));
            } else {
                value = JOptionPane.showInputDialog("Value:");
            }
            List<Student> results = db.findStudentsByField(field, value);
            showSearchResults(results);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Search error");
        }
    }



    private void showSearchResults(List<Student> results) {
        StringBuilder sb = new StringBuilder("Found:\n");
        for (Student s : results) {
            sb.append(s).append("\n");
        }
        JOptionPane.showMessageDialog(this, sb.toString());
    }

    private void editStudent() {
        try {
            int id = Integer.parseInt(JOptionPane.showInputDialog("ID to edit:"));
            Student s = db.findStudentById(id);
            if (s == null) {
                JOptionPane.showMessageDialog(this, "Not found!");
                return;
            }
            String name = JOptionPane.showInputDialog("New name:", s.getName());
            double gpa = Double.parseDouble(JOptionPane.showInputDialog("New GPA:", s.getGpa()));
            int year = Integer.parseInt(JOptionPane.showInputDialog("New Year:", s.getEnrollmentYear()));
            s.setName(name);
            s.setGpa(gpa);
            s.setEnrollmentYear(year);
            db.updateStudent(s);
            refreshTable();
            JOptionPane.showMessageDialog(this, "Updated!");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Edit error");
        }
    }

    private void backupDB() {
        try {
            String name = JOptionPane.showInputDialog("Backup name:");
            if (name != null && !name.isEmpty()) {
                db.backup(name);
                JOptionPane.showMessageDialog(this, "Backup created!");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Backup failed");
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(StudentGUI::new);
    }


}


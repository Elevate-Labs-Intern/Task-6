import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import org.jdesktop.swingx.JXDatePicker;
import java.awt.*;
import java.awt.event.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class TaskManager {

    private static final java.util.List<Task> taskList = new ArrayList<>();
    private static final DefaultTableModel tableModel;
    private static final JTable table;
    private static final ImageIcon deleteIcon;
    private static final ImageIcon addIcon;

    static {
        deleteIcon = new ImageIcon(Objects.requireNonNull(TaskManager.class.getResource("/delete_icon.png")));
        addIcon = new ImageIcon(Objects.requireNonNull(TaskManager.class.getResource("/add_icon.png")));
        // Resize icons to 20x20
        Image deleteImg = deleteIcon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
        deleteIcon.setImage(deleteImg);
        Image addImg = addIcon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
        addIcon.setImage(addImg);
        tableModel = new DefaultTableModel(new String[]{"Title", "Due Date", "Priority", ""}, 0) {
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(25);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TaskManager::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Task Manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLocationRelativeTo(null);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(JLabel.CENTER);
                if (column == 2) {
                    String priority = value.toString();
                    switch (priority) {
                        case "High" -> c.setForeground(Color.RED);
                        case "Medium" -> c.setForeground(new Color(255, 140, 0));
                        case "Low" -> c.setForeground(new Color(0, 128, 0));
                        default -> c.setForeground(Color.BLACK);
                    }
                } else {
                    c.setForeground(Color.BLACK);
                }
                return c;
            }
        };

        JTableHeader header = table.getTableHeader();
        DefaultTableCellRenderer headerRenderer = (DefaultTableCellRenderer) header.getDefaultRenderer();
        headerRenderer.setHorizontalAlignment(JLabel.CENTER);

        for (int i = 0; i < table.getColumnCount() - 1; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JButton button = new JButton(deleteIcon);
                button.setPreferredSize(new Dimension(20, 20));
                button.setBorderPainted(false);
                button.setContentAreaFilled(false);
                return button;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        frame.add(scrollPane, BorderLayout.CENTER);

        JButton addButton = new JButton(addIcon);
        addButton.setToolTipText("Add Task");
        addButton.setPreferredSize(new Dimension(30, 30));
        addButton.addActionListener(e -> addTask());

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.add(addButton);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 3) {
                    int confirm = JOptionPane.showConfirmDialog(null, "Delete this task?", "Confirm", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        taskList.remove(row);
                        tableModel.removeRow(row);
                    }
                } else if (row >= 0) {
                    Task oldTask = taskList.get(row);
                    Task updatedTask = showTaskDialog(oldTask);
                    if (updatedTask != null) {
                        if (updatedTask.equals(oldTask)) {
                            JOptionPane.showMessageDialog(null, "Task details not changed.");
                        } else if (!taskList.contains(updatedTask)) {
                            taskList.set(row, updatedTask);
                            refreshTable();
                        } else {
                            JOptionPane.showMessageDialog(null, "Duplicate task. Update aborted.");
                        }
                    }
                }
            }
        });

        JMenuBar menuBar = new JMenuBar();
        JMenu sortMenu = new JMenu("\u2728 Sort \u2728");
        sortMenu.setForeground(new Color(0, 102, 204));
        sortMenu.setFont(new Font("Arial", Font.BOLD, 13));

        JMenuItem sortByDate = new JMenuItem("By Due Date");
        JMenuItem sortByPriority = new JMenuItem("By Priority");

        sortByDate.addActionListener(e -> {
            sortByDueDate();
            refreshTable();
        });

        sortByPriority.addActionListener(e -> {
            taskList.sort(Comparator.comparing(task -> switch (task.priority) {
                case "High" -> 1;
                case "Medium" -> 2;
                case "Low" -> 3;
                default -> 4;
            }));
            refreshTable();
        });

        sortMenu.add(sortByDate);
        sortMenu.add(sortByPriority);
        menuBar.add(sortMenu);
        frame.setJMenuBar(menuBar);

        JLabel info = new JLabel("Click task to alter task details.");
        info.setHorizontalAlignment(SwingConstants.CENTER);
        info.setFont(new Font("Arial", Font.ITALIC, 12));
        frame.add(info, BorderLayout.NORTH);

        sortByDueDate();
        refreshTable();

        frame.setVisible(true);
    }

    private static void sortByDueDate() {
        taskList.sort(Comparator.comparing(task -> {
            try {
                return new SimpleDateFormat("yyyy-MM-dd").parse(task.dueDate);
            } catch (ParseException ex) {
                return new Date();
            }
        }));
    }

    private static void addTask() {
        Task newTask = showTaskDialog(null);
        if (newTask != null && !taskList.contains(newTask)) {
            taskList.add(newTask);
            sortByDueDate();
            refreshTable();
        } else if (newTask != null) {
            JOptionPane.showMessageDialog(null, "Duplicate task. Cannot add.");
        }
    }

    private static Task showTaskDialog(Task task) {
        JTextField titleField = new JTextField(task != null ? task.title : "");

        JXDatePicker datePicker = new JXDatePicker();
        datePicker.setFormats("yyyy-MM-dd");
        if (task != null) {
            try {
                Date date = new SimpleDateFormat("yyyy-MM-dd").parse(task.dueDate);
                datePicker.setDate(date);
            } catch (Exception ignored) {}
        }

        String[] priorities = {"Low", "Medium", "High"};
        JComboBox<String> priorityBox = new JComboBox<>(priorities);
        if (task != null) priorityBox.setSelectedItem(task.priority);

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Title:"));
        panel.add(titleField);
        panel.add(new JLabel("Due Date:"));
        panel.add(datePicker);
        panel.add(new JLabel("Priority:"));
        panel.add(priorityBox);

        int result = JOptionPane.showConfirmDialog(null, panel, task == null ? "Add Task" : "Update Task",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(datePicker.getDate());
            return new Task(titleField.getText().trim(), dateStr, priorityBox.getSelectedItem().toString());
        }
        return null;
    }

    private static void refreshTable() {
        tableModel.setRowCount(0);
        for (Task task : taskList) {
            tableModel.addRow(new Object[]{task.title, task.dueDate, task.priority, deleteIcon});
        }
    }

    static class Task {
        String title;
        String dueDate;
        String priority;

        Task(String title, String dueDate, String priority) {
            this.title = title;
            this.dueDate = dueDate;
            this.priority = priority;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Task task = (Task) o;
            return Objects.equals(title, task.title) &&
                    Objects.equals(dueDate, task.dueDate) &&
                    Objects.equals(priority, task.priority);
        }

        @Override
        public int hashCode() {
            return Objects.hash(title, dueDate, priority);
        }
    }
}
package FoodFinder.ui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.*;
import java.util.List;

public class PagedSearchPanel extends JPanel {
    private List<String> originalOptions;
    private List<String> filteredOptions;
    private Map<String, Boolean> selectionMap;
    private int currentPage = 0;
    private final int itemsPerPage = 12;

    private JTextField searchField;
    private JPanel gridPanel;
    private JButton leftButton, rightButton;

    public PagedSearchPanel(String[] options) {
        originalOptions = new ArrayList<>(Arrays.asList(options));
        filteredOptions = new ArrayList<>(originalOptions);
        selectionMap = new HashMap<>();
        for (String opt : originalOptions) {
            selectionMap.put(opt, false);
        }
        setLayout(new BorderLayout(10, 10));

        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateFilter(); }
            public void removeUpdate(DocumentEvent e) { updateFilter(); }
            public void changedUpdate(DocumentEvent e) { updateFilter(); }
        });
        add(searchField, BorderLayout.NORTH);

        gridPanel = new JPanel(new GridLayout(3, 4, 15, 15));
        add(gridPanel, BorderLayout.CENTER);

        JPanel arrowPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        leftButton = new JButton("<");
        rightButton = new JButton(">");
        leftButton.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        rightButton.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        leftButton.addActionListener(e -> {
            if (currentPage > 0) { currentPage--; refreshGrid(); }
        });
        rightButton.addActionListener(e -> {
            if ((currentPage + 1) * itemsPerPage < filteredOptions.size()) { currentPage++; refreshGrid(); }
        });
        arrowPanel.add(leftButton);
        arrowPanel.add(rightButton);
        add(arrowPanel, BorderLayout.SOUTH);

        refreshGrid();
    }

    private void updateFilter() {
        String text = searchField.getText().trim().toLowerCase();
        filteredOptions.clear();
        for (String opt : originalOptions) {
            if (opt.toLowerCase().contains(text)) {
                filteredOptions.add(opt);
            }
        }
        currentPage = 0;
        refreshGrid();
    }

    private void refreshGrid() {
        gridPanel.removeAll();
        int start = currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, filteredOptions.size());
        if (filteredOptions.isEmpty()) {
            gridPanel.setLayout(new BorderLayout());
            gridPanel.add(new JLabel("No results found", SwingConstants.CENTER), BorderLayout.CENTER);
        } else {
            gridPanel.setLayout(new GridLayout(3, 4, 15, 15));
            for (int i = start; i < end; i++) {
                String option = filteredOptions.get(i);
                JToggleButton btn = new JToggleButton(option);
                btn.setFont(new Font("Segoe UI", Font.PLAIN, 24));
                btn.setFocusPainted(false);
                btn.setBackground(Color.WHITE);
                btn.setForeground(Color.DARK_GRAY);
                boolean selected = selectionMap.getOrDefault(option, false);
                btn.setSelected(selected);
                btn.setBackground(selected ? new Color(0xFF6666) : Color.WHITE);
                btn.setForeground(selected ? Color.WHITE : Color.DARK_GRAY);
                btn.addItemListener(e -> {
                    boolean isSelected = btn.isSelected();
                    selectionMap.put(option, isSelected);
                    btn.setBackground(isSelected ? new Color(0xFF6666) : Color.WHITE);
                    btn.setForeground(isSelected ? Color.WHITE : Color.DARK_GRAY);
                });
                gridPanel.add(btn);
            }
            int itemsAdded = end - start;
            for (int i = itemsAdded; i < itemsPerPage; i++) {
                gridPanel.add(new JPanel());
            }
        }
        leftButton.setEnabled(currentPage > 0);
        rightButton.setEnabled((currentPage + 1) * itemsPerPage < filteredOptions.size());
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    /**
     * Updates the available options while preserving the selection state
     * of options that remain in the new list.
     *
     * @param options the new array of options
     */
    public void updateOptions(String[] options) {
        // Create a new list of options.
        List<String> newOptions = new ArrayList<>(Arrays.asList(options));
        // Build a new selection map by retaining the state of previously selected options.
        Map<String, Boolean> newSelectionMap = new HashMap<>();
        for (String opt : newOptions) {
            // If the option existed before, retain its selection; otherwise, default to false.
            newSelectionMap.put(opt, selectionMap.getOrDefault(opt, false));
        }
        originalOptions = newOptions;
        filteredOptions = new ArrayList<>(originalOptions);
        selectionMap = newSelectionMap;
        currentPage = 0;
        refreshGrid();
    }

    public List<String> getSelectedOptions() {
        List<String> selected = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : selectionMap.entrySet()) {
            if (entry.getValue()) {
                selected.add(entry.getKey());
            }
        }
        return selected;
    }

    /**
     * Sets the selection state based on a list of options.
     * Options in selectedOptions will be marked as selected (true)
     * and all other options will be unselected.
     *
     * @param selectedOptions the list of options to be selected
     */
    public void setSelectedOptions(List<String> selectedOptions) {
        for (String opt : originalOptions) {
            if (selectedOptions.contains(opt)) {
                selectionMap.put(opt, true);
            } else {
                selectionMap.put(opt, false);
            }
        }
        refreshGrid();
    }
}

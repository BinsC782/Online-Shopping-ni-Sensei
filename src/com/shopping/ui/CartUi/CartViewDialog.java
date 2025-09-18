package com.shopping.ui.CartUi;

import com.shopping.model.Product;
import com.shopping.data.FileHandler;
import com.shopping.ui.CheckOutUi.CheckoutDialog;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class CartViewDialog extends JDialog {
    private Map<Product, Integer> cartItems;
    private DefaultTableModel tableModel;
    private JTable cartTable;
    private JLabel totalLabel;
    private JButton checkoutButton;
    private JButton closeButton;
    private String username;

    public CartViewDialog(Frame parent, Map<Product, Integer> cartItems, String username) {
        super(parent, "Shopping Cart", true);
        this.cartItems = new HashMap<>(cartItems);
        this.username = username;

        // Set to full screen
        setSize(Toolkit.getDefaultToolkit().getScreenSize());
        setLocation(0, 0);
        setUndecorated(false);

        initializeComponents();
        updateTotal();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(255, 165, 0)); // Orange background

        // Title
        JLabel titleLabel = new JLabel("Your Shopping Cart", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setOpaque(true);
        titleLabel.setBackground(new Color(255, 140, 0));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(titleLabel, BorderLayout.NORTH);

        // Cart table
        String[] columnNames = {"Product", "Price", "Quantity", "Subtotal", "Actions"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2; // Only quantity column is editable
            }
        };

        cartTable = new JTable(tableModel);
        cartTable.setRowHeight(40);
        cartTable.setFont(new Font("Arial", Font.PLAIN, 14));
        cartTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 16));
        cartTable.getTableHeader().setBackground(new Color(255, 140, 0));
        cartTable.getTableHeader().setForeground(Color.WHITE);

        // Custom renderer for actions column
        cartTable.getColumnModel().getColumn(4).setCellRenderer(new ButtonRenderer());
        cartTable.getColumnModel().getColumn(4).setCellEditor(new ButtonEditor(new JCheckBox()));

        JScrollPane scrollPane = new JScrollPane(cartTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(scrollPane, BorderLayout.CENTER);

        // Add listener for quantity changes
        tableModel.addTableModelListener(e -> {
            int row = e.getFirstRow();
            int column = e.getColumn();
            if (column == 2) { // quantity column
                try {
                    int newQuantity = Integer.parseInt(tableModel.getValueAt(row, 2).toString());
                    if (newQuantity > 0) {
                        Product product = (Product) cartItems.keySet().toArray()[row];
                        cartItems.put(product, newQuantity);
                        // Update subtotal
                        double subtotal = product.getPrice() * newQuantity;
                        tableModel.setValueAt("$" + String.format("%.2f", subtotal), row, 3);
                        updateTotal();
                    } else {
                        // Invalid quantity, revert to 1
                        tableModel.setValueAt(1, row, 2);
                        Product product = (Product) cartItems.keySet().toArray()[row];
                        cartItems.put(product, 1);
                        double subtotal = product.getPrice() * 1;
                        tableModel.setValueAt("$" + String.format("%.2f", subtotal), row, 3);
                        updateTotal();
                    }
                } catch (NumberFormatException ex) {
                    // Invalid input, revert to previous value
                    Product product = (Product) cartItems.keySet().toArray()[row];
                    int currentQty = cartItems.get(product);
                    tableModel.setValueAt(currentQty, row, 2);
                }
            }
        });

        // Bottom panel
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(new Color(255, 165, 0));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        totalLabel = new JLabel("Total: $0.00", SwingConstants.CENTER);
        totalLabel.setFont(new Font("Arial", Font.BOLD, 20));
        totalLabel.setForeground(Color.WHITE);
        totalLabel.setOpaque(true);
        totalLabel.setBackground(new Color(255, 140, 0));
        totalLabel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(new Color(255, 165, 0));

        checkoutButton = createStyledButton("Checkout", new Color(34, 139, 34));
        checkoutButton.addActionListener(new CheckoutAction());

        closeButton = createStyledButton("Continue Shopping", new Color(70, 130, 180));
        closeButton.addActionListener(e -> setVisible(false));

        buttonPanel.add(checkoutButton);
        buttonPanel.add(closeButton);

        bottomPanel.add(totalLabel, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);

        populateTable();
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setFocusPainted(false);
        return button;
    }

    private void populateTable() {
        tableModel.setRowCount(0);
        for (Map.Entry<Product, Integer> entry : cartItems.entrySet()) {
            Product product = entry.getKey();
            int quantity = entry.getValue();
            double subtotal = product.getPrice() * quantity;

            Object[] row = {
                product.getName(),
                "$" + String.format("%.2f", product.getPrice()),
                quantity,
                "$" + String.format("%.2f", subtotal),
                "Remove"
            };
            tableModel.addRow(row);
        }
    }

    private void updateTotal() {
        double total = 0.0;
        for (Map.Entry<Product, Integer> entry : cartItems.entrySet()) {
            total += entry.getKey().getPrice() * entry.getValue();
        }
        totalLabel.setText("Total: $" + String.format("%.2f", total));
    }

    // Custom button renderer for table
    class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            setBackground(Color.RED);
            setForeground(Color.WHITE);
            return this;
        }
    }

    // Custom button editor for table
    class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private String label;
        private boolean isPushed;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    fireEditingStopped();
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            button.setBackground(Color.RED);
            button.setForeground(Color.WHITE);
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                // Get the product from the current row
                int row = cartTable.getSelectedRow();
                if (row >= 0 && row < cartItems.size()) {
                    Product productToRemove = (Product) cartItems.keySet().toArray()[row];
                    cartItems.remove(productToRemove);
                    populateTable();
                    updateTotal();
                }
            }
            isPushed = false;
            return label;
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }

    private class CheckoutAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (cartItems.isEmpty()) {
                JOptionPane.showMessageDialog(CartViewDialog.this, "Your cart is empty!");
                return;
            }

            // Create checkout dialog
            CheckoutDialog checkoutDialog = new CheckoutDialog((JFrame) SwingUtilities.getWindowAncestor(CartViewDialog.this), cartItems, username);
            checkoutDialog.setVisible(true);

            if (checkoutDialog.isOrderPlaced()) {
                // Clear cart and close
                cartItems.clear();
                populateTable();
                updateTotal();
                JOptionPane.showMessageDialog(CartViewDialog.this, "Order placed successfully!");
                setVisible(false);
            }
        }
    }

    public Map<Product, Integer> getUpdatedCartItems() {
        return new HashMap<>(cartItems);
    }
}
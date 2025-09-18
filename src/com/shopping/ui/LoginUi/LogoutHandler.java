package com.shopping.ui.LoginUi;

import com.shopping.model.User;
import com.shopping.ui.MainProgram.MainFrame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class LogoutHandler implements ActionListener {
    private JFrame parentFrame;

    public LogoutHandler(JFrame parentFrame) {
        this.parentFrame = parentFrame;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        logout();
    }

    private void logout() {
        int option = JOptionPane.showConfirmDialog(parentFrame,
            "Are you sure you want to logout?",
            "Logout Confirmation",
            JOptionPane.YES_NO_OPTION);

        if (option == JOptionPane.YES_OPTION) {
            parentFrame.dispose();

            // Restart login process
            JFrame tempFrame = new JFrame();
            LoginDialog loginDialog = new LoginDialog(tempFrame);
            User user = loginDialog.showDialog();
            tempFrame.dispose();

            if (user != null) {
                new MainFrame(user);
            } else {
                System.exit(0);
            }
        }
    }
}
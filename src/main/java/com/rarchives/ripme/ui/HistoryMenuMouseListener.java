package com.rarchives.ripme.ui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class HistoryMenuMouseListener extends MouseAdapter {
    private JPopupMenu popup = new JPopupMenu();
    private JTable tableComponent;

    @SuppressWarnings("serial")
    public HistoryMenuMouseListener() {
        Action checkAllAction = new AbstractAction("Check All") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                for (int row = 0; row < tableComponent.getRowCount(); row++)
                    tableComponent.setValueAt(Boolean.TRUE, row, 4);
            }
        };
        popup.add(checkAllAction);

        Action uncheckAllAction = new AbstractAction("Check None") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                for (int row = 0; row < tableComponent.getRowCount(); row++)
                    tableComponent.setValueAt(Boolean.FALSE, row, 4);
            }
        };
        popup.add(uncheckAllAction);

        popup.addSeparator();

        Action checkSelected = new AbstractAction("Check Selected") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                for (int row : tableComponent.getSelectedRows())
                    tableComponent.setValueAt(Boolean.TRUE, row, 4);
            }
        };
        popup.add(checkSelected);

        Action uncheckSelected = new AbstractAction("Uncheck Selected") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                for (int row : tableComponent.getSelectedRows())
                    tableComponent.setValueAt(Boolean.FALSE, row, 4);
            }
        };
        popup.add(uncheckSelected);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getModifiers() == InputEvent.BUTTON3_MASK) {
            if (!(e.getSource() instanceof JTable))
                return;

            tableComponent = (JTable) e.getSource();
            tableComponent.requestFocus();

            int nx = e.getX();

            if (nx > 500)
                nx = nx - popup.getSize().width;

            popup.show(e.getComponent(), nx, e.getY() - popup.getSize().height);
        }
    }
}
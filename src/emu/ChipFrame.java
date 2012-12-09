package emu;

import chip.Chip;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class ChipFrame extends JFrame implements KeyListener {

    private ChipPanel panel;
    private int[] keyBuffer;
    private int[] keyIdtoKey;

    public ChipFrame(Chip c) {
        setPreferredSize(new Dimension(640, 320));
        pack();
        setPreferredSize(new Dimension(
                640 + getInsets().left + getInsets().right,
                320 + getInsets().top  + getInsets().bottom));
        panel = new ChipPanel(c);
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle("CHIP-8 Emulator");
        pack();
        setVisible(true);
        addKeyListener(this);

        keyIdtoKey = new int[256];
        keyBuffer = new int[16];
        fillKeyIds();
    }

    private void fillKeyIds() {
        for (int i = 0; i < keyIdtoKey.length; i++) {
            keyIdtoKey[i] = -1;
        }
        keyIdtoKey['1'] = 0x1;
        keyIdtoKey['2'] = 0x2;
        keyIdtoKey['3'] = 0x3;
        keyIdtoKey['Q'] = 0x4;
        keyIdtoKey['W'] = 0x5;
        keyIdtoKey['E'] = 0x6;
        keyIdtoKey['A'] = 0x7;
        keyIdtoKey['S'] = 0x8;
        keyIdtoKey['D'] = 0x9;
        keyIdtoKey['Z'] = 0xA;
        keyIdtoKey['X'] = 0x0;
        keyIdtoKey['C'] = 0xB;
        keyIdtoKey['4'] = 0xC;
        keyIdtoKey['R'] = 0xD;
        keyIdtoKey['F'] = 0xE;
        keyIdtoKey['V'] = 0xF;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if(keyIdtoKey[e.getKeyCode()] != -1) {
            keyBuffer[keyIdtoKey[e.getKeyCode()]] = 1;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if(keyIdtoKey[e.getKeyCode()] != -1) {
            keyBuffer[keyIdtoKey[e.getKeyCode()]] = 0;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    public int[] getKeyBuffer() {
        return keyBuffer;
    }

}

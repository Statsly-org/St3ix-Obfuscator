package st3ix.obfuscator.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Toast-style notification that fades in, stays briefly, then fades out.
 */
public final class ToastNotification {

    private static final int TOAST_DURATION_MS = 3500;
    private static final Color BG_SUCCESS = new Color(0x2E7D32);
    private static final Color BG_ERROR = new Color(0xC62828);
    private static final Color BG_WARN = new Color(0xF9A825);
    private static final Color BG_INFO = new Color(0x1565C0);

    public enum Type { SUCCESS, ERROR, WARN, INFO }

    public static void show(Component parent, String message, Type type) {
        JPanel toast = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        toast.setOpaque(true);
        toast.setBackground(switch (type) {
            case SUCCESS -> BG_SUCCESS;
            case ERROR -> BG_ERROR;
            case WARN -> BG_WARN;
            case INFO -> BG_INFO;
        });
        JLabel label = new JLabel(message);
        label.setForeground(Color.WHITE);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 13f));
        toast.add(label);
        toast.setBorder(BorderFactory.createEmptyBorder(4, 16, 4, 16));
        toast.setMaximumSize(new Dimension(500, 60));

        if (parent instanceof JFrame frame) {
            showOnFrame(frame, toast);
        } else if (parent != null) {
            for (Container c = parent.getParent(); c != null; c = c.getParent()) {
                if (c instanceof JFrame f) {
                    showOnFrame(f, toast);
                    return;
                }
            }
        }
    }

    private static void showOnFrame(JFrame frame, JPanel toast) {
        JLayeredPane layered = frame.getRootPane().getLayeredPane();
        Dimension ps = toast.getPreferredSize();
        int w = Math.max(frame.getWidth(), 400);
        int h = Math.max(frame.getHeight(), 300);
        toast.setBounds((w - ps.width) / 2, h - 90, ps.width, ps.height);
        layered.add(toast, Integer.valueOf(JLayeredPane.POPUP_LAYER));
        frame.repaint();

        Timer timer = new Timer(TOAST_DURATION_MS, e -> {
            layered.remove(toast);
            frame.repaint();
        });
        timer.setRepeats(false);
        timer.start();
    }
}

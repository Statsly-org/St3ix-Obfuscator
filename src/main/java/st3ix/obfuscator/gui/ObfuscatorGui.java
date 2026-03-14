package st3ix.obfuscator.gui;

import st3ix.obfuscator.config.ConfigLoader;
import st3ix.obfuscator.config.ObfuscatorConfig;
import st3ix.obfuscator.core.ObfuscationPipeline;
import st3ix.obfuscator.log.Logger;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;
import java.awt.*;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GUI for the St3ix Obfuscator. Step-based sidebar with Input, Obfuscation, Advanced, Run.
 */
public final class ObfuscatorGui {

    private static final String OBFUSCATE_FOLDER = "Obfuscate";

    private static final Color BG_MAIN = new Color(0xF0F2F5);
    private static final Color BG_PANEL = new Color(0xFAFBFC);
    private static final Color BORDER = new Color(0xD1D5DB);
    private static final Color ACCENT = new Color(0x238636);
    private static final Color SIDEBAR_BG = new Color(0x2D333B);
    private static final Color SIDEBAR_TEXT = new Color(0xB1BAC4);
    private static final Color SIDEBAR_ACTIVE_BG = new Color(0x21262D);
    private static final Color SIDEBAR_HOVER = new Color(0x58A6FF);

    private static final String EXCLUDE_INFO = "Use \"*\" to exclude all classes. Prefix: com.example excludes com.example.*. java.*, javax.*, etc. are always excluded.";

    private static final String STEP_INPUT = "input";
    private static final String STEP_OBFUSCATION = "obfuscation";
    private static final String STEP_ADVANCED = "advanced";
    private static final String STEP_RUN = "run";

    public static void show() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("St3ix Obfuscator");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setMinimumSize(new Dimension(720, 700));
            frame.setSize(820, 750);
            frame.setLocationRelativeTo(null);
            frame.getContentPane().setBackground(BG_MAIN);

            Image icon = loadLogo();
            if (icon != null) {
                frame.setIconImage(icon);
            }
            final Image sidebarIcon = icon;

            // Shared form fields (accessible across steps)
            JTextField inputPath = new JTextField(22);
            inputPath.setEditable(false);
            inputPath.setMargin(new Insets(4, 6, 4, 6));
            JTextField outputName = new JTextField(18);
            outputName.setMargin(new Insets(4, 6, 4, 6));
            JTextField outputDir = new JTextField(32);
            outputDir.setEditable(true);
            outputDir.setMargin(new Insets(4, 6, 4, 6));
            Path defaultOutputDir = ConfigLoader.getJarDirectory().resolve(OBFUSCATE_FOLDER);
            outputDir.setText(defaultOutputDir.toAbsolutePath().toString());

            JCheckBox classRename = new JCheckBox("Class renaming", true);
            JCheckBox methodRename = new JCheckBox("Method renaming", true);
            JCheckBox fieldRename = new JCheckBox("Field renaming", true);
            JCheckBox numberObf = new JCheckBox("Number obfuscation", true);
            JCheckBox arrayObf = new JCheckBox("Array obfuscation", true);
            JCheckBox booleanObf = new JCheckBox("Boolean obfuscation", true);
            JCheckBox stringObf = new JCheckBox("String obfuscation", true);
            JCheckBox flowObf = new JCheckBox("Flow obfuscation", false);
            JCheckBox debugInfoStrip = new JCheckBox("Strip debug info", true);
            JCheckBox classNamesRandom = new JCheckBox("Random class names", false);
            JCheckBox classNamesHomoglyph = new JCheckBox("Homoglyph class names", false);
            JCheckBox classNamesInvisibleChars = new JCheckBox("Invisible chars in names", false);
            JCheckBox numberKeyRandom = new JCheckBox("Random number key", false);
            JCheckBox arrayKeyRandom = new JCheckBox("Random array key", false);
            JCheckBox booleanKeyRandom = new JCheckBox("Random boolean key", false);
            JCheckBox stringKeyRandom = new JCheckBox("Random string key", false);
            JCheckBox flowKeyRandom = new JCheckBox("Random flow key", false);
            JSpinner classNameLength = new JSpinner(new SpinnerNumberModel(6, 1, 32, 1));
            classNameLength.setPreferredSize(new Dimension(60, 24));
            JSpinner methodNameLength = new JSpinner(new SpinnerNumberModel(4, 1, 32, 1));
            methodNameLength.setPreferredSize(new Dimension(60, 24));
            JCheckBox methodNamesRandom = new JCheckBox("Random method names", false);
            JCheckBox methodNamesHomoglyph = new JCheckBox("Homoglyph method names", false);
            JCheckBox methodNamesInvisibleChars = new JCheckBox("Invisible chars in method names", false);
            JCheckBox fieldNamesRandom = new JCheckBox("Random field names", false);
            JCheckBox fieldNamesHomoglyph = new JCheckBox("Homoglyph field names", false);
            JCheckBox fieldNamesInvisibleChars = new JCheckBox("Invisible chars in field names", false);
            JSpinner fieldNameLength = new JSpinner(new SpinnerNumberModel(4, 1, 32, 1));
            fieldNameLength.setPreferredSize(new Dimension(60, 24));
            JTextArea excludeArea = new JTextArea(4, 28);
            excludeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            excludeArea.setLineWrap(false);
            excludeArea.setMargin(new Insets(4, 6, 4, 6));
            excludeArea.setPreferredSize(new Dimension(300, 70));

            JTextPane outputPane = new JTextPane();
            outputPane.setEditable(false);
            outputPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            outputPane.setBackground(new Color(0x2D2D30));
            outputPane.setForeground(new Color(0xD4D4D4));
            outputPane.setCaretColor(new Color(0xD4D4D4));
            outputPane.setMargin(new Insets(8, 10, 8, 10));
            outputPane.setPreferredSize(new Dimension(0, 160));

            // Sidebar
            JPanel sidebar = new JPanel(new BorderLayout(0, 0));
            sidebar.setPreferredSize(new Dimension(210, 0));
            sidebar.setBackground(SIDEBAR_BG);
            sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0x30363D)));

            JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 8));
            headerPanel.setBackground(SIDEBAR_BG);
            headerPanel.setBorder(new EmptyBorder(12, 12, 8, 12));
            JLabel titleLabel = new JLabel("St3ix Obfuscator");
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));
            titleLabel.setForeground(SIDEBAR_TEXT);
            if (sidebarIcon != null) {
                Image scaled = sidebarIcon.getScaledInstance(24, 24, Image.SCALE_SMOOTH);
                headerPanel.add(new JLabel(new ImageIcon(scaled)));
            }
            headerPanel.add(titleLabel);
            sidebar.add(headerPanel, BorderLayout.NORTH);

            JPanel stepsPanel = new JPanel(new GridLayout(4, 1, 0, 0));
            stepsPanel.setBackground(SIDEBAR_BG);
            stepsPanel.setBorder(new EmptyBorder(4, 0, 12, 0));

            JLabel step1Label = new JLabel("  1. Input & Output");
            JLabel step2Label = new JLabel("  2. Obfuscation");
            JLabel step3Label = new JLabel("  3. Advanced");
            JLabel step4Label = new JLabel("  4. Run");
            for (JLabel l : List.of(step1Label, step2Label, step3Label, step4Label)) {
                l.setFont(l.getFont().deriveFont(Font.PLAIN, 12f));
                l.setForeground(SIDEBAR_TEXT);
                l.setBorder(new EmptyBorder(8, 12, 8, 12));
                l.setBackground(SIDEBAR_BG);
                l.setOpaque(true);
                l.setAlignmentY(Component.CENTER_ALIGNMENT);
                stepsPanel.add(l);
            }
            sidebar.add(stepsPanel, BorderLayout.CENTER);

            // Content with CardLayout
            JPanel content = new JPanel(new CardLayout(8, 8));
            content.setBackground(BG_MAIN);

            // Step 1: Input & Output
            JPanel step1 = buildStep1Panel(frame, inputPath, outputName, outputDir);
            content.add(step1, STEP_INPUT);

            // Step 2: Obfuscation
            JPanel step2 = buildStep2Panel(classRename, methodRename, fieldRename, numberObf, arrayObf, booleanObf, stringObf, flowObf, debugInfoStrip, classNameLength, methodNameLength, fieldNameLength);
            content.add(step2, STEP_OBFUSCATION);

            // Step 3: Advanced
            JPanel step3 = buildStep3Panel(classNamesRandom, classNamesHomoglyph, classNamesInvisibleChars,
                methodNamesRandom, methodNamesHomoglyph, methodNamesInvisibleChars,
                fieldNamesRandom, fieldNamesHomoglyph, fieldNamesInvisibleChars,
                numberKeyRandom, arrayKeyRandom, booleanKeyRandom, stringKeyRandom, flowKeyRandom, excludeArea,
                frame, classRename, methodRename, fieldRename, numberObf, arrayObf, booleanObf, stringObf, flowObf, debugInfoStrip, classNameLength, methodNameLength, fieldNameLength);
            content.add(step3, STEP_ADVANCED);

            // Step 4: Run
            JPanel step4 = buildStep4Panel(inputPath, outputName, outputDir, excludeArea,
                classRename, methodRename, fieldRename, numberObf, arrayObf, booleanObf, stringObf, flowObf, debugInfoStrip,
                classNamesRandom, classNamesHomoglyph, classNamesInvisibleChars,
                methodNamesRandom, methodNameLength, methodNamesHomoglyph, methodNamesInvisibleChars,
                fieldNamesRandom, fieldNameLength, fieldNamesHomoglyph, fieldNamesInvisibleChars,
                numberKeyRandom, arrayKeyRandom, booleanKeyRandom, stringKeyRandom, flowKeyRandom, classNameLength,
                frame, outputPane);
            content.add(step4, STEP_RUN);

            CardLayout cards = (CardLayout) content.getLayout();

            JLabel[] stepLabels = {step1Label, step2Label, step3Label, step4Label};
            String[] stepKeys = {STEP_INPUT, STEP_OBFUSCATION, STEP_ADVANCED, STEP_RUN};

            Runnable updateHighlight = () -> {
                String active = getCurrentStep(content);
                for (int i = 0; i < stepLabels.length; i++) {
                    JLabel l = stepLabels[i];
                    boolean isActive = stepKeys[i].equals(active);
                    l.setForeground(isActive ? Color.WHITE : SIDEBAR_TEXT);
                    l.setBackground(isActive ? SIDEBAR_ACTIVE_BG : SIDEBAR_BG);
                    l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, isActive ? 4 : 0, 0, 0, ACCENT),
                        new EmptyBorder(8, isActive ? 8 : 12, 8, 12)));
                }
            };

            for (int i = 0; i < stepLabels.length; i++) {
                JLabel lbl = stepLabels[i];
                String key = stepKeys[i];
                lbl.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        cards.show(content, key);
                        updateHighlight.run();
                    }
                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        if (!key.equals(getCurrentStep(content))) {
                            lbl.setForeground(SIDEBAR_HOVER);
                        }
                    }
                    @Override
                    public void mouseExited(java.awt.event.MouseEvent e) {
                        updateHighlight.run();
                    }
                });
                lbl.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            // Navigation buttons (shown in step content)
            JButton prevBtn = new JButton("Previous");
            JButton nextBtn = new JButton("Next");
            prevBtn.setFocusPainted(false);
            nextBtn.setFocusPainted(false);
            prevBtn.addActionListener(e -> {
                String current = getCurrentStep(content);
                if (STEP_OBFUSCATION.equals(current)) { cards.show(content, STEP_INPUT); }
                else if (STEP_ADVANCED.equals(current)) { cards.show(content, STEP_OBFUSCATION); }
                else if (STEP_RUN.equals(current)) { cards.show(content, STEP_ADVANCED); }
                updateHighlight.run();
            });
            nextBtn.addActionListener(e -> {
                String current = getCurrentStep(content);
                if (STEP_INPUT.equals(current)) { cards.show(content, STEP_OBFUSCATION); }
                else if (STEP_OBFUSCATION.equals(current)) { cards.show(content, STEP_ADVANCED); }
                else if (STEP_ADVANCED.equals(current)) { cards.show(content, STEP_RUN); }
                updateHighlight.run();
            });

            updateHighlight.run();

            addNavToStep(step1, null, nextBtn);
            addNavToStep(step2, prevBtn, nextBtn);
            addNavToStep(step3, prevBtn, nextBtn);
            addNavToStep(step4, prevBtn, null);

            // Main layout
            JPanel main = new JPanel(new BorderLayout(0, 0));
            main.setBackground(BG_MAIN);
            main.add(sidebar, BorderLayout.WEST);

            JPanel centerWrap = new JPanel(new BorderLayout(8, 8));
            centerWrap.setBorder(new EmptyBorder(16, 20, 16, 20));
            centerWrap.setBackground(BG_MAIN);
            centerWrap.add(content, BorderLayout.CENTER);
            main.add(centerWrap, BorderLayout.CENTER);

            frame.setContentPane(main);
            frame.setVisible(true);
        });
    }

    private static String getCurrentStep(JPanel content) {
        for (Component c : content.getComponents()) {
            if (c.isVisible()) {
                String name = c.getName();
                return name != null ? name : STEP_INPUT;
            }
        }
        return STEP_INPUT;
    }

    private static void addNavToStep(JPanel step, JButton prev, JButton next) {
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        nav.setBackground(BG_MAIN);
        if (prev != null) nav.add(prev);
        if (next != null) nav.add(next);
        step.add(nav, BorderLayout.SOUTH);
    }

    private static JPanel buildStep1Panel(JFrame frame, JTextField inputPath, JTextField outputName, JTextField outputDir) {
        JPanel step = new JPanel(new BorderLayout(8, 8));
        step.setBackground(BG_MAIN);
        step.setName(STEP_INPUT);

        JPanel inner = new JPanel(new BorderLayout(8, 8));
        inner.setBackground(BG_PANEL);
        inner.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(new LineBorder(BORDER, 1), "Step 1: Input & Output",
                0, 0, null, new Color(0x24292F)),
            new EmptyBorder(12, 16, 16, 16)));

        JButton browseInputBtn = new JButton("Browse...");
        browseInputBtn.setFocusPainted(false);
        browseInputBtn.setMaximumSize(new Dimension(85, 28));
        browseInputBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("JAR files (*.jar)", "jar"));
            if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                inputPath.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        JButton browseOutputBtn = new JButton("Browse...");
        browseOutputBtn.setFocusPainted(false);
        browseOutputBtn.setMaximumSize(new Dimension(85, 28));
        browseOutputBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            Path current = Path.of(outputDir.getText().trim());
            if (Files.isDirectory(current)) fc.setCurrentDirectory(current.toFile());
            if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                outputDir.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });

        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        filePanel.setBackground(BG_PANEL);
        filePanel.add(new JLabel("Input JAR:"));
        filePanel.add(inputPath);
        filePanel.add(browseInputBtn);
        inner.add(filePanel, BorderLayout.NORTH);

        JPanel outputPanel = new JPanel(new GridLayout(2, 1, 6, 6));
        outputPanel.setBackground(BG_PANEL);
        JPanel outputNameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        outputNameRow.setBackground(BG_PANEL);
        outputNameRow.add(new JLabel("Output name:"));
        outputNameRow.add(outputName);
        JPanel outputDirRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        outputDirRow.setBackground(BG_PANEL);
        outputDirRow.add(new JLabel("Output folder:"));
        outputDirRow.add(outputDir);
        outputDirRow.add(browseOutputBtn);
        outputPanel.add(outputNameRow);
        outputPanel.add(outputDirRow);
        inner.add(outputPanel, BorderLayout.CENTER);

        JLabel hint = new JLabel("<html>Select the JAR file to obfuscate and choose output location.</html>");
        hint.setFont(hint.getFont().deriveFont(10f));
        hint.setForeground(new Color(0x666666));
        hint.setBorder(new EmptyBorder(8, 0, 0, 0));
        inner.add(hint, BorderLayout.SOUTH);

        step.add(inner, BorderLayout.CENTER);
        return step;
    }

    private static JPanel buildStep2Panel(JCheckBox classRename, JCheckBox methodRename, JCheckBox fieldRename, JCheckBox numberObf, JCheckBox arrayObf,
            JCheckBox booleanObf, JCheckBox stringObf, JCheckBox flowObf, JCheckBox debugInfoStrip, JSpinner classNameLength, JSpinner methodNameLength, JSpinner fieldNameLength) {
        JPanel step = new JPanel(new BorderLayout(8, 8));
        step.setBackground(BG_MAIN);
        step.setName(STEP_OBFUSCATION);

        JPanel inner = new JPanel(new BorderLayout(8, 8));
        inner.setBackground(BG_PANEL);
        inner.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(new LineBorder(BORDER, 1), "Step 2: Obfuscation Options",
                0, 0, null, new Color(0x24292F)),
            new EmptyBorder(12, 16, 16, 16)));

        JPanel grid = new JPanel() {
            private static final int MIN_COL_WIDTH = 180;
            @Override
            public void doLayout() {
                int w = getWidth();
                int cols = w <= 0 ? 1 : Math.min(3, Math.max(1, w / MIN_COL_WIDTH));
                if (getLayout() == null || !(getLayout() instanceof GridLayout) || ((GridLayout) getLayout()).getColumns() != cols) {
                    setLayout(new GridLayout(0, cols, 12, 6));
                }
                super.doLayout();
            }
        };
        grid.setBackground(BG_PANEL);
        grid.add(classRename);
        grid.add(methodRename);
        grid.add(fieldRename);
        grid.add(numberObf);
        grid.add(arrayObf);
        grid.add(booleanObf);
        grid.add(stringObf);
        grid.add(flowObf);
        grid.add(debugInfoStrip);
        inner.add(grid, BorderLayout.CENTER);

        JLabel hint = new JLabel("<html>Enable or disable obfuscation types. Name length and other options are in Advanced.</html>");
        hint.setFont(hint.getFont().deriveFont(10f));
        hint.setForeground(new Color(0x666666));
        hint.setBorder(new EmptyBorder(8, 0, 0, 0));
        inner.add(hint, BorderLayout.SOUTH);

        step.add(inner, BorderLayout.CENTER);
        return step;
    }

    /** Creates an expandable section with header (clickable) and content panel. */
    private static JPanel createExpandableSection(String title, JPanel contentPanel, boolean expandedByDefault) {
        JPanel section = new JPanel(new BorderLayout(0, 0));
        section.setBackground(BG_PANEL);

        JLabel arrowLabel = new JLabel(expandedByDefault ? "▼ " : "▶ ");
        arrowLabel.setFont(arrowLabel.getFont().deriveFont(Font.BOLD, 11f));
        arrowLabel.setForeground(new Color(0x24292F));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 6));
        header.setBackground(BG_PANEL);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
            new EmptyBorder(6, 8, 6, 8)));
        header.add(arrowLabel);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 11f));
        titleLabel.setForeground(new Color(0x24292F));
        header.add(titleLabel);
        header.setCursor(new Cursor(Cursor.HAND_CURSOR));

        contentPanel.setBackground(BG_PANEL);
        contentPanel.setBorder(new EmptyBorder(0, 20, 12, 8));
        contentPanel.setVisible(expandedByDefault);

        Runnable toggle = () -> {
            boolean nowVisible = !contentPanel.isVisible();
            contentPanel.setVisible(nowVisible);
            arrowLabel.setText(nowVisible ? "▼ " : "▶ ");
        };

        header.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) { toggle.run(); }
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                header.setBackground(new Color(0xF0F0F0));
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                header.setBackground(BG_PANEL);
            }
        });

        section.add(header, BorderLayout.NORTH);
        section.add(contentPanel, BorderLayout.CENTER);
        return section;
    }

    private static JPanel buildStep3Panel(JCheckBox classNamesRandom, JCheckBox classNamesHomoglyph, JCheckBox classNamesInvisibleChars,
            JCheckBox methodNamesRandom, JCheckBox methodNamesHomoglyph, JCheckBox methodNamesInvisibleChars,
            JCheckBox fieldNamesRandom, JCheckBox fieldNamesHomoglyph, JCheckBox fieldNamesInvisibleChars,
            JCheckBox numberKeyRandom, JCheckBox arrayKeyRandom, JCheckBox booleanKeyRandom, JCheckBox stringKeyRandom, JCheckBox flowKeyRandom,
            JTextArea excludeArea, JFrame frame,
            JCheckBox classRename, JCheckBox methodRename, JCheckBox fieldRename, JCheckBox numberObf, JCheckBox arrayObf, JCheckBox booleanObf, JCheckBox stringObf, JCheckBox flowObf,
            JCheckBox debugInfoStrip, JSpinner classNameLength, JSpinner methodNameLength, JSpinner fieldNameLength) {
        JPanel step = new JPanel(new BorderLayout(8, 8));
        step.setBackground(BG_MAIN);
        step.setName(STEP_ADVANCED);

        JPanel inner = new JPanel(new BorderLayout(8, 8));
        inner.setBackground(BG_PANEL);
        inner.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(new LineBorder(BORDER, 1), "Step 3: Advanced",
                0, 0, null, new Color(0x24292F)),
            new EmptyBorder(12, 16, 16, 16)));

        JPanel classContent = new JPanel(new GridLayout(0, 2, 6, 4));
        classContent.setBackground(BG_PANEL);
        JPanel classLengthRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        classLengthRow.setBackground(BG_PANEL);
        classLengthRow.add(new JLabel("Name length:"));
        classLengthRow.add(classNameLength);
        classContent.add(classLengthRow);
        classContent.add(new JPanel());
        classContent.add(classNamesRandom);
        classContent.add(classNamesHomoglyph);
        classContent.add(classNamesInvisibleChars);

        JPanel methodContent = new JPanel(new GridLayout(0, 2, 6, 4));
        methodContent.setBackground(BG_PANEL);
        JPanel methodLengthRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        methodLengthRow.setBackground(BG_PANEL);
        methodLengthRow.add(new JLabel("Name length:"));
        methodLengthRow.add(methodNameLength);
        methodContent.add(methodLengthRow);
        methodContent.add(new JPanel());
        methodContent.add(methodNamesRandom);
        methodContent.add(methodNamesHomoglyph);
        methodContent.add(methodNamesInvisibleChars);

        JPanel fieldContent = new JPanel(new GridLayout(0, 2, 6, 4));
        fieldContent.setBackground(BG_PANEL);
        JPanel fieldLengthRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        fieldLengthRow.setBackground(BG_PANEL);
        fieldLengthRow.add(new JLabel("Name length:"));
        fieldLengthRow.add(fieldNameLength);
        fieldContent.add(fieldLengthRow);
        fieldContent.add(new JPanel());
        fieldContent.add(fieldNamesRandom);
        fieldContent.add(fieldNamesHomoglyph);
        fieldContent.add(fieldNamesInvisibleChars);

        JPanel keyContent = new JPanel(new GridLayout(0, 2, 6, 4));
        keyContent.setBackground(BG_PANEL);
        keyContent.add(numberKeyRandom);
        keyContent.add(arrayKeyRandom);
        keyContent.add(booleanKeyRandom);
        keyContent.add(stringKeyRandom);

        JPanel sectionsContainer = new JPanel();
        sectionsContainer.setLayout(new BoxLayout(sectionsContainer, BoxLayout.Y_AXIS));
        sectionsContainer.setBackground(BG_PANEL);
        sectionsContainer.add(createExpandableSection("Class Renaming", classContent, false));
        sectionsContainer.add(createExpandableSection("Method Renaming", methodContent, false));
        sectionsContainer.add(createExpandableSection("Field Renaming", fieldContent, false));
        sectionsContainer.add(createExpandableSection("Obfuscation Keys (XOR)", keyContent, false));

        JPanel homoglyphInfo = new JPanel(new BorderLayout(4, 4));
        homoglyphInfo.setBackground(BG_PANEL);
        String info = "<html><b>Homoglyphs & invisible characters</b> – Unicode lookalikes and zero-width chars. Names appear normal but copy-paste fails.</html>";
        JLabel infoLbl = new JLabel(info);
        infoLbl.setFont(infoLbl.getFont().deriveFont(10f));
        infoLbl.setForeground(new Color(0x555555));
        infoLbl.setBorder(new EmptyBorder(4, 0, 8, 0));
        homoglyphInfo.add(infoLbl, BorderLayout.NORTH);

        JPanel configRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        configRow.setBackground(BG_PANEL);
        JButton loadConfigBtn = new JButton("Load config.yml (JAR dir)");
        loadConfigBtn.setFocusPainted(false);
        loadConfigBtn.setMaximumSize(new Dimension(200, 28));
        loadConfigBtn.addActionListener(ev -> {
            var result = ConfigLoader.loadWithPath();
            if (result.configPath() == null) {
                ToastNotification.show(frame, "No config.yml found next to JAR. Using defaults.", ToastNotification.Type.INFO);
                return;
            }
            applyConfig(classRename, methodRename, fieldRename, numberObf, arrayObf, booleanObf, stringObf, debugInfoStrip, classNamesRandom,
                classNamesHomoglyph, classNamesInvisibleChars, methodNamesRandom, methodNameLength, methodNamesHomoglyph, methodNamesInvisibleChars,
                fieldNamesRandom, fieldNameLength, fieldNamesHomoglyph, fieldNamesInvisibleChars,
                numberKeyRandom, arrayKeyRandom, booleanKeyRandom, stringKeyRandom, classNameLength, excludeArea, result.config());
            ToastNotification.show(frame, "Config loaded.", ToastNotification.Type.SUCCESS);
        });
        JButton browseConfigBtn = new JButton("Browse config...");
        browseConfigBtn.setFocusPainted(false);
        browseConfigBtn.setMaximumSize(new Dimension(130, 28));
        browseConfigBtn.addActionListener(ev -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new FileNameExtensionFilter("YAML files (*.yml, *.yaml)", "yml", "yaml"));
            if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                try {
                    var result = ConfigLoader.loadFrom(fc.getSelectedFile().toPath());
                    applyConfig(classRename, methodRename, fieldRename, numberObf, arrayObf, booleanObf, stringObf, debugInfoStrip, classNamesRandom,
                        classNamesHomoglyph, classNamesInvisibleChars, methodNamesRandom, methodNameLength, methodNamesHomoglyph, methodNamesInvisibleChars,
                        fieldNamesRandom, fieldNameLength, fieldNamesHomoglyph, fieldNamesInvisibleChars,
                        numberKeyRandom, arrayKeyRandom, booleanKeyRandom, stringKeyRandom, classNameLength, excludeArea, result.config());
                    ToastNotification.show(frame, "Config loaded from " + result.configPath().getFileName(), ToastNotification.Type.SUCCESS);
                } catch (IOException ex) {
                    ToastNotification.show(frame, "Config invalid: " + ex.getMessage(), ToastNotification.Type.ERROR);
                }
            }
        });
        configRow.add(loadConfigBtn);
        configRow.add(browseConfigBtn);

        JPanel topSection = new JPanel(new BorderLayout(6, 6));
        topSection.setBackground(BG_PANEL);
        topSection.add(sectionsContainer, BorderLayout.NORTH);
        topSection.add(homoglyphInfo, BorderLayout.CENTER);
        topSection.add(configRow, BorderLayout.SOUTH);

        JScrollPane scrollPane = new JScrollPane(topSection);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(BG_PANEL);
        scrollPane.setMinimumSize(new Dimension(0, 140));

        JPanel excludePanel = new JPanel(new BorderLayout(4, 4));
        excludePanel.setBackground(BG_PANEL);
        excludePanel.setBorder(BorderFactory.createTitledBorder(new LineBorder(BORDER, 1), "Exclude classes (one per line)"));
        JScrollPane excludeScroll = new JScrollPane(excludeArea);
        excludePanel.setMinimumSize(new Dimension(0, 80));
        excludePanel.add(excludeScroll, BorderLayout.CENTER);
        JLabel excludeInfo = new JLabel(EXCLUDE_INFO);
        excludeInfo.setFont(excludeInfo.getFont().deriveFont(10f));
        excludeInfo.setForeground(new Color(0x666666));
        excludePanel.add(excludeInfo, BorderLayout.SOUTH);

        // Proportional layout: sections ~65%, exclude ~35%
        inner.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 8, 0);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0.65;
        inner.add(scrollPane, gbc);
        gbc.gridy = 1;
        gbc.weighty = 0.35;
        gbc.insets = new Insets(0, 0, 0, 0);
        inner.add(excludePanel, gbc);

        step.add(inner, BorderLayout.CENTER);
        return step;
    }

    private static JPanel buildStep4Panel(JTextField inputPath, JTextField outputName, JTextField outputDir, JTextArea excludeArea,
            JCheckBox classRename, JCheckBox methodRename, JCheckBox fieldRename, JCheckBox numberObf, JCheckBox arrayObf, JCheckBox booleanObf, JCheckBox stringObf,
            JCheckBox flowObf, JCheckBox debugInfoStrip, JCheckBox classNamesRandom, JCheckBox classNamesHomoglyph, JCheckBox classNamesInvisibleChars,
            JCheckBox methodNamesRandom, JSpinner methodNameLength, JCheckBox methodNamesHomoglyph, JCheckBox methodNamesInvisibleChars,
            JCheckBox fieldNamesRandom, JSpinner fieldNameLength, JCheckBox fieldNamesHomoglyph, JCheckBox fieldNamesInvisibleChars,
            JCheckBox numberKeyRandom, JCheckBox arrayKeyRandom, JCheckBox booleanKeyRandom, JCheckBox stringKeyRandom, JCheckBox flowKeyRandom,
            JSpinner classNameLength, JFrame frame, JTextPane outputPane) {
        JPanel step = new JPanel(new BorderLayout(8, 8));
        step.setBackground(BG_MAIN);
        step.setName(STEP_RUN);

        JPanel inner = new JPanel(new BorderLayout(12, 12));
        inner.setBackground(BG_PANEL);
        inner.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(new LineBorder(BORDER, 1), "Step 4: Run Obfuscation",
                0, 0, null, new Color(0x24292F)),
            new EmptyBorder(12, 16, 16, 16)));

        JLabel summary = new JLabel("<html><b>Ready to obfuscate.</b> Output log will appear below.</html>");
        summary.setBorder(new EmptyBorder(0, 0, 12, 0));
        inner.add(summary, BorderLayout.NORTH);

        JButton obfuscateBtn = new JButton("Obfuscate");
        obfuscateBtn.setFont(obfuscateBtn.getFont().deriveFont(Font.BOLD, 14f));
        obfuscateBtn.setBackground(ACCENT);
        obfuscateBtn.setForeground(Color.WHITE);
        obfuscateBtn.setFocusPainted(false);
        obfuscateBtn.setBorderPainted(true);
        obfuscateBtn.setOpaque(true);
        obfuscateBtn.setBorder(new EmptyBorder(12, 24, 12, 24));
        obfuscateBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnPanel.setBackground(BG_PANEL);
        btnPanel.add(obfuscateBtn);
        inner.add(btnPanel, BorderLayout.NORTH);

        JScrollPane outputScroll = new JScrollPane(outputPane);
        outputScroll.setBorder(new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(0, 0, 0, 0)));
        outputScroll.getViewport().setBackground(new Color(0x2D2D30));
        outputScroll.setPreferredSize(new Dimension(0, 180));
        inner.add(outputScroll, BorderLayout.CENTER);

        obfuscateBtn.addActionListener(e -> {
            String pathStr = inputPath.getText().trim();
            if (pathStr.isEmpty()) {
                ToastNotification.show(frame, "Please select an input JAR file.", ToastNotification.Type.WARN);
                return;
            }
            Path input = Path.of(pathStr);
            if (!Files.isRegularFile(input)) {
                ToastNotification.show(frame, "Input file does not exist.", ToastNotification.Type.ERROR);
                return;
            }

            String outNameStr = outputName.getText().trim();
            if (outNameStr.isEmpty()) {
                outNameStr = input.getFileName().toString().replaceAll("\\.jar$", "") + "-obfuscated.jar";
            } else if (!outNameStr.endsWith(".jar")) {
                outNameStr += ".jar";
            }
            Path outputDirPath = Path.of(outputDir.getText().trim());
            if (!Files.isDirectory(outputDirPath)) {
                try {
                    Files.createDirectories(outputDirPath);
                } catch (IOException ex) {
                    ToastNotification.show(frame, "Cannot create output folder: " + ex.getMessage(), ToastNotification.Type.ERROR);
                    return;
                }
            }
            Path outputPath = outputDirPath.resolve(outNameStr);

            List<String> excludeList = Arrays.stream(excludeArea.getText().split("\\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

            obfuscateBtn.setEnabled(false);
            outputPane.setText("");

            ObfuscatorConfig config = new ObfuscatorConfig(
                classRename.isSelected(),
                methodRename.isSelected(),
                fieldRename.isSelected(),
                numberObf.isSelected(),
                arrayObf.isSelected(),
                booleanObf.isSelected(),
                stringObf.isSelected(),
                flowObf.isSelected(),
                debugInfoStrip.isSelected(),
                classNamesRandom.isSelected(),
                (Integer) classNameLength.getValue(),
                classNamesHomoglyph.isSelected(),
                classNamesInvisibleChars.isSelected(),
                methodNamesRandom.isSelected(),
                (Integer) methodNameLength.getValue(),
                methodNamesHomoglyph.isSelected(),
                methodNamesInvisibleChars.isSelected(),
                fieldNamesRandom.isSelected(),
                (Integer) fieldNameLength.getValue(),
                fieldNamesHomoglyph.isSelected(),
                fieldNamesInvisibleChars.isSelected(),
                numberKeyRandom.isSelected(),
                arrayKeyRandom.isSelected(),
                booleanKeyRandom.isSelected(),
                stringKeyRandom.isSelected(),
                flowKeyRandom.isSelected(),
                excludeList
            );

            SwingWorker<Void, String> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    Logger.setGuiOutput(line -> publish(line));
                    try {
                        if (Files.exists(outputPath)) {
                            Files.delete(outputPath);
                        }
                        ObfuscationPipeline pipeline = new ObfuscationPipeline();
                        pipeline.run(input, outputPath, config);
                        publish("Done. Output: " + outputPath.toAbsolutePath());
                        SwingUtilities.invokeLater(() -> ToastNotification.show(frame, "Obfuscation completed.", ToastNotification.Type.SUCCESS));
                    } catch (IOException ex) {
                        publish("ERROR: " + ex.getMessage());
                        SwingUtilities.invokeLater(() -> ToastNotification.show(frame, "Failed: " + ex.getMessage(), ToastNotification.Type.ERROR));
                    } finally {
                        Logger.setGuiOutput(null);
                    }
                    return null;
                }

                @Override
                protected void process(java.util.List<String> chunks) {
                    for (String line : chunks) {
                        appendColored(outputPane, line);
                    }
                }

                @Override
                protected void done() {
                    obfuscateBtn.setEnabled(true);
                }
            };
            worker.execute();
        });

        step.add(inner, BorderLayout.CENTER);
        return step;
    }

    private static void applyConfig(JCheckBox classRename, JCheckBox methodRename, JCheckBox fieldRename, JCheckBox numberObf, JCheckBox arrayObf,
            JCheckBox booleanObf, JCheckBox stringObf, JCheckBox debugInfoStrip, JCheckBox classNamesRandom,
            JCheckBox classNamesHomoglyph, JCheckBox classNamesInvisibleChars, JCheckBox methodNamesRandom,
            JSpinner methodNameLength, JCheckBox methodNamesHomoglyph, JCheckBox methodNamesInvisibleChars,
            JCheckBox fieldNamesRandom, JSpinner fieldNameLength, JCheckBox fieldNamesHomoglyph, JCheckBox fieldNamesInvisibleChars,
            JCheckBox numberKeyRandom, JCheckBox arrayKeyRandom, JCheckBox booleanKeyRandom, JCheckBox stringKeyRandom,
            JSpinner classNameLength, JTextArea excludeArea, ObfuscatorConfig c) {
        classRename.setSelected(c.classRenamingEnabled());
        methodRename.setSelected(c.methodRenamingEnabled());
        fieldRename.setSelected(c.fieldRenamingEnabled());
        numberObf.setSelected(c.numberObfuscationEnabled());
        arrayObf.setSelected(c.arrayObfuscationEnabled());
        booleanObf.setSelected(c.booleanObfuscationEnabled());
        stringObf.setSelected(c.stringObfuscationEnabled());
        debugInfoStrip.setSelected(c.debugInfoStrippingEnabled());
        classNamesRandom.setSelected(c.classNamesRandom());
        classNamesHomoglyph.setSelected(c.classNamesHomoglyph());
        classNamesInvisibleChars.setSelected(c.classNamesInvisibleChars());
        methodNamesRandom.setSelected(c.methodNamesRandom());
        methodNameLength.setValue(c.methodNameLength());
        methodNamesHomoglyph.setSelected(c.methodNamesHomoglyph());
        methodNamesInvisibleChars.setSelected(c.methodNamesInvisibleChars());
        numberKeyRandom.setSelected(c.numberKeyRandom());
        arrayKeyRandom.setSelected(c.arrayKeyRandom());
        booleanKeyRandom.setSelected(c.booleanKeyRandom());
        stringKeyRandom.setSelected(c.stringKeyRandom());
        classNameLength.setValue(c.classNameLength());
        excludeArea.setText(String.join("\n", c.excludeClasses()));
    }

    private static void appendColored(JTextPane pane, String line) {
        Color color;
        if (line.startsWith("[INFO]")) {
            color = new Color(0x58A6FF);
        } else if (line.startsWith("[STEP]")) {
            color = new Color(0x79C0FF);
        } else if (line.startsWith("[OK]")) {
            color = new Color(0x3FB950);
        } else if (line.startsWith("[WARN]") || line.toUpperCase().contains("WARNING")) {
            color = new Color(0xFF7B72);
        } else if (line.startsWith("[ERROR]") || line.startsWith("ERROR:")) {
            color = new Color(0xF85149);
        } else if (line.startsWith("Done.")) {
            color = new Color(0x3FB950);
        } else {
            color = new Color(0xD4D4D4);
        }
        StyledDocument doc = pane.getStyledDocument();
        SimpleAttributeSet set = new SimpleAttributeSet();
        StyleConstants.setForeground(set, color);
        try {
            doc.insertString(doc.getLength(), line + "\n", set);
            pane.setCaretPosition(doc.getLength());
        } catch (BadLocationException ignored) {}
    }

    private static Image loadLogo() {
        try (InputStream in = ObfuscatorGui.class.getResourceAsStream("/Images/logo.png")) {
            if (in != null) {
                return new ImageIcon(javax.imageio.ImageIO.read(in)).getImage();
            }
        } catch (IOException ignored) {}
        try {
            Path jarDir = ConfigLoader.getJarDirectory();
            Path logoPath = jarDir.resolve("Images").resolve("logo.png");
            if (Files.isRegularFile(logoPath)) {
                return new ImageIcon(logoPath.toString()).getImage();
            }
        } catch (Exception ignored) {}
        return null;
    }
}

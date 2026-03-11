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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GUI for the St3ix Obfuscator. Select JAR, configure options, run obfuscation.
 */
public final class ObfuscatorGui {

    private static final String OBFUSCATE_FOLDER = "Obfuscate";

    private static final Color BG_MAIN = new Color(0xF5F5F7);
    private static final Color BG_PANEL = Color.WHITE;
    private static final Color BORDER = new Color(0xE0E0E0);
    private static final Color ACCENT = new Color(0x5C6BC0);

    private static final String EXCLUDE_INFO = "java.*, javax.*, org.bukkit.*, net.minecraft.* etc. are always excluded.";

    public static void show() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("St3ix Obfuscator");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setMinimumSize(new Dimension(680, 700));
            frame.setSize(780, 750);
            frame.setLocationRelativeTo(null);
            frame.getContentPane().setBackground(BG_MAIN);

            Image icon = loadLogo();
            if (icon != null) {
                frame.setIconImage(icon);
            }

            JPanel main = new JPanel(new BorderLayout(10, 10));
            main.setBorder(new EmptyBorder(12, 12, 12, 12));
            main.setBackground(BG_MAIN);

            JPanel top = new JPanel(new BorderLayout(6, 6));
            top.setBackground(BG_MAIN);

            JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
            filePanel.setBackground(BG_PANEL);
            filePanel.setBorder(new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(8, 10, 8, 10)));
            JTextField inputPath = new JTextField(28);
            inputPath.setEditable(false);
            inputPath.setMargin(new Insets(4, 6, 4, 6));
            JButton browseInputBtn = new JButton("Browse...");
            browseInputBtn.setFocusPainted(false);
            browseInputBtn.addActionListener(e -> {
                JFileChooser fc = new JFileChooser();
                fc.setFileFilter(new FileNameExtensionFilter("JAR files (*.jar)", "jar"));
                if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    inputPath.setText(fc.getSelectedFile().getAbsolutePath());
                }
            });
            filePanel.add(new JLabel("Input JAR:"));
            filePanel.add(inputPath);
            filePanel.add(browseInputBtn);
            top.add(filePanel, BorderLayout.NORTH);

            JPanel outputPanel = new JPanel(new GridLayout(2, 1, 6, 6));
            outputPanel.setBackground(BG_PANEL);
            outputPanel.setBorder(new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(8, 10, 8, 10)));

            JPanel outputNameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            outputNameRow.setBackground(BG_PANEL);
            JTextField outputName = new JTextField(25);
            outputName.setMargin(new Insets(4, 6, 4, 6));
            outputNameRow.add(new JLabel("Output name:"));
            outputNameRow.add(outputName);

            JPanel outputDirRow = new JPanel(new BorderLayout(6, 0));
            outputDirRow.setBackground(BG_PANEL);
            JTextField outputDir = new JTextField(50);
            outputDir.setEditable(true);
            outputDir.setMargin(new Insets(4, 6, 4, 6));
            Path defaultOutputDir = ConfigLoader.getJarDirectory().resolve(OBFUSCATE_FOLDER);
            outputDir.setText(defaultOutputDir.toAbsolutePath().toString());
            JButton browseOutputBtn = new JButton("Browse...");
            browseOutputBtn.setFocusPainted(false);
            browseOutputBtn.addActionListener(e -> {
                JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                Path current = Path.of(outputDir.getText().trim());
                if (Files.isDirectory(current)) {
                    fc.setCurrentDirectory(current.toFile());
                }
                if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    outputDir.setText(fc.getSelectedFile().getAbsolutePath());
                }
            });
            outputDirRow.add(new JLabel("Output folder:"), BorderLayout.WEST);
            outputDirRow.add(outputDir, BorderLayout.CENTER);
            outputDirRow.add(browseOutputBtn, BorderLayout.EAST);

            outputPanel.add(outputNameRow);
            outputPanel.add(outputDirRow);
            top.add(outputPanel, BorderLayout.CENTER);

            JPanel center = new JPanel(new BorderLayout(6, 6));
            center.setBackground(BG_MAIN);

            JPanel configPanel = new JPanel(new GridLayout(0, 2, 6, 4));
            configPanel.setBackground(BG_PANEL);
            configPanel.setBorder(BorderFactory.createTitledBorder(new LineBorder(BORDER, 1), "Obfuscation options"));
            JCheckBox classRename = new JCheckBox("Class renaming", true);
            JCheckBox numberObf = new JCheckBox("Number obfuscation", true);
            JCheckBox arrayObf = new JCheckBox("Array obfuscation", true);
            JCheckBox booleanObf = new JCheckBox("Boolean obfuscation", true);
            JCheckBox stringObf = new JCheckBox("String obfuscation", true);
            JCheckBox debugInfoStrip = new JCheckBox("Strip debug info", true);
            JCheckBox classNamesRandom = new JCheckBox("Random class names", false);
            JCheckBox classNamesHomoglyph = new JCheckBox("Homoglyph class names", false);
            JCheckBox classNamesInvisibleChars = new JCheckBox("Invisible chars in names", false);
            JCheckBox numberKeyRandom = new JCheckBox("Random number key", false);
            JCheckBox arrayKeyRandom = new JCheckBox("Random array key", false);
            JCheckBox booleanKeyRandom = new JCheckBox("Random boolean key", false);
            JCheckBox stringKeyRandom = new JCheckBox("Random string key", false);
            JSpinner classNameLength = new JSpinner(new SpinnerNumberModel(6, 1, 32, 1));
            configPanel.add(classRename);
            configPanel.add(numberObf);
            configPanel.add(arrayObf);
            configPanel.add(booleanObf);
            configPanel.add(stringObf);
            configPanel.add(debugInfoStrip);
            configPanel.add(classNamesRandom);
            configPanel.add(classNamesHomoglyph);
            configPanel.add(classNamesInvisibleChars);
            configPanel.add(numberKeyRandom);
            configPanel.add(arrayKeyRandom);
            configPanel.add(booleanKeyRandom);
            configPanel.add(stringKeyRandom);
            configPanel.add(new JLabel("Class name length:"));
            configPanel.add(classNameLength);
            center.add(configPanel, BorderLayout.NORTH);

            JPanel advancedObfPanel = new JPanel(new BorderLayout(4, 4));
            advancedObfPanel.setBackground(BG_PANEL);
            advancedObfPanel.setBorder(BorderFactory.createTitledBorder(new LineBorder(BORDER, 1), "Advanced class name obfuscation"));
            String homoglyphInfo = "<html><b>Homoglyphs & invisible characters</b> – Use Unicode lookalikes (e.g. Cyrillic а instead of Latin a) and zero-width chars. "
                + "Names appear normal but copy-paste and search fail. Safe for JVM; increases reverse-engineering effort.</html>";
            JLabel homoglyphInfoLabel = new JLabel(homoglyphInfo);
            homoglyphInfoLabel.setFont(homoglyphInfoLabel.getFont().deriveFont(10f));
            homoglyphInfoLabel.setForeground(new Color(0x555555));
            homoglyphInfoLabel.setBorder(new EmptyBorder(4, 6, 8, 6));
            advancedObfPanel.add(homoglyphInfoLabel, BorderLayout.NORTH);
            JPanel excludePanel = new JPanel(new BorderLayout(4, 4));
            excludePanel.setBackground(BG_PANEL);
            excludePanel.setBorder(BorderFactory.createTitledBorder(new LineBorder(BORDER, 1), "Exclude classes (one per line)"));
            JTextArea excludeArea = new JTextArea(3, 30);
            excludeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            excludeArea.setLineWrap(false);
            excludeArea.setMargin(new Insets(4, 6, 4, 6));
            JScrollPane excludeScroll = new JScrollPane(excludeArea);
            JLabel excludeInfo = new JLabel(EXCLUDE_INFO);
            excludeInfo.setFont(excludeInfo.getFont().deriveFont(10f));
            excludeInfo.setForeground(new Color(0x666666));
            excludePanel.add(excludeScroll, BorderLayout.CENTER);
            excludePanel.add(excludeInfo, BorderLayout.SOUTH);

            JPanel centerContent = new JPanel();
            centerContent.setLayout(new BoxLayout(centerContent, BoxLayout.Y_AXIS));
            centerContent.setBackground(BG_MAIN);
            centerContent.add(advancedObfPanel);
            centerContent.add(excludePanel);
            center.add(centerContent, BorderLayout.CENTER);

            JPanel configRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
            configRow.setBackground(BG_MAIN);
            JButton loadConfigBtn = new JButton("Load config.yml (JAR dir)");
            loadConfigBtn.setFocusPainted(false);
            loadConfigBtn.addActionListener(ev -> {
                var result = ConfigLoader.loadWithPath();
                if (result.configPath() == null) {
                    ToastNotification.show(frame, "No config.yml found next to JAR. Using defaults.", ToastNotification.Type.INFO);
                    return;
                }
                applyConfig(classRename, numberObf, arrayObf, booleanObf, stringObf, debugInfoStrip, classNamesRandom,
                    classNamesHomoglyph, classNamesInvisibleChars, numberKeyRandom, arrayKeyRandom, booleanKeyRandom,
                    stringKeyRandom, classNameLength, excludeArea, result.config());
                ToastNotification.show(frame, "Config loaded.", ToastNotification.Type.SUCCESS);
            });
            JButton browseConfigBtn = new JButton("Browse config...");
            browseConfigBtn.setFocusPainted(false);
            browseConfigBtn.addActionListener(ev -> {
                JFileChooser fc = new JFileChooser();
                fc.setFileFilter(new FileNameExtensionFilter("YAML files (*.yml, *.yaml)", "yml", "yaml"));
                if (fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    try {
                        var result = ConfigLoader.loadFrom(fc.getSelectedFile().toPath());
                        applyConfig(classRename, numberObf, arrayObf, booleanObf, stringObf, debugInfoStrip, classNamesRandom,
                            classNamesHomoglyph, classNamesInvisibleChars, numberKeyRandom, arrayKeyRandom, booleanKeyRandom,
                            stringKeyRandom, classNameLength, excludeArea, result.config());
                        ToastNotification.show(frame, "Config loaded from " + result.configPath().getFileName(), ToastNotification.Type.SUCCESS);
                    } catch (IOException ex) {
                        ToastNotification.show(frame, "Config invalid: " + ex.getMessage(), ToastNotification.Type.ERROR);
                    }
                }
            });
            configRow.add(loadConfigBtn);
            configRow.add(browseConfigBtn);

            JPanel bottomCenter = new JPanel(new BorderLayout(4, 4));
            bottomCenter.setBackground(BG_MAIN);
            bottomCenter.add(configRow, BorderLayout.NORTH);
            JButton obfuscateBtn = new JButton("Obfuscate");
            obfuscateBtn.setFont(obfuscateBtn.getFont().deriveFont(Font.BOLD, 13f));
            obfuscateBtn.setBackground(ACCENT);
            obfuscateBtn.setForeground(Color.WHITE);
            obfuscateBtn.setFocusPainted(false);
            obfuscateBtn.setBorderPainted(true);
            obfuscateBtn.setOpaque(true);
            obfuscateBtn.setBorder(new EmptyBorder(8, 20, 8, 20));
            obfuscateBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            bottomCenter.add(obfuscateBtn, BorderLayout.CENTER);
            center.add(bottomCenter, BorderLayout.SOUTH);

            main.add(top, BorderLayout.NORTH);
            main.add(center, BorderLayout.CENTER);

            JTextPane outputPane = new JTextPane();
            outputPane.setEditable(false);
            outputPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            outputPane.setBackground(new Color(0x2D2D30));
            outputPane.setForeground(new Color(0xD4D4D4));
            outputPane.setCaretColor(new Color(0xD4D4D4));
            outputPane.setMargin(new Insets(8, 10, 8, 10));
            outputPane.setPreferredSize(new Dimension(0, 220));
            JScrollPane outputScroll = new JScrollPane(outputPane);
            outputScroll.setBorder(new CompoundBorder(new LineBorder(BORDER, 1), new EmptyBorder(0, 0, 0, 0)));
            outputScroll.getViewport().setBackground(new Color(0x2D2D30));
            main.add(outputScroll, BorderLayout.SOUTH);

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
                    numberObf.isSelected(),
                    arrayObf.isSelected(),
                    booleanObf.isSelected(),
                    stringObf.isSelected(),
                    debugInfoStrip.isSelected(),
                    classNamesRandom.isSelected(),
                    (Integer) classNameLength.getValue(),
                    classNamesHomoglyph.isSelected(),
                    classNamesInvisibleChars.isSelected(),
                    numberKeyRandom.isSelected(),
                    arrayKeyRandom.isSelected(),
                    booleanKeyRandom.isSelected(),
                    stringKeyRandom.isSelected(),
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

            frame.addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override
                public void componentResized(java.awt.event.ComponentEvent ev) {
                    frame.repaint();
                }
            });

            frame.setContentPane(main);
            frame.setVisible(true);
        });
    }

    private static void applyConfig(JCheckBox classRename, JCheckBox numberObf, JCheckBox arrayObf, JCheckBox booleanObf,
            JCheckBox stringObf, JCheckBox debugInfoStrip, JCheckBox classNamesRandom, JCheckBox classNamesHomoglyph,
            JCheckBox classNamesInvisibleChars, JCheckBox numberKeyRandom, JCheckBox arrayKeyRandom,
            JCheckBox booleanKeyRandom, JCheckBox stringKeyRandom, JSpinner classNameLength, JTextArea excludeArea,
            ObfuscatorConfig c) {
        classRename.setSelected(c.classRenamingEnabled());
        numberObf.setSelected(c.numberObfuscationEnabled());
        arrayObf.setSelected(c.arrayObfuscationEnabled());
        booleanObf.setSelected(c.booleanObfuscationEnabled());
        stringObf.setSelected(c.stringObfuscationEnabled());
        debugInfoStrip.setSelected(c.debugInfoStrippingEnabled());
        classNamesRandom.setSelected(c.classNamesRandom());
        classNamesHomoglyph.setSelected(c.classNamesHomoglyph());
        classNamesInvisibleChars.setSelected(c.classNamesInvisibleChars());
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
            color = new Color(0x4EC9B0);
        } else if (line.startsWith("[STEP]")) {
            color = new Color(0x569CD6);
        } else if (line.startsWith("[OK]")) {
            color = new Color(0x6A9955);
        } else if (line.startsWith("[WARN]")) {
            color = new Color(0xDCDCAA);
        } else if (line.startsWith("[ERROR]") || line.startsWith("ERROR:")) {
            color = new Color(0xF44747);
        } else if (line.startsWith("Done.")) {
            color = new Color(0x6A9955);
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

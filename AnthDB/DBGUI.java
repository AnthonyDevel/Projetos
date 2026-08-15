import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Cliente gráfico estilo "workbench" (MySQL Workbench / pgAdmin) para o
 * banco de dados relacional próprio.
 *
 * Layout:
 *  - Barra de conexão no topo (host/porta/conectar/status)
 *  - Sidebar esquerda: lista de tabelas do banco (clique duplo monta um
 *    SELECT * FROM <tabela> LIMIT 200 e executa)
 *  - Centro: editor SQL (aceita múltiplos comandos separados por ";",
 *    executados em sequência) + botões de execução e de transação
 *    (BEGIN/COMMIT/ROLLBACK)
 *  - Abaixo do editor: abas com "Resultados" (grade/JTable, populada a
 *    partir do formato estruturado que o CommandProcessor agora retorna
 *    para SELECT) e "Mensagens" (log de tudo que não é SELECT: OK, erros,
 *    confirmações de transação etc.)
 *  - Barra de status: estado da conexão e estado da transação
 *    (autocommit / em transação)
 */
public class DBGUI extends JFrame {

    // Marcadores de protocolo (espelham CommandProcessor)
    private static final String TABLE_MARKER_START = "§TABLE§";
    private static final String TABLE_MARKER_END = "§ENDTABLE§";
    private static final String FIELD_SEP = "\u001F";

    // Cores do tema
    private static final Color BG_DARK      = new Color(12, 12, 18);
    private static final Color BG_PANEL     = new Color(18, 18, 28);
    private static final Color BG_INPUT     = new Color(22, 22, 34);
    private static final Color BG_HEADER    = new Color(26, 26, 40);
    private static final Color ACCENT_CYAN  = new Color(0, 255, 200);
    private static final Color ACCENT_MAGENTA = new Color(255, 0, 128);
    private static final Color ACCENT_PURPLE = new Color(128, 0, 255);
    private static final Color ACCENT_GREEN = new Color(80, 220, 120);
    private static final Color ACCENT_YELLOW = new Color(240, 200, 60);
    private static final Color ACCENT_RED   = new Color(255, 90, 90);
    private static final Color TEXT_MAIN    = new Color(220, 220, 230);
    private static final Color TEXT_DIM     = new Color(140, 140, 160);

    // Fontes
    private static final Font FONT_TITLE = new Font("Consolas", Font.BOLD, 18);
    private static final Font FONT_LABEL = new Font("Consolas", Font.PLAIN, 11);
    private static final Font FONT_TEXT  = new Font("Consolas", Font.PLAIN, 14);
    private static final Font FONT_MONO  = new Font("Consolas", Font.PLAIN, 14);
    private static final Font FONT_BUTTON = new Font("Consolas", Font.BOLD, 11);

    // --- Componentes: conexão ---
    private JTextField hostField;
    private JTextField portField;
    private JButton connectButton;
    private JButton disconnectButton;
    private JLabel statusDot;
    private JLabel statusLabel;
    private JLabel txStatusLabel;

    // --- Componentes: sidebar ---
    private DefaultListModel<String> tablesModel;
    private JList<String> tablesList;
    private JButton refreshTablesButton;

    // --- Componentes: editor / resultados ---
    private JTextArea sqlEditor;
    private JButton executeButton;
    private JButton beginButton;
    private JButton commitButton;
    private JButton rollbackButton;
    private JButton clearEditorButton;
    private JTable resultsTable;
    private DefaultTableModel resultsModel;
    private JTextArea logArea;
    private JTabbedPane resultsTabs;
    private JLabel rowCountLabel;

    // --- Conexão de rede ---
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private boolean inTransaction = false;

    public DBGUI() {
        initComponents();
        applyTheme();
        setConnectionControlsEnabled(false);
    }

    // =========================================================
    // Construção da UI
    // =========================================================
    private void initComponents() {
        setTitle("DarkDB Workbench");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 820);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_DARK);

        add(buildTopBar(), BorderLayout.NORTH);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildSidebar(), buildCenterArea());
        mainSplit.setDividerLocation(230);
        mainSplit.setBorder(null);
        mainSplit.setBackground(BG_DARK);
        mainSplit.setOpaque(true);
        add(mainSplit, BorderLayout.CENTER);

        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel buildTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(BG_HEADER);
        top.setBorder(new EmptyBorder(10, 15, 10, 15));

        JLabel titleLabel = new JLabel("DARKDB WORKBENCH");
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(ACCENT_CYAN);
        top.add(titleLabel, BorderLayout.WEST);

        JPanel connPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        connPanel.setOpaque(false);

        JLabel hostLabel = new JLabel("HOST");
        hostLabel.setFont(FONT_LABEL);
        hostLabel.setForeground(ACCENT_MAGENTA);
        hostField = styledTextField("localhost", 10);

        JLabel portLabel = new JLabel("PORTA");
        portLabel.setFont(FONT_LABEL);
        portLabel.setForeground(ACCENT_MAGENTA);
        portField = styledTextField("12345", 5);

        connectButton = createButton("CONECTAR", ACCENT_CYAN);
        disconnectButton = createButton("DESCONECTAR", ACCENT_MAGENTA);
        disconnectButton.setEnabled(false);

        connPanel.add(hostLabel);
        connPanel.add(hostField);
        connPanel.add(portLabel);
        connPanel.add(portField);
        connPanel.add(connectButton);
        connPanel.add(disconnectButton);
        top.add(connPanel, BorderLayout.EAST);

        connectButton.addActionListener(e -> connect());
        disconnectButton.addActionListener(e -> disconnect());

        return top;
    }

    private JPanel buildSidebar() {
        RoundedPanel panel = new RoundedPanel(16, BG_PANEL);
        panel.setLayout(new BorderLayout(0, 8));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        JLabel header = new JLabel("TABELAS");
        header.setFont(FONT_LABEL);
        header.setForeground(ACCENT_PURPLE);
        refreshTablesButton = createSmallButton("⟳", ACCENT_PURPLE);
        refreshTablesButton.setToolTipText("Atualizar lista de tabelas");
        headerRow.add(header, BorderLayout.WEST);
        headerRow.add(refreshTablesButton, BorderLayout.EAST);
        panel.add(headerRow, BorderLayout.NORTH);

        tablesModel = new DefaultListModel<>();
        tablesList = new JList<>(tablesModel);
        tablesList.setFont(FONT_TEXT);
        tablesList.setBackground(BG_INPUT);
        tablesList.setForeground(TEXT_MAIN);
        tablesList.setSelectionBackground(ACCENT_PURPLE.darker());
        tablesList.setSelectionForeground(Color.WHITE);
        tablesList.setBorder(new EmptyBorder(6, 6, 6, 6));
        tablesList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String table = tablesList.getSelectedValue();
                    if (table != null) {
                        sqlEditor.setText("SELECT * FROM " + table + " LIMIT 200;");
                        executeAll();
                    }
                }
            }
        });
        JScrollPane listScroll = new JScrollPane(tablesList);
        listScroll.setBorder(BorderFactory.createLineBorder(BG_INPUT, 1));
        panel.add(listScroll, BorderLayout.CENTER);

        refreshTablesButton.addActionListener(e -> refreshTables());

        return panel;
    }

    private JPanel buildCenterArea() {
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(10, 10, 10, 10));

        center.add(buildEditorPanel(), BorderLayout.NORTH);
        center.add(buildResultsPanel(), BorderLayout.CENTER);

        return center;
    }

    private JPanel buildEditorPanel() {
        RoundedPanel panel = new RoundedPanel(16, BG_PANEL);
        panel.setLayout(new BorderLayout(0, 8));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        panel.setPreferredSize(new Dimension(100, 220));

        JLabel header = new JLabel("EDITOR SQL  (Ctrl+Enter executa • ';' separa múltiplos comandos)");
        header.setFont(FONT_LABEL);
        header.setForeground(TEXT_DIM);
        panel.add(header, BorderLayout.NORTH);

        sqlEditor = new JTextArea();
        sqlEditor.setFont(FONT_MONO);
        sqlEditor.setBackground(BG_INPUT);
        sqlEditor.setForeground(TEXT_MAIN);
        sqlEditor.setCaretColor(ACCENT_CYAN);
        sqlEditor.setLineWrap(true);
        sqlEditor.setWrapStyleWord(true);
        sqlEditor.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_CYAN, 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        sqlEditor.setText("SELECT * FROM ");

        // Ctrl+Enter para executar
        InputMap im = sqlEditor.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = sqlEditor.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "executeQuery");
        am.put("executeQuery", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { executeAll(); }
        });

        JScrollPane editorScroll = new JScrollPane(sqlEditor);
        editorScroll.setBorder(null);
        panel.add(editorScroll, BorderLayout.CENTER);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonRow.setOpaque(false);
        executeButton = createButton("▶ EXECUTAR", ACCENT_CYAN);
        beginButton = createButton("BEGIN", ACCENT_PURPLE);
        commitButton = createButton("COMMIT", ACCENT_GREEN);
        rollbackButton = createButton("ROLLBACK", ACCENT_RED);
        clearEditorButton = createButton("LIMPAR", TEXT_DIM);

        buttonRow.add(executeButton);
        buttonRow.add(beginButton);
        buttonRow.add(commitButton);
        buttonRow.add(rollbackButton);
        buttonRow.add(clearEditorButton);
        panel.add(buttonRow, BorderLayout.SOUTH);

        executeButton.addActionListener(e -> executeAll());
        beginButton.addActionListener(e -> executeSingleControl("BEGIN"));
        commitButton.addActionListener(e -> executeSingleControl("COMMIT"));
        rollbackButton.addActionListener(e -> executeSingleControl("ROLLBACK"));
        clearEditorButton.addActionListener(e -> sqlEditor.setText(""));

        return panel;
    }

    private JPanel buildResultsPanel() {
        RoundedPanel panel = new RoundedPanel(16, BG_PANEL);
        panel.setLayout(new BorderLayout());
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        resultsTabs = new JTabbedPane();
        resultsTabs.setFont(FONT_LABEL);
        resultsTabs.setBackground(BG_PANEL);
        resultsTabs.setForeground(TEXT_MAIN);

        resultsModel = new DefaultTableModel();
        resultsTable = new JTable(resultsModel);
        styleTable(resultsTable);
        JScrollPane tableScroll = new JScrollPane(resultsTable);
        tableScroll.getViewport().setBackground(BG_INPUT);
        tableScroll.setBorder(null);
        resultsTabs.addTab("Resultados", tableScroll);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(FONT_MONO);
        logArea.setBackground(BG_INPUT);
        logArea.setForeground(TEXT_MAIN);
        logArea.setBorder(new EmptyBorder(8, 8, 8, 8));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(null);
        resultsTabs.addTab("Mensagens", logScroll);

        panel.add(resultsTabs, BorderLayout.CENTER);

        rowCountLabel = new JLabel(" ");
        rowCountLabel.setFont(FONT_LABEL);
        rowCountLabel.setForeground(TEXT_DIM);
        rowCountLabel.setBorder(new EmptyBorder(6, 4, 0, 0));
        panel.add(rowCountLabel, BorderLayout.SOUTH);

        return panel;
    }

    private void styleTable(JTable table) {
        table.setFont(FONT_TEXT);
        table.setBackground(BG_INPUT);
        table.setForeground(TEXT_MAIN);
        table.setGridColor(BG_HEADER);
        table.setSelectionBackground(ACCENT_CYAN.darker());
        table.setSelectionForeground(Color.BLACK);
        table.setRowHeight(24);
        table.getTableHeader().setFont(FONT_BUTTON);
        table.getTableHeader().setBackground(BG_HEADER);
        table.getTableHeader().setForeground(ACCENT_CYAN);
        table.setFillsViewportHeight(true);
    }

    private JPanel buildStatusBar() {
        JPanel status = new JPanel(new BorderLayout());
        status.setBackground(BG_HEADER);
        status.setBorder(new EmptyBorder(6, 15, 6, 15));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        statusDot = new JLabel("●");
        statusDot.setForeground(ACCENT_MAGENTA);
        statusLabel = new JLabel("Desconectado");
        statusLabel.setFont(FONT_LABEL);
        statusLabel.setForeground(TEXT_DIM);
        left.add(statusDot);
        left.add(statusLabel);
        status.add(left, BorderLayout.WEST);

        txStatusLabel = new JLabel("AUTOCOMMIT");
        txStatusLabel.setFont(FONT_LABEL);
        txStatusLabel.setForeground(TEXT_DIM);
        status.add(txStatusLabel, BorderLayout.EAST);

        return status;
    }

    // =========================================================
    // Helpers visuais
    // =========================================================
    private JTextField styledTextField(String text, int cols) {
        JTextField f = new JTextField(text, cols);
        f.setFont(FONT_TEXT);
        f.setBackground(BG_INPUT);
        f.setForeground(TEXT_MAIN);
        f.setCaretColor(ACCENT_CYAN);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_PURPLE, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        return f;
    }

    private JButton createButton(String text, Color accent) {
        JButton button = new JButton(text);
        button.setFont(FONT_BUTTON);
        button.setForeground(accent);
        button.setBackground(BG_INPUT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent, 1),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) { button.setBackground(accent); button.setForeground(BG_DARK); }
            }
            @Override public void mouseExited(MouseEvent e) {
                button.setBackground(BG_INPUT); button.setForeground(accent);
            }
        });
        return button;
    }

    private JButton createSmallButton(String text, Color accent) {
        JButton b = createButton(text, accent);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent, 1),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        return b;
    }

    private void applyTheme() {
        getContentPane().setBackground(BG_DARK);
        setBackground(BG_DARK);
    }

    private static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color bg;
        RoundedPanel(int radius, Color bg) {
            this.radius = radius;
            this.bg = bg;
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), radius, radius));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // =========================================================
    // Conexão
    // =========================================================
    private void setConnectionControlsEnabled(boolean connected) {
        hostField.setEnabled(!connected);
        portField.setEnabled(!connected);
        connectButton.setEnabled(!connected);
        disconnectButton.setEnabled(connected);
        executeButton.setEnabled(connected);
        beginButton.setEnabled(connected);
        commitButton.setEnabled(connected);
        rollbackButton.setEnabled(connected);
        refreshTablesButton.setEnabled(connected);

        statusDot.setForeground(connected ? ACCENT_GREEN : ACCENT_MAGENTA);
        statusLabel.setText(connected ?
                "Conectado a " + hostField.getText() + ":" + portField.getText() :
                "Desconectado");
        if (!connected) {
            tablesModel.clear();
            resultsModel.setRowCount(0);
            resultsModel.setColumnCount(0);
            setTransactionState(false);
        }
    }

    private void setTransactionState(boolean active) {
        inTransaction = active;
        txStatusLabel.setText(active ? "EM TRANSAÇÃO" : "AUTOCOMMIT");
        txStatusLabel.setForeground(active ? ACCENT_YELLOW : TEXT_DIM);
    }

    private void connect() {
        String host = hostField.getText().trim();
        String portStr = portField.getText().trim();
        if (host.isEmpty() || portStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha host e porta.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Porta inválida.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        connectButton.setEnabled(false);
        statusLabel.setText("Conectando...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                socket = new Socket(host, port);
                out = new DataOutputStream(socket.getOutputStream());
                in = new DataInputStream(socket.getInputStream());
                return null;
            }
            @Override
            protected void done() {
                try {
                    get();
                    setConnectionControlsEnabled(true);
                    log("Conectado ao servidor " + host + ":" + port + ".\n");
                    refreshTables();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    JOptionPane.showMessageDialog(DBGUI.this,
                            "Não foi possível conectar: " + cause.getMessage(),
                            "Erro de conexão", JOptionPane.ERROR_MESSAGE);
                    setConnectionControlsEnabled(false);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void disconnect() {
        try {
            if (out != null) out.writeUTF("EXIT");
            if (socket != null) socket.close();
        } catch (IOException e) {
            // ignora
        } finally {
            socket = null;
            out = null;
            in = null;
            setConnectionControlsEnabled(false);
            log("Desconectado do servidor.\n");
        }
    }

    // =========================================================
    // Execução de comandos
    // =========================================================

    /** Executa BEGIN/COMMIT/ROLLBACK isoladamente, ignorando o texto do editor. */
    private void executeSingleControl(String command) {
        runCommands(Collections.singletonList(command));
    }

    /** Executa o conteúdo do editor, separando múltiplos comandos por ";". */
    private void executeAll() {
        String text = sqlEditor.getText();
        if (text == null || text.trim().isEmpty()) return;
        List<String> statements = new ArrayList<>();
        for (String part : text.split(";")) {
            String s = part.trim();
            if (!s.isEmpty()) statements.add(s);
        }
        if (statements.isEmpty()) return;
        runCommands(statements);
    }

    private void runCommands(List<String> statements) {
        if (socket == null || out == null || in == null) {
            JOptionPane.showMessageDialog(this, "Conecte-se ao servidor primeiro.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        setExecutionControlsEnabled(false);
        statusLabel.setText("Executando...");
        long start = System.currentTimeMillis();

        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                List<String> responses = new ArrayList<>();
                for (String stmt : statements) {
                    out.writeUTF(stmt);
                    out.flush();
                    responses.add(in.readUTF());
                }
                return responses;
            }
            @Override
            protected void done() {
                long elapsedMs = System.currentTimeMillis() - start;
                try {
                    List<String> responses = get();
                    String lastTableResponse = null;
                    for (int i = 0; i < responses.size(); i++) {
                        String stmt = statements.get(i);
                        String resp = responses.get(i);
                        if (resp.startsWith(TABLE_MARKER_START)) {
                            lastTableResponse = resp;
                            log("> " + stmt + "\n(resultado exibido na aba Resultados)\n");
                        } else {
                            log("> " + stmt + "\n" + resp + (resp.endsWith("\n") ? "" : "\n"));
                            updateTransactionStateFromResponse(resp);
                        }
                    }
                    if (lastTableResponse != null) {
                        populateResultsTable(lastTableResponse);
                        resultsTabs.setSelectedIndex(0);
                    }
                } catch (ExecutionException e) {
                    log("Erro de comunicação: " + e.getCause().getMessage() + "\n");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    setExecutionControlsEnabled(true);
                    statusLabel.setText("Conectado a " + hostField.getText() + ":" + portField.getText()
                            + "  (" + elapsedMs + " ms)");
                }
            }
        }.execute();
    }

    private void updateTransactionStateFromResponse(String resp) {
        if (resp.contains("Transação iniciada")) {
            setTransactionState(true);
        } else if (resp.contains("Transação commitada") || resp.contains("Transação revertida")) {
            setTransactionState(false);
            refreshTables();
        }
    }

    private void setExecutionControlsEnabled(boolean enabled) {
        executeButton.setEnabled(enabled);
        beginButton.setEnabled(enabled && !inTransaction);
        commitButton.setEnabled(enabled);
        rollbackButton.setEnabled(enabled);
    }

    private void refreshTables() {
        if (socket == null || out == null || in == null) return;
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                out.writeUTF("LIST TABLES");
                out.flush();
                return in.readUTF();
            }
            @Override
            protected void done() {
                try {
                    String resp = get();
                    tablesModel.clear();
                    if (!resp.trim().equals("(nenhuma tabela)")) {
                        for (String line : resp.split("\n")) {
                            String t = line.trim();
                            if (!t.isEmpty()) tablesModel.addElement(t);
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    // =========================================================
    // Renderização de resultados
    // =========================================================
    private void populateResultsTable(String tableResponse) {
        String[] lines = tableResponse.split("\n", -1);
        if (lines.length < 2) {
            resultsModel.setColumnCount(0);
            resultsModel.setRowCount(0);
            rowCountLabel.setText("0 linhas");
            return;
        }
        String[] columns = lines[1].split(FIELD_SEP, -1);
        Vector<String> colVector = new Vector<>(Arrays.asList(columns));

        Vector<Vector<Object>> dataVector = new Vector<>();
        for (int i = 2; i < lines.length; i++) {
            String l = lines[i];
            if (l.equals(TABLE_MARKER_END) || l.isEmpty()) continue;
            String[] vals = l.split(FIELD_SEP, -1);
            Vector<Object> row = new Vector<>(Arrays.asList(vals));
            dataVector.add(row);
        }

        resultsModel.setDataVector(dataVector, colVector);
        styleTable(resultsTable);
        rowCountLabel.setText(dataVector.size() + " linha(s)");
    }

    private void log(String text) {
        logArea.append(text);
        logArea.setCaretPosition(logArea.getDocument().getLength());
        if (text.startsWith("Erro") || text.contains("\nErro")) {
            resultsTabs.setSelectedIndex(1);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DBGUI().setVisible(true));
    }
}

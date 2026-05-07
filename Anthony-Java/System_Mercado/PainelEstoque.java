package System_Mercado;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;

public class PainelEstoque extends JPanel {

    private JTable tabelaEstoque;
    private DefaultTableModel modeloTabela;
    private JTextField txtBusca;
    private TableRowSorter<DefaultTableModel> sorter;

    public PainelEstoque() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(Color.WHITE);

        // --- Painel de Busca (Norte) ---
        JPanel painelBusca = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painelBusca.setBorder(new TitledBorder("Controle de Estoque"));
        painelBusca.add(new JLabel("Buscar Produto:"));
        txtBusca = new JTextField(30);
        painelBusca.add(txtBusca);

        // --- Tabela de Estoque (Centro) ---
        String[] colunas = {"Código", "Nome do Produto", "Quantidade Atual", "Status"};
        modeloTabela = new DefaultTableModel(colunas, 0);
        tabelaEstoque = new JTable(modeloTabela);
        sorter = new TableRowSorter<>(modeloTabela);
        tabelaEstoque.setRowSorter(sorter);

        tabelaEstoque.setFillsViewportHeight(true);
        tabelaEstoque.setRowHeight(25);
        tabelaEstoque.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));

        // Exemplo de dados
        modeloTabela.addRow(new Object[]{"789001", "Arroz Tipo 1 (5kg)", 150, "OK"});
        modeloTabela.addRow(new Object[]{"789002", "Feijão Carioca (1kg)", 200, "OK"});
        modeloTabela.addRow(new Object[]{"789015", "Leite Integral (1L)", 15, "Baixo"});
        modeloTabela.addRow(new Object[]{"789020", "Refrigerante (2L)", 0, "Sem Estoque"});

        // Adiciona funcionalidade de filtro à busca
        txtBusca.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            private void filter() {
                String text = txtBusca.getText();
                if (text.trim().length() == 0) {
                    sorter.setRowFilter(null);
                } else {
                    // O (?i) torna a busca case-insensitive
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
                }
            }
        });

        add(painelBusca, BorderLayout.NORTH);
        add(new JScrollPane(tabelaEstoque), BorderLayout.CENTER);
    }
}
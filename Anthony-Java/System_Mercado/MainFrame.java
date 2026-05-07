package System_Mercado;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MainFrame extends JFrame {

    private CardLayout cardLayout = new CardLayout();
    private JPanel painelPrincipal; // Painel que usará o CardLayout

    // Painéis de cada módulo
    private JPanel painelCaixa;
    private JPanel painelGestao;
    private JPanel painelEstoque;
    private JPanel painelProdutos;

    public MainFrame() {
        // --- Configurações básicas da Janela ---
        setTitle("Sistema de Supermercado - Super Java");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(null); // Centraliza a janela na tela
        setLayout(new BorderLayout());

        // --- Painel do Menu Lateral ---
        JPanel painelMenu = new JPanel();
        painelMenu.setBackground(new Color(45, 52, 54)); // Cor de fundo escura
        painelMenu.setLayout(new BoxLayout(painelMenu, BoxLayout.Y_AXIS));
        painelMenu.setPreferredSize(new Dimension(200, 0));

        // Título no menu
        JLabel tituloMenu = new JLabel("MENU");
        tituloMenu.setForeground(Color.WHITE);
        tituloMenu.setFont(new Font("Arial", Font.BOLD, 24));
        tituloMenu.setAlignmentX(Component.CENTER_ALIGNMENT);
        tituloMenu.setBorder(new EmptyBorder(20, 0, 20, 0));

        painelMenu.add(tituloMenu);
        painelMenu.add(Box.createRigidArea(new Dimension(0, 20))); // Espaçamento

        // Botões do menu
        String[] nomesBotoes = {"Caixa", "Gestão", "Estoque", "Produtos"};
        for (String nome : nomesBotoes) {
            JButton botao = criarBotaoMenu(nome);
            painelMenu.add(botao);
            painelMenu.add(Box.createRigidArea(new Dimension(0, 10))); // Espaçamento entre botões
        }

        // --- Painel Principal (onde as telas serão trocadas) ---
        painelPrincipal = new JPanel(cardLayout);

        // Instancia cada painel de módulo
        painelCaixa = new PainelCaixa();
        painelGestao = new PainelGestao();
        painelEstoque = new PainelEstoque();
        painelProdutos = new PainelProdutos();

        // Adiciona os painéis ao CardLayout
        painelPrincipal.add(painelCaixa, "Caixa");
        painelPrincipal.add(painelGestao, "Gestão");
        painelPrincipal.add(painelEstoque, "Estoque");
        painelPrincipal.add(painelProdutos, "Produtos");

        // --- Adiciona os painéis à janela principal ---
        add(painelMenu, BorderLayout.WEST);
        add(painelPrincipal, BorderLayout.CENTER);
    }

    private JButton criarBotaoMenu(String nome) {
        JButton botao = new JButton(nome);
        botao.setForeground(Color.WHITE);
        botao.setBackground(new Color(99, 110, 114));
        botao.setFont(new Font("Arial", Font.PLAIN, 18));
        botao.setFocusPainted(false);
        botao.setBorder(new EmptyBorder(15, 0, 15, 0));
        botao.setMaximumSize(new Dimension(Integer.MAX_VALUE, botao.getMinimumSize().height));
        botao.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Adiciona a ação para trocar de tela
        botao.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(painelPrincipal, nome);
            }
        });
        return botao;
    }

    public static void main(String[] args) {
        // Garante que a UI será executada na thread de eventos do Swing
        SwingUtilities.invokeLater(() -> {
            try {
                // Tenta aplicar um Look and Feel mais moderno
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception e) {
                System.out.println("Nimbus Look and Feel não encontrado. Usando o padrão.");
            }
            new MainFrame().setVisible(true);
        });
    }
}
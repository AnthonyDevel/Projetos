package Programs_Corp;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AnaliseDadosEmpresa {

    static class Dia {
        String data;
        double faturamento;
        double perda;

        public Dia(String data, double faturamento, double perda) {
            this.data = data;
            this.faturamento = faturamento;
            this.perda = perda;
        }

        public double getLucro() {
            return faturamento - perda;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Análise de Dados da Empresa");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(900, 700);
            frame.setLayout(new BorderLayout());

            JLabel title = new JLabel("Análise de Dados da Empresa", SwingConstants.CENTER);
            title.setFont(new Font("Arial", Font.BOLD, 24));
            frame.add(title, BorderLayout.NORTH);

            JPanel optionsPanel = new JPanel();
            optionsPanel.setLayout(new GridLayout(4, 1, 10, 10));
            JButton btnInserirDados = new JButton("Inserir Dados do Dia");
            JButton btnExibirResumo = new JButton("Exibir Resumo");
            JButton btnExibirGrafico = new JButton("Exibir Gráfico");
            JButton btnSair = new JButton("Sair");

            optionsPanel.add(btnInserirDados);
            optionsPanel.add(btnExibirResumo);
            optionsPanel.add(btnExibirGrafico);
            optionsPanel.add(btnSair);

            frame.add(optionsPanel, BorderLayout.WEST);

            JTextArea outputArea = new JTextArea();
            outputArea.setEditable(false);
            outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
            JScrollPane scrollPane = new JScrollPane(outputArea);
            frame.add(scrollPane, BorderLayout.CENTER);

            List<Dia> dados = new ArrayList<>();

            btnInserirDados.addActionListener(e -> {
                String data = JOptionPane.showInputDialog(frame, "Digite a data (dd/mm/aaaa):");
                if (data == null || data.trim().isEmpty()) return;

                String faturamentoStr = JOptionPane.showInputDialog(frame, "Digite o faturamento:");
                if (faturamentoStr == null || faturamentoStr.trim().isEmpty()) return;

                String perdaStr = JOptionPane.showInputDialog(frame, "Digite a perda:");
                if (perdaStr == null || perdaStr.trim().isEmpty()) return;

                try {
                    double faturamento = Double.parseDouble(faturamentoStr);
                    double perda = Double.parseDouble(perdaStr);

                    dados.add(new Dia(data, faturamento, perda));
                    outputArea.append("Dados do dia " + data + " adicionados com sucesso!\n");
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Entrada inválida. Tente novamente.", "Erro", JOptionPane.ERROR_MESSAGE);
                }
            });

            btnExibirResumo.addActionListener(e -> {
                double totalFaturado = 0;
                double totalPerdido = 0;

                for (Dia dia : dados) {
                    totalFaturado += dia.faturamento;
                    totalPerdido += dia.perda;
                }

                outputArea.append("\nResumo:\n");
                outputArea.append(String.format("Total Faturado: R$ %.2f\n", totalFaturado));
                outputArea.append(String.format("Total Perdido: R$ %.2f\n", totalPerdido));
                outputArea.append(String.format("Lucro Total: R$ %.2f\n\n", totalFaturado - totalPerdido));
            });

            btnExibirGrafico.addActionListener(e -> {
                JFrame graficoFrame = new JFrame("Gráfico de Estatísticas");
                graficoFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                graficoFrame.setSize(900, 600);
                graficoFrame.add(new PainelGrafico(dados));
                graficoFrame.setVisible(true);
            });

            btnSair.addActionListener(e -> System.exit(0));

            frame.setVisible(true);
        });
    }

    static class PainelGrafico extends JPanel {
        List<Dia> dados;
        Image logo; // Variável para armazenar a imagem

        public PainelGrafico(List<Dia> dados) {
            this.dados = dados;

            // Carregar a imagem (ajuste o caminho para sua imagem)
            logo = new ImageIcon("C:\\Users\\Admin\\logos\\logo2.png").getImage();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int largura = getWidth();
            int altura = getHeight();
            int margem = 50;
            int larguraGrafico = largura - 2 * margem;
            int alturaGrafico = altura - 2 * margem;

            // Desenhar os eixos
            g.drawLine(margem, margem, margem, altura - margem); // Eixo Y
            g.drawLine(margem, altura - margem, largura - margem, altura - margem); // Eixo X

            // Encontrar o maior valor para escalonar os dados
            double maiorValor = 0;
            for (Dia dia : dados) {
                maiorValor = Math.max(maiorValor, Math.max(dia.faturamento, Math.max(dia.perda, dia.getLucro())));
            }

            if (maiorValor == 0) return; // Evita divisão por zero

            // Desenhar os dados
            int numeroDeDias = dados.size();
            int larguraBarra = larguraGrafico / (numeroDeDias * 4);

            for (int i = 0; i < numeroDeDias; i++) {
                Dia dia = dados.get(i);
                int xBase = margem + i * 4 * larguraBarra;

                int alturaFaturamento = (int) ((dia.faturamento / maiorValor) * alturaGrafico);
                int alturaPerda = (int) ((dia.perda / maiorValor) * alturaGrafico);
                int alturaLucro = (int) ((dia.getLucro() / maiorValor) * alturaGrafico);

                // Desenhar barras
                g.setColor(new Color(66, 135, 245));
                g.fillRect(xBase, altura - margem - alturaFaturamento, larguraBarra, alturaFaturamento);

                g.setColor(new Color(245, 66, 66));
                g.fillRect(xBase + larguraBarra, altura - margem - alturaPerda, larguraBarra, alturaPerda);

                g.setColor(new Color(66, 245, 96));
                g.fillRect(xBase + 2 * larguraBarra, altura - margem - alturaLucro, larguraBarra, alturaLucro);

                // Escrever a data abaixo
                g.setColor(Color.BLACK);
                g.drawString(dia.data, xBase, altura - margem + 15);
            }

            // Legenda
            g.setColor(new Color(66, 135, 245));
            g.fillRect(largura - margem - 150, margem, 20, 20);
            g.setColor(Color.BLACK);
            g.drawString("Faturamento", largura - margem - 120, margem + 15);

            g.setColor(new Color(245, 66, 66));
            g.fillRect(largura - margem - 150, margem + 30, 20, 20);
            g.setColor(Color.BLACK);
            g.drawString("Perda", largura - margem - 120, margem + 45);

            g.setColor(new Color(66, 245, 96));
            g.fillRect(largura - margem - 150, margem + 60, 20, 20);
            g.setColor(Color.BLACK);
            g.drawString("Lucro", largura - margem - 120, margem + 75);

            // Desenhar a logo no fundo (marca d'água)
            if (logo != null) {
                int logoLargura = logo.getWidth(this);
                int logoAltura = logo.getHeight(this);

                int xLogo = (largura - logoLargura) / 2;
                int yLogo = (altura - logoAltura) / 12;

                g.drawImage(logo, xLogo, yLogo, this);
            }
        }
    }
}
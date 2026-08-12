package New_MercadoSystem.controllers;

import New_MercadoSystem.models.Produto;
import New_MercadoSystem.models.Venda;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class GestaoController {

    private List<Venda> historicoVendas;
    private Map<LocalDate, Double> vendasPorDia;
    private EstoqueController estoqueController;

    public GestaoController(EstoqueController estoqueController) {
        this.estoqueController = estoqueController;
        this.historicoVendas = new ArrayList<>();
        this.vendasPorDia = new HashMap<>();

        inicializarDadosExemplo();
    }

    private void inicializarDadosExemplo() {
        // Criar algumas vendas de exemplo para os últimos 7 dias
        LocalDateTime hoje = LocalDateTime.now();
        Random random = new Random();

        for (int i = 0; i < 50; i++) {
            Venda venda = new Venda();
            LocalDateTime dataVenda = hoje.minusDays(random.nextInt(30));

            // Forçar a data da venda (isso exigiria um setter, mas para exemplo vamos só adicionar)
            // Em um sistema real, você teria um método setDataHora
            historicoVendas.add(venda);

            LocalDate dia = dataVenda.toLocalDate();
            double valorVenda = 50 + random.nextDouble() * 450;
            vendasPorDia.merge(dia, valorVenda, Double::sum);
        }
    }

    public Map<String, Object> getRelatorioVendasDia(LocalDate data) {
        Map<String, Object> relatorio = new HashMap<>();

        List<Venda> vendasDoDia = historicoVendas.stream()
                .filter(v -> v.getDataHora().toLocalDate().equals(data))
                .collect(Collectors.toList());

        int totalVendas = vendasDoDia.size();
        double faturamento = vendasDoDia.stream().mapToDouble(Venda::getTotal).sum();
        int totalItens = vendasDoDia.stream()
                .mapToInt(v -> v.getItens().size())
                .sum();

        relatorio.put("data", data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        relatorio.put("totalVendas", totalVendas);
        relatorio.put("faturamento", faturamento);
        relatorio.put("totalItens", totalItens);
        relatorio.put("ticketMedio", totalVendas > 0 ? faturamento / totalVendas : 0);

        return relatorio;
    }

    public Map<String, Object> getRelatorioSemanal() {
        Map<String, Object> relatorio = new HashMap<>();
        LocalDate hoje = LocalDate.now();

        double[] vendasPorDiaSemana = new double[7];
        String[] dias = {"Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"};

        for (int i = 0; i < 7; i++) {
            LocalDate dia = hoje.minusDays(6 - i);
            vendasPorDiaSemana[i] = vendasPorDia.getOrDefault(dia, 0.0);
        }

        relatorio.put("dias", dias);
        relatorio.put("valores", vendasPorDiaSemana);
        relatorio.put("totalSemana", Arrays.stream(vendasPorDiaSemana).sum());

        return relatorio;
    }

    public XYChart.Series<String, Number> getDadosGraficoVendas() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Vendas");

        LocalDate hoje = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

        for (int i = 6; i >= 0; i--) {
            LocalDate dia = hoje.minusDays(i);
            double valor = vendasPorDia.getOrDefault(dia, 0.0);
            series.getData().add(new XYChart.Data<>(dia.format(formatter), valor));
        }

        return series;
    }

    public List<Map<String, Object>> getRelatorioEstoqueBaixo() {
        List<Map<String, Object>> produtosCriticos = new ArrayList<>();

        List<Produto> estoqueBaixo = estoqueController.getProdutosEstoqueBaixo();
        List<Produto> semEstoque = estoqueController.getProdutosSemEstoque();

        for (Produto p : estoqueBaixo) {
            Map<String, Object> item = new HashMap<>();
            item.put("codigo", p.getCodigo());
            item.put("nome", p.getNome());
            item.put("quantidade", p.getQuantidadeEstoque());
            item.put("status", "BAIXO");
            produtosCriticos.add(item);
        }

        for (Produto p : semEstoque) {
            Map<String, Object> item = new HashMap<>();
            item.put("codigo", p.getCodigo());
            item.put("nome", p.getNome());
            item.put("quantidade", p.getQuantidadeEstoque());
            item.put("status", "ZERO");
            produtosCriticos.add(item);
        }

        return produtosCriticos;
    }

    public Map<String, List<Map<String, Object>>> getAnaliseCurvaABC() {
        Map<String, List<Map<String, Object>>> resultado = new HashMap<>();

        List<Produto> produtos = new ArrayList<>(estoqueController.getProdutosList());

        // Calcular faturamento potencial (preço * estoque)
        double totalFaturamento = produtos.stream()
                .mapToDouble(p -> p.getPreco() * p.getQuantidadeEstoque())
                .sum();

        // Ordenar por valor de estoque (decrescente)
        produtos.sort((p1, p2) ->
                Double.compare(p2.getPreco() * p2.getQuantidadeEstoque(),
                        p1.getPreco() * p1.getQuantidadeEstoque()));

        List<Map<String, Object>> classeA = new ArrayList<>();
        List<Map<String, Object>> classeB = new ArrayList<>();
        List<Map<String, Object>> classeC = new ArrayList<>();

        double acumulado = 0;
        for (Produto p : produtos) {
            double valorEstoque = p.getPreco() * p.getQuantidadeEstoque();
            double percentual = (valorEstoque / totalFaturamento) * 100;
            acumulado += percentual;

            Map<String, Object> item = new HashMap<>();
            item.put("codigo", p.getCodigo());
            item.put("nome", p.getNome());
            item.put("valor", valorEstoque);
            item.put("percentual", percentual);

            if (acumulado <= 80) {
                classeA.add(item);
            } else if (acumulado <= 95) {
                classeB.add(item);
            } else {
                classeC.add(item);
            }
        }

        resultado.put("A", classeA);
        resultado.put("B", classeB);
        resultado.put("C", classeC);

        return resultado;
    }

    public Map<String, Object> getDashboardIndicadores() {
        Map<String, Object> indicadores = new HashMap<>();

        LocalDate hoje = LocalDate.now();
        Map<String, Object> vendasHoje = getRelatorioVendasDia(hoje);

        indicadores.put("faturamentoHoje", vendasHoje.get("faturamento"));
        indicadores.put("vendasHoje", vendasHoje.get("totalVendas"));
        indicadores.put("ticketMedio", vendasHoje.get("ticketMedio"));

        int totalEstoque = estoqueController.getTotalItensEstoque();
        indicadores.put("totalEstoque", totalEstoque);

        double valorEstoque = estoqueController.getValorTotalEstoque();
        indicadores.put("valorEstoque", valorEstoque);

        int produtosBaixo = estoqueController.getProdutosEstoqueBaixo().size();
        indicadores.put("produtosBaixo", produtosBaixo);

        int produtosZero = estoqueController.getProdutosSemEstoque().size();
        indicadores.put("produtosZero", produtosZero);

        return indicadores;
    }

    public void registrarVenda(Venda venda) {
        if (venda != null && venda.isFinalizada()) {
            historicoVendas.add(venda);

            LocalDate data = venda.getDataHora().toLocalDate();
            vendasPorDia.merge(data, venda.getTotal(), Double::sum);
        }
    }

    public void mostrarAlerta(String titulo, String mensagem, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}
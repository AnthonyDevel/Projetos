package New_MercadoSystem.controllers;

import New_MercadoSystem.models.Produto;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.Alert;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class EstoqueController {

    private ObservableList<Produto> produtosList;
    private FilteredList<Produto> filteredData;
    private TableView<Produto> tabelaEstoque;
    private TextField txtBusca;

    public EstoqueController(TableView<Produto> tabelaEstoque, TextField txtBusca) {
        this.tabelaEstoque = tabelaEstoque;
        this.txtBusca = txtBusca;
        this.produtosList = FXCollections.observableArrayList();

        inicializarProdutos();
        configurarFiltro();
    }

    private void inicializarProdutos() {
        produtosList.addAll(
                new Produto("789001", "Arroz Tipo 1 (5kg)", 25.50, 150),
                new Produto("789002", "Feijão Carioca (1kg)", 8.90, 200),
                new Produto("789003", "Óleo de Soja (900ml)", 7.25, 300),
                new Produto("789004", "Leite Integral (1L)", 4.50, 15),
                new Produto("789005", "Refrigerante (2L)", 8.99, 0),
                new Produto("789006", "Café (500g)", 18.90, 45),
                new Produto("789007", "Açúcar (2kg)", 6.50, 120),
                new Produto("789008", "Farinha (1kg)", 5.80, 200),
                new Produto("789009", "Sabão em Pó (1kg)", 12.90, 80),
                new Produto("789010", "Detergente (500ml)", 2.50, 250)
        );

        filteredData = new FilteredList<>(produtosList, p -> true);
        tabelaEstoque.setItems(filteredData);
    }

    private void configurarFiltro() {
        txtBusca.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(produto -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();

                if (produto.getCodigo().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (produto.getNome().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (produto.getStatus().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }

                return false;
            });
        });
    }

    public void adicionarProduto(Produto produto) {
        if (produto != null) {
            produtosList.add(produto);
            ordenarPorNome();
        }
    }

    public void atualizarProduto(Produto produtoAntigo, Produto produtoNovo) {
        int index = produtosList.indexOf(produtoAntigo);
        if (index >= 0) {
            produtosList.set(index, produtoNovo);
            ordenarPorNome();
        }
    }

    public void removerProduto(Produto produto) {
        produtosList.remove(produto);
    }

    public Produto buscarProdutoPorCodigo(String codigo) {
        return produtosList.stream()
                .filter(p -> p.getCodigo().equals(codigo))
                .findFirst()
                .orElse(null);
    }

    public List<Produto> buscarProdutosPorNome(String nome) {
        return produtosList.stream()
                .filter(p -> p.getNome().toLowerCase().contains(nome.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Produto> getProdutosEstoqueBaixo() {
        return produtosList.stream()
                .filter(p -> p.getQuantidadeEstoque() < 10 && p.getQuantidadeEstoque() > 0)
                .collect(Collectors.toList());
    }

    public List<Produto> getProdutosSemEstoque() {
        return produtosList.stream()
                .filter(p -> p.getQuantidadeEstoque() <= 0)
                .collect(Collectors.toList());
    }

    public boolean entradaEstoque(String codigo, int quantidade) {
        Produto produto = buscarProdutoPorCodigo(codigo);
        if (produto != null && quantidade > 0) {
            produto.setQuantidadeEstoque(produto.getQuantidadeEstoque() + quantidade);
            tabelaEstoque.refresh();
            return true;
        }
        return false;
    }

    public boolean saidaEstoque(String codigo, int quantidade) {
        Produto produto = buscarProdutoPorCodigo(codigo);
        if (produto != null && quantidade > 0) {
            int novoEstoque = produto.getQuantidadeEstoque() - quantidade;
            if (novoEstoque >= 0) {
                produto.setQuantidadeEstoque(novoEstoque);
                tabelaEstoque.refresh();
                return true;
            }
        }
        return false;
    }

    public int getTotalItensEstoque() {
        return produtosList.stream()
                .mapToInt(Produto::getQuantidadeEstoque)
                .sum();
    }

    public double getValorTotalEstoque() {
        return produtosList.stream()
                .mapToDouble(p -> p.getPreco() * p.getQuantidadeEstoque())
                .sum();
    }

    public ObservableList<Produto> getProdutosList() {
        return produtosList;
    }

    public FilteredList<Produto> getFilteredData() {
        return filteredData;
    }

    private void ordenarPorNome() {
        produtosList.sort(Comparator.comparing(Produto::getNome));
    }

    public void limparFiltro() {
        txtBusca.clear();
    }

    public void mostrarAlerta(String titulo, String mensagem, Alert.AlertType tipo) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}
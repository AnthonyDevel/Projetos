package New_MercadoSystem.controllers;

import New_MercadoSystem.models.Produto;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ProdutosController {

    private ObservableList<Produto> produtosList;
    private TableView<Produto> tabelaProdutos;
    private EstoqueController estoqueController;

    public ProdutosController(TableView<Produto> tabelaProdutos, EstoqueController estoqueController) {
        this.tabelaProdutos = tabelaProdutos;
        this.estoqueController = estoqueController;
        this.produtosList = FXCollections.observableArrayList();

        // Inicializar com os mesmos produtos do estoque
        this.produtosList.addAll(estoqueController.getProdutosList());
        tabelaProdutos.setItems(produtosList);
    }

    public boolean salvarProduto(String codigo, String nome, String precoStr, String quantidadeStr) {
        // Validações
        if (codigo == null || codigo.trim().isEmpty()) {
            mostrarAlerta("Erro", "O código do produto é obrigatório!", Alert.AlertType.ERROR);
            return false;
        }

        if (nome == null || nome.trim().isEmpty()) {
            mostrarAlerta("Erro", "O nome do produto é obrigatório!", Alert.AlertType.ERROR);
            return false;
        }

        double preco;
        try {
            preco = Double.parseDouble(precoStr.trim().replace(",", "."));
            if (preco <= 0) {
                mostrarAlerta("Erro", "O preço deve ser maior que zero!", Alert.AlertType.ERROR);
                return false;
            }
        } catch (NumberFormatException e) {
            mostrarAlerta("Erro", "Preço inválido! Use apenas números.", Alert.AlertType.ERROR);
            return false;
        }

        int quantidade;
        try {
            quantidade = Integer.parseInt(quantidadeStr.trim());
            if (quantidade < 0) {
                mostrarAlerta("Erro", "A quantidade não pode ser negativa!", Alert.AlertType.ERROR);
                return false;
            }
        } catch (NumberFormatException e) {
            mostrarAlerta("Erro", "Quantidade inválida! Use apenas números inteiros.", Alert.AlertType.ERROR);
            return false;
        }

        // Verificar se código já existe
        Produto existente = buscarProdutoPorCodigo(codigo);
        if (existente != null) {
            // Atualizar produto existente
            existente.setNome(nome);
            existente.setPreco(preco);
            existente.setQuantidadeEstoque(quantidade);
            tabelaProdutos.refresh();
            mostrarAlerta("Sucesso", "Produto atualizado com sucesso!", Alert.AlertType.INFORMATION);
            return true;
        } else {
            // Criar novo produto
            Produto novoProduto = new Produto(codigo, nome, preco, quantidade);
            produtosList.add(novoProduto);
            estoqueController.adicionarProduto(novoProduto);
            ordenarPorNome();
            mostrarAlerta("Sucesso", "Produto cadastrado com sucesso!", Alert.AlertType.INFORMATION);
            return true;
        }
    }

    public boolean editarProduto(Produto produtoSelecionado, TextField txtCodigo, TextField txtNome,
                                 TextField txtPreco, TextField txtQuantidade) {
        if (produtoSelecionado == null) {
            mostrarAlerta("Aviso", "Selecione um produto para editar!", Alert.AlertType.WARNING);
            return false;
        }

        txtCodigo.setText(produtoSelecionado.getCodigo());
        txtNome.setText(produtoSelecionado.getNome());
        txtPreco.setText(String.valueOf(produtoSelecionado.getPreco()));
        txtQuantidade.setText(String.valueOf(produtoSelecionado.getQuantidadeEstoque()));

        txtCodigo.setEditable(false);

        return true;
    }

    public boolean excluirProduto(Produto produtoSelecionado) {
        if (produtoSelecionado == null) {
            mostrarAlerta("Aviso", "Selecione um produto para excluir!", Alert.AlertType.WARNING);
            return false;
        }

        produtosList.remove(produtoSelecionado);
        estoqueController.removerProduto(produtoSelecionado);

        return true;
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

    public ObservableList<Produto> getProdutosList() {
        return produtosList;
    }

    public void limparFormulario(TextField txtCodigo, TextField txtNome,
                                 TextField txtPreco, TextField txtQuantidade) {
        txtCodigo.clear();
        txtNome.clear();
        txtPreco.clear();
        txtQuantidade.clear();
        txtCodigo.setEditable(true);
    }

    private void ordenarPorNome() {
        produtosList.sort(Comparator.comparing(Produto::getNome));
    }

    public boolean validarPreco(String preco) {
        try {
            double valor = Double.parseDouble(preco.replace(",", "."));
            return valor > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean validarQuantidade(String quantidade) {
        try {
            int valor = Integer.parseInt(quantidade);
            return valor >= 0;
        } catch (NumberFormatException e) {
            return false;
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
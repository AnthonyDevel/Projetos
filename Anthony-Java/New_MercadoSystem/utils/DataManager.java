package New_MercadoSystem.utils;

import New_MercadoSystem.models.Produto;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class DataManager {

    private static DataManager instance;
    private ObservableList<Produto> produtos;

    private DataManager() {
        produtos = FXCollections.observableArrayList();
        inicializarProdutos();
    }

    public static DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    private void inicializarProdutos() {
        produtos.addAll(
                new Produto("789001", "Arroz Tipo 1 (5kg)", 25.50, 150, "Alimentos", "Fornecedor A"),
                new Produto("789002", "Feijão Carioca (1kg)", 8.90, 200, "Alimentos", "Fornecedor A"),
                new Produto("789003", "Óleo de Soja (900ml)", 7.25, 300, "Alimentos", "Fornecedor B"),
                new Produto("789004", "Leite Integral (1L)", 4.50, 15, "Laticínios", "Fornecedor C"),
                new Produto("789005", "Refrigerante (2L)", 8.99, 0, "Bebidas", "Fornecedor D"),
                new Produto("789006", "Café (500g)", 18.90, 45, "Alimentos", "Fornecedor E"),
                new Produto("789007", "Açúcar (2kg)", 6.50, 120, "Alimentos", "Fornecedor A"),
                new Produto("789008", "Farinha (1kg)", 5.80, 200, "Alimentos", "Fornecedor B"),
                new Produto("789009", "Sabão em Pó (1kg)", 12.90, 80, "Limpeza", "Fornecedor F"),
                new Produto("789010", "Detergente (500ml)", 2.50, 250, "Limpeza", "Fornecedor F"),
                new Produto("789011", "Macarrão (500g)", 4.20, 180, "Alimentos", "Fornecedor G"),
                new Produto("789012", "Molho de Tomate (340g)", 3.80, 220, "Alimentos", "Fornecedor G"),
                new Produto("789013", "Creme Dental (90g)", 5.90, 150, "Higiene", "Fornecedor H"),
                new Produto("789014", "Shampoo (350ml)", 12.50, 90, "Higiene", "Fornecedor H"),
                new Produto("789015", "Sabonete (90g)", 2.30, 300, "Higiene", "Fornecedor H")
        );
    }

    public ObservableList<Produto> getProdutos() {
        return produtos;
    }

    public Produto buscarProdutoPorCodigo(String codigo) {
        for (Produto p : produtos) {
            if (p.getCodigo().equals(codigo)) {
                return p;
            }
        }
        return null;
    }

    public Produto buscarProdutoPorNome(String nome) {
        for (Produto p : produtos) {
            if (p.getNome().toLowerCase().contains(nome.toLowerCase())) {
                return p;
            }
        }
        return null;
    }

    public void adicionarProduto(Produto produto) {
        produtos.add(produto);
    }

    public void removerProduto(Produto produto) {
        produtos.remove(produto);
    }

    public void atualizarProduto(Produto produtoAntigo, Produto produtoNovo) {
        int index = produtos.indexOf(produtoAntigo);
        if (index >= 0) {
            produtos.set(index, produtoNovo);
        }
    }

    public void removerProdutosSelecionados() {
        produtos.removeIf(Produto::isSelecionado);
    }

    public int getTotalProdutos() {
        return produtos.size();
    }

    public int getTotalItensEstoque() {
        return produtos.stream().mapToInt(Produto::getQuantidadeEstoque).sum();
    }

    public double getValorTotalEstoque() {
        return produtos.stream().mapToDouble(p -> p.getPreco() * p.getQuantidadeEstoque()).sum();
    }

    public long getProdutosEstoqueBaixo() {
        return produtos.stream().filter(p -> p.getQuantidadeEstoque() < 10 && p.getQuantidadeEstoque() > 0).count();
    }

    public long getProdutosSemEstoque() {
        return produtos.stream().filter(p -> p.getQuantidadeEstoque() <= 0).count();
    }
}
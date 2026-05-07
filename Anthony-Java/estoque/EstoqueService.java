package estoque;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class EstoqueService {

    private final List<Produto> produtos;
    private final AtomicInteger proximoId; // Para gerar IDs únicos

    public EstoqueService() {
        this.produtos = new ArrayList<>();
        this.proximoId = new AtomicInteger(1); // Começa a contagem de IDs em 1
        // Adicionar alguns dados iniciais (opcional)
        adicionarProdutoInicial(new Produto(0, "Laptop Dell", "Core i7, 16GB RAM", 5500.00, 10));
        adicionarProdutoInicial(new Produto(0, "Mouse Logitech", "Sem fio, ergonômico", 150.00, 50));
        adicionarProdutoInicial(new Produto(0, "Teclado Mecânico", "RGB, Switch Blue", 350.00, 25));
    }

    // Método auxiliar para dados iniciais, garantindo ID único
    private void adicionarProdutoInicial(Produto produto) {
        produto = new Produto(proximoId.getAndIncrement(), produto.getNome(), produto.getDescricao(), produto.getPreco(), produto.getQuantidade());
        this.produtos.add(produto);
    }


    // CREATE
    public Produto adicionarProduto(String nome, String descricao, double preco, int quantidade) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome do produto não pode ser vazio.");
        }
        if (preco < 0) {
            throw new IllegalArgumentException("Preço não pode ser negativo.");
        }
        if (quantidade < 0) {
            throw new IllegalArgumentException("Quantidade não pode ser negativa.");
        }

        int id = proximoId.getAndIncrement();
        Produto novoProduto = new Produto(id, nome, descricao, preco, quantidade);
        produtos.add(novoProduto);
        System.out.println("Produto adicionado: " + novoProduto); // Log
        return novoProduto;
    }

    // READ - Listar todos
    public List<Produto> listarProdutos() {
        // Retorna uma cópia para evitar modificações externas na lista original
        return new ArrayList<>(produtos);
    }

    // READ - Buscar por ID
    public Optional<Produto> buscarProdutoPorId(int id) {
        return produtos.stream()
                .filter(p -> p.getId() == id)
                .findFirst();
    }

    // UPDATE
    public boolean atualizarProduto(int id, String nome, String descricao, double preco, int quantidade) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome do produto não pode ser vazio.");
        }
        if (preco < 0) {
            throw new IllegalArgumentException("Preço não pode ser negativo.");
        }
        if (quantidade < 0) {
            throw new IllegalArgumentException("Quantidade não pode ser negativa.");
        }

        Optional<Produto> produtoOpt = buscarProdutoPorId(id);
        if (produtoOpt.isPresent()) {
            Produto produto = produtoOpt.get();
            produto.setNome(nome);
            produto.setDescricao(descricao);
            produto.setPreco(preco);
            produto.setQuantidade(quantidade);
            System.out.println("Produto atualizado: " + produto); // Log
            return true;
        }
        System.out.println("Falha ao atualizar: Produto com ID " + id + " não encontrado."); // Log
        return false; // Produto não encontrado
    }

    // DELETE
    public boolean removerProduto(int id) {
        boolean removido = produtos.removeIf(p -> p.getId() == id);
        if(removido) {
            System.out.println("Produto removido: ID " + id); // Log
        } else {
            System.out.println("Falha ao remover: Produto com ID " + id + " não encontrado."); // Log
        }
        return removido;
    }
}
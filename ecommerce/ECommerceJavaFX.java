package ecommerce;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class ECommerceJavaFX extends Application {

    // ---------- Modelos (entidades) ----------
    public static class Produto implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private String nome;
        private String descricao;
        private BigDecimal preco;
        private Integer quantidadeEstoque;
        private String categoria;
        private LocalDateTime dataCadastro;
        private LocalDateTime dataAtualizacao;
        private boolean ativo;
        private String imagemPath; // NOVO: caminho para a imagem PNG

        public Produto() {
            this.dataCadastro = LocalDateTime.now();
            this.ativo = true;
            this.imagemPath = "imagens/default.png"; // imagem padrão
        }

        public Produto(Long id, String nome, String descricao, BigDecimal preco,
                       Integer quantidadeEstoque, String categoria, String imagemPath) {
            this();
            this.id = id;
            this.nome = nome;
            this.descricao = descricao;
            this.preco = preco;
            this.quantidadeEstoque = quantidadeEstoque;
            this.categoria = categoria;
            this.imagemPath = imagemPath != null ? imagemPath : "imagens/default.png";
        }

        // Getters e setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public String getDescricao() { return descricao; }
        public void setDescricao(String descricao) { this.descricao = descricao; }
        public BigDecimal getPreco() { return preco; }
        public void setPreco(BigDecimal preco) { this.preco = preco; }
        public Integer getQuantidadeEstoque() { return quantidadeEstoque; }
        public void setQuantidadeEstoque(Integer quantidadeEstoque) { this.quantidadeEstoque = quantidadeEstoque; }
        public String getCategoria() { return categoria; }
        public void setCategoria(String categoria) { this.categoria = categoria; }
        public LocalDateTime getDataCadastro() { return dataCadastro; }
        public void setDataCadastro(LocalDateTime dataCadastro) { this.dataCadastro = dataCadastro; }
        public LocalDateTime getDataAtualizacao() { return dataAtualizacao; }
        public void setDataAtualizacao(LocalDateTime dataAtualizacao) { this.dataAtualizacao = dataAtualizacao; }
        public boolean isAtivo() { return ativo; }
        public void setAtivo(boolean ativo) { this.ativo = ativo; }
        public String getImagemPath() { return imagemPath; }
        public void setImagemPath(String imagemPath) { this.imagemPath = imagemPath; }

        @Override
        public String toString() {
            return String.format("%s - R$ %.2f (Estoque: %d)", nome, preco, quantidadeEstoque);
        }
    }

    public static class Cliente implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private String nome;
        private String email;
        private String senha; // NOVO: campo para autenticação
        private String telefone;
        private String endereco;
        private String cidade;
        private String estado;
        private String cep;
        private LocalDateTime dataCadastro;
        private List<Pedido> historicoPedidos;

        public Cliente() {
            this.dataCadastro = LocalDateTime.now();
            this.historicoPedidos = new ArrayList<>();
        }

        public Cliente(Long id, String nome, String email, String senha, String telefone,
                       String endereco, String cidade, String estado, String cep) {
            this();
            this.id = id;
            this.nome = nome;
            this.email = email;
            this.senha = senha;
            this.telefone = telefone;
            this.endereco = endereco;
            this.cidade = cidade;
            this.estado = estado;
            this.cep = cep;
        }

        // Getters e setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getSenha() { return senha; }
        public void setSenha(String senha) { this.senha = senha; }
        public String getTelefone() { return telefone; }
        public void setTelefone(String telefone) { this.telefone = telefone; }
        public String getEndereco() { return endereco; }
        public void setEndereco(String endereco) { this.endereco = endereco; }
        public String getCidade() { return cidade; }
        public void setCidade(String cidade) { this.cidade = cidade; }
        public String getEstado() { return estado; }
        public void setEstado(String estado) { this.estado = estado; }
        public String getCep() { return cep; }
        public void setCep(String cep) { this.cep = cep; }
        public LocalDateTime getDataCadastro() { return dataCadastro; }
        public void setDataCadastro(LocalDateTime dataCadastro) { this.dataCadastro = dataCadastro; }
        public List<Pedido> getHistoricoPedidos() { return historicoPedidos; }
        public void setHistoricoPedidos(List<Pedido> historicoPedidos) { this.historicoPedidos = historicoPedidos; }

        public void adicionarPedido(Pedido pedido) {
            this.historicoPedidos.add(pedido);
        }

        @Override
        public String toString() {
            return nome + " (" + email + ")";
        }
    }

    public static class ItemPedido implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private Produto produto;
        private Integer quantidade;
        private BigDecimal precoUnitario;
        private BigDecimal subtotal;

        public ItemPedido() {}

        public ItemPedido(Long id, Produto produto, Integer quantidade) {
            this.id = id;
            this.produto = produto;
            this.quantidade = quantidade;
            this.precoUnitario = produto.getPreco();
            this.subtotal = produto.getPreco().multiply(BigDecimal.valueOf(quantidade));
        }

        // Getters e setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Produto getProduto() { return produto; }
        public void setProduto(Produto produto) {
            this.produto = produto;
            this.precoUnitario = produto.getPreco();
            calcularSubtotal();
        }
        public Integer getQuantidade() { return quantidade; }
        public void setQuantidade(Integer quantidade) {
            this.quantidade = quantidade;
            calcularSubtotal();
        }
        public BigDecimal getPrecoUnitario() { return precoUnitario; }
        public void setPrecoUnitario(BigDecimal precoUnitario) {
            this.precoUnitario = precoUnitario;
            calcularSubtotal();
        }
        public BigDecimal getSubtotal() { return subtotal; }

        private void calcularSubtotal() {
            if (precoUnitario != null && quantidade != null) {
                this.subtotal = precoUnitario.multiply(BigDecimal.valueOf(quantidade));
            }
        }
    }

    public enum StatusPedido implements Serializable {
        AGUARDANDO_PAGAMENTO, PAGO, EM_PREPARACAO, ENVIADO, ENTREGUE, CANCELADO
    }

    public static class Pedido implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private Cliente cliente;
        private List<ItemPedido> itens;
        private LocalDateTime dataPedido;
        private StatusPedido status;
        private BigDecimal valorTotal;
        private String enderecoEntrega;
        private LocalDateTime dataEntrega;
        // NOVOS campos
        private String metodoPagamento;
        private String horarioEntrega;
        private LocalDateTime dataEntregaEstimada;

        public Pedido() {
            this.itens = new ArrayList<>();
            this.dataPedido = LocalDateTime.now();
            this.status = StatusPedido.AGUARDANDO_PAGAMENTO;
            this.valorTotal = BigDecimal.ZERO;
        }

        public Pedido(Long id, Cliente cliente, String enderecoEntrega) {
            this();
            this.id = id;
            this.cliente = cliente;
            this.enderecoEntrega = enderecoEntrega;
        }

        // Getters e setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Cliente getCliente() { return cliente; }
        public void setCliente(Cliente cliente) { this.cliente = cliente; }
        public List<ItemPedido> getItens() { return itens; }
        public void setItens(List<ItemPedido> itens) {
            this.itens = itens;
            calcularValorTotal();
        }
        public LocalDateTime getDataPedido() { return dataPedido; }
        public void setDataPedido(LocalDateTime dataPedido) { this.dataPedido = dataPedido; }
        public StatusPedido getStatus() { return status; }
        public void setStatus(StatusPedido status) { this.status = status; }
        public BigDecimal getValorTotal() { return valorTotal; }
        public String getEnderecoEntrega() { return enderecoEntrega; }
        public void setEnderecoEntrega(String enderecoEntrega) { this.enderecoEntrega = enderecoEntrega; }
        public LocalDateTime getDataEntrega() { return dataEntrega; }
        public void setDataEntrega(LocalDateTime dataEntrega) { this.dataEntrega = dataEntrega; }
        public String getMetodoPagamento() { return metodoPagamento; }
        public void setMetodoPagamento(String metodoPagamento) { this.metodoPagamento = metodoPagamento; }
        public String getHorarioEntrega() { return horarioEntrega; }
        public void setHorarioEntrega(String horarioEntrega) { this.horarioEntrega = horarioEntrega; }
        public LocalDateTime getDataEntregaEstimada() { return dataEntregaEstimada; }
        public void setDataEntregaEstimada(LocalDateTime dataEntregaEstimada) { this.dataEntregaEstimada = dataEntregaEstimada; }

        public void adicionarItem(ItemPedido item) {
            this.itens.add(item);
            calcularValorTotal();
        }

        public void removerItem(ItemPedido item) {
            this.itens.remove(item);
            calcularValorTotal();
        }

        private void calcularValorTotal() {
            this.valorTotal = itens.stream()
                    .map(ItemPedido::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public boolean processarPagamento() {
            if (status == StatusPedido.AGUARDANDO_PAGAMENTO) {
                this.status = StatusPedido.PAGO;
                return true;
            }
            return false;
        }

        public boolean enviarPedido() {
            if (status == StatusPedido.PAGO || status == StatusPedido.EM_PREPARACAO) {
                this.status = StatusPedido.ENVIADO;
                return true;
            }
            return false;
        }

        public boolean entregarPedido() {
            if (status == StatusPedido.ENVIADO) {
                this.status = StatusPedido.ENTREGUE;
                this.dataEntrega = LocalDateTime.now();
                return true;
            }
            return false;
        }
    }

    // ---------- Repositórios (singleton com persistência em arquivo) ----------
    public static class ProdutoRepository {
        private static final String FILE_NAME = "produtos.dat";
        private Map<Long, Produto> produtos;
        private AtomicLong currentId;
        private static ProdutoRepository instance;

        private ProdutoRepository() {
            produtos = new HashMap<>();
            currentId = new AtomicLong(1);
            carregarDados();
        }

        public static synchronized ProdutoRepository getInstance() {
            if (instance == null) {
                instance = new ProdutoRepository();
            }
            return instance;
        }

        public Produto salvar(Produto produto) {
            if (produto.getId() == null) {
                produto.setId(currentId.getAndIncrement());
            }
            produto.setDataAtualizacao(LocalDateTime.now());
            produtos.put(produto.getId(), produto);
            salvarDados();
            return produto;
        }

        public Optional<Produto> buscarPorId(Long id) {
            return Optional.ofNullable(produtos.get(id));
        }

        public List<Produto> buscarTodos() {
            return new ArrayList<>(produtos.values());
        }

        public List<Produto> buscarAtivos() {
            List<Produto> ativos = new ArrayList<>();
            for (Produto p : produtos.values()) {
                if (p.isAtivo()) ativos.add(p);
            }
            return ativos;
        }

        public void deletar(Long id) {
            produtos.remove(id);
            salvarDados();
        }

        public void deletarLogico(Long id) {
            Produto p = produtos.get(id);
            if (p != null) {
                p.setAtivo(false);
                salvarDados();
            }
        }

        public boolean existe(Long id) {
            return produtos.containsKey(id);
        }

        public boolean atualizarEstoque(Long id, Integer quantidade) {
            Produto p = produtos.get(id);
            if (p != null && p.getQuantidadeEstoque() >= quantidade) {
                p.setQuantidadeEstoque(p.getQuantidadeEstoque() - quantidade);
                salvarDados();
                return true;
            }
            return false;
        }

        @SuppressWarnings("unchecked")
        private void carregarDados() {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_NAME))) {
                produtos = (Map<Long, Produto>) ois.readObject();
                long maxId = produtos.keySet().stream().max(Long::compareTo).orElse(0L);
                currentId = new AtomicLong(maxId + 1);
            } catch (FileNotFoundException e) {
                produtos = new HashMap<>();
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Erro ao carregar produtos: " + e.getMessage());
                produtos = new HashMap<>();
            }
        }

        private void salvarDados() {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
                oos.writeObject(produtos);
            } catch (IOException e) {
                System.err.println("Erro ao salvar produtos: " + e.getMessage());
            }
        }
    }

    public static class ClienteRepository {
        private static final String FILE_NAME = "clientes.dat";
        private Map<Long, Cliente> clientes;
        private AtomicLong currentId;
        private static ClienteRepository instance;

        private ClienteRepository() {
            clientes = new HashMap<>();
            currentId = new AtomicLong(1);
            carregarDados();
        }

        public static synchronized ClienteRepository getInstance() {
            if (instance == null) {
                instance = new ClienteRepository();
            }
            return instance;
        }

        public Cliente salvar(Cliente cliente) {
            if (cliente.getId() == null) {
                cliente.setId(currentId.getAndIncrement());
            }
            clientes.put(cliente.getId(), cliente);
            salvarDados();
            return cliente;
        }

        public Optional<Cliente> buscarPorId(Long id) {
            return Optional.ofNullable(clientes.get(id));
        }

        public Optional<Cliente> buscarPorEmail(String email) {
            return clientes.values().stream()
                    .filter(c -> c.getEmail().equalsIgnoreCase(email))
                    .findFirst();
        }

        public List<Cliente> buscarTodos() {
            return new ArrayList<>(clientes.values());
        }

        public void deletar(Long id) {
            clientes.remove(id);
            salvarDados();
        }

        @SuppressWarnings("unchecked")
        private void carregarDados() {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_NAME))) {
                clientes = (Map<Long, Cliente>) ois.readObject();
                long maxId = clientes.keySet().stream().max(Long::compareTo).orElse(0L);
                currentId = new AtomicLong(maxId + 1);
            } catch (FileNotFoundException e) {
                clientes = new HashMap<>();
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Erro ao carregar clientes: " + e.getMessage());
                clientes = new HashMap<>();
            }
        }

        private void salvarDados() {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
                oos.writeObject(clientes);
            } catch (IOException e) {
                System.err.println("Erro ao salvar clientes: " + e.getMessage());
            }
        }
    }

    public static class PedidoRepository {
        private static final String FILE_NAME = "pedidos.dat";
        private Map<Long, Pedido> pedidos;
        private AtomicLong currentId;
        private static PedidoRepository instance;

        private PedidoRepository() {
            pedidos = new HashMap<>();
            currentId = new AtomicLong(1);
            carregarDados();
        }

        public static synchronized PedidoRepository getInstance() {
            if (instance == null) {
                instance = new PedidoRepository();
            }
            return instance;
        }

        public Pedido salvar(Pedido pedido) {
            if (pedido.getId() == null) {
                pedido.setId(currentId.getAndIncrement());
            }
            pedidos.put(pedido.getId(), pedido);
            // Atualiza também no histórico do cliente
            Cliente cliente = pedido.getCliente();
            if (cliente != null) {
                cliente.getHistoricoPedidos().removeIf(p -> p.getId().equals(pedido.getId()));
                cliente.getHistoricoPedidos().add(pedido);
                ClienteRepository.getInstance().salvar(cliente); // persiste cliente com histórico
            }
            salvarDados();
            return pedido;
        }

        public Optional<Pedido> buscarPorId(Long id) {
            return Optional.ofNullable(pedidos.get(id));
        }

        public List<Pedido> buscarTodos() {
            return new ArrayList<>(pedidos.values());
        }

        public List<Pedido> buscarPorCliente(Long clienteId) {
            List<Pedido> resultado = new ArrayList<>();
            for (Pedido p : pedidos.values()) {
                if (p.getCliente().getId().equals(clienteId)) {
                    resultado.add(p);
                }
            }
            return resultado;
        }

        public List<Pedido> buscarPorStatus(StatusPedido status) {
            List<Pedido> resultado = new ArrayList<>();
            for (Pedido p : pedidos.values()) {
                if (p.getStatus() == status) {
                    resultado.add(p);
                }
            }
            return resultado;
        }

        public void deletar(Long id) {
            pedidos.remove(id);
            salvarDados();
        }

        public boolean existe(Long id) {
            return pedidos.containsKey(id);
        }

        @SuppressWarnings("unchecked")
        private void carregarDados() {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_NAME))) {
                pedidos = (Map<Long, Pedido>) ois.readObject();
                long maxId = pedidos.keySet().stream().max(Long::compareTo).orElse(0L);
                currentId = new AtomicLong(maxId + 1);
            } catch (FileNotFoundException e) {
                pedidos = new HashMap<>();
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Erro ao carregar pedidos: " + e.getMessage());
                pedidos = new HashMap<>();
            }
        }

        private void salvarDados() {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
                oos.writeObject(pedidos);
            } catch (IOException e) {
                System.err.println("Erro ao salvar pedidos: " + e.getMessage());
            }
        }
    }

    // ---------- Serviço de Vendas ----------
    public static class VendaService {
        private ProdutoRepository produtoRepository;
        private ClienteRepository clienteRepository;
        private PedidoRepository pedidoRepository;
        private static VendaService instance;

        private VendaService() {
            produtoRepository = ProdutoRepository.getInstance();
            clienteRepository = ClienteRepository.getInstance();
            pedidoRepository = PedidoRepository.getInstance();
        }

        public static synchronized VendaService getInstance() {
            if (instance == null) {
                instance = new VendaService();
            }
            return instance;
        }

        // Produtos
        public Produto adicionarProduto(String nome, String descricao, BigDecimal preco,
                                        Integer quantidade, String categoria, String imagemPath) {
            if (preco.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Preço deve ser maior que zero");
            }
            if (quantidade < 0) {
                throw new IllegalArgumentException("Quantidade não pode ser negativa");
            }
            Produto produto = new Produto(null, nome, descricao, preco, quantidade, categoria, imagemPath);
            return produtoRepository.salvar(produto);
        }

        public List<Produto> listarProdutosAtivos() {
            return produtoRepository.buscarAtivos();
        }

        public Optional<Produto> buscarProduto(Long id) {
            return produtoRepository.buscarPorId(id);
        }

        public void removerProduto(Long id) {
            produtoRepository.deletarLogico(id);
        }

        // Clientes
        public Cliente cadastrarCliente(String nome, String email, String senha, String telefone,
                                        String endereco, String cidade, String estado, String cep) {
            Optional<Cliente> existente = clienteRepository.buscarPorEmail(email);
            if (existente.isPresent()) {
                throw new IllegalArgumentException("Email já cadastrado");
            }
            Cliente cliente = new Cliente(null, nome, email, senha, telefone, endereco, cidade, estado, cep);
            return clienteRepository.salvar(cliente);
        }

        public Optional<Cliente> autenticar(String email, String senha) {
            return clienteRepository.buscarPorEmail(email)
                    .filter(c -> c.getSenha().equals(senha));
        }

        public List<Cliente> listarClientes() {
            return clienteRepository.buscarTodos();
        }

        public Optional<Cliente> buscarCliente(Long id) {
            return clienteRepository.buscarPorId(id);
        }

        // Pedidos
        public Pedido criarPedido(Long clienteId, String enderecoEntrega) {
            Cliente cliente = clienteRepository.buscarPorId(clienteId)
                    .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
            Pedido pedido = new Pedido(null, cliente, enderecoEntrega);
            return pedidoRepository.salvar(pedido);
        }

        public Pedido adicionarItemAoCarrinho(Long pedidoId, Long produtoId, Integer quantidade) {
            Pedido pedido = pedidoRepository.buscarPorId(pedidoId)
                    .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
            Produto produto = produtoRepository.buscarPorId(produtoId)
                    .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

            if (produto.getQuantidadeEstoque() < quantidade) {
                throw new RuntimeException("Estoque insuficiente");
            }
            if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
                throw new RuntimeException("Não é possível modificar um pedido já processado");
            }

            // Gerar ID para o item (simples)
            Long itemId = System.currentTimeMillis() + new Random().nextInt(1000);
            ItemPedido item = new ItemPedido(itemId, produto, quantidade);
            pedido.adicionarItem(item);
            return pedidoRepository.salvar(pedido);
        }

        public Pedido removerItemDoCarrinho(Long pedidoId, Long itemId) {
            Pedido pedido = pedidoRepository.buscarPorId(pedidoId)
                    .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
            if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
                throw new RuntimeException("Não é possível modificar um pedido já processado");
            }
            pedido.getItens().removeIf(item -> item.getId().equals(itemId));
            return pedidoRepository.salvar(pedido);
        }

        public Pedido finalizarCompra(Long pedidoId, String metodoPagamento, String horarioEntrega) {
            Pedido pedido = pedidoRepository.buscarPorId(pedidoId)
                    .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
            if (pedido.getItens().isEmpty()) {
                throw new RuntimeException("Carrinho vazio");
            }
            // Verificar e atualizar estoque
            for (ItemPedido item : pedido.getItens()) {
                boolean ok = produtoRepository.atualizarEstoque(item.getProduto().getId(), item.getQuantidade());
                if (!ok) {
                    throw new RuntimeException("Estoque insuficiente para: " + item.getProduto().getNome());
                }
            }

            // Definir método de pagamento e horário de entrega
            pedido.setMetodoPagamento(metodoPagamento);
            pedido.setHorarioEntrega(horarioEntrega);

            // Calcular data estimada de entrega (5 dias úteis após a compra)
            LocalDateTime hoje = LocalDateTime.now();
            LocalDateTime entregaEstimada = hoje.plusDays(5); // Simplificado - em um sistema real, pularia fins de semana
            pedido.setDataEntregaEstimada(entregaEstimada);

            pedido.processarPagamento();
            return pedidoRepository.salvar(pedido);
        }

        // Entrega
        public Pedido atualizarStatusEntrega(Long pedidoId, StatusPedido novoStatus) {
            Pedido pedido = pedidoRepository.buscarPorId(pedidoId)
                    .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));
            if (novoStatus == StatusPedido.ENVIADO) {
                pedido.enviarPedido();
            } else if (novoStatus == StatusPedido.ENTREGUE) {
                pedido.entregarPedido();
            } else {
                pedido.setStatus(novoStatus);
            }
            return pedidoRepository.salvar(pedido);
        }

        public List<Pedido> listarPedidosCliente(Long clienteId) {
            return pedidoRepository.buscarPorCliente(clienteId);
        }

        public List<Pedido> listarPedidosPorStatus(StatusPedido status) {
            return pedidoRepository.buscarPorStatus(status);
        }
    }

    // ---------- JavaFX Application ----------
    private Stage primaryStage;
    private VendaService service = VendaService.getInstance();
    private Cliente clienteLogado;
    private Pedido carrinhoAtual; // pedido em andamento (aguardando pagamento)

    // Observables
    private ObservableList<Produto> produtosObservable = FXCollections.observableArrayList();
    private ObservableList<Pedido> pedidosObservable = FXCollections.observableArrayList();
    private ObservableList<ItemPedido> itensCarrinhoObservable = FXCollections.observableArrayList();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("Sistema de E-Commerce Moderno");
        primaryStage.setWidth(1000);
        primaryStage.setHeight(700);

        // Inicializar dados de exemplo
        inicializarDadosExemplo();

        // Mostrar tela de login
        mostrarTelaLogin();
        primaryStage.show();
    }

    private void inicializarDadosExemplo() {
        // Adicionar produtos de exemplo com imagens
        if (service.listarProdutosAtivos().isEmpty()) {
            service.adicionarProduto("Notebook Dell", "Notebook Dell Inspiron 15, 8GB RAM, SSD 256GB",
                    new BigDecimal("3500.00"), 10, "Eletrônicos", "imagens/notebook.png");
            service.adicionarProduto("Smartphone Samsung", "Samsung Galaxy S21, 128GB, Câmera 64MP",
                    new BigDecimal("2800.00"), 15, "Eletrônicos", "imagens/smartphone.png");
            service.adicionarProduto("Camiseta Polo", "Camiseta Polo Preta, Algodão, Tamanho M",
                    new BigDecimal("89.90"), 50, "Vestuário", "imagens/camiseta.png");
            service.adicionarProduto("Livro Java", "Java: Como Programar, Deitel, 10ª Edição",
                    new BigDecimal("250.00"), 30, "Livros", "imagens/livro.png");
            service.adicionarProduto("Fone de Ouvido", "Fone Bluetooth, Cancelamento de Ruído",
                    new BigDecimal("199.90"), 25, "Eletrônicos", "imagens/fone.png");
            service.adicionarProduto("Tênis Esportivo", "Tênis para corrida, Tamanho 40",
                    new BigDecimal("299.90"), 20, "Calçados", "imagens/tenis.png");
        }

        // Adicionar cliente exemplo se não houver nenhum
        if (service.listarClientes().isEmpty()) {
            service.cadastrarCliente("João Silva", "joao@email.com", "123456", "(11)99999-9999",
                    "Rua das Flores, 123", "São Paulo", "SP", "01234-567");
            service.cadastrarCliente("Maria Souza", "maria@email.com", "123456", "(11)88888-8888",
                    "Av. Paulista, 1000", "São Paulo", "SP", "01310-100");
        }
    }

    private void mostrarTelaLogin() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Aba de Login
        Tab tabLogin = new Tab("Entrar");
        GridPane loginGrid = new GridPane();
        loginGrid.setHgap(10);
        loginGrid.setVgap(10);
        loginGrid.setPadding(new Insets(20));
        loginGrid.setAlignment(Pos.CENTER);

        TextField tfEmailLogin = new TextField();
        tfEmailLogin.setPromptText("email@exemplo.com");
        PasswordField pfSenhaLogin = new PasswordField();
        pfSenhaLogin.setPromptText("Sua senha");

        Button btnLogin = new Button("Entrar");
        btnLogin.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px;");
        btnLogin.setPrefWidth(200);

        Label lblMensagemLogin = new Label();
        lblMensagemLogin.setStyle("-fx-text-fill: red;");

        btnLogin.setOnAction(e -> {
            String email = tfEmailLogin.getText();
            String senha = pfSenhaLogin.getText();

            Optional<Cliente> clienteOpt = service.autenticar(email, senha);
            if (clienteOpt.isPresent()) {
                clienteLogado = clienteOpt.get();
                carrinhoAtual = service.criarPedido(clienteLogado.getId(), clienteLogado.getEndereco());
                mostrarTelaPrincipal();
            } else {
                lblMensagemLogin.setText("E-mail ou senha inválidos!");
            }
        });

        loginGrid.add(new Label("E-mail:"), 0, 0);
        loginGrid.add(tfEmailLogin, 1, 0);
        loginGrid.add(new Label("Senha:"), 0, 1);
        loginGrid.add(pfSenhaLogin, 1, 1);
        loginGrid.add(btnLogin, 1, 2);
        loginGrid.add(lblMensagemLogin, 1, 3);

        tabLogin.setContent(loginGrid);

        // Aba de Registro
        Tab tabRegistro = new Tab("Criar Conta");
        GridPane registroGrid = new GridPane();
        registroGrid.setHgap(10);
        registroGrid.setVgap(10);
        registroGrid.setPadding(new Insets(20));
        registroGrid.setAlignment(Pos.CENTER);

        TextField tfNome = new TextField();
        tfNome.setPromptText("Seu nome completo");
        TextField tfEmail = new TextField();
        tfEmail.setPromptText("email@exemplo.com");
        PasswordField pfSenha = new PasswordField();
        pfSenha.setPromptText("Crie uma senha");
        PasswordField pfConfirmaSenha = new PasswordField();
        pfConfirmaSenha.setPromptText("Confirme sua senha");
        TextField tfTelefone = new TextField();
        tfTelefone.setPromptText("(11) 99999-9999");
        TextField tfEndereco = new TextField();
        tfEndereco.setPromptText("Rua, número");
        TextField tfCidade = new TextField();
        tfCidade.setPromptText("Cidade");
        TextField tfEstado = new TextField();
        tfEstado.setPromptText("UF");
        TextField tfCep = new TextField();
        tfCep.setPromptText("00000-000");

        Button btnRegistrar = new Button("Criar Conta");
        btnRegistrar.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 14px;");
        btnRegistrar.setPrefWidth(200);

        Label lblMensagemRegistro = new Label();
        lblMensagemRegistro.setStyle("-fx-text-fill: red;");

        btnRegistrar.setOnAction(e -> {
            if (tfNome.getText().isEmpty() || tfEmail.getText().isEmpty() ||
                    pfSenha.getText().isEmpty() || tfTelefone.getText().isEmpty()) {
                lblMensagemRegistro.setText("Preencha todos os campos obrigatórios!");
                return;
            }

            if (!pfSenha.getText().equals(pfConfirmaSenha.getText())) {
                lblMensagemRegistro.setText("As senhas não conferem!");
                return;
            }

            try {
                Cliente cliente = service.cadastrarCliente(
                        tfNome.getText(),
                        tfEmail.getText(),
                        pfSenha.getText(),
                        tfTelefone.getText(),
                        tfEndereco.getText(),
                        tfCidade.getText(),
                        tfEstado.getText(),
                        tfCep.getText());

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Sucesso");
                alert.setHeaderText(null);
                alert.setContentText("Conta criada com sucesso! Faça o login para continuar.");
                alert.showAndWait();

                // Voltar para a aba de login
                tabPane.getSelectionModel().select(tabLogin);

            } catch (Exception ex) {
                lblMensagemRegistro.setText("Erro: " + ex.getMessage());
            }
        });

        int row = 0;
        registroGrid.add(new Label("Nome:*"), 0, row); registroGrid.add(tfNome, 1, row++);
        registroGrid.add(new Label("E-mail:*"), 0, row); registroGrid.add(tfEmail, 1, row++);
        registroGrid.add(new Label("Senha:*"), 0, row); registroGrid.add(pfSenha, 1, row++);
        registroGrid.add(new Label("Confirmar Senha:*"), 0, row); registroGrid.add(pfConfirmaSenha, 1, row++);
        registroGrid.add(new Label("Telefone:*"), 0, row); registroGrid.add(tfTelefone, 1, row++);
        registroGrid.add(new Label("Endereço:"), 0, row); registroGrid.add(tfEndereco, 1, row++);
        registroGrid.add(new Label("Cidade:"), 0, row); registroGrid.add(tfCidade, 1, row++);
        registroGrid.add(new Label("Estado:"), 0, row); registroGrid.add(tfEstado, 1, row++);
        registroGrid.add(new Label("CEP:"), 0, row); registroGrid.add(tfCep, 1, row++);
        registroGrid.add(btnRegistrar, 1, row++);
        registroGrid.add(lblMensagemRegistro, 1, row);

        tabRegistro.setContent(registroGrid);

        tabPane.getTabs().addAll(tabLogin, tabRegistro);

        VBox vbox = new VBox(20);
        vbox.setAlignment(Pos.CENTER);
        vbox.getChildren().addAll(new Label("Bem-vindo ao E-Commerce"), tabPane);

        primaryStage.setScene(new Scene(vbox));
    }

    private void mostrarTelaPrincipal() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #f4f4f4;");

        // Menu superior
        MenuBar menuBar = new MenuBar();
        menuBar.setStyle("-fx-background-color: #f4f4f4; -fx-text-fill: white;");

        Menu menuCliente = new Menu("Cliente");
        MenuItem itemPerfil = new MenuItem("Meu Perfil");
        itemPerfil.setOnAction(e -> mostrarPerfil(root));
        MenuItem itemTrocarCliente = new MenuItem("Trocar Conta");
        itemTrocarCliente.setOnAction(e -> mostrarTelaLogin());
        MenuItem itemSair = new MenuItem("Sair");
        itemSair.setOnAction(e -> Platform.exit());
        menuCliente.getItems().addAll(itemPerfil, new SeparatorMenuItem(), itemTrocarCliente, itemSair);

        Menu menuProdutos = new Menu("Produtos");
        MenuItem itemListarProdutos = new MenuItem("Ver Produtos");
        itemListarProdutos.setOnAction(e -> mostrarListaProdutos(root));
        menuProdutos.getItems().add(itemListarProdutos);

        Menu menuPedidos = new Menu("Pedidos");
        MenuItem itemMeusPedidos = new MenuItem("Meus Pedidos");
        itemMeusPedidos.setOnAction(e -> mostrarMeusPedidos(root));
        MenuItem itemCarrinho = new MenuItem("Ver Carrinho");
        itemCarrinho.setOnAction(e -> mostrarCarrinho(root));
        menuPedidos.getItems().addAll(itemMeusPedidos, itemCarrinho);

        // Menu Admin (visível apenas para admins - simplificado)
        Menu menuAdmin = new Menu("Admin");
        MenuItem itemAdicionarProduto = new MenuItem("Adicionar Produto");
        itemAdicionarProduto.setOnAction(e -> mostrarFormAdicionarProduto(root));
        menuAdmin.getItems().add(itemAdicionarProduto);

        menuBar.getMenus().addAll(menuCliente, menuProdutos, menuPedidos, menuAdmin);
        root.setTop(menuBar);

        // Área central com boas-vindas e resumo
        VBox welcomeBox = new VBox(20);
        welcomeBox.setAlignment(Pos.CENTER);

        Label bemVindo = new Label("Olá, " + clienteLogado.getNome() + "!");
        bemVindo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #333;");

        Label resumo = new Label("Bem-vindo ao nosso e-commerce. Explore nossos produtos e faça suas compras!");
        resumo.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

        Button btnVerProdutos = new Button("Ver Produtos");
        btnVerProdutos.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");
        btnVerProdutos.setOnAction(e -> mostrarListaProdutos(root));

        welcomeBox.getChildren().addAll(bemVindo, resumo, btnVerProdutos);
        root.setCenter(welcomeBox);

        primaryStage.setScene(new Scene(root));
    }

    private void mostrarListaProdutos(BorderPane root) {
        produtosObservable.setAll(service.listarProdutosAtivos());

        // Usar TilePane para mostrar produtos em cards
        TilePane tilePane = new TilePane();
        tilePane.setPadding(new Insets(10));
        tilePane.setHgap(15);
        tilePane.setVgap(15);
        tilePane.setPrefColumns(3);

        for (Produto produto : produtosObservable) {
            VBox card = criarCardProduto(produto);
            tilePane.getChildren().add(card);
        }

        ScrollPane scrollPane = new ScrollPane(tilePane);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        Button btnVoltar = new Button("Voltar");
        btnVoltar.setStyle("-fx-background-color: #666; -fx-text-fill: white;");
        btnVoltar.setOnAction(e -> mostrarTelaPrincipal());

        VBox vbox = new VBox(10, scrollPane, btnVoltar);
        vbox.setPadding(new Insets(10));

        root.setCenter(vbox);
    }

    private VBox criarCardProduto(Produto produto) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-border-radius: 5; -fx-background-radius: 5; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");
        card.setPrefWidth(250);
        card.setAlignment(Pos.CENTER);

        // Imagem do produto
        ImageView imagem = new ImageView();
        try {
            File file = new File(produto.getImagemPath());
            if (file.exists()) {
                Image img = new Image(file.toURI().toString(), 150, 150, true, true);
                imagem.setImage(img);
            } else {
                // Imagem padrão se não encontrar
                imagem.setFitWidth(150);
                imagem.setFitHeight(150);
                imagem.setStyle("-fx-background-color: #ddd; -fx-background-radius: 5;");
            }
        } catch (Exception e) {
            imagem.setFitWidth(150);
            imagem.setFitHeight(150);
            imagem.setStyle("-fx-background-color: #ddd; -fx-background-radius: 5;");
        }
        imagem.setPreserveRatio(true);

        Label nome = new Label(produto.getNome());
        nome.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        nome.setWrapText(true);
        nome.setAlignment(Pos.CENTER);

        Label preco = new Label(String.format("R$ %.2f", produto.getPreco()));
        preco.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");

        Label estoque = new Label("Em estoque: " + produto.getQuantidadeEstoque());
        estoque.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        Button btnComprar = new Button("Comprar");
        btnComprar.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        btnComprar.setPrefWidth(150);
        btnComprar.setOnAction(e -> mostrarDialogQuantidade(produto));

        card.getChildren().addAll(imagem, nome, preco, estoque, btnComprar);
        return card;
    }

    private void mostrarDialogQuantidade(Produto produto) {
        Dialog<Integer> dialog = new Dialog<>();
        dialog.setTitle("Comprar " + produto.getNome());
        dialog.setHeaderText("Informe a quantidade (máx " + produto.getQuantidadeEstoque() + ")");

        ButtonType btnComprar = new ButtonType("Adicionar ao Carrinho", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnComprar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField tfQuantidade = new TextField();
        tfQuantidade.setPromptText("Quantidade");

        Spinner<Integer> spinnerQuantidade = new Spinner<>(1, produto.getQuantidadeEstoque(), 1);
        spinnerQuantidade.setEditable(true);

        grid.add(new Label("Quantidade:"), 0, 0);
        grid.add(spinnerQuantidade, 1, 0);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnComprar) {
                return spinnerQuantidade.getValue();
            }
            return null;
        });

        Optional<Integer> result = dialog.showAndWait();
        result.ifPresent(quantidade -> {
            try {
                carrinhoAtual = service.adicionarItemAoCarrinho(carrinhoAtual.getId(), produto.getId(), quantidade);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Sucesso");
                alert.setHeaderText(null);
                alert.setContentText("Produto adicionado ao carrinho!");
                alert.showAndWait();
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erro");
                alert.setHeaderText(null);
                alert.setContentText("Erro: " + ex.getMessage());
                alert.showAndWait();
            }
        });
    }

    private void mostrarCarrinho(BorderPane root) {
        // Atualizar carrinho atual
        carrinhoAtual = service.buscarCliente(clienteLogado.getId())
                .flatMap(c -> c.getHistoricoPedidos().stream()
                        .filter(p -> p.getId().equals(carrinhoAtual.getId()))
                        .findFirst())
                .orElse(carrinhoAtual);

        itensCarrinhoObservable.setAll(carrinhoAtual.getItens());

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));

        if (itensCarrinhoObservable.isEmpty()) {
            vbox.getChildren().add(new Label("Seu carrinho está vazio."));
        } else {
            TableView<ItemPedido> table = new TableView<>(itensCarrinhoObservable);
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            TableColumn<ItemPedido, String> colProd = new TableColumn<>("Produto");
            colProd.setCellValueFactory(cellData ->
                    new SimpleStringProperty(cellData.getValue().getProduto().getNome()));

            TableColumn<ItemPedido, Integer> colQtd = new TableColumn<>("Quantidade");
            colQtd.setCellValueFactory(new PropertyValueFactory<>("quantidade"));

            TableColumn<ItemPedido, BigDecimal> colPreco = new TableColumn<>("Preço Unit.");
            colPreco.setCellValueFactory(new PropertyValueFactory<>("precoUnitario"));

            TableColumn<ItemPedido, BigDecimal> colSub = new TableColumn<>("Subtotal");
            colSub.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

            table.getColumns().addAll(colProd, colQtd, colPreco, colSub);

            Label lbTotal = new Label("Total: R$ " + String.format("%.2f", carrinhoAtual.getValorTotal()));
            lbTotal.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");

            Button btnFinalizar = new Button("Finalizar Compra");
            btnFinalizar.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px;");
            btnFinalizar.setOnAction(e -> mostrarDialogFinalizarCompra());

            Button btnRemover = new Button("Remover Item");
            btnRemover.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
            btnRemover.setOnAction(e -> {
                ItemPedido item = table.getSelectionModel().getSelectedItem();
                if (item != null) {
                    try {
                        carrinhoAtual = service.removerItemDoCarrinho(carrinhoAtual.getId(), item.getId());
                        itensCarrinhoObservable.setAll(carrinhoAtual.getItens());
                        lbTotal.setText("Total: R$ " + String.format("%.2f", carrinhoAtual.getValorTotal()));
                    } catch (Exception ex) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Erro: " + ex.getMessage());
                        alert.show();
                    }
                }
            });

            HBox botoes = new HBox(10, btnFinalizar, btnRemover);
            botoes.setAlignment(Pos.CENTER);

            vbox.getChildren().addAll(table, lbTotal, botoes);
        }

        Button btnVoltar = new Button("Voltar");
        btnVoltar.setStyle("-fx-background-color: #666; -fx-text-fill: white;");
        btnVoltar.setOnAction(e -> mostrarTelaPrincipal());
        vbox.getChildren().add(btnVoltar);

        root.setCenter(vbox);
    }

    private void mostrarDialogFinalizarCompra() {
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Finalizar Compra");
        dialog.setHeaderText("Escolha a forma de pagamento e horário de entrega");

        ButtonType btnConfirmar = new ButtonType("Confirmar Compra", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnConfirmar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        // Formas de pagamento
        ComboBox<String> cbPagamento = new ComboBox<>();
        cbPagamento.getItems().addAll("Cartão de Crédito", "Cartão de Débito", "Boleto Bancário", "PIX", "Dinheiro");
        cbPagamento.setValue("Cartão de Crédito");

        // Horários de entrega
        ComboBox<String> cbHorario = new ComboBox<>();
        cbHorario.getItems().addAll("Manhã (08:00 - 12:00)", "Tarde (12:00 - 18:00)", "Noite (18:00 - 22:00)");
        cbHorario.setValue("Tarde (12:00 - 18:00)");

        // Data estimada (calculada automaticamente)
        LocalDateTime dataEstimada = LocalDateTime.now().plusDays(5);
        String dataFormatada = dataEstimada.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        Label lbData = new Label("Previsão de entrega: " + dataFormatada);
        lbData.setStyle("-fx-font-weight: bold; -fx-text-fill: #4CAF50;");

        grid.add(new Label("Forma de Pagamento:"), 0, 0);
        grid.add(cbPagamento, 1, 0);
        grid.add(new Label("Horário de Entrega:"), 0, 1);
        grid.add(cbHorario, 1, 1);
        grid.add(lbData, 0, 2, 2, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnConfirmar) {
                Map<String, String> resultado = new HashMap<>();
                resultado.put("pagamento", cbPagamento.getValue());
                resultado.put("horario", cbHorario.getValue());
                return resultado;
            }
            return null;
        });

        Optional<Map<String, String>> result = dialog.showAndWait();
        result.ifPresent(dados -> {
            try {
                carrinhoAtual = service.finalizarCompra(
                        carrinhoAtual.getId(),
                        dados.get("pagamento"),
                        dados.get("horario"));

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Compra Realizada!");
                alert.setHeaderText(null);
                alert.setContentText("Compra finalizada com sucesso!\n" +
                        "Pedido #" + carrinhoAtual.getId() + "\n" +
                        "Pagamento: " + dados.get("pagamento") + "\n" +
                        "Previsão de entrega: " + carrinhoAtual.getDataEntregaEstimada()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + " - " + dados.get("horario"));
                alert.showAndWait();

                // Criar novo carrinho vazio
                carrinhoAtual = service.criarPedido(clienteLogado.getId(), clienteLogado.getEndereco());
                mostrarTelaPrincipal();
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Erro: " + ex.getMessage());
                alert.show();
            }
        });
    }

    private void mostrarMeusPedidos(BorderPane root) {
        List<Pedido> pedidos = service.listarPedidosCliente(clienteLogado.getId());
        pedidosObservable.setAll(pedidos);

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));

        if (pedidosObservable.isEmpty()) {
            vbox.getChildren().add(new Label("Você ainda não realizou nenhum pedido."));
        } else {
            TableView<Pedido> table = new TableView<>(pedidosObservable);
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            TableColumn<Pedido, Long> colId = new TableColumn<>("Pedido #");
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));

            TableColumn<Pedido, String> colData = new TableColumn<>("Data");
            colData.setCellValueFactory(cellData ->
                    new SimpleStringProperty(cellData.getValue().getDataPedido()
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));

            TableColumn<Pedido, StatusPedido> colStatus = new TableColumn<>("Status");
            colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

            TableColumn<Pedido, BigDecimal> colTotal = new TableColumn<>("Total");
            colTotal.setCellValueFactory(new PropertyValueFactory<>("valorTotal"));

            TableColumn<Pedido, String> colPagamento = new TableColumn<>("Pagamento");
            colPagamento.setCellValueFactory(new PropertyValueFactory<>("metodoPagamento"));

            TableColumn<Pedido, String> colEntrega = new TableColumn<>("Previsão Entrega");
            colEntrega.setCellValueFactory(cellData -> {
                if (cellData.getValue().getDataEntregaEstimada() != null) {
                    String data = cellData.getValue().getDataEntregaEstimada()
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    String horario = cellData.getValue().getHorarioEntrega() != null ?
                            " - " + cellData.getValue().getHorarioEntrega() : "";
                    return new SimpleStringProperty(data + horario);
                }
                return new SimpleStringProperty("A calcular");
            });

            table.getColumns().addAll(colId, colData, colStatus, colTotal, colPagamento, colEntrega);

            Button btnDetalhes = new Button("Ver Detalhes");
            btnDetalhes.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
            btnDetalhes.setOnAction(e -> {
                Pedido selecionado = table.getSelectionModel().getSelectedItem();
                if (selecionado != null) {
                    mostrarDetalhesPedido(selecionado);
                }
            });

            vbox.getChildren().addAll(table, btnDetalhes);
        }

        Button btnVoltar = new Button("Voltar");
        btnVoltar.setStyle("-fx-background-color: #666; -fx-text-fill: white;");
        btnVoltar.setOnAction(e -> mostrarTelaPrincipal());
        vbox.getChildren().add(btnVoltar);

        root.setCenter(vbox);
    }

    private void mostrarDetalhesPedido(Pedido pedido) {
        Stage stage = new Stage();
        stage.setTitle("Detalhes do Pedido #" + pedido.getId());

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));

        Label lbInfo = new Label(
                "Cliente: " + pedido.getCliente().getNome() +
                        "\nData do Pedido: " + pedido.getDataPedido().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) +
                        "\nStatus: " + pedido.getStatus() +
                        "\nForma de Pagamento: " + (pedido.getMetodoPagamento() != null ? pedido.getMetodoPagamento() : "Não informado") +
                        "\nEndereço de entrega: " + pedido.getEnderecoEntrega() +
                        (pedido.getDataEntregaEstimada() != null ?
                                "\nPrevisão de Entrega: " + pedido.getDataEntregaEstimada().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                                        " - " + (pedido.getHorarioEntrega() != null ? pedido.getHorarioEntrega() : "Horário não informado") : "") +
                        (pedido.getDataEntrega() != null ? "\nEntregue em: " + pedido.getDataEntrega().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "")
        );
        lbInfo.setStyle("-fx-font-size: 14px; -fx-padding: 10; -fx-background-color: #f9f9f9; -fx-background-radius: 5;");

        TableView<ItemPedido> tableItens = new TableView<>();
        tableItens.setItems(FXCollections.observableArrayList(pedido.getItens()));
        tableItens.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<ItemPedido, String> colProd = new TableColumn<>("Produto");
        colProd.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getProduto().getNome()));

        TableColumn<ItemPedido, Integer> colQtd = new TableColumn<>("Qtd");
        colQtd.setCellValueFactory(new PropertyValueFactory<>("quantidade"));

        TableColumn<ItemPedido, BigDecimal> colPreco = new TableColumn<>("Preço Unit.");
        colPreco.setCellValueFactory(new PropertyValueFactory<>("precoUnitario"));

        TableColumn<ItemPedido, BigDecimal> colSub = new TableColumn<>("Subtotal");
        colSub.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        tableItens.getColumns().addAll(colProd, colQtd, colPreco, colSub);

        Label lbTotal = new Label("Total do Pedido: R$ " + String.format("%.2f", pedido.getValorTotal()));
        lbTotal.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");

        Button btnFechar = new Button("Fechar");
        btnFechar.setStyle("-fx-background-color: #666; -fx-text-fill: white;");
        btnFechar.setOnAction(e -> stage.close());

        vbox.getChildren().addAll(lbInfo, new Label("Itens do Pedido:"), tableItens, lbTotal, btnFechar);

        Scene scene = new Scene(vbox, 700, 500);
        stage.setScene(scene);
        stage.show();
    }

    private void mostrarPerfil(BorderPane root) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.setAlignment(Pos.CENTER);

        Label lbTitulo = new Label("Meu Perfil");
        lbTitulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Label lbNome = new Label("Nome: " + clienteLogado.getNome());
        Label lbEmail = new Label("E-mail: " + clienteLogado.getEmail());
        Label lbTelefone = new Label("Telefone: " + clienteLogado.getTelefone());
        Label lbEndereco = new Label("Endereço: " + clienteLogado.getEndereco() +
                (clienteLogado.getCidade() != null ? ", " + clienteLogado.getCidade() : "") +
                (clienteLogado.getEstado() != null ? " - " + clienteLogado.getEstado() : "") +
                (clienteLogado.getCep() != null ? " - CEP: " + clienteLogado.getCep() : ""));
        Label lbDataCadastro = new Label("Cliente desde: " + clienteLogado.getDataCadastro()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        lbNome.setStyle("-fx-font-size: 14px;");
        lbEmail.setStyle("-fx-font-size: 14px;");
        lbTelefone.setStyle("-fx-font-size: 14px;");
        lbEndereco.setStyle("-fx-font-size: 14px;");
        lbDataCadastro.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

        Button btnVoltar = new Button("Voltar");
        btnVoltar.setStyle("-fx-background-color: #666; -fx-text-fill: white;");
        btnVoltar.setOnAction(e -> mostrarTelaPrincipal());

        int row = 0;
        grid.add(lbTitulo, 0, row++, 2, 1);
        grid.add(new Separator(), 0, row++, 2, 1);
        grid.add(lbNome, 0, row++, 2, 1);
        grid.add(lbEmail, 0, row++, 2, 1);
        grid.add(lbTelefone, 0, row++, 2, 1);
        grid.add(lbEndereco, 0, row++, 2, 1);
        grid.add(lbDataCadastro, 0, row++, 2, 1);
        grid.add(btnVoltar, 0, row, 2, 1);

        VBox vbox = new VBox(grid);
        vbox.setAlignment(Pos.CENTER);
        root.setCenter(vbox);
    }

    private void mostrarFormAdicionarProduto(BorderPane root) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.setAlignment(Pos.CENTER);

        Label lbTitulo = new Label("Adicionar Novo Produto");
        lbTitulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        TextField tfNome = new TextField();
        tfNome.setPrefWidth(300);
        TextField tfDesc = new TextField();
        TextField tfPreco = new TextField();
        TextField tfQtd = new TextField();
        TextField tfCategoria = new TextField();
        TextField tfImagem = new TextField();
        tfImagem.setPromptText("caminho/para/imagem.png");

        int row = 1;
        grid.add(new Label("Nome:*"), 0, row); grid.add(tfNome, 1, row++);
        grid.add(new Label("Descrição:"), 0, row); grid.add(tfDesc, 1, row++);
        grid.add(new Label("Preço:*"), 0, row); grid.add(tfPreco, 1, row++);
        grid.add(new Label("Quantidade:*"), 0, row); grid.add(tfQtd, 1, row++);
        grid.add(new Label("Categoria:"), 0, row); grid.add(tfCategoria, 1, row++);
        grid.add(new Label("Imagem:"), 0, row); grid.add(tfImagem, 1, row++);

        Button btnSalvar = new Button("Salvar Produto");
        btnSalvar.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        Button btnVoltar = new Button("Cancelar");
        btnVoltar.setStyle("-fx-background-color: #666; -fx-text-fill: white;");

        btnSalvar.setOnAction(e -> {
            try {
                if (tfNome.getText().isEmpty() || tfPreco.getText().isEmpty() || tfQtd.getText().isEmpty()) {
                    throw new IllegalArgumentException("Preencha os campos obrigatórios!");
                }

                BigDecimal preco = new BigDecimal(tfPreco.getText());
                int qtd = Integer.parseInt(tfQtd.getText());
                String imagemPath = tfImagem.getText().isEmpty() ? "imagens/default.png" : tfImagem.getText();

                Produto p = service.adicionarProduto(
                        tfNome.getText(),
                        tfDesc.getText(),
                        preco,
                        qtd,
                        tfCategoria.getText(),
                        imagemPath);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Sucesso");
                alert.setHeaderText(null);
                alert.setContentText("Produto adicionado: " + p.getNome());
                alert.showAndWait();

                mostrarTelaPrincipal();
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Erro: " + ex.getMessage());
                alert.show();
            }
        });

        btnVoltar.setOnAction(e -> mostrarTelaPrincipal());

        HBox botoes = new HBox(10, btnSalvar, btnVoltar);
        botoes.setAlignment(Pos.CENTER);

        VBox vbox = new VBox(20, lbTitulo, grid, botoes);
        vbox.setAlignment(Pos.TOP_CENTER);

        root.setCenter(vbox);
    }
}
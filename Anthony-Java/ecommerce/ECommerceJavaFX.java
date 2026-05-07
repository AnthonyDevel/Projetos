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

    // ---------- Constantes de Cores ----------
    private static final String AZUL_PRINCIPAL = "#1976D2";      // Azul mais escuro
    private static final String AZUL_MEDIO = "#2196F3";         // Azul médio
    private static final String AZUL_CLARO = "#64B5F6";         // Azul claro
    private static final String DOURADO = "#FFC107";            // Dourado
    private static final String DOURADO_CLARO = "#FFD54F";      // Dourado claro
    private static final String CINZA_GELO = "#ECEFF1";         // Cinza gelo
    private static final String CINZA_GELO_ESCURO = "#CFD8DC";  // Cinza gelo escuro
    private static final String BRANCO = "#FFFFFF";
    private static final String TEXTO_ESCURO = "#263238";
    private static final String TEXTO_CLARO = "#FFFFFF";

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
        private String imagemPath;

        public Produto() {
            this.dataCadastro = LocalDateTime.now();
            this.ativo = true;
            this.imagemPath = "imagens/default.png";
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
        private String senha;
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

    // ---------- Classe Admin ----------
    public static class Administrador implements Serializable {
        private static final long serialVersionUID = 1L;
        private String usuario;
        private String senha;

        public Administrador(String usuario, String senha) {
            this.usuario = usuario;
            this.senha = senha;
        }

        public String getUsuario() { return usuario; }
        public void setUsuario(String usuario) { this.usuario = usuario; }
        public String getSenha() { return senha; }
        public void setSenha(String senha) { this.senha = senha; }
    }

    // ---------- Repositórios ----------
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

        public void deletarFisico(Long id) {
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

        public void deletarTodos() {
            produtos.clear();
            currentId = new AtomicLong(1);
            salvarDados();
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
            Cliente cliente = pedido.getCliente();
            if (cliente != null) {
                cliente.getHistoricoPedidos().removeIf(p -> p.getId().equals(pedido.getId()));
                cliente.getHistoricoPedidos().add(pedido);
                ClienteRepository.getInstance().salvar(cliente);
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

        public List<Pedido> buscarPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
            List<Pedido> resultado = new ArrayList<>();
            for (Pedido p : pedidos.values()) {
                if (p.getDataPedido().isAfter(inicio) && p.getDataPedido().isBefore(fim)) {
                    resultado.add(p);
                }
            }
            return resultado;
        }

        public void deletar(Long id) {
            pedidos.remove(id);
            salvarDados();
        }

        public void deletarTodos() {
            pedidos.clear();
            currentId = new AtomicLong(1);
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

    public static class AdminRepository {
        private static final String FILE_NAME = "admin.dat";
        private Administrador admin;
        private static AdminRepository instance;

        private AdminRepository() {
            carregarDados();
            if (admin == null) {
                admin = new Administrador("admin", "admin123");
                salvarDados();
            }
        }

        public static synchronized AdminRepository getInstance() {
            if (instance == null) {
                instance = new AdminRepository();
            }
            return instance;
        }

        public boolean autenticar(String usuario, String senha) {
            return admin != null && admin.getUsuario().equals(usuario) && admin.getSenha().equals(senha);
        }

        public void alterarSenha(String novaSenha) {
            admin.setSenha(novaSenha);
            salvarDados();
        }

        private void carregarDados() {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_NAME))) {
                admin = (Administrador) ois.readObject();
            } catch (FileNotFoundException e) {
                admin = null;
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Erro ao carregar admin: " + e.getMessage());
                admin = null;
            }
        }

        private void salvarDados() {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
                oos.writeObject(admin);
            } catch (IOException e) {
                System.err.println("Erro ao salvar admin: " + e.getMessage());
            }
        }
    }

    // ---------- Serviço de Vendas ----------
    public static class VendaService {
        private ProdutoRepository produtoRepository;
        private ClienteRepository clienteRepository;
        private PedidoRepository pedidoRepository;
        private AdminRepository adminRepository;
        private static VendaService instance;

        private VendaService() {
            produtoRepository = ProdutoRepository.getInstance();
            clienteRepository = ClienteRepository.getInstance();
            pedidoRepository = PedidoRepository.getInstance();
            adminRepository = AdminRepository.getInstance();
        }

        public static synchronized VendaService getInstance() {
            if (instance == null) {
                instance = new VendaService();
            }
            return instance;
        }

        // Admin
        public boolean autenticarAdmin(String usuario, String senha) {
            return adminRepository.autenticar(usuario, senha);
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

        public List<Produto> listarTodosProdutos() {
            return produtoRepository.buscarTodos();
        }

        public Optional<Produto> buscarProduto(Long id) {
            return produtoRepository.buscarPorId(id);
        }

        public void removerProduto(Long id) {
            produtoRepository.deletarLogico(id);
        }

        public void excluirProdutoPermanente(Long id) {
            produtoRepository.deletarFisico(id);
        }

        public void excluirTodosProdutos() {
            produtoRepository.deletarTodos();
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
            for (ItemPedido item : pedido.getItens()) {
                boolean ok = produtoRepository.atualizarEstoque(item.getProduto().getId(), item.getQuantidade());
                if (!ok) {
                    throw new RuntimeException("Estoque insuficiente para: " + item.getProduto().getNome());
                }
            }

            pedido.setMetodoPagamento(metodoPagamento);
            pedido.setHorarioEntrega(horarioEntrega);

            LocalDateTime hoje = LocalDateTime.now();
            LocalDateTime entregaEstimada = hoje.plusDays(5);
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

        public List<Pedido> listarTodosPedidos() {
            return pedidoRepository.buscarTodos();
        }

        public List<Pedido> listarPedidosPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
            return pedidoRepository.buscarPorPeriodo(inicio, fim);
        }

        // Relatórios
        public Map<String, Object> gerarRelatorioVendas() {
            Map<String, Object> relatorio = new HashMap<>();
            List<Pedido> todosPedidos = pedidoRepository.buscarTodos();

            BigDecimal faturamentoTotal = BigDecimal.ZERO;
            int totalPedidos = todosPedidos.size();
            int pedidosFinalizados = 0;
            int pedidosCancelados = 0;
            Map<String, Integer> produtosVendidos = new HashMap<>();
            Map<String, BigDecimal> faturamentoPorCategoria = new HashMap<>();

            for (Pedido p : todosPedidos) {
                if (p.getStatus() != StatusPedido.CANCELADO) {
                    faturamentoTotal = faturamentoTotal.add(p.getValorTotal());
                    pedidosFinalizados++;

                    for (ItemPedido item : p.getItens()) {
                        String nomeProduto = item.getProduto().getNome();
                        produtosVendidos.merge(nomeProduto, item.getQuantidade(), Integer::sum);

                        String categoria = item.getProduto().getCategoria();
                        if (categoria != null && !categoria.isEmpty()) {
                            BigDecimal valorItem = item.getSubtotal();
                            faturamentoPorCategoria.merge(categoria, valorItem, BigDecimal::add);
                        }
                    }
                } else {
                    pedidosCancelados++;
                }
            }

            relatorio.put("faturamentoTotal", faturamentoTotal);
            relatorio.put("totalPedidos", totalPedidos);
            relatorio.put("pedidosFinalizados", pedidosFinalizados);
            relatorio.put("pedidosCancelados", pedidosCancelados);
            relatorio.put("produtosVendidos", produtosVendidos);
            relatorio.put("faturamentoPorCategoria", faturamentoPorCategoria);

            return relatorio;
        }

        public Map<String, Object> gerarRelatorioEstoque() {
            Map<String, Object> relatorio = new HashMap<>();
            List<Produto> todosProdutos = produtoRepository.buscarTodos();

            int totalProdutos = todosProdutos.size();
            int produtosAtivos = 0;
            int produtosInativos = 0;
            int totalItensEstoque = 0;
            BigDecimal valorTotalEstoque = BigDecimal.ZERO;
            List<Produto> estoqueBaixo = new ArrayList<>();

            for (Produto p : todosProdutos) {
                if (p.isAtivo()) {
                    produtosAtivos++;
                    totalItensEstoque += p.getQuantidadeEstoque();
                    valorTotalEstoque = valorTotalEstoque.add(
                            p.getPreco().multiply(BigDecimal.valueOf(p.getQuantidadeEstoque())));

                    if (p.getQuantidadeEstoque() < 5) {
                        estoqueBaixo.add(p);
                    }
                } else {
                    produtosInativos++;
                }
            }

            relatorio.put("totalProdutos", totalProdutos);
            relatorio.put("produtosAtivos", produtosAtivos);
            relatorio.put("produtosInativos", produtosInativos);
            relatorio.put("totalItensEstoque", totalItensEstoque);
            relatorio.put("valorTotalEstoque", valorTotalEstoque);
            relatorio.put("estoqueBaixo", estoqueBaixo);

            return relatorio;
        }

        public Map<String, Object> gerarRelatorioClientes() {
            Map<String, Object> relatorio = new HashMap<>();
            List<Cliente> todosClientes = clienteRepository.buscarTodos();

            int totalClientes = todosClientes.size();
            Map<Cliente, Integer> comprasPorCliente = new HashMap<>();
            Cliente clienteTop = null;
            int maxCompras = 0;

            for (Cliente c : todosClientes) {
                int numPedidos = c.getHistoricoPedidos().size();
                comprasPorCliente.put(c, numPedidos);

                if (numPedidos > maxCompras) {
                    maxCompras = numPedidos;
                    clienteTop = c;
                }
            }

            relatorio.put("totalClientes", totalClientes);
            relatorio.put("comprasPorCliente", comprasPorCliente);
            relatorio.put("clienteTop", clienteTop);
            relatorio.put("maxCompras", maxCompras);

            return relatorio;
        }
    }

    // ---------- JavaFX Application ----------
    private Stage primaryStage;
    private VendaService service = VendaService.getInstance();
    private Cliente clienteLogado;
    private boolean isAdminLogado = false;
    private Pedido carrinhoAtual;

    // Observables
    private ObservableList<Produto> produtosObservable = FXCollections.observableArrayList();
    private ObservableList<Pedido> pedidosObservable = FXCollections.observableArrayList();
    private ObservableList<ItemPedido> itensCarrinhoObservable = FXCollections.observableArrayList();
    private ObservableList<Cliente> clientesObservable = FXCollections.observableArrayList();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("Sistema de E-Commerce Premium");
        primaryStage.setWidth(1200);
        primaryStage.setHeight(800);

        // REMOVA esta linha daqui
        // aplicarEstiloGlobal();

        // Inicializar dados de exemplo
        inicializarDadosExemplo();

        // Mostrar tela de login
        mostrarTelaLogin();
        primaryStage.show();
    }

    private void aplicarEstiloGlobal() {
        String estiloGlobal = String.format(
                ".root { -fx-font-family: 'Segoe UI', Arial, sans-serif; -fx-background-color: %s; }" +
                        ".button { -fx-background-radius: 5; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2); }" +
                        ".button:hover { -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 8, 0, 0, 3); }" +
                        ".table-view { -fx-background-color: %s; -fx-background-radius: 5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2); }" +
                        ".table-view .column-header { -fx-background-color: %s; -fx-text-fill: %s; }" +
                        ".menu-bar { -fx-background-color: %s; }" +
                        ".menu-bar .menu { -fx-text-fill: %s; }" +
                        ".menu-bar .menu-item { -fx-text-fill: %s; }" +
                        ".label { -fx-text-fill: %s; }",
                CINZA_GELO, BRANCO, AZUL_PRINCIPAL, TEXTO_CLARO, AZUL_PRINCIPAL, TEXTO_CLARO, TEXTO_ESCURO, TEXTO_ESCURO
        );

        // Aplicar estilo ao stage
        primaryStage.getScene().getStylesheets().add("data:text/css," + estiloGlobal);
    }

    private String getBotaoEstilo(String corFundo) {
        return String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: 14px; -fx-padding: 10 20; -fx-font-weight: bold;",
                corFundo, TEXTO_CLARO);
    }

    private void inicializarDadosExemplo() {
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

        if (service.listarClientes().isEmpty()) {
            service.cadastrarCliente("João Silva", "joao@email.com", "123456", "(11)99999-9999",
                    "Rua das Flores, 123", "São Paulo", "SP", "01234-567");
            service.cadastrarCliente("Maria Souza", "maria@email.com", "123456", "(11)88888-8888",
                    "Av. Paulista, 1000", "São Paulo", "SP", "01310-100");
            service.cadastrarCliente("Pedro Santos", "pedro@email.com", "123456", "(11)77777-7777",
                    "Rua Augusta, 500", "São Paulo", "SP", "01450-000");
        }
    }

    private void mostrarTelaLogin() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle(String.format("-fx-background-color: %s;", BRANCO));

        // Aba de Login Cliente
        Tab tabLoginCliente = new Tab("Cliente");
        VBox loginClienteBox = new VBox(20);
        loginClienteBox.setAlignment(Pos.CENTER);
        loginClienteBox.setPadding(new Insets(40));
        loginClienteBox.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);", BRANCO));

        Label tituloCliente = new Label("Área do Cliente");
        tituloCliente.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + AZUL_PRINCIPAL + ";");

        GridPane loginClienteGrid = new GridPane();
        loginClienteGrid.setHgap(10);
        loginClienteGrid.setVgap(10);
        loginClienteGrid.setAlignment(Pos.CENTER);

        TextField tfEmailLogin = new TextField();
        tfEmailLogin.setPromptText("email@exemplo.com");
        tfEmailLogin.setPrefWidth(250);
        tfEmailLogin.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: " + CINZA_GELO_ESCURO + ";");

        PasswordField pfSenhaLogin = new PasswordField();
        pfSenhaLogin.setPromptText("Sua senha");
        pfSenhaLogin.setPrefWidth(250);
        pfSenhaLogin.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: " + CINZA_GELO_ESCURO + ";");

        Button btnLoginCliente = new Button("Entrar como Cliente");
        btnLoginCliente.setStyle(getBotaoEstilo(AZUL_PRINCIPAL));
        btnLoginCliente.setPrefWidth(250);

        Label lblMensagemLogin = new Label();
        lblMensagemLogin.setStyle("-fx-text-fill: #f44336;");

        btnLoginCliente.setOnAction(e -> {
            String email = tfEmailLogin.getText();
            String senha = pfSenhaLogin.getText();

            Optional<Cliente> clienteOpt = service.autenticar(email, senha);
            if (clienteOpt.isPresent()) {
                clienteLogado = clienteOpt.get();
                isAdminLogado = false;
                carrinhoAtual = service.criarPedido(clienteLogado.getId(), clienteLogado.getEndereco());
                mostrarTelaPrincipal();
            } else {
                lblMensagemLogin.setText("E-mail ou senha inválidos!");
            }
        });

        int rowCliente = 0;
        loginClienteGrid.add(new Label("E-mail:"), 0, rowCliente);
        loginClienteGrid.add(tfEmailLogin, 1, rowCliente++);
        loginClienteGrid.add(new Label("Senha:"), 0, rowCliente);
        loginClienteGrid.add(pfSenhaLogin, 1, rowCliente++);

        loginClienteBox.getChildren().addAll(tituloCliente, loginClienteGrid, btnLoginCliente, lblMensagemLogin);
        tabLoginCliente.setContent(loginClienteBox);

        // Aba de Login Admin
        Tab tabLoginAdmin = new Tab("Administrador");
        VBox loginAdminBox = new VBox(20);
        loginAdminBox.setAlignment(Pos.CENTER);
        loginAdminBox.setPadding(new Insets(40));
        loginAdminBox.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);", BRANCO));

        Label tituloAdmin = new Label("Área do Administrador");
        tituloAdmin.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + DOURADO + ";");

        GridPane loginAdminGrid = new GridPane();
        loginAdminGrid.setHgap(10);
        loginAdminGrid.setVgap(10);
        loginAdminGrid.setAlignment(Pos.CENTER);

        TextField tfUsuarioAdmin = new TextField();
        tfUsuarioAdmin.setPromptText("admin");
        tfUsuarioAdmin.setPrefWidth(250);
        tfUsuarioAdmin.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: " + CINZA_GELO_ESCURO + ";");

        PasswordField pfSenhaAdmin = new PasswordField();
        pfSenhaAdmin.setPromptText("admin123");
        pfSenhaAdmin.setPrefWidth(250);
        pfSenhaAdmin.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: " + CINZA_GELO_ESCURO + ";");

        Button btnLoginAdmin = new Button("Entrar como Admin");
        btnLoginAdmin.setStyle(getBotaoEstilo(DOURADO));
        btnLoginAdmin.setPrefWidth(250);

        Label lblMensagemAdmin = new Label();
        lblMensagemAdmin.setStyle("-fx-text-fill: #f44336;");

        btnLoginAdmin.setOnAction(e -> {
            String usuario = tfUsuarioAdmin.getText();
            String senha = pfSenhaAdmin.getText();

            if (service.autenticarAdmin(usuario, senha)) {
                isAdminLogado = true;
                clienteLogado = null;
                mostrarTelaAdmin();
            } else {
                lblMensagemAdmin.setText("Usuário ou senha inválidos!");
            }
        });

        int rowAdmin = 0;
        loginAdminGrid.add(new Label("Usuário:"), 0, rowAdmin);
        loginAdminGrid.add(tfUsuarioAdmin, 1, rowAdmin++);
        loginAdminGrid.add(new Label("Senha:"), 0, rowAdmin);
        loginAdminGrid.add(pfSenhaAdmin, 1, rowAdmin++);

        loginAdminBox.getChildren().addAll(tituloAdmin, loginAdminGrid, btnLoginAdmin, lblMensagemAdmin);
        tabLoginAdmin.setContent(loginAdminBox);

        // Aba de Registro
        Tab tabRegistro = new Tab("Criar Conta");
        VBox registroBox = new VBox(20);
        registroBox.setAlignment(Pos.CENTER);
        registroBox.setPadding(new Insets(40));
        registroBox.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);", BRANCO));

        Label tituloRegistro = new Label("Criar Nova Conta");
        tituloRegistro.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + AZUL_PRINCIPAL + ";");

        GridPane registroGrid = new GridPane();
        registroGrid.setHgap(10);
        registroGrid.setVgap(10);
        registroGrid.setAlignment(Pos.CENTER);

        TextField tfNome = new TextField();
        tfNome.setPromptText("Seu nome completo");
        tfNome.setPrefWidth(250);
        tfNome.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: " + CINZA_GELO_ESCURO + ";");

        TextField tfEmail = new TextField();
        tfEmail.setPromptText("email@exemplo.com");
        tfEmail.setPrefWidth(250);
        tfEmail.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: " + CINZA_GELO_ESCURO + ";");

        PasswordField pfSenha = new PasswordField();
        pfSenha.setPromptText("Crie uma senha");
        pfSenha.setPrefWidth(250);
        pfSenha.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: " + CINZA_GELO_ESCURO + ";");

        PasswordField pfConfirmaSenha = new PasswordField();
        pfConfirmaSenha.setPromptText("Confirme sua senha");
        pfConfirmaSenha.setPrefWidth(250);
        pfConfirmaSenha.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: " + CINZA_GELO_ESCURO + ";");

        TextField tfTelefone = new TextField();
        tfTelefone.setPromptText("(11) 99999-9999");
        tfTelefone.setPrefWidth(250);
        tfTelefone.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: " + CINZA_GELO_ESCURO + ";");

        TextField tfEndereco = new TextField();
        tfEndereco.setPromptText("Rua, número");
        tfEndereco.setPrefWidth(250);
        tfEndereco.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: " + CINZA_GELO_ESCURO + ";");

        TextField tfCidade = new TextField();
        tfCidade.setPromptText("Cidade");
        tfCidade.setPrefWidth(250);
        tfCidade.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: " + CINZA_GELO_ESCURO + ";");

        TextField tfEstado = new TextField();
        tfEstado.setPromptText("UF");
        tfEstado.setPrefWidth(250);
        tfEstado.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: " + CINZA_GELO_ESCURO + ";");

        TextField tfCep = new TextField();
        tfCep.setPromptText("00000-000");
        tfCep.setPrefWidth(250);
        tfCep.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: " + CINZA_GELO_ESCURO + ";");

        Button btnRegistrar = new Button("Criar Conta");
        btnRegistrar.setStyle(getBotaoEstilo(AZUL_MEDIO));
        btnRegistrar.setPrefWidth(250);

        Label lblMensagemRegistro = new Label();
        lblMensagemRegistro.setStyle("-fx-text-fill: #f44336;");

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

                tabPane.getSelectionModel().select(tabLoginCliente);

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

        registroBox.getChildren().addAll(tituloRegistro, registroGrid, btnRegistrar, lblMensagemRegistro);
        tabRegistro.setContent(registroBox);

        tabPane.getTabs().addAll(tabLoginCliente, tabLoginAdmin, tabRegistro);

        VBox vbox = new VBox(20);
        vbox.setAlignment(Pos.CENTER);
        vbox.setStyle(String.format("-fx-background-color: %s; -fx-padding: 20;", CINZA_GELO));
        vbox.getChildren().addAll(
                new Label("Bem-vindo ao E-Commerce Premium") {{
                    setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + AZUL_PRINCIPAL + ";");
                }},
                tabPane
        );

        Scene scene = new Scene(vbox);
        primaryStage.setScene(scene);
    }

    private void mostrarTelaPrincipal() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setStyle(String.format("-fx-background-color: %s;", CINZA_GELO));

        // Menu superior
        MenuBar menuBar = new MenuBar();
        menuBar.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s;", AZUL_PRINCIPAL, TEXTO_CLARO));

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

        menuBar.getMenus().addAll(menuCliente, menuProdutos, menuPedidos);
        root.setTop(menuBar);

        VBox welcomeBox = new VBox(20);
        welcomeBox.setAlignment(Pos.CENTER);
        welcomeBox.setStyle(String.format("-fx-background-color: %s; -fx-padding: 40; -fx-background-radius: 10;", BRANCO));

        Label bemVindo = new Label("Olá, " + clienteLogado.getNome() + "!");
        bemVindo.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + AZUL_PRINCIPAL + ";");

        Label resumo = new Label("Bem-vindo ao nosso e-commerce. Explore nossos produtos e faça suas compras!");
        resumo.setStyle("-fx-font-size: 16px; -fx-text-fill: " + TEXTO_ESCURO + ";");

        Button btnVerProdutos = new Button("Ver Produtos");
        btnVerProdutos.setStyle(getBotaoEstilo(AZUL_MEDIO));
        btnVerProdutos.setOnAction(e -> mostrarListaProdutos(root));

        welcomeBox.getChildren().addAll(bemVindo, resumo, btnVerProdutos);
        root.setCenter(welcomeBox);

        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setScene(scene);
    }

    private void mostrarTelaAdmin() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setStyle(String.format("-fx-background-color: %s;", CINZA_GELO));

        // Menu superior Admin
        MenuBar menuBar = new MenuBar();
        menuBar.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s;", DOURADO, TEXTO_ESCURO));

        Menu menuAdmin = new Menu("Admin");
        MenuItem itemSair = new MenuItem("Sair do Modo Admin");
        itemSair.setOnAction(e -> mostrarTelaLogin());
        MenuItem itemFechar = new MenuItem("Fechar");
        itemFechar.setOnAction(e -> Platform.exit());
        menuAdmin.getItems().addAll(itemSair, itemFechar);

        Menu menuProdutos = new Menu("Produtos");
        MenuItem itemAdicionar = new MenuItem("Adicionar Produto");
        itemAdicionar.setOnAction(e -> mostrarFormAdicionarProduto(root));
        MenuItem itemListar = new MenuItem("Gerenciar Produtos");
        itemListar.setOnAction(e -> mostrarGerenciarProdutos(root));
        MenuItem itemExcluirTodos = new MenuItem("Excluir Todos Produtos");
        itemExcluirTodos.setOnAction(e -> confirmarExcluirTodosProdutos(root));
        menuProdutos.getItems().addAll(itemAdicionar, itemListar, itemExcluirTodos);

        // NOVO: Menu Clientes
        Menu menuClientes = new Menu("Clientes");
        MenuItem itemListarClientes = new MenuItem("Listar Todos Clientes");
        itemListarClientes.setOnAction(e -> mostrarListaClientes(root));
        MenuItem itemBuscarCliente = new MenuItem("Buscar Cliente");
        itemBuscarCliente.setOnAction(e -> mostrarBuscaCliente(root));
        menuClientes.getItems().addAll(itemListarClientes, itemBuscarCliente);

        Menu menuRelatorios = new Menu("Relatórios");
        MenuItem itemRelVendas = new MenuItem("Relatório de Vendas");
        itemRelVendas.setOnAction(e -> mostrarRelatorioVendas(root));
        MenuItem itemRelEstoque = new MenuItem("Relatório de Estoque");
        itemRelEstoque.setOnAction(e -> mostrarRelatorioEstoque(root));
        MenuItem itemRelClientes = new MenuItem("Relatório de Clientes");
        itemRelClientes.setOnAction(e -> mostrarRelatorioClientes(root));
        menuRelatorios.getItems().addAll(itemRelVendas, itemRelEstoque, itemRelClientes);

        menuBar.getMenus().addAll(menuAdmin, menuProdutos, menuClientes, menuRelatorios);
        root.setTop(menuBar);

        VBox welcomeBox = new VBox(20);
        welcomeBox.setAlignment(Pos.CENTER);
        welcomeBox.setStyle(String.format("-fx-background-color: %s; -fx-padding: 40; -fx-background-radius: 10;", BRANCO));

        Label bemVindo = new Label("Painel do Administrador");
        bemVindo.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + DOURADO + ";");

        Label resumo = new Label("Bem-vindo ao painel administrativo. Aqui você pode gerenciar produtos, clientes, visualizar pedidos e gerar relatórios.");
        resumo.setStyle("-fx-font-size: 16px; -fx-text-fill: " + TEXTO_ESCURO + ";");
        resumo.setWrapText(true);
        resumo.setMaxWidth(600);

        HBox botoes = new HBox(20);
        botoes.setAlignment(Pos.CENTER);

        Button btnGerenciar = new Button("Gerenciar Produtos");
        btnGerenciar.setStyle(getBotaoEstilo(DOURADO));
        btnGerenciar.setOnAction(e -> mostrarGerenciarProdutos(root));

        Button btnClientes = new Button("Ver Clientes");
        btnClientes.setStyle(getBotaoEstilo(AZUL_MEDIO));
        btnClientes.setOnAction(e -> mostrarListaClientes(root));

        Button btnRelatorios = new Button("Ver Relatórios");
        btnRelatorios.setStyle(getBotaoEstilo(AZUL_PRINCIPAL));
        btnRelatorios.setOnAction(e -> mostrarRelatorioVendas(root));

        botoes.getChildren().addAll(btnGerenciar, btnClientes, btnRelatorios);

        welcomeBox.getChildren().addAll(bemVindo, resumo, botoes);
        root.setCenter(welcomeBox);

        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setScene(scene);
    }

    // NOVO: Método para listar todos os clientes
    private void mostrarListaClientes(BorderPane root) {
        clientesObservable.setAll(service.listarClientes());

        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        vbox.setStyle(String.format("-fx-background-color: %s;", BRANCO));

        Label titulo = new Label("LISTA DE CLIENTES CADASTRADOS");
        titulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + AZUL_PRINCIPAL + ";");

        TableView<Cliente> table = new TableView<>(clientesObservable);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-background-radius: 5;");

        TableColumn<Cliente, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Cliente, String> colNome = new TableColumn<>("Nome");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));

        TableColumn<Cliente, String> colEmail = new TableColumn<>("E-mail");
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<Cliente, String> colTelefone = new TableColumn<>("Telefone");
        colTelefone.setCellValueFactory(new PropertyValueFactory<>("telefone"));

        TableColumn<Cliente, String> colEndereco = new TableColumn<>("Endereço");
        colEndereco.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getEndereco() + ", " +
                        cellData.getValue().getCidade() + " - " +
                        cellData.getValue().getEstado()));

        TableColumn<Cliente, String> colData = new TableColumn<>("Cadastro");
        colData.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDataCadastro()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));

        TableColumn<Cliente, String> colPedidos = new TableColumn<>("Pedidos");
        colPedidos.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getHistoricoPedidos().size())));

        table.getColumns().addAll(colId, colNome, colEmail, colTelefone, colEndereco, colData, colPedidos);

        Button btnVerDetalhes = new Button("Ver Detalhes do Cliente");
        btnVerDetalhes.setStyle(getBotaoEstilo(AZUL_MEDIO));
        btnVerDetalhes.setOnAction(e -> {
            Cliente selecionado = table.getSelectionModel().getSelectedItem();
            if (selecionado != null) {
                mostrarDetalhesCliente(selecionado);
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Atenção");
                alert.setHeaderText(null);
                alert.setContentText("Selecione um cliente para ver os detalhes.");
                alert.showAndWait();
            }
        });

        Button btnVoltar = new Button("Voltar");
        btnVoltar.setStyle(getBotaoEstilo(CINZA_GELO_ESCURO));
        btnVoltar.setOnAction(e -> mostrarTelaAdmin());

        HBox botoes = new HBox(10, btnVerDetalhes, btnVoltar);
        botoes.setAlignment(Pos.CENTER);

        vbox.getChildren().addAll(titulo, table, botoes);
        root.setCenter(vbox);
    }

    // NOVO: Método para buscar cliente específico
    private void mostrarBuscaCliente(BorderPane root) {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        vbox.setStyle(String.format("-fx-background-color: %s;", BRANCO));
        vbox.setAlignment(Pos.TOP_CENTER);

        Label titulo = new Label("BUSCAR CLIENTE");
        titulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + AZUL_PRINCIPAL + ";");

        HBox buscaBox = new HBox(10);
        buscaBox.setAlignment(Pos.CENTER);

        TextField tfBusca = new TextField();
        tfBusca.setPromptText("Digite nome ou e-mail do cliente");
        tfBusca.setPrefWidth(300);
        tfBusca.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: " + CINZA_GELO_ESCURO + ";");

        Button btnBuscar = new Button("Buscar");
        btnBuscar.setStyle(getBotaoEstilo(AZUL_MEDIO));

        Button btnVoltar = new Button("Voltar");
        btnVoltar.setStyle(getBotaoEstilo(CINZA_GELO_ESCURO));
        btnVoltar.setOnAction(e -> mostrarTelaAdmin());

        VBox resultadoBox = new VBox(10);
        resultadoBox.setPadding(new Insets(20));
        resultadoBox.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: 5;", CINZA_GELO));

        btnBuscar.setOnAction(e -> {
            String termo = tfBusca.getText().toLowerCase();
            resultadoBox.getChildren().clear();

            List<Cliente> clientes = service.listarClientes();
            List<Cliente> resultados = new ArrayList<>();

            for (Cliente c : clientes) {
                if (c.getNome().toLowerCase().contains(termo) ||
                        c.getEmail().toLowerCase().contains(termo)) {
                    resultados.add(c);
                }
            }

            if (resultados.isEmpty()) {
                resultadoBox.getChildren().add(new Label("Nenhum cliente encontrado."));
            } else {
                ListView<Cliente> listView = new ListView<>();
                listView.setItems(FXCollections.observableArrayList(resultados));
                listView.setPrefHeight(200);

                Button btnSelecionar = new Button("Ver Detalhes");
                btnSelecionar.setStyle(getBotaoEstilo(AZUL_MEDIO));
                btnSelecionar.setOnAction(ev -> {
                    Cliente selecionado = listView.getSelectionModel().getSelectedItem();
                    if (selecionado != null) {
                        mostrarDetalhesCliente(selecionado);
                    }
                });

                resultadoBox.getChildren().addAll(listView, btnSelecionar);
            }
        });

        buscaBox.getChildren().addAll(tfBusca, btnBuscar);
        vbox.getChildren().addAll(titulo, buscaBox, resultadoBox, btnVoltar);
        root.setCenter(vbox);
    }

    // NOVO: Método para mostrar detalhes completos do cliente
    private void mostrarDetalhesCliente(Cliente cliente) {
        Stage stage = new Stage();
        stage.setTitle("Detalhes do Cliente: " + cliente.getNome());

        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        vbox.setStyle(String.format("-fx-background-color: %s;", BRANCO));

        // Informações pessoais
        TitledPane infoPessoal = new TitledPane();
        infoPessoal.setText("Informações Pessoais");
        infoPessoal.setExpanded(true);
        infoPessoal.setStyle("-fx-background-color: " + CINZA_GELO + ";");

        GridPane gridInfo = new GridPane();
        gridInfo.setHgap(10);
        gridInfo.setVgap(10);
        gridInfo.setPadding(new Insets(10));

        int row = 0;
        gridInfo.add(new Label("ID:"), 0, row);
        gridInfo.add(new Label(String.valueOf(cliente.getId())), 1, row++);
        gridInfo.add(new Label("Nome:"), 0, row);
        gridInfo.add(new Label(cliente.getNome()), 1, row++);
        gridInfo.add(new Label("E-mail:"), 0, row);
        gridInfo.add(new Label(cliente.getEmail()), 1, row++);
        gridInfo.add(new Label("Telefone:"), 0, row);
        gridInfo.add(new Label(cliente.getTelefone()), 1, row++);
        gridInfo.add(new Label("Data Cadastro:"), 0, row);
        gridInfo.add(new Label(cliente.getDataCadastro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))), 1, row++);

        infoPessoal.setContent(gridInfo);

        // Endereço
        TitledPane infoEndereco = new TitledPane();
        infoEndereco.setText("Endereço");
        infoEndereco.setExpanded(true);
        infoEndereco.setStyle("-fx-background-color: " + CINZA_GELO + ";");

        GridPane gridEnd = new GridPane();
        gridEnd.setHgap(10);
        gridEnd.setVgap(10);
        gridEnd.setPadding(new Insets(10));

        row = 0;
        gridEnd.add(new Label("Endereço:"), 0, row);
        gridEnd.add(new Label(cliente.getEndereco()), 1, row++);
        gridEnd.add(new Label("Cidade:"), 0, row);
        gridEnd.add(new Label(cliente.getCidade()), 1, row++);
        gridEnd.add(new Label("Estado:"), 0, row);
        gridEnd.add(new Label(cliente.getEstado()), 1, row++);
        gridEnd.add(new Label("CEP:"), 0, row);
        gridEnd.add(new Label(cliente.getCep()), 1, row++);

        infoEndereco.setContent(gridEnd);

        // Histórico de Pedidos
        TitledPane infoPedidos = new TitledPane();
        infoPedidos.setText("Histórico de Pedidos (" + cliente.getHistoricoPedidos().size() + ")");
        infoPedidos.setExpanded(true);
        infoPedidos.setStyle("-fx-background-color: " + CINZA_GELO + ";");

        TableView<Pedido> tablePedidos = new TableView<>();
        tablePedidos.setItems(FXCollections.observableArrayList(cliente.getHistoricoPedidos()));
        tablePedidos.setPrefHeight(200);

        TableColumn<Pedido, Long> colId = new TableColumn<>("Pedido #");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Pedido, String> colData = new TableColumn<>("Data");
        colData.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDataPedido()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));

        TableColumn<Pedido, StatusPedido> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        TableColumn<Pedido, BigDecimal> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(new PropertyValueFactory<>("valorTotal"));

        tablePedidos.getColumns().addAll(colId, colData, colStatus, colTotal);

        Button btnVerPedido = new Button("Ver Detalhes do Pedido");
        btnVerPedido.setStyle(getBotaoEstilo(AZUL_MEDIO));
        btnVerPedido.setOnAction(e -> {
            Pedido selecionado = tablePedidos.getSelectionModel().getSelectedItem();
            if (selecionado != null) {
                mostrarDetalhesPedido(selecionado);
            }
        });

        VBox pedidosBox = new VBox(10, tablePedidos, btnVerPedido);
        infoPedidos.setContent(pedidosBox);

        // Estatísticas
        TitledPane infoStats = new TitledPane();
        infoStats.setText("Estatísticas");
        infoStats.setExpanded(true);
        infoStats.setStyle("-fx-background-color: " + CINZA_GELO + ";");

        BigDecimal totalGasto = cliente.getHistoricoPedidos().stream()
                .filter(p -> p.getStatus() != StatusPedido.CANCELADO)
                .map(Pedido::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pedidosFinalizados = cliente.getHistoricoPedidos().stream()
                .filter(p -> p.getStatus() == StatusPedido.ENTREGUE)
                .count();

        GridPane gridStats = new GridPane();
        gridStats.setHgap(10);
        gridStats.setVgap(10);
        gridStats.setPadding(new Insets(10));

        row = 0;
        gridStats.add(new Label("Total gasto:"), 0, row);
        gridStats.add(new Label(String.format("R$ %.2f", totalGasto)), 1, row++);
        gridStats.add(new Label("Pedidos realizados:"), 0, row);
        gridStats.add(new Label(String.valueOf(cliente.getHistoricoPedidos().size())), 1, row++);
        gridStats.add(new Label("Pedidos finalizados:"), 0, row);
        gridStats.add(new Label(String.valueOf(pedidosFinalizados)), 1, row++);

        infoStats.setContent(gridStats);

        Button btnFechar = new Button("Fechar");
        btnFechar.setStyle(getBotaoEstilo(CINZA_GELO_ESCURO));
        btnFechar.setOnAction(e -> stage.close());

        VBox content = new VBox(10, infoPessoal, infoEndereco, infoPedidos, infoStats, btnFechar);
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(scrollPane, 800, 700);
        stage.setScene(scene);
        stage.show();
    }

    private void mostrarGerenciarProdutos(BorderPane root) {
        produtosObservable.setAll(service.listarTodosProdutos());

        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        vbox.setStyle(String.format("-fx-background-color: %s;", BRANCO));

        Label titulo = new Label("GERENCIAR PRODUTOS");
        titulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + AZUL_PRINCIPAL + ";");

        TableView<Produto> table = new TableView<>(produtosObservable);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Produto, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Produto, String> colNome = new TableColumn<>("Nome");
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));

        TableColumn<Produto, BigDecimal> colPreco = new TableColumn<>("Preço");
        colPreco.setCellValueFactory(new PropertyValueFactory<>("preco"));

        TableColumn<Produto, Integer> colEstoque = new TableColumn<>("Estoque");
        colEstoque.setCellValueFactory(new PropertyValueFactory<>("quantidadeEstoque"));

        TableColumn<Produto, String> colCategoria = new TableColumn<>("Categoria");
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));

        TableColumn<Produto, Boolean> colAtivo = new TableColumn<>("Ativo");
        colAtivo.setCellValueFactory(new PropertyValueFactory<>("ativo"));

        table.getColumns().addAll(colId, colNome, colPreco, colEstoque, colCategoria, colAtivo);

        HBox botoes = new HBox(10);
        botoes.setAlignment(Pos.CENTER);

        Button btnExcluir = new Button("Excluir (Lógico)");
        btnExcluir.setStyle(getBotaoEstilo(DOURADO));
        btnExcluir.setOnAction(e -> {
            Produto selecionado = table.getSelectionModel().getSelectedItem();
            if (selecionado != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Desativar produto: " + selecionado.getNome() + "?",
                        ButtonType.YES, ButtonType.NO);
                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.YES) {
                        service.removerProduto(selecionado.getId());
                        produtosObservable.setAll(service.listarTodosProdutos());
                    }
                });
            }
        });

        Button btnExcluirPermanente = new Button("Excluir Permanentemente");
        btnExcluirPermanente.setStyle(getBotaoEstilo("#d32f2f"));
        btnExcluirPermanente.setOnAction(e -> {
            Produto selecionado = table.getSelectionModel().getSelectedItem();
            if (selecionado != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Excluir PERMANENTEMENTE o produto: " + selecionado.getNome() + "? Esta ação não pode ser desfeita!",
                        ButtonType.YES, ButtonType.NO);
                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.YES) {
                        service.excluirProdutoPermanente(selecionado.getId());
                        produtosObservable.setAll(service.listarTodosProdutos());
                    }
                });
            }
        });

        Button btnVoltar = new Button("Voltar");
        btnVoltar.setStyle(getBotaoEstilo(CINZA_GELO_ESCURO));
        btnVoltar.setOnAction(e -> mostrarTelaAdmin());

        botoes.getChildren().addAll(btnExcluir, btnExcluirPermanente, btnVoltar);
        vbox.getChildren().addAll(titulo, table, botoes);
        root.setCenter(vbox);
    }

    private void confirmarExcluirTodosProdutos(BorderPane root) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Tem certeza que deseja excluir TODOS os produtos? Esta ação não pode ser desfeita!",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                service.excluirTodosProdutos();
                mostrarTelaAdmin();
            }
        });
    }

    private void mostrarRelatorioVendas(BorderPane root) {
        Map<String, Object> relatorio = service.gerarRelatorioVendas();

        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        vbox.setStyle(String.format("-fx-background-color: %s;", BRANCO));
        vbox.setAlignment(Pos.TOP_CENTER);

        Label titulo = new Label("RELATÓRIO DE VENDAS");
        titulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + DOURADO + ";");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);
        grid.setStyle("-fx-padding: 20; -fx-background-color: " + CINZA_GELO + "; -fx-background-radius: 5;");

        BigDecimal faturamento = (BigDecimal) relatorio.get("faturamentoTotal");
        int totalPedidos = (int) relatorio.get("totalPedidos");
        int finalizados = (int) relatorio.get("pedidosFinalizados");
        int cancelados = (int) relatorio.get("pedidosCancelados");
        Map<String, Integer> produtosVendidos = (Map<String, Integer>) relatorio.get("produtosVendidos");
        Map<String, BigDecimal> faturamentoCat = (Map<String, BigDecimal>) relatorio.get("faturamentoPorCategoria");

        int row = 0;
        grid.add(new Label("Faturamento Total:"), 0, row);
        grid.add(new Label(String.format("R$ %.2f", faturamento)), 1, row++);
        grid.add(new Label("Total de Pedidos:"), 0, row);
        grid.add(new Label(String.valueOf(totalPedidos)), 1, row++);
        grid.add(new Label("Pedidos Finalizados:"), 0, row);
        grid.add(new Label(String.valueOf(finalizados)), 1, row++);
        grid.add(new Label("Pedidos Cancelados:"), 0, row);
        grid.add(new Label(String.valueOf(cancelados)), 1, row++);

        VBox produtosBox = new VBox(5);
        produtosBox.setStyle("-fx-padding: 10; -fx-background-color: " + BRANCO + "; -fx-background-radius: 5;");
        produtosBox.getChildren().add(new Label("Produtos Mais Vendidos:") {{
            setStyle("-fx-font-weight: bold; -fx-text-fill: " + AZUL_PRINCIPAL + ";");
        }});
        produtosVendidos.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> produtosBox.getChildren().add(
                        new Label("  • " + e.getKey() + ": " + e.getValue() + " unidades")));

        VBox catBox = new VBox(5);
        catBox.setStyle("-fx-padding: 10; -fx-background-color: " + BRANCO + "; -fx-background-radius: 5;");
        catBox.getChildren().add(new Label("Faturamento por Categoria:") {{
            setStyle("-fx-font-weight: bold; -fx-text-fill: " + AZUL_PRINCIPAL + ";");
        }});
        faturamentoCat.forEach((cat, valor) ->
                catBox.getChildren().add(new Label("  • " + cat + ": R$ " + String.format("%.2f", valor))));

        HBox detalhes = new HBox(30, produtosBox, catBox);
        detalhes.setAlignment(Pos.CENTER);

        Button btnVoltar = new Button("Voltar");
        btnVoltar.setStyle(getBotaoEstilo(CINZA_GELO_ESCURO));
        btnVoltar.setOnAction(e -> mostrarTelaAdmin());

        vbox.getChildren().addAll(titulo, grid, detalhes, btnVoltar);
        root.setCenter(vbox);
    }

    private void mostrarRelatorioEstoque(BorderPane root) {
        Map<String, Object> relatorio = service.gerarRelatorioEstoque();

        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        vbox.setStyle(String.format("-fx-background-color: %s;", BRANCO));
        vbox.setAlignment(Pos.TOP_CENTER);

        Label titulo = new Label("RELATÓRIO DE ESTOQUE");
        titulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + AZUL_PRINCIPAL + ";");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);
        grid.setStyle("-fx-padding: 20; -fx-background-color: " + CINZA_GELO + "; -fx-background-radius: 5;");

        int totalProdutos = (int) relatorio.get("totalProdutos");
        int ativos = (int) relatorio.get("produtosAtivos");
        int inativos = (int) relatorio.get("produtosInativos");
        int totalItens = (int) relatorio.get("totalItensEstoque");
        BigDecimal valorTotal = (BigDecimal) relatorio.get("valorTotalEstoque");
        List<Produto> estoqueBaixo = (List<Produto>) relatorio.get("estoqueBaixo");

        int row = 0;
        grid.add(new Label("Total de Produtos:"), 0, row);
        grid.add(new Label(String.valueOf(totalProdutos)), 1, row++);
        grid.add(new Label("Produtos Ativos:"), 0, row);
        grid.add(new Label(String.valueOf(ativos)), 1, row++);
        grid.add(new Label("Produtos Inativos:"), 0, row);
        grid.add(new Label(String.valueOf(inativos)), 1, row++);
        grid.add(new Label("Total Itens em Estoque:"), 0, row);
        grid.add(new Label(String.valueOf(totalItens)), 1, row++);
        grid.add(new Label("Valor Total do Estoque:"), 0, row);
        grid.add(new Label(String.format("R$ %.2f", valorTotal)), 1, row++);

        VBox baixoBox = new VBox(5);
        baixoBox.setStyle("-fx-padding: 10; -fx-background-color: " + BRANCO + "; -fx-background-radius: 5;");
        baixoBox.getChildren().add(new Label("Produtos com Estoque Baixo (<5):") {{
            setStyle("-fx-font-weight: bold; -fx-text-fill: #d32f2f;");
        }});
        if (estoqueBaixo.isEmpty()) {
            baixoBox.getChildren().add(new Label("  Nenhum produto com estoque baixo"));
        } else {
            estoqueBaixo.forEach(p -> baixoBox.getChildren().add(
                    new Label("  • " + p.getNome() + " - Estoque: " + p.getQuantidadeEstoque())));
        }

        Button btnVoltar = new Button("Voltar");
        btnVoltar.setStyle(getBotaoEstilo(CINZA_GELO_ESCURO));
        btnVoltar.setOnAction(e -> mostrarTelaAdmin());

        vbox.getChildren().addAll(titulo, grid, baixoBox, btnVoltar);
        root.setCenter(vbox);
    }

    private void mostrarRelatorioClientes(BorderPane root) {
        Map<String, Object> relatorio = service.gerarRelatorioClientes();

        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        vbox.setStyle(String.format("-fx-background-color: %s;", BRANCO));
        vbox.setAlignment(Pos.TOP_CENTER);

        Label titulo = new Label("RELATÓRIO DE CLIENTES");
        titulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + AZUL_PRINCIPAL + ";");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);
        grid.setStyle("-fx-padding: 20; -fx-background-color: " + CINZA_GELO + "; -fx-background-radius: 5;");

        int totalClientes = (int) relatorio.get("totalClientes");
        Cliente clienteTop = (Cliente) relatorio.get("clienteTop");
        int maxCompras = (int) relatorio.get("maxCompras");
        Map<Cliente, Integer> comprasPorCliente = (Map<Cliente, Integer>) relatorio.get("comprasPorCliente");

        int row = 0;
        grid.add(new Label("Total de Clientes:"), 0, row);
        grid.add(new Label(String.valueOf(totalClientes)), 1, row++);

        if (clienteTop != null) {
            grid.add(new Label("Cliente Destaque:"), 0, row);
            grid.add(new Label(clienteTop.getNome() + " (" + maxCompras + " compras)"), 1, row++);
        }

        VBox comprasBox = new VBox(5);
        comprasBox.setStyle("-fx-padding: 10; -fx-background-color: " + BRANCO + "; -fx-background-radius: 5;");
        comprasBox.getChildren().add(new Label("Top 5 Clientes por Compras:") {{
            setStyle("-fx-font-weight: bold; -fx-text-fill: " + AZUL_PRINCIPAL + ";");
        }});
        comprasPorCliente.entrySet().stream()
                .sorted(Map.Entry.<Cliente, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> comprasBox.getChildren().add(
                        new Label("  • " + e.getKey().getNome() + ": " + e.getValue() + " compras")));

        Button btnVoltar = new Button("Voltar");
        btnVoltar.setStyle(getBotaoEstilo(CINZA_GELO_ESCURO));
        btnVoltar.setOnAction(e -> mostrarTelaAdmin());

        vbox.getChildren().addAll(titulo, grid, comprasBox, btnVoltar);
        root.setCenter(vbox);
    }

    private void mostrarListaProdutos(BorderPane root) {
        produtosObservable.setAll(service.listarProdutosAtivos());

        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        vbox.setStyle(String.format("-fx-background-color: %s;", BRANCO));

        Label titulo = new Label("NOSSOS PRODUTOS");
        titulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + AZUL_PRINCIPAL + ";");

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
        btnVoltar.setStyle(getBotaoEstilo(CINZA_GELO_ESCURO));
        btnVoltar.setOnAction(e -> mostrarTelaPrincipal());

        vbox.getChildren().addAll(titulo, scrollPane, btnVoltar);
        root.setCenter(vbox);
    }

    private VBox criarCardProduto(Produto produto) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle(String.format("-fx-background-color: %s; -fx-border-radius: 10; -fx-background-radius: 10; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);", BRANCO));
        card.setPrefWidth(250);
        card.setAlignment(Pos.CENTER);

        ImageView imagem = new ImageView();
        try {
            File file = new File(produto.getImagemPath());
            if (file.exists()) {
                Image img = new Image(file.toURI().toString(), 150, 150, true, true);
                imagem.setImage(img);
            } else {
                imagem.setFitWidth(150);
                imagem.setFitHeight(150);
                imagem.setStyle("-fx-background-color: " + CINZA_GELO + "; -fx-background-radius: 5;");
            }
        } catch (Exception e) {
            imagem.setFitWidth(150);
            imagem.setFitHeight(150);
            imagem.setStyle("-fx-background-color: " + CINZA_GELO + "; -fx-background-radius: 5;");
        }
        imagem.setPreserveRatio(true);

        Label nome = new Label(produto.getNome());
        nome.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + TEXTO_ESCURO + ";");
        nome.setWrapText(true);
        nome.setAlignment(Pos.CENTER);

        Label preco = new Label(String.format("R$ %.2f", produto.getPreco()));
        preco.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + AZUL_PRINCIPAL + ";");

        Label estoque = new Label("Em estoque: " + produto.getQuantidadeEstoque());
        estoque.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        Button btnComprar = new Button("Comprar");
        btnComprar.setStyle(getBotaoEstilo(AZUL_MEDIO));
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

        Spinner<Integer> spinnerQuantidade = new Spinner<>(1, produto.getQuantidadeEstoque(), 1);
        spinnerQuantidade.setEditable(true);
        spinnerQuantidade.setStyle("-fx-background-radius: 5;");

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
        carrinhoAtual = service.buscarCliente(clienteLogado.getId())
                .flatMap(c -> c.getHistoricoPedidos().stream()
                        .filter(p -> p.getId().equals(carrinhoAtual.getId()))
                        .findFirst())
                .orElse(carrinhoAtual);

        itensCarrinhoObservable.setAll(carrinhoAtual.getItens());

        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        vbox.setStyle(String.format("-fx-background-color: %s;", BRANCO));

        Label titulo = new Label("MEU CARRINHO");
        titulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + AZUL_PRINCIPAL + ";");

        if (itensCarrinhoObservable.isEmpty()) {
            vbox.getChildren().addAll(titulo, new Label("Seu carrinho está vazio."));
        } else {
            TableView<ItemPedido> table = new TableView<>(itensCarrinhoObservable);
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            table.setPrefHeight(300);

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
            lbTotal.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + AZUL_PRINCIPAL + ";");

            HBox botoes = new HBox(10);
            botoes.setAlignment(Pos.CENTER);

            Button btnFinalizar = new Button("Finalizar Compra");
            btnFinalizar.setStyle(getBotaoEstilo(AZUL_MEDIO));
            btnFinalizar.setOnAction(e -> mostrarDialogFinalizarCompra());

            Button btnRemover = new Button("Remover Item");
            btnRemover.setStyle(getBotaoEstilo(DOURADO));
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

            botoes.getChildren().addAll(btnFinalizar, btnRemover);
            vbox.getChildren().addAll(titulo, table, lbTotal, botoes);
        }

        Button btnVoltar = new Button("Voltar");
        btnVoltar.setStyle(getBotaoEstilo(CINZA_GELO_ESCURO));
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

        ComboBox<String> cbPagamento = new ComboBox<>();
        cbPagamento.getItems().addAll("Cartão de Crédito", "Cartão de Débito", "Boleto Bancário", "PIX", "Dinheiro");
        cbPagamento.setValue("Cartão de Crédito");
        cbPagamento.setStyle("-fx-background-radius: 5;");

        ComboBox<String> cbHorario = new ComboBox<>();
        cbHorario.getItems().addAll("Manhã (08:00 - 12:00)", "Tarde (12:00 - 18:00)", "Noite (18:00 - 22:00)");
        cbHorario.setValue("Tarde (12:00 - 18:00)");
        cbHorario.setStyle("-fx-background-radius: 5;");

        LocalDateTime dataEstimada = LocalDateTime.now().plusDays(5);
        String dataFormatada = dataEstimada.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        Label lbData = new Label("Previsão de entrega: " + dataFormatada);
        lbData.setStyle("-fx-font-weight: bold; -fx-text-fill: " + AZUL_PRINCIPAL + ";");

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

        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        vbox.setStyle(String.format("-fx-background-color: %s;", BRANCO));

        Label titulo = new Label("MEUS PEDIDOS");
        titulo.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + AZUL_PRINCIPAL + ";");

        if (pedidosObservable.isEmpty()) {
            vbox.getChildren().addAll(titulo, new Label("Você ainda não realizou nenhum pedido."));
        } else {
            TableView<Pedido> table = new TableView<>(pedidosObservable);
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            table.setPrefHeight(300);

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
            btnDetalhes.setStyle(getBotaoEstilo(AZUL_MEDIO));
            btnDetalhes.setOnAction(e -> {
                Pedido selecionado = table.getSelectionModel().getSelectedItem();
                if (selecionado != null) {
                    mostrarDetalhesPedido(selecionado);
                }
            });

            vbox.getChildren().addAll(titulo, table, btnDetalhes);
        }

        Button btnVoltar = new Button("Voltar");
        btnVoltar.setStyle(getBotaoEstilo(CINZA_GELO_ESCURO));
        btnVoltar.setOnAction(e -> mostrarTelaPrincipal());
        vbox.getChildren().add(btnVoltar);

        root.setCenter(vbox);
    }

    private void mostrarDetalhesPedido(Pedido pedido) {
        Stage stage = new Stage();
        stage.setTitle("Detalhes do Pedido #" + pedido.getId());

        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        vbox.setStyle(String.format("-fx-background-color: %s;", BRANCO));

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
        lbInfo.setStyle("-fx-font-size: 14px; -fx-padding: 15; -fx-background-color: " + CINZA_GELO + "; -fx-background-radius: 5;");

        TableView<ItemPedido> tableItens = new TableView<>();
        tableItens.setItems(FXCollections.observableArrayList(pedido.getItens()));
        tableItens.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableItens.setPrefHeight(200);

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
        lbTotal.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + AZUL_PRINCIPAL + ";");

        Button btnFechar = new Button("Fechar");
        btnFechar.setStyle(getBotaoEstilo(CINZA_GELO_ESCURO));
        btnFechar.setOnAction(e -> stage.close());

        vbox.getChildren().addAll(lbInfo, new Label("Itens do Pedido:") {{
            setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + AZUL_PRINCIPAL + ";");
        }}, tableItens, lbTotal, btnFechar);

        Scene scene = new Scene(vbox, 700, 600);
        stage.setScene(scene);
        stage.show();
    }

    private void mostrarPerfil(BorderPane root) {
        VBox vbox = new VBox(20);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(40));
        vbox.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: 10;", BRANCO));

        Label lbTitulo = new Label("Meu Perfil");
        lbTitulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + AZUL_PRINCIPAL + ";");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(15);
        grid.setAlignment(Pos.CENTER);

        int row = 0;
        grid.add(new Label("Nome:"), 0, row);
        grid.add(new Label(clienteLogado.getNome()), 1, row++);
        grid.add(new Label("E-mail:"), 0, row);
        grid.add(new Label(clienteLogado.getEmail()), 1, row++);
        grid.add(new Label("Telefone:"), 0, row);
        grid.add(new Label(clienteLogado.getTelefone()), 1, row++);
        grid.add(new Label("Endereço:"), 0, row);
        grid.add(new Label(clienteLogado.getEndereco()), 1, row++);
        grid.add(new Label("Cidade:"), 0, row);
        grid.add(new Label(clienteLogado.getCidade()), 1, row++);
        grid.add(new Label("Estado:"), 0, row);
        grid.add(new Label(clienteLogado.getEstado()), 1, row++);
        grid.add(new Label("CEP:"), 0, row);
        grid.add(new Label(clienteLogado.getCep()), 1, row++);
        grid.add(new Label("Cliente desde:"), 0, row);
        grid.add(new Label(clienteLogado.getDataCadastro().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))), 1, row++);

        Button btnVoltar = new Button("Voltar");
        btnVoltar.setStyle(getBotaoEstilo(CINZA_GELO_ESCURO));
        btnVoltar.setOnAction(e -> mostrarTelaPrincipal());

        vbox.getChildren().addAll(lbTitulo, grid, btnVoltar);
        root.setCenter(vbox);
    }

    private void mostrarFormAdicionarProduto(BorderPane root) {
        VBox vbox = new VBox(20);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(40));
        vbox.setStyle(String.format("-fx-background-color: %s; -fx-background-radius: 10;", BRANCO));

        Label lbTitulo = new Label("Adicionar Novo Produto");
        lbTitulo.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + AZUL_PRINCIPAL + ";");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);

        TextField tfNome = new TextField();
        tfNome.setPrefWidth(300);
        tfNome.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: " + CINZA_GELO_ESCURO + ";");

        TextField tfDesc = new TextField();
        tfDesc.setPrefWidth(300);
        tfDesc.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: " + CINZA_GELO_ESCURO + ";");

        TextField tfPreco = new TextField();
        tfPreco.setPrefWidth(300);
        tfPreco.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: " + CINZA_GELO_ESCURO + ";");

        TextField tfQtd = new TextField();
        tfQtd.setPrefWidth(300);
        tfQtd.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: " + CINZA_GELO_ESCURO + ";");

        TextField tfCategoria = new TextField();
        tfCategoria.setPrefWidth(300);
        tfCategoria.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: " + CINZA_GELO_ESCURO + ";");

        TextField tfImagem = new TextField();
        tfImagem.setPromptText("caminho/para/imagem.png");
        tfImagem.setPrefWidth(300);
        tfImagem.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: " + CINZA_GELO_ESCURO + ";");

        int row = 0;
        grid.add(new Label("Nome:*"), 0, row); grid.add(tfNome, 1, row++);
        grid.add(new Label("Descrição:"), 0, row); grid.add(tfDesc, 1, row++);
        grid.add(new Label("Preço:*"), 0, row); grid.add(tfPreco, 1, row++);
        grid.add(new Label("Quantidade:*"), 0, row); grid.add(tfQtd, 1, row++);
        grid.add(new Label("Categoria:"), 0, row); grid.add(tfCategoria, 1, row++);
        grid.add(new Label("Imagem:"), 0, row); grid.add(tfImagem, 1, row++);

        HBox botoes = new HBox(10);
        botoes.setAlignment(Pos.CENTER);

        Button btnSalvar = new Button("Salvar Produto");
        btnSalvar.setStyle(getBotaoEstilo(AZUL_MEDIO));

        Button btnVoltar = new Button("Cancelar");
        btnVoltar.setStyle(getBotaoEstilo(CINZA_GELO_ESCURO));

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

                mostrarTelaAdmin();
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Erro: " + ex.getMessage());
                alert.show();
            }
        });

        btnVoltar.setOnAction(e -> mostrarTelaAdmin());

        botoes.getChildren().addAll(btnSalvar, btnVoltar);
        vbox.getChildren().addAll(lbTitulo, grid, botoes);
        root.setCenter(vbox);
    }
}
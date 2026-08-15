import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * Servidor que aceita conexões de clientes e executa comandos no banco.
 *
 * Correções em relação à versão anterior:
 *  - Cada cliente tem seu próprio CommandProcessor + RelationalDB.Session,
 *    então transações de clientes diferentes não se misturam mais.
 *  - Pool de threads com limite (em vez de "new Thread()" ilimitado por
 *    conexão), evitando esgotar recursos do processo sob carga.
 *  - Timeout de leitura no socket, para não deixar thread presa para
 *    sempre em cliente que conecta e nunca envia nada.
 *  - Se a conexão cai com uma transação aberta, ela é abandonada
 *    explicitamente para liberar o "gate" de transação do banco.
 */
public class DBServer {
    private static final int MAX_CLIENTS = 64;
    private static final int SOCKET_READ_TIMEOUT_MS = 5 * 60 * 1000; // 5 min de ociosidade

    private final int port;
    private final RelationalDB db;
    private final ExecutorService pool;

    public DBServer(int port, String dbFile) throws IOException {
        this.port = port;
        this.db = new RelationalDB(dbFile);
        this.pool = new ThreadPoolExecutor(
                4, MAX_CLIENTS,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                r -> {
                    Thread t = new Thread(r);
                    t.setDaemon(true);
                    return t;
                },
                (r, executor) -> {
                    // Pool cheio: recusa educadamente em vez de derrubar o servidor
                    System.err.println("Limite de clientes simultâneos atingido, conexão recusada.");
                });
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Servidor ouvindo na porta " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Cliente conectado: " + clientSocket.getInetAddress());
                try {
                    clientSocket.setSoTimeout(SOCKET_READ_TIMEOUT_MS);
                    pool.execute(() -> handleClient(clientSocket));
                } catch (RejectedExecutionException ex) {
                    rejectClient(clientSocket);
                }
            }
        } finally {
            pool.shutdown();
        }
    }

    private void rejectClient(Socket socket) {
        try (DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
            out.writeUTF("Servidor ocupado, tente novamente em instantes.\n");
        } catch (IOException ignored) {
        } finally {
            try { socket.close(); } catch (IOException ignored) { }
        }
    }

    private void handleClient(Socket socket) {
        // Sessão de transação isolada para esta conexão
        RelationalDB.Session session = db.newSession();
        CommandProcessor processor = new CommandProcessor(db, session);
        try (DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
            while (true) {
                String command;
                try {
                    command = in.readUTF();
                } catch (SocketTimeoutException e) {
                    System.out.println("Cliente ocioso demais, encerrando conexão.");
                    break;
                }
                if (command.equalsIgnoreCase("EXIT")) {
                    break;
                }
                String result = processor.execute(command);
                out.writeUTF(result);
                out.flush();
            }
        } catch (EOFException e) {
            // Cliente desconectou
        } catch (IOException e) {
            System.err.println("Erro na comunicação com cliente: " + e.getMessage());
        } finally {
            processor.onDisconnect();
            try { socket.close(); } catch (IOException e) { /* ignora */ }
            System.out.println("Cliente desconectado.");
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Uso: java DBServer <porta> <arquivo-do-banco>");
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);
        String dbFile = args[1];
        try {
            DBServer server = new DBServer(port, dbFile);
            server.start();
        } catch (IOException e) {
            System.err.println("Erro ao iniciar servidor: " + e.getMessage());
        }
    }
}

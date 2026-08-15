import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Scanner;

/**
 * Cliente de linha de comando que conecta ao servidor e envia comandos.
 *
 * Atualizado para reconhecer o formato estruturado de resposta de SELECT
 * (marcadores §TABLE§ / §ENDTABLE§, ver CommandProcessor) e imprimir como
 * uma tabela ASCII alinhada, em vez de despejar o texto cru na tela.
 */
public class DBClient {
    private static final String TABLE_MARKER_START = "§TABLE§";
    private static final String TABLE_MARKER_END = "§ENDTABLE§";
    private static final String FIELD_SEP = "\u001F";

    private final String host;
    private final int port;

    public DBClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        try (Socket socket = new Socket(host, port);
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             Scanner scanner = new Scanner(System.in)) {
            System.out.println("Conectado ao servidor " + host + ":" + port);
            System.out.println("Digite comandos SQL (';' opcional no final) ou EXIT para sair.");
            while (true) {
                System.out.print("> ");
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;
                out.writeUTF(line);
                out.flush();
                if (line.equalsIgnoreCase("EXIT")) {
                    break;
                }
                String response = in.readUTF();
                printResponse(response);
            }
        } catch (IOException e) {
            System.err.println("Erro de comunicação: " + e.getMessage());
        }
    }

    private void printResponse(String response) {
        if (response.startsWith(TABLE_MARKER_START)) {
            printAsTable(response);
        } else {
            System.out.print(response);
        }
    }

    private void printAsTable(String response) {
        String[] lines = response.split("\n", -1);
        // lines[0] = marcador de início
        if (lines.length < 2) {
            System.out.println("(nenhuma linha)");
            return;
        }
        String[] columns = lines[1].split(FIELD_SEP, -1);
        List<String[]> rows = new ArrayList<>();
        for (int i = 2; i < lines.length; i++) {
            String l = lines[i];
            if (l.equals(TABLE_MARKER_END) || l.isEmpty()) continue;
            rows.add(l.split(FIELD_SEP, -1));
        }

        if (columns.length == 0 || (columns.length == 1 && columns[0].isEmpty())) {
            System.out.println("(tabela sem colunas)");
            return;
        }

        int[] widths = new int[columns.length];
        for (int i = 0; i < columns.length; i++) widths[i] = columns[i].length();
        for (String[] row : rows) {
            for (int i = 0; i < columns.length && i < row.length; i++) {
                widths[i] = Math.max(widths[i], row[i].length());
            }
        }

        printSeparator(widths);
        printRow(columns, widths);
        printSeparator(widths);
        if (rows.isEmpty()) {
            System.out.println("(nenhuma linha)");
        } else {
            for (String[] row : rows) printRow(row, widths);
            printSeparator(widths);
            System.out.println(rows.size() + " linha(s)");
        }
    }

    private void printSeparator(int[] widths) {
        StringBuilder sb = new StringBuilder("+");
        for (int w : widths) {
            for (int i = 0; i < w + 2; i++) sb.append('-');
            sb.append('+');
        }
        System.out.println(sb);
    }

    private void printRow(String[] values, int[] widths) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < widths.length; i++) {
            String v = i < values.length ? values[i] : "";
            sb.append(' ').append(v);
            for (int p = v.length(); p < widths[i]; p++) sb.append(' ');
            sb.append(" |");
        }
        System.out.println(sb);
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Uso: java DBClient <host> <porta>");
            System.exit(1);
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        new DBClient(host, port).start();
    }
}

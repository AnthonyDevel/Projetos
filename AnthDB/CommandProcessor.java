import java.io.*;
import java.util.*;

/**
 * Processa comandos de banco de dados e retorna o resultado como string.
 *
 * CORREÇÃO IMPORTANTE: a versão anterior trocava System.out globalmente
 * (System.setOut / System.setIn) para "capturar" a saída de cada comando.
 * Como System.out é um único objeto compartilhado pela JVM inteira, duas
 * threads (dois clientes) executando comandos ao mesmo tempo podiam roubar
 * a saída uma da outra. Agora cada método simplesmente monta e retorna a
 * String de resultado — nada de redirecionar streams globais.
 *
 * Cada instância de CommandProcessor deve ser usada por UMA conexão/sessão
 * de cliente por vez (o DBServer cria uma instância por cliente conectado).
 */
public class CommandProcessor {
    // Marcadores de protocolo para resultados tabulares (SELECT).
    // Formato da resposta de um SELECT bem-sucedido:
    //   §TABLE§
    //   col1<US>col2<US>col3
    //   val1<US>val2<US>val3
    //   ...
    //   §ENDTABLE§
    // <US> é o caractere de controle "Unit Separator" (0x1F), que não
    // aparece em texto digitado por usuário, então serve como delimitador
    // seguro de coluna sem precisar escapar vírgulas dentro de valores.
    // Isso permite que a GUI (e o CLI) montem uma tabela de verdade em vez
    // de depender de fazer parsing de "{chave=valor, ...}".
    public static final String TABLE_MARKER_START = "§TABLE§";
    public static final String TABLE_MARKER_END = "§ENDTABLE§";
    public static final String FIELD_SEP = "\u001F";

    private final RelationalDB db;
    private final RelationalDB.Session session;

    public CommandProcessor(RelationalDB db) {
        this(db, db.newSession());
    }

    public CommandProcessor(RelationalDB db, RelationalDB.Session session) {
        this.db = db;
        this.session = session;
    }

    /** Chamado pelo servidor quando a conexão do cliente cai, para não deixar transação presa. */
    public void onDisconnect() {
        db.abandonSessionTransaction(session);
    }

    /**
     * Executa um comando e retorna a saída como string.
     */
    public String execute(String line) {
        try {
            return executeInternal(line);
        } catch (Exception ex) {
            return "Erro: " + ex.getMessage() + "\n";
        }
    }

    private String executeInternal(String rawLine) throws IOException {
        String line = rawLine.trim();
        // Aceita ";" opcional no fim do comando (hábito de quem vem de
        // MySQL/Postgres), removendo-o antes de qualquer parsing.
        if (line.endsWith(";")) line = line.substring(0, line.length() - 1).trim();
        String[] tokens = line.split("\\s+");
        if (tokens.length == 0 || line.isEmpty()) return "";
        String cmd = tokens[0].toUpperCase();

        switch (cmd) {
            case "CREATE":
                return handleCreate(tokens, line);
            case "INSERT":
                return handleInsert(tokens, line);
            case "SELECT":
                return handleSelect(tokens, line);
            case "UPDATE":
                return handleUpdate(tokens, line);
            case "DELETE":
                return handleDelete(tokens, line);
            case "BEGIN":
                db.beginTransaction(session);
                return "Transação iniciada.\n";
            case "COMMIT":
                db.commitTransaction(session);
                return "Transação commitada.\n";
            case "ROLLBACK":
                db.rollbackTransaction(session);
                return "Transação revertida.\n";
            case "LIST":
                if (tokens.length > 1 && tokens[1].equalsIgnoreCase("TABLES")) {
                    return handleListTables();
                }
                return "Comando LIST inválido. Use LIST TABLES.\n";
            case "EXIT":
                return "EXIT\n";
            default:
                return "Comando desconhecido\n";
        }
    }

    private String handleCreate(String[] tokens, String line) throws IOException {
        if (tokens.length < 4 || !tokens[1].equalsIgnoreCase("TABLE")) {
            return "Uso: CREATE TABLE <nome> (<col> <tipo> [INDEX], ...)\n";
        }
        String tableName = tokens[2];
        int s = line.indexOf('(');
        int e = line.indexOf(')');
        if (s == -1 || e == -1) return "Definição inválida\n";
        String schemaDef = line.substring(s + 1, e).trim();
        db.createTable(tableName, schemaDef);
        return "Tabela criada: " + tableName + "\n";
    }

    private String handleInsert(String[] tokens, String line) throws IOException {
        if (tokens.length < 4 || !tokens[1].equalsIgnoreCase("INTO")) {
            return "Uso: INSERT INTO <nome> VALUES (...)\n";
        }
        String tableName = tokens[2];
        int s = line.indexOf('(');
        int e = line.lastIndexOf(')');
        if (s == -1 || e == -1) return "Valores inválidos\n";
        String valsStr = line.substring(s + 1, e);
        String[] vals = valsStr.split(",");
        String schema = db.getSchema(tableName);
        if (schema == null) return "Tabela não encontrada\n";
        List<String[]> cols = db.parseSchema(schema);
        if (vals.length != cols.size()) return "Número de valores incorreto\n";
        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < cols.size(); i++) {
            String cname = cols.get(i)[0];
            String ctype = cols.get(i)[1];
            String v = vals[i].trim();
            row.put(cname, db.convertColumnValue(v, ctype));
        }
        boolean applied = db.insert(session, tableName, row);
        return applied ? "OK\n" : "(pendente na transação) OK\n";
    }

    private String handleSelect(String[] tokens, String line) throws IOException {
        if (tokens.length < 3 || !tokens[1].equals("*") || !tokens[2].equalsIgnoreCase("FROM")) {
            return "Uso: SELECT * FROM <nome> [WHERE <condições>] [ORDER BY <col> [ASC|DESC]] [LIMIT <n>]\n";
        }
        String afterFrom = line.substring(line.toUpperCase().indexOf(" FROM ") + 6).trim();
        int whereIdx = afterFrom.toUpperCase().indexOf(" WHERE ");
        int orderIdx = afterFrom.toUpperCase().indexOf(" ORDER BY ");
        int limitIdx = afterFrom.toUpperCase().indexOf(" LIMIT ");
        int tableEnd = afterFrom.length();
        for (int idx : new int[]{whereIdx, orderIdx, limitIdx}) {
            if (idx != -1 && idx < tableEnd) tableEnd = idx;
        }
        String tableName = afterFrom.substring(0, tableEnd).trim();
        String whereClause = "";
        String orderByClause = "";
        String limitClause = "";
        if (whereIdx != -1) {
            int endWhere = afterFrom.length();
            if (orderIdx != -1 && orderIdx < endWhere) endWhere = orderIdx;
            if (limitIdx != -1 && limitIdx < endWhere) endWhere = limitIdx;
            whereClause = afterFrom.substring(whereIdx + 7, endWhere).trim();
        }
        if (orderIdx != -1) {
            int endOrder = afterFrom.length();
            if (limitIdx != -1 && limitIdx < endOrder) endOrder = limitIdx;
            orderByClause = afterFrom.substring(orderIdx + 9, endOrder).trim();
        }
        if (limitIdx != -1) {
            limitClause = afterFrom.substring(limitIdx + 7).trim();
        }
        List<LinkedHashMap<String, Object>> result = db.select(tableName, whereClause, orderByClause, limitClause);

        // Determina os nomes de coluna: usa a ordem das chaves da primeira
        // linha retornada; se não houver linhas, cai para a ordem do schema
        // (assim a GUI ainda consegue montar um cabeçalho de tabela vazia).
        List<String> columnNames;
        if (!result.isEmpty()) {
            columnNames = new ArrayList<>(result.get(0).keySet());
        } else {
            columnNames = new ArrayList<>();
            String schema = db.getSchema(tableName);
            if (schema != null) {
                for (String[] c : db.parseSchema(schema)) columnNames.add(c[0]);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(TABLE_MARKER_START).append("\n");
        sb.append(String.join(FIELD_SEP, columnNames)).append("\n");
        for (LinkedHashMap<String, Object> row : result) {
            List<String> vals = new ArrayList<>(columnNames.size());
            for (String col : columnNames) {
                Object v = row.get(col);
                vals.add(v == null ? "NULL" : String.valueOf(v));
            }
            sb.append(String.join(FIELD_SEP, vals)).append("\n");
        }
        sb.append(TABLE_MARKER_END).append("\n");
        return sb.toString();
    }

    private String handleUpdate(String[] tokens, String line) throws IOException {
        if (tokens.length < 4 || !tokens[2].equalsIgnoreCase("SET")) {
            return "Uso: UPDATE <nome> SET col=val,... WHERE ...\n";
        }
        String tableName = tokens[1];
        int whereIdx = line.toUpperCase().indexOf(" WHERE ");
        String setPart = line.substring(line.indexOf(" SET ") + 5, whereIdx == -1 ? line.length() : whereIdx);
        String wherePart = whereIdx == -1 ? "" : line.substring(whereIdx + 7);
        LinkedHashMap<String, Object> updates = new LinkedHashMap<>();
        String schema = db.getSchema(tableName);
        if (schema == null) return "Tabela não encontrada\n";
        for (String pair : setPart.split(",")) {
            String[] kv = pair.split("=");
            if (kv.length != 2) continue;
            String colName = kv[0].trim();
            String valStr = kv[1].trim();
            for (String[] c : db.parseSchema(schema)) {
                if (c[0].equals(colName)) {
                    updates.put(colName, db.convertColumnValue(valStr, c[1]));
                    break;
                }
            }
        }
        String wcol = null, op = "=";
        Object wval = null;
        if (!wherePart.isEmpty()) {
            String[] parts = wherePart.split(" ");
            if (parts.length >= 3) {
                wcol = parts[0];
                op = parts[1];
                wval = db.convertWhereValue(tableName, wcol, parts[2]);
            }
        }
        int affected = db.update(session, tableName, updates, wcol, op, wval);
        return affected + " linha(s) afetada(s)\n";
    }

    private String handleDelete(String[] tokens, String line) throws IOException {
        if (tokens.length < 3 || !tokens[1].equalsIgnoreCase("FROM")) {
            return "Uso: DELETE FROM <nome> WHERE ...\n";
        }
        String tableName = tokens[2];
        if (tokens.length > 3 && tokens[3].equalsIgnoreCase("WHERE")) {
            String wcol = tokens[4];
            String op = tokens[5];
            String valStr = tokens[6];
            Object wval = db.convertWhereValue(tableName, wcol, valStr);
            int affected = db.delete(session, tableName, wcol, op, wval);
            return affected + " linha(s) removida(s)\n";
        }
        return "WHERE é obrigatório para DELETE\n";
    }

    private String handleListTables() throws IOException {
        List<String> names = db.listTableNames();
        if (names.isEmpty()) return "(nenhuma tabela)\n";
        StringBuilder sb = new StringBuilder();
        for (String n : names) sb.append(n).append("\n");
        return sb.toString();
    }
}

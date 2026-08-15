import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.*;
import java.util.stream.*;

/**
 * Camada relacional sobre o SimpleDB.
 *
 * IMPORTANTE (correção de arquitetura): antes, o estado de transação
 * (inTransaction / pendingOperations) era um campo de instância único,
 * compartilhado por TODAS as conexões de cliente. Isso significava que,
 * em uso concorrente, um BEGIN de um cliente "vazava" para outro cliente.
 * Agora esse estado vive em um objeto Session, um por conexão/cliente,
 * e é passado explicitamente para os métodos transacionais.
 *
 * Também: os métodos aqui não escrevem mais em System.out. Eles retornam
 * valores ou lançam exceção; quem decide o que exibir é a camada de
 * apresentação (CommandProcessor).
 */
public class RelationalDB {

    // ---------- Sessão de cliente (transação isolada) ----------
    public static class Session {
        boolean inTransaction = false;
        List<String> pendingOperations = new ArrayList<>();
        Set<String> writtenKeysInTx = new HashSet<>();
    }

    // ---------- Classes auxiliares para condições ----------
    private static class Condition {
        String column;
        String operator;
        Object value;
    }

    private static class ConditionGroup {
        List<Condition> conditions = new ArrayList<>();
    }

    // ---------- Constantes e campos ----------
    private static final String SCHEMA_TABLE = "__schema__";
    private static final String WAL_FILE_SUFFIX = ".wal";
    private static final String INDEX_FILE_SUFFIX = ".btree";

    private final SimpleDB storage;
    private final Path walPath;
    private final String baseIndexPath;

    // Lock global de leitura/escrita do banco. O código original só
    // usava este lock dentro de begin/commit/rollback; select/insert/
    // update/delete mexiam no storage e nos índices sem nenhuma
    // coordenação entre threads. Agora todo acesso passa pelo lock.
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    // Só uma transação por vez pode estar aberta no banco inteiro.
    // Não é MVCC de verdade, mas é uma regra simples, explícita e segura
    // — em vez de silenciosamente misturar operações de sessões
    // diferentes na mesma transação, como acontecia antes.
    private final Object txGate = new Object();
    private Session activeTxOwner = null;

    private final Map<String, BTreeIndex> indexes = new HashMap<>();
    private final Map<String, String> indexTypes = new HashMap<>();

    private final LRUCache<String, LinkedHashMap<String, Object>> rowCache =
            new LRUCache<>(1000);

    public RelationalDB(String dbFile) throws IOException {
        this.storage = new SimpleDB(dbFile);
        this.walPath = Paths.get(dbFile + WAL_FILE_SUFFIX);
        this.baseIndexPath = dbFile + ".idx.";
        loadAllIndexes();
        recoverFromWAL();
        initializeSchemaTable();
    }

    public Session newSession() {
        return new Session();
    }

    // ---------- Recuperação do WAL ----------
    private void recoverFromWAL() throws IOException {
        if (!Files.exists(walPath)) return;
        List<String> lines = Files.readAllLines(walPath, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            Files.deleteIfExists(walPath);
            return;
        }
        String last = lines.get(lines.size() - 1);
        if (last.equals("COMMIT")) {
            boolean inBlock = false;
            for (String line : lines) {
                if (line.equals("BEGIN")) { inBlock = true; continue; }
                if (line.equals("COMMIT") || line.equals("ROLLBACK")) { inBlock = false; continue; }
                if (inBlock) {
                    try {
                        applyOperation(line);
                    } catch (ClassNotFoundException e) {
                        System.err.println("Erro ao reaplicar operação do WAL: " + e.getMessage());
                    }
                }
            }
        }
        Files.deleteIfExists(walPath);
    }

    private void applyOperation(String op) throws IOException, ClassNotFoundException {
        String[] parts = op.split(" ");
        if (parts.length < 2) return;
        String cmd = parts[0];
        String key = parts[1];
        if ("INSERT".equals(cmd) || "UPDATE".equals(cmd)) {
            byte[] valueBytes = Base64.getDecoder().decode(parts[2]);
            LinkedHashMap<String, Object> row = deserializeRow(valueBytes);
            if ("UPDATE".equals(cmd)) {
                LinkedHashMap<String, Object> oldRow = getRow(key);
                if (oldRow != null) {
                    removeFromIndexes(key, oldRow);
                }
            }
            putRow(key, row);
            updateIndexesAfterInsert(key, row);
        } else if ("DELETE".equals(cmd)) {
            LinkedHashMap<String, Object> oldRow = getRow(key);
            if (oldRow != null) {
                removeFromIndexes(key, oldRow);
                storage.delete(key);
                rowCache.remove(key);
            }
        }
    }

    // ---------- Inicialização e esquema ----------
    private void initializeSchemaTable() throws IOException {
        if (!storage.keys().contains(SCHEMA_TABLE)) {
            storage.putString(SCHEMA_TABLE, "");
        }
    }

    private void saveSchema(String tableName, String schemaDefinition) throws IOException {
        String current = storage.getString(SCHEMA_TABLE);
        if (current == null) current = "";
        Map<String, String> schemaMap = new LinkedHashMap<>();
        if (!current.isEmpty()) {
            String[] entries = current.split(";");
            for (String e : entries) {
                if (e.isEmpty()) continue;
                String[] parts = e.split(":");
                schemaMap.put(parts[0], parts[1]);
            }
        }
        schemaMap.put(tableName, schemaDefinition);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : schemaMap.entrySet()) {
            sb.append(e.getKey()).append(":").append(e.getValue()).append(";");
        }
        storage.putString(SCHEMA_TABLE, sb.toString());
    }

    public String getSchema(String tableName) throws IOException {
        String current = storage.getString(SCHEMA_TABLE);
        if (current == null) return null;
        String[] entries = current.split(";");
        for (String e : entries) {
            if (e.isEmpty()) continue;
            String[] parts = e.split(":");
            if (parts[0].equals(tableName)) return parts[1];
        }
        return null;
    }

    public List<String[]> parseSchema(String schemaDefinition) {
        List<String[]> cols = new ArrayList<>();
        if (schemaDefinition == null || schemaDefinition.isEmpty()) return cols;
        String[] parts = schemaDefinition.split(",");
        for (String part : parts) {
            String[] tokens = part.trim().split("\\s+");
            if (tokens.length >= 2) {
                String name = tokens[0];
                String type = tokens[1].toUpperCase();
                boolean indexed = tokens.length > 2 && tokens[2].equalsIgnoreCase("INDEX");
                cols.add(new String[]{name, type, indexed ? "true" : "false"});
            }
        }
        return cols;
    }

    // ---------- Índices secundários ----------
    private void loadAllIndexes() throws IOException {
        String current = storage.getString(SCHEMA_TABLE);
        if (current == null || current.isEmpty()) return;
        String[] entries = current.split(";");
        for (String entry : entries) {
            if (entry.isEmpty()) continue;
            String tableName = entry.split(":")[0];
            String schemaDef = entry.split(":")[1];
            List<String[]> cols = parseSchema(schemaDef);
            for (String[] col : cols) {
                if (col[2].equals("true")) {
                    String indexName = tableName + "." + col[0];
                    BTreeIndex idx = new BTreeIndex(Paths.get(baseIndexPath + indexName + INDEX_FILE_SUFFIX));
                    idx.load();
                    indexes.put(indexName, idx);
                    indexTypes.put(indexName, col[1]);
                }
            }
        }
    }

    private void saveAllIndexes() throws IOException {
        for (BTreeIndex idx : indexes.values()) {
            idx.save();
        }
    }

    private Object convertValue(String value, String type) {
        switch (type) {
            case "INT": return Integer.parseInt(value.trim());
            case "LONG": return Long.parseLong(value.trim());
            case "DOUBLE": return Double.parseDouble(value.trim());
            case "BOOLEAN": return Boolean.parseBoolean(value.trim());
            default: return value.trim();
        }
    }

    public Object convertWhereValue(String tableName, String colName, String valStr) throws IOException {
        String schema = getSchema(tableName);
        if (schema == null) return valStr;
        for (String[] c : parseSchema(schema)) {
            if (c[0].equals(colName)) {
                return convertValue(valStr, c[1]);
            }
        }
        return valStr;
    }

    public Object convertColumnValue(String value, String type) {
        return convertValue(value, type);
    }

    private void updateIndexesAfterInsert(String primaryKey, Map<String, Object> row) throws IOException {
        String tableName = primaryKey.split(":")[0];
        String schemaDef = getSchema(tableName);
        if (schemaDef == null) return;
        List<String[]> columns = parseSchema(schemaDef);
        for (String[] col : columns) {
            if (col[2].equals("true")) {
                String indexName = tableName + "." + col[0];
                Object value = row.get(col[0]);
                if (value != null) {
                    BTreeIndex idx = indexes.get(indexName);
                    if (idx == null) {
                        idx = new BTreeIndex(Paths.get(baseIndexPath + indexName + INDEX_FILE_SUFFIX));
                        indexes.put(indexName, idx);
                    }
                    idx.put(value, primaryKey);
                }
            }
        }
        saveAllIndexes();
    }

    private void removeFromIndexes(String primaryKey, Map<String, Object> row) throws IOException {
        String tableName = primaryKey.split(":")[0];
        String schemaDef = getSchema(tableName);
        if (schemaDef == null) return;
        List<String[]> columns = parseSchema(schemaDef);
        for (String[] col : columns) {
            if (col[2].equals("true")) {
                String indexName = tableName + "." + col[0];
                Object value = row.get(col[0]);
                if (value != null) {
                    BTreeIndex idx = indexes.get(indexName);
                    if (idx != null) {
                        idx.remove(value, primaryKey);
                    }
                }
            }
        }
        saveAllIndexes();
    }

    private Set<String> getCandidateKeys(String tableName, String whereCol, Object value) throws IOException {
        String indexName = tableName + "." + whereCol;
        BTreeIndex idx = indexes.get(indexName);
        if (idx != null) {
            return idx.get(value);
        } else {
            String prefix = tableName + ":";
            return storage.keys().stream()
                    .filter(k -> k.startsWith(prefix))
                    .collect(Collectors.toSet());
        }
    }

    // ---------- Operações de tabela ----------
    public void createTable(String tableName, String schemaDefinition) throws IOException {
        lock.writeLock().lock();
        try {
            if (getSchema(tableName) != null) {
                throw new IOException("Tabela já existe: " + tableName);
            }
            List<String[]> cols = parseSchema(schemaDefinition);
            if (cols.isEmpty()) throw new IOException("Esquema vazio");
            saveSchema(tableName, schemaDefinition);
            for (String[] col : cols) {
                if (col[2].equals("true")) {
                    String indexName = tableName + "." + col[0];
                    if (!indexes.containsKey(indexName)) {
                        BTreeIndex idx = new BTreeIndex(Paths.get(baseIndexPath + indexName + INDEX_FILE_SUFFIX));
                        indexes.put(indexName, idx);
                        indexTypes.put(indexName, col[1]);
                    }
                }
            }
            saveAllIndexes();
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ---------- INSERT ----------
    /** Retorna true se aplicado imediatamente; false se ficou pendente em transação. */
    public boolean insert(Session session, String tableName, LinkedHashMap<String, Object> row) throws IOException {
        String schemaDef = getSchema(tableName);
        if (schemaDef == null) throw new IOException("Tabela não encontrada: " + tableName);
        List<String[]> columns = parseSchema(schemaDef);
        String primaryKeyColumn = columns.get(0)[0];
        Object primaryKeyValue = row.get(primaryKeyColumn);
        if (primaryKeyValue == null) throw new IOException("Chave primária ausente");
        String storageKey = tableName + ":" + primaryKeyValue.toString();

        if (session.inTransaction) {
            requireTxOwnership(session);
            session.pendingOperations.add("INSERT " + storageKey + " " + serializeRow(row));
            session.writtenKeysInTx.add(storageKey);
            return false;
        } else {
            lock.writeLock().lock();
            try {
                putRow(storageKey, row);
                updateIndexesAfterInsert(storageKey, row);
            } finally {
                lock.writeLock().unlock();
            }
            return true;
        }
    }

    // ---------- UPDATE ----------
    /** Retorna o número de linhas afetadas. */
    public int update(Session session, String tableName, LinkedHashMap<String, Object> updates,
                       String whereCol, String operator, Object whereVal) throws IOException {
        if (session.inTransaction) requireTxOwnership(session);
        int affected = 0;

        lock.readLock().lock();
        Set<String> candidateKeys;
        try {
            candidateKeys = getCandidateKeys(tableName, whereCol, whereVal);
        } finally {
            lock.readLock().unlock();
        }
        if (candidateKeys.isEmpty()) return 0;

        for (String key : candidateKeys) {
            if (session.inTransaction) {
                LinkedHashMap<String, Object> oldRow = getRow(key);
                if (oldRow == null) continue;
                for (Map.Entry<String, Object> upd : updates.entrySet()) {
                    oldRow.put(upd.getKey(), upd.getValue());
                }
                session.pendingOperations.add("UPDATE " + key + " " + serializeRow(oldRow));
                session.writtenKeysInTx.add(key);
                affected++;
            } else {
                lock.writeLock().lock();
                try {
                    LinkedHashMap<String, Object> oldRow = getRow(key);
                    if (oldRow == null) continue;
                    for (Map.Entry<String, Object> upd : updates.entrySet()) {
                        oldRow.put(upd.getKey(), upd.getValue());
                    }
                    removeFromIndexes(key, oldRow);
                    putRow(key, oldRow);
                    updateIndexesAfterInsert(key, oldRow);
                    affected++;
                } finally {
                    lock.writeLock().unlock();
                }
            }
        }
        return affected;
    }

    // ---------- DELETE ----------
    public int delete(Session session, String tableName, String whereCol, String operator, Object whereVal) throws IOException {
        if (session.inTransaction) requireTxOwnership(session);
        int affected = 0;

        lock.readLock().lock();
        Set<String> candidateKeys;
        try {
            candidateKeys = getCandidateKeys(tableName, whereCol, whereVal);
        } finally {
            lock.readLock().unlock();
        }
        if (candidateKeys.isEmpty()) return 0;

        for (String key : candidateKeys) {
            if (session.inTransaction) {
                session.pendingOperations.add("DELETE " + key);
                session.writtenKeysInTx.add(key);
                affected++;
            } else {
                lock.writeLock().lock();
                try {
                    LinkedHashMap<String, Object> oldRow = getRow(key);
                    if (oldRow != null) {
                        removeFromIndexes(key, oldRow);
                        storage.delete(key);
                        rowCache.remove(key);
                        affected++;
                    }
                } finally {
                    lock.writeLock().unlock();
                }
            }
        }
        return affected;
    }

    // ---------- SELECT ----------
    public List<LinkedHashMap<String, Object>> select(String tableName, String whereClause,
                                                        String orderByClause, String limitClause) throws IOException {
        lock.readLock().lock();
        try {
            String schemaDef = getSchema(tableName);
            if (schemaDef == null) throw new IOException("Tabela não encontrada: " + tableName);
            List<String[]> columns = parseSchema(schemaDef);

            List<ConditionGroup> groups = parseWhereClause(whereClause);
            convertConditionValues(groups, columns);

            List<LinkedHashMap<String, Object>> result = new ArrayList<>();
            String prefix = tableName + ":";
            for (String key : storage.keys()) {
                if (!key.startsWith(prefix)) continue;
                LinkedHashMap<String, Object> row = getRow(key);
                if (row != null && evaluateWhere(row, groups)) {
                    result.add(row);
                }
            }

            if (orderByClause != null && !orderByClause.isEmpty()) {
                String[] parts = orderByClause.split("\\s+");
                String orderCol = parts[0];
                boolean ascending = !(parts.length > 1 && parts[1].equalsIgnoreCase("DESC"));
                final boolean asc = ascending;
                result.sort((r1, r2) -> {
                    Object v1 = r1.get(orderCol);
                    Object v2 = r2.get(orderCol);
                    if (v1 == null && v2 == null) return 0;
                    if (v1 == null) return asc ? -1 : 1;
                    if (v2 == null) return asc ? 1 : -1;
                    if (v1 instanceof Comparable && v2 instanceof Comparable) {
                        @SuppressWarnings("unchecked")
                        int cmp = ((Comparable<Object>) v1).compareTo(v2);
                        return asc ? cmp : -cmp;
                    }
                    return 0;
                });
            }

            if (limitClause != null && !limitClause.trim().isEmpty()) {
                int limit = Integer.parseInt(limitClause.trim());
                if (limit >= 0 && result.size() > limit) {
                    result = result.subList(0, limit);
                }
            }

            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    // ---------- Métodos auxiliares para WHERE ----------
    private List<ConditionGroup> parseWhereClause(String whereClause) throws IOException {
        List<ConditionGroup> groups = new ArrayList<>();
        if (whereClause == null || whereClause.trim().isEmpty()) return groups;

        List<String> orParts = splitByLogicalOperator(whereClause, "OR");
        for (String orPart : orParts) {
            ConditionGroup group = new ConditionGroup();
            List<String> andParts = splitByLogicalOperator(orPart, "AND");
            for (String andPart : andParts) {
                Condition cond = parseCondition(andPart.trim());
                if (cond != null) {
                    group.conditions.add(cond);
                }
            }
            if (!group.conditions.isEmpty()) {
                groups.add(group);
            }
        }
        return groups;
    }

    private List<String> splitByLogicalOperator(String input, String operator) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = 0;
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (!inQuotes && (c == '\'' || c == '"')) {
                inQuotes = true;
                quoteChar = c;
                current.append(c);
            } else if (inQuotes && c == quoteChar) {
                inQuotes = false;
                current.append(c);
            } else if (!inQuotes && i + operator.length() <= input.length()
                    && input.substring(i, i + operator.length()).equalsIgnoreCase(operator)) {
                boolean leftOk = (i == 0) || Character.isWhitespace(input.charAt(i - 1));
                boolean rightOk = (i + operator.length() == input.length()) || Character.isWhitespace(input.charAt(i + operator.length()));
                if (leftOk && rightOk) {
                    parts.add(current.toString().trim());
                    current.setLength(0);
                    i += operator.length();
                    continue;
                }
            }
            current.append(c);
            i++;
        }
        if (current.length() > 0) {
            parts.add(current.toString().trim());
        }
        return parts;
    }

    private Condition parseCondition(String condStr) throws IOException {
        condStr = condStr.trim();
        if (condStr.isEmpty()) return null;

        String[] operators = {">=", "<=", "!=", "<>", "=", "==", ">", "<"};
        String foundOp = null;
        int opIndex = -1;
        for (String op : operators) {
            int idx = condStr.indexOf(op);
            if (idx != -1) {
                foundOp = op;
                opIndex = idx;
                break;
            }
        }
        if (foundOp == null) throw new IOException("Operador não reconhecido em: " + condStr);

        String column = condStr.substring(0, opIndex).trim();
        String valueStr = condStr.substring(opIndex + foundOp.length()).trim();
        if (valueStr.length() >= 2) {
            char first = valueStr.charAt(0);
            char last = valueStr.charAt(valueStr.length() - 1);
            if ((first == '\'' && last == '\'') || (first == '"' && last == '"')) {
                valueStr = valueStr.substring(1, valueStr.length() - 1);
            }
        }

        Condition cond = new Condition();
        cond.column = column;
        cond.operator = foundOp;
        cond.value = valueStr;
        return cond;
    }

    private void convertConditionValues(List<ConditionGroup> groups, List<String[]> columns) {
        for (ConditionGroup group : groups) {
            for (Condition cond : group.conditions) {
                for (String[] col : columns) {
                    if (col[0].equals(cond.column)) {
                        cond.value = convertValue((String) cond.value, col[1]);
                        break;
                    }
                }
            }
        }
    }

    private boolean evaluateWhere(LinkedHashMap<String, Object> row, List<ConditionGroup> groups) {
        if (groups.isEmpty()) return true;
        for (ConditionGroup group : groups) {
            boolean groupResult = true;
            for (Condition cond : group.conditions) {
                Object cell = row.get(cond.column);
                boolean condResult = evaluateCondition(cell, cond.operator, cond.value);
                groupResult = groupResult && condResult;
            }
            if (groupResult) return true;
        }
        return false;
    }

    private boolean evaluateCondition(Object cell, String operator, Object value) {
        if (cell == null || value == null) return false;
        switch (operator) {
            case "=":
            case "==":
                return cell.equals(value);
            case ">":
                if (cell instanceof Number && value instanceof Number) return ((Number) cell).doubleValue() > ((Number) value).doubleValue();
                return false;
            case "<":
                if (cell instanceof Number && value instanceof Number) return ((Number) cell).doubleValue() < ((Number) value).doubleValue();
                return false;
            case ">=":
                if (cell instanceof Number && value instanceof Number) return ((Number) cell).doubleValue() >= ((Number) value).doubleValue();
                return false;
            case "<=":
                if (cell instanceof Number && value instanceof Number) return ((Number) cell).doubleValue() <= ((Number) value).doubleValue();
                return false;
            case "!=":
            case "<>":
                return !cell.equals(value);
            default:
                return false;
        }
    }

    // ---------- Transações (agora por Session, não mais campo global) ----------
    private void requireTxOwnership(Session session) {
        synchronized (txGate) {
            if (activeTxOwner != session) {
                throw new IllegalStateException("Sessão sem transação ativa reconhecida pelo banco");
            }
        }
    }

    public void beginTransaction(Session session) {
        synchronized (txGate) {
            if (session.inTransaction) throw new IllegalStateException("Transação já ativa nesta sessão");
            if (activeTxOwner != null) {
                throw new IllegalStateException("Outra sessão já possui uma transação aberta; tente novamente em instantes");
            }
            activeTxOwner = session;
            session.inTransaction = true;
            session.pendingOperations.clear();
            session.writtenKeysInTx.clear();
        }
    }

    public void commitTransaction(Session session) throws IOException {
        synchronized (txGate) {
            if (!session.inTransaction) throw new IllegalStateException("Nenhuma transação ativa nesta sessão");
        }
        lock.writeLock().lock();
        try {
            writeWAL("BEGIN");
            for (String op : session.pendingOperations) writeWAL(op);
            writeWAL("COMMIT");
            for (String op : session.pendingOperations) {
                try {
                    applyOperation(op);
                } catch (ClassNotFoundException e) {
                    throw new IOException("Erro ao aplicar operação: " + e.getMessage(), e);
                }
            }
            Files.deleteIfExists(walPath);
        } finally {
            lock.writeLock().unlock();
            synchronized (txGate) {
                session.pendingOperations.clear();
                session.writtenKeysInTx.clear();
                session.inTransaction = false;
                activeTxOwner = null;
            }
        }
    }

    public void rollbackTransaction(Session session) throws IOException {
        synchronized (txGate) {
            if (!session.inTransaction) throw new IllegalStateException("Nenhuma transação ativa nesta sessão");
            session.pendingOperations.clear();
            session.writtenKeysInTx.clear();
            session.inTransaction = false;
            activeTxOwner = null;
        }
    }

    /** Chamado quando uma conexão cai/fecha com transação pendente: libera o gate global. */
    public void abandonSessionTransaction(Session session) {
        synchronized (txGate) {
            if (session.inTransaction && activeTxOwner == session) {
                session.pendingOperations.clear();
                session.writtenKeysInTx.clear();
                session.inTransaction = false;
                activeTxOwner = null;
            }
        }
    }

    private void writeWAL(String line) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(walPath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(line);
            writer.newLine();
        }
    }

    // ---------- Serialização e cache ----------
    @SuppressWarnings("unchecked")
    private LinkedHashMap<String, Object> getRow(String key) throws IOException {
        LinkedHashMap<String, Object> cached = rowCache.get(key);
        if (cached != null) return cached;
        try {
            Object obj = storage.getObject(key);
            if (obj instanceof LinkedHashMap) {
                LinkedHashMap<String, Object> row = (LinkedHashMap<String, Object>) obj;
                rowCache.put(key, row);
                return row;
            }
            return null;
        } catch (ClassNotFoundException e) {
            throw new IOException("Classe não encontrada ao ler linha: " + e.getMessage(), e);
        }
    }

    private void putRow(String key, LinkedHashMap<String, Object> row) throws IOException {
        storage.putObject(key, row);
        rowCache.remove(key);
    }

    private String serializeRow(LinkedHashMap<String, Object> row) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(row);
        }
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    @SuppressWarnings("unchecked")
    private LinkedHashMap<String, Object> deserializeRow(byte[] data) throws IOException {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data))) {
            Object obj = ois.readObject();
            if (obj instanceof LinkedHashMap) {
                return (LinkedHashMap<String, Object>) obj;
            }
            throw new IOException("Formato de linha inválido");
        } catch (ClassNotFoundException e) {
            throw new IOException("Classe não encontrada ao desserializar", e);
        }
    }

    // ---------- Utilitários ----------
    public List<String> listTableNames() throws IOException {
        List<String> names = new ArrayList<>();
        String current = storage.getString(SCHEMA_TABLE);
        if (current == null || current.isEmpty()) return names;
        for (String e : current.split(";")) {
            if (!e.isEmpty()) names.add(e.split(":")[0]);
        }
        return names;
    }

    public void close() throws IOException {
        lock.writeLock().lock();
        try {
            saveAllIndexes();
            storage.close();
        } finally {
            lock.writeLock().unlock();
        }
    }
}

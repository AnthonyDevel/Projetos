import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.*;

public class SimpleDB {

    // Enumeração dos tipos suportados
    public enum ValueType {
        STRING((byte) 1),
        INT((byte) 2),
        LONG((byte) 3),
        DOUBLE((byte) 4),
        BOOLEAN((byte) 5),
        OBJECT((byte) 6);

        private final byte code;
        ValueType(byte code) { this.code = code; }
        public byte getCode() { return code; }

        public static ValueType fromCode(byte code) {
            for (ValueType t : values()) {
                if (t.code == code) return t;
            }
            throw new IllegalArgumentException("Tipo desconhecido: " + code);
        }
    }

    // Metadados de índices secundários
    public static class IndexMetadata {
        public final String name;
        public final ValueType type;

        public IndexMetadata(String name, ValueType type) {
            this.name = name;
            this.type = type;
        }
    }

    private final Path filePath;
    private final Path indexPath;
    private final Path secondaryIndexPath;
    private static final String INDEX_SUFFIX = ".idx";
    private static final String SECONDARY_INDEX_SUFFIX = ".idx2";

    private final Map<String, Long> index;
    private final Map<String, Map<Object, Set<String>>> secondaryIndexes;
    private final Map<String, IndexMetadata> indexMetadata;
    private final ReentrantReadWriteLock lock;
    private RandomAccessFile raf;

    public SimpleDB(String filename) throws IOException {
        this.filePath = Paths.get(filename);
        this.indexPath = Paths.get(filename + INDEX_SUFFIX);
        this.secondaryIndexPath = Paths.get(filename + SECONDARY_INDEX_SUFFIX);
        this.index = new HashMap<>();
        this.secondaryIndexes = new HashMap<>();
        this.indexMetadata = new HashMap<>();
        this.lock = new ReentrantReadWriteLock();

        boolean dataExists = Files.exists(filePath);
        boolean indexExists = Files.exists(indexPath);

        if (dataExists) {
            if (indexExists) {
                try {
                    loadIndexFromFile();
                } catch (IOException | NumberFormatException e) {
                    System.err.println("Aviso: índice corrompido, reconstruindo...");
                    raf = new RandomAccessFile(filePath.toFile(), "rw");
                    buildIndexFromDataFile();
                    saveIndexToFile();
                }
            } else {
                raf = new RandomAccessFile(filePath.toFile(), "rw");
                buildIndexFromDataFile();
                saveIndexToFile();
            }
        } else {
            raf = new RandomAccessFile(filePath.toFile(), "rw");
        }

        loadSecondaryIndexes();
    }

    // ---------- Persistência do índice principal ----------
    private void loadIndexFromFile() throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(indexPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split("\\t");
                if (parts.length != 2) {
                    throw new IOException("Formato inválido no índice: " + line);
                }
                String key = parts[0];
                long offset = Long.parseLong(parts[1]);
                index.put(key, offset);
            }
        }
        raf = new RandomAccessFile(filePath.toFile(), "rw");
    }

    private void buildIndexFromDataFile() throws IOException {
        raf.seek(0);
        while (raf.getFilePointer() < raf.length()) {
            long offset = raf.getFilePointer();
            int keyLength = raf.readInt();
            byte[] keyBytes = new byte[keyLength];
            raf.readFully(keyBytes);
            String key = new String(keyBytes, StandardCharsets.UTF_8);
            int valueLength = raf.readInt();
            raf.skipBytes(valueLength + Long.BYTES);
            index.put(key, offset);
        }
    }

    private void saveIndexToFile() throws IOException {
        Path parent = indexPath.toAbsolutePath().getParent();
        Path tempFile = Files.createTempFile(parent, "idx", ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, Long> entry : index.entrySet()) {
                writer.write(entry.getKey());
                writer.write('\t');
                writer.write(Long.toString(entry.getValue()));
                writer.newLine();
            }
        }
        Files.move(tempFile, indexPath, StandardCopyOption.REPLACE_EXISTING);
    }

    // ---------- Persistência dos índices secundários ----------
    private void loadSecondaryIndexes() throws IOException {
        if (!Files.exists(secondaryIndexPath)) return;

        try (BufferedReader reader = Files.newBufferedReader(secondaryIndexPath, StandardCharsets.UTF_8)) {
            String line;
            String currentIndexName = null;
            ValueType currentType = null;
            Map<Object, Set<String>> currentIndex = null;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;

                if (line.startsWith("#INDEX:")) {
                    if (currentIndexName != null) {
                        secondaryIndexes.put(currentIndexName, currentIndex);
                        indexMetadata.put(currentIndexName, new IndexMetadata(currentIndexName, currentType));
                    }
                    String[] parts = line.substring(7).split("\\t");
                    currentIndexName = parts[0];
                    currentType = ValueType.valueOf(parts[1]);
                    currentIndex = new HashMap<>();
                } else if (line.startsWith("#END")) {
                    if (currentIndexName != null) {
                        secondaryIndexes.put(currentIndexName, currentIndex);
                        indexMetadata.put(currentIndexName, new IndexMetadata(currentIndexName, currentType));
                    }
                } else if (currentIndex != null) {
                    String[] parts = line.split("\\t");
                    if (parts.length == 2) {
                        Object value = parseValue(parts[0], currentType);
                        Set<String> keys = new HashSet<>(Arrays.asList(parts[1].split(",")));
                        currentIndex.put(value, keys);
                    }
                }
            }
        }
    }

    private void saveSecondaryIndexes() throws IOException {
        Path parent = secondaryIndexPath.toAbsolutePath().getParent();
        Path tempFile = Files.createTempFile(parent, "idx2", ".tmp");

        try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, Map<Object, Set<String>>> indexEntry : secondaryIndexes.entrySet()) {
                String indexName = indexEntry.getKey();
                IndexMetadata metadata = indexMetadata.get(indexName);

                writer.write("#INDEX:" + indexName + "\t" + metadata.type.name());
                writer.newLine();

                for (Map.Entry<Object, Set<String>> valueEntry : indexEntry.getValue().entrySet()) {
                    writer.write(valueToString(valueEntry.getKey()) + "\t");
                    writer.write(String.join(",", valueEntry.getValue()));
                    writer.newLine();
                }

                writer.write("#END");
                writer.newLine();
            }
        }

        Files.move(tempFile, secondaryIndexPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private Object parseValue(String str, ValueType type) {
        switch (type) {
            case INT: return Integer.parseInt(str);
            case LONG: return Long.parseLong(str);
            case DOUBLE: return Double.parseDouble(str);
            case BOOLEAN: return Boolean.parseBoolean(str);
            case STRING: return str;
            default: return str;
        }
    }

    private String valueToString(Object value) {
        return value.toString();
    }

    // ---------- API de índices secundários ----------
    public void createSecondaryIndex(String indexName, ValueType type) throws IOException {
        lock.writeLock().lock();
        try {
            if (secondaryIndexes.containsKey(indexName)) {
                throw new IOException("Índice já existe: " + indexName);
            }
            secondaryIndexes.put(indexName, new HashMap<>());
            indexMetadata.put(indexName, new IndexMetadata(indexName, type));
            saveSecondaryIndexes();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void putIndexed(String key, Object value, String indexName) throws IOException {
        lock.writeLock().lock();
        try {
            IndexMetadata metadata = indexMetadata.get(indexName);
            if (metadata == null) {
                throw new IOException("Índice não encontrado: " + indexName);
            }

            Object typedValue = value;
            if (metadata.type == ValueType.INT && value instanceof String) {
                typedValue = Integer.parseInt((String) value);
            } else if (metadata.type == ValueType.LONG && value instanceof String) {
                typedValue = Long.parseLong((String) value);
            } else if (metadata.type == ValueType.DOUBLE && value instanceof String) {
                typedValue = Double.parseDouble((String) value);
            }

            switch (metadata.type) {
                case INT: putInt(key, (Integer) typedValue); break;
                case LONG: putLong(key, (Long) typedValue); break;
                case DOUBLE: putDouble(key, (Double) typedValue); break;
                case BOOLEAN: putBoolean(key, (Boolean) typedValue); break;
                case STRING: putString(key, (String) typedValue); break;
                default: throw new IOException("Tipo não suportado para índice: " + metadata.type);
            }

            Map<Object, Set<String>> indexData = secondaryIndexes.get(indexName);
            Set<String> keys = indexData.computeIfAbsent(typedValue, k -> new HashSet<>());
            keys.add(key);

            saveSecondaryIndexes();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Set<String> findByIndex(String indexName, Object value) throws IOException {
        lock.readLock().lock();
        try {
            Map<Object, Set<String>> indexData = secondaryIndexes.get(indexName);
            if (indexData == null) {
                throw new IOException("Índice não encontrado: " + indexName);
            }

            IndexMetadata metadata = indexMetadata.get(indexName);
            Object typedValue = value;
            if (metadata.type == ValueType.INT && value instanceof String) {
                typedValue = Integer.parseInt((String) value);
            } else if (metadata.type == ValueType.LONG && value instanceof String) {
                typedValue = Long.parseLong((String) value);
            } else if (metadata.type == ValueType.DOUBLE && value instanceof String) {
                typedValue = Double.parseDouble((String) value);
            }

            return indexData.getOrDefault(typedValue, new HashSet<>());
        } finally {
            lock.readLock().unlock();
        }
    }

    public Set<String> findByRange(String indexName, Object min, Object max) throws IOException {
        lock.readLock().lock();
        try {
            Map<Object, Set<String>> indexData = secondaryIndexes.get(indexName);
            if (indexData == null) {
                throw new IOException("Índice não encontrado: " + indexName);
            }

            IndexMetadata metadata = indexMetadata.get(indexName);
            if (metadata.type != ValueType.INT && metadata.type != ValueType.LONG && 
                metadata.type != ValueType.DOUBLE) {
                throw new IOException("Busca por faixa só funciona para tipos numéricos");
            }

            Set<String> result = new HashSet<>();

            if (metadata.type == ValueType.INT) {
                int minVal = (Integer) min;
                int maxVal = (Integer) max;
                for (Map.Entry<Object, Set<String>> entry : indexData.entrySet()) {
                    int key = (Integer) entry.getKey();
                    if (key >= minVal && key <= maxVal) {
                        result.addAll(entry.getValue());
                    }
                }
            } else if (metadata.type == ValueType.LONG) {
                long minVal = (Long) min;
                long maxVal = (Long) max;
                for (Map.Entry<Object, Set<String>> entry : indexData.entrySet()) {
                    long key = (Long) entry.getKey();
                    if (key >= minVal && key <= maxVal) {
                        result.addAll(entry.getValue());
                    }
                }
            } else if (metadata.type == ValueType.DOUBLE) {
                double minVal = (Double) min;
                double maxVal = (Double) max;
                for (Map.Entry<Object, Set<String>> entry : indexData.entrySet()) {
                    double key = (Double) entry.getKey();
                    if (key >= minVal && key <= maxVal) {
                        result.addAll(entry.getValue());
                    }
                }
            }

            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    public Set<String> listSecondaryIndexes() {
        lock.readLock().lock();
        try {
            return new HashSet<>(secondaryIndexes.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }

    // ---------- Métodos básicos de armazenamento tipado ----------
    private void putRaw(String key, byte[] value) throws IOException {
        lock.writeLock().lock();
        try {
            long offset = raf.length();
            raf.seek(offset);

            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            raf.writeInt(keyBytes.length);
            raf.write(keyBytes);
            raf.writeInt(value.length);
            raf.write(value);
            raf.writeLong(System.currentTimeMillis());

            index.put(key, offset);
            saveIndexToFile();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private byte[] getRaw(String key) throws IOException {
        lock.readLock().lock();
        try {
            Long offset = index.get(key);
            if (offset == null) return null;

            raf.seek(offset);
            int keyLength = raf.readInt();
            raf.skipBytes(keyLength);
            int valueLength = raf.readInt();
            byte[] value = new byte[valueLength];
            raf.readFully(value);
            return value;
        } finally {
            lock.readLock().unlock();
        }
    }

    private void putWithType(String key, byte[] data, ValueType type) throws IOException {
        byte[] valueWithType = new byte[data.length + 1];
        valueWithType[0] = type.getCode();
        System.arraycopy(data, 0, valueWithType, 1, data.length);
        putRaw(key, valueWithType);
    }

    public void putString(String key, String value) throws IOException {
        putWithType(key, value.getBytes(StandardCharsets.UTF_8), ValueType.STRING);
    }

    public void putInt(String key, int value) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES);
        buf.putInt(value);
        putWithType(key, buf.array(), ValueType.INT);
    }

    public void putLong(String key, long value) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);
        buf.putLong(value);
        putWithType(key, buf.array(), ValueType.LONG);
    }

    public void putDouble(String key, double value) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(Double.BYTES);
        buf.putDouble(value);
        putWithType(key, buf.array(), ValueType.DOUBLE);
    }

    public void putBoolean(String key, boolean value) throws IOException {
        byte[] data = new byte[] { (byte) (value ? 1 : 0) };
        putWithType(key, data, ValueType.BOOLEAN);
    }

    public void putObject(String key, Serializable obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        putWithType(key, baos.toByteArray(), ValueType.OBJECT);
    }

    public String getString(String key) throws IOException {
        byte[] raw = getRaw(key);
        if (raw == null) return null;
        checkType(raw, ValueType.STRING);
        return new String(raw, 1, raw.length - 1, StandardCharsets.UTF_8);
    }

    public int getInt(String key) throws IOException {
        byte[] raw = getRaw(key);
        if (raw == null) return 0;
        checkType(raw, ValueType.INT);
        return ByteBuffer.wrap(raw, 1, Integer.BYTES).getInt();
    }

    public long getLong(String key) throws IOException {
        byte[] raw = getRaw(key);
        if (raw == null) return 0L;
        checkType(raw, ValueType.LONG);
        return ByteBuffer.wrap(raw, 1, Long.BYTES).getLong();
    }

    public double getDouble(String key) throws IOException {
        byte[] raw = getRaw(key);
        if (raw == null) return 0.0;
        checkType(raw, ValueType.DOUBLE);
        return ByteBuffer.wrap(raw, 1, Double.BYTES).getDouble();
    }

    public boolean getBoolean(String key) throws IOException {
        byte[] raw = getRaw(key);
        if (raw == null) return false;
        checkType(raw, ValueType.BOOLEAN);
        return raw[1] != 0;
    }

    public Object getObject(String key) throws IOException, ClassNotFoundException {
        byte[] raw = getRaw(key);
        if (raw == null) return null;
        checkType(raw, ValueType.OBJECT);
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(raw, 1, raw.length - 1))) {
            return ois.readObject();
        }
    }

    public String getAsString(String key) throws IOException {
        byte[] raw = getRaw(key);
        if (raw == null) return null;
        ValueType type = ValueType.fromCode(raw[0]);
        switch (type) {
            case STRING:
                return new String(raw, 1, raw.length - 1, StandardCharsets.UTF_8);
            case INT:
                return Integer.toString(ByteBuffer.wrap(raw, 1, Integer.BYTES).getInt());
            case LONG:
                return Long.toString(ByteBuffer.wrap(raw, 1, Long.BYTES).getLong());
            case DOUBLE:
                return Double.toString(ByteBuffer.wrap(raw, 1, Double.BYTES).getDouble());
            case BOOLEAN:
                return Boolean.toString(raw[1] != 0);
            case OBJECT:
                return "[objeto serializado]";
            default:
                return "?";
        }
    }

    private void checkType(byte[] raw, ValueType expected) throws IOException {
        if (raw.length < 1) throw new IOException("Dado corrompido");
        ValueType actual = ValueType.fromCode(raw[0]);
        if (actual != expected) {
            throw new IOException("Tipo incorreto: esperado " + expected + ", encontrado " + actual);
        }
    }

    public void delete(String key) throws IOException {
        lock.writeLock().lock();
        try {
            if (!index.containsKey(key)) return;
            index.remove(key);
            compactFile();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void compactFile() throws IOException {
        Path tempFile = Files.createTempFile("simpledb", ".tmp");
        raf.close();
        try (RandomAccessFile newRaf = new RandomAccessFile(tempFile.toFile(), "rw")) {
            List<Map.Entry<String, Long>> entries = new ArrayList<>(index.entrySet());
            for (Map.Entry<String, Long> entry : entries) {
                String key = entry.getKey();
                long oldOffset = entry.getValue();

                raf.seek(oldOffset);
                int keyLength = raf.readInt();
                byte[] keyBytes = new byte[keyLength];
                raf.readFully(keyBytes);
                int valueLength = raf.readInt();
                byte[] value = new byte[valueLength];
                raf.readFully(value);
                raf.readLong();

                long newOffset = newRaf.length();
                newRaf.writeInt(keyLength);
                newRaf.write(keyBytes);
                newRaf.writeInt(valueLength);
                newRaf.write(value);
                newRaf.writeLong(System.currentTimeMillis());

                index.put(key, newOffset);
            }
        }
        Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING);
        raf = new RandomAccessFile(filePath.toFile(), "rw");
        saveIndexToFile();
    }

    public Set<String> keys() {
        lock.readLock().lock();
        try {
            return new HashSet<>(index.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }

    public void close() throws IOException {
        raf.close();
    }

    // ---------- Interface de linha de comando ----------
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Uso: java SimpleDB <arquivo-do-banco>");
            System.exit(1);
        }

        String dbFile = args[0];
        SimpleDB db = null;
        try {
            db = new SimpleDB(dbFile);
            System.out.println("Banco de dados aberto: " + dbFile);
            System.out.println("Comandos disponíveis:");
            System.out.println("  PUT <chave> <valor>       - insere string");
            System.out.println("  PUT_INT <chave> <valor>   - insere inteiro");
            System.out.println("  PUT_LONG <chave> <valor>  - insere long");
            System.out.println("  PUT_DOUBLE <chave> <valor>- insere double");
            System.out.println("  PUT_BOOL <chave> <valor>  - insere booleano (true/false)");
            System.out.println("  GET <chave>               - consulta valor");
            System.out.println("  DELETE <chave>            - remove");
            System.out.println("  KEYS                      - lista todas as chaves");
            System.out.println("  CREATE_INDEX <nome> <tipo>- cria índice secundário (STRING, INT, DOUBLE, etc)");
            System.out.println("  PUT_INDEXED <chave> <valor> <índice> - insere valor indexado");
            System.out.println("  FIND <índice> <valor>     - busca chaves por valor no índice");
            System.out.println("  RANGE <índice> <min> <max>- busca chaves em faixa numérica");
            System.out.println("  LIST_INDEXES              - lista índices secundários");
            System.out.println("  EXIT                      - sai do programa");
            System.out.println();

            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("> ");
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+", 4);
                String command = parts[0].toUpperCase();

                try {
                    switch (command) {
                        case "PUT":
                            if (parts.length < 3) {
                                System.out.println("Uso: PUT <chave> <valor>");
                                break;
                            }
                            db.putString(parts[1], parts[2]);
                            System.out.println("OK");
                            break;

                        case "PUT_INT":
                            if (parts.length < 3) {
                                System.out.println("Uso: PUT_INT <chave> <valor>");
                                break;
                            }
                            try {
                                db.putInt(parts[1], Integer.parseInt(parts[2]));
                                System.out.println("OK");
                            } catch (NumberFormatException e) {
                                System.out.println("Valor inteiro inválido.");
                            }
                            break;

                        case "PUT_LONG":
                            if (parts.length < 3) {
                                System.out.println("Uso: PUT_LONG <chave> <valor>");
                                break;
                            }
                            try {
                                db.putLong(parts[1], Long.parseLong(parts[2]));
                                System.out.println("OK");
                            } catch (NumberFormatException e) {
                                System.out.println("Valor long inválido.");
                            }
                            break;

                        case "PUT_DOUBLE":
                            if (parts.length < 3) {
                                System.out.println("Uso: PUT_DOUBLE <chave> <valor>");
                                break;
                            }
                            try {
                                db.putDouble(parts[1], Double.parseDouble(parts[2]));
                                System.out.println("OK");
                            } catch (NumberFormatException e) {
                                System.out.println("Valor double inválido.");
                            }
                            break;

                        case "PUT_BOOL":
                            if (parts.length < 3) {
                                System.out.println("Uso: PUT_BOOL <chave> <valor>");
                                break;
                            }
                            boolean boolVal = parts[2].equalsIgnoreCase("true") || parts[2].equals("1");
                            db.putBoolean(parts[1], boolVal);
                            System.out.println("OK");
                            break;

                        case "GET":
                            if (parts.length < 2) {
                                System.out.println("Uso: GET <chave>");
                                break;
                            }
                            String valor = db.getAsString(parts[1]);
                            if (valor == null) {
                                System.out.println("(não encontrado)");
                            } else {
                                System.out.println(valor);
                            }
                            break;

                        case "DELETE":
                            if (parts.length < 2) {
                                System.out.println("Uso: DELETE <chave>");
                                break;
                            }
                            db.delete(parts[1]);
                            System.out.println("OK");
                            break;

                        case "KEYS":
                            Set<String> keys = db.keys();
                            if (keys.isEmpty()) {
                                System.out.println("(vazio)");
                            } else {
                                keys.forEach(System.out::println);
                            }
                            break;

                        case "CREATE_INDEX":
                            if (parts.length < 3) {
                                System.out.println("Uso: CREATE_INDEX <nome> <tipo>");
                                break;
                            }
                            try {
                                ValueType type = ValueType.valueOf(parts[2].toUpperCase());
                                db.createSecondaryIndex(parts[1], type);
                                System.out.println("Índice criado: " + parts[1]);
                            } catch (IllegalArgumentException e) {
                                System.out.println("Tipo inválido. Use: STRING, INT, LONG, DOUBLE, BOOLEAN");
                            }
                            break;

                        case "PUT_INDEXED":
                            if (parts.length < 4) {
                                System.out.println("Uso: PUT_INDEXED <chave> <valor> <índice>");
                                break;
                            }
                            db.putIndexed(parts[1], parts[2], parts[3]);
                            System.out.println("OK");
                            break;

                        case "FIND":
                            if (parts.length < 3) {
                                System.out.println("Uso: FIND <índice> <valor>");
                                break;
                            }
                            Set<String> found = db.findByIndex(parts[1], parts[2]);
                            if (found.isEmpty()) {
                                System.out.println("(nenhuma chave encontrada)");
                            } else {
                                found.forEach(System.out::println);
                            }
                            break;

                        case "RANGE":
                            if (parts.length < 4) {
                                System.out.println("Uso: RANGE <índice> <min> <max>");
                                break;
                            }
                            IndexMetadata meta = db.indexMetadata.get(parts[1]);
                            if (meta == null) {
                                System.out.println("Índice não encontrado");
                                break;
                            }
                            Set<String> rangeResult = null;
                            try {
                                switch (meta.type) {
                                    case INT:
                                        rangeResult = db.findByRange(parts[1], 
                                            Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                                        break;
                                    case LONG:
                                        rangeResult = db.findByRange(parts[1], 
                                            Long.parseLong(parts[2]), Long.parseLong(parts[3]));
                                        break;
                                    case DOUBLE:
                                        rangeResult = db.findByRange(parts[1], 
                                            Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
                                        break;
                                    default:
                                        System.out.println("RANGE só funciona com tipos numéricos");
                                        break;
                                }
                            } catch (NumberFormatException e) {
                                System.out.println("Valores numéricos inválidos.");
                                break;
                            }
                            if (rangeResult != null) {
                                if (rangeResult.isEmpty()) {
                                    System.out.println("(nenhuma chave encontrada)");
                                } else {
                                    rangeResult.forEach(System.out::println);
                                }
                            }
                            break;

                            case "LIST_INDEXES":
                            Set<String> indexes = db.listSecondaryIndexes();
                            if (indexes.isEmpty()) {
                                System.out.println("(nenhum índice secundário)");
                            } else {
                                final SimpleDB dbFinal = db;
                                indexes.forEach(idx -> {
                                    IndexMetadata m = dbFinal.indexMetadata.get(idx);
                                    System.out.println(idx + " (" + m.type + ")");
                                });
                            }
                            break;

                        case "EXIT":
                            System.out.println("Encerrando...");
                            db.close();
                            scanner.close();
                            System.exit(0);
                            break;

                        default:
                            System.out.println("Comando desconhecido: " + command);
                    }
                } catch (IOException e) {
                    System.err.println("Erro: " + e.getMessage());
                } catch (NumberFormatException e) {
                    System.err.println("Erro: valor numérico inválido.");
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao abrir o banco: " + e.getMessage());
            System.exit(1);
        }
    }
}
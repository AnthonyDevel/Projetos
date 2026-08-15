import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Índice secundário baseado em árvore balanceada (TreeMap) com persistência em disco.
 * Permite buscas por valor exato e por faixa (se o tipo for numérico).
 */
public class BTreeIndex {
    private final TreeMap<Object, Set<String>> tree = new TreeMap<>();
    private final Path filePath;

    public BTreeIndex(Path filePath) {
        this.filePath = filePath;
    }

    // Carrega o índice do arquivo
    public void load() throws IOException {
        if (!Files.exists(filePath)) return;
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(filePath)))) {
            Object obj = ois.readObject();
            if (obj instanceof Map) {
                tree.clear();
                tree.putAll((Map<Object, Set<String>>) obj);
            }
        } catch (ClassNotFoundException e) {
            throw new IOException("Erro ao carregar índice: " + e.getMessage(), e);
        }
    }

    // Salva o índice no arquivo (serialização completa)
    public void save() throws IOException {
        Path parent = filePath.toAbsolutePath().getParent();
        Path tempFile = Files.createTempFile(parent, "btree", ".tmp");
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(tempFile)))) {
            oos.writeObject(tree);
        }
        Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING);
    }

    // Insere uma chave primária associada a um valor
    public void put(Object value, String primaryKey) {
        Set<String> keys = tree.computeIfAbsent(value, k -> new HashSet<>());
        keys.add(primaryKey);
    }

    // Remove uma chave primária associada a um valor
    public void remove(Object value, String primaryKey) {
        Set<String> keys = tree.get(value);
        if (keys != null) {
            keys.remove(primaryKey);
            if (keys.isEmpty()) {
                tree.remove(value);
            }
        }
    }

    // Busca todas as chaves primárias com exatamente o valor dado
    public Set<String> get(Object value) {
        Set<String> keys = tree.get(value);
        return keys != null ? new HashSet<>(keys) : new HashSet<>();
    }

    // Busca chaves primárias em uma faixa de valores (apenas para tipos numéricos)
    public Set<String> range(Object minValue, Object maxValue) {
        Set<String> result = new HashSet<>();
        if (minValue instanceof Comparable && maxValue instanceof Comparable) {
            NavigableMap<Object, Set<String>> subMap = tree.subMap(minValue, true, maxValue, true);
            for (Set<String> keys : subMap.values()) {
                result.addAll(keys);
            }
        }
        return result;
    }

    // Retorna todos os valores distintos (para exibição ou depuração)
    public Set<Object> values() {
        return new HashSet<>(tree.keySet());
    }

    // Limpa o índice
    public void clear() {
        tree.clear();
    }
}
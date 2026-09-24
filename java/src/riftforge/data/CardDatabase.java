package riftforge.data;

import riftforge.engine.GameEngine;
import riftforge.model.Card;
import riftforge.model.CardType;
import riftforge.model.Element;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Ficha maestra de cartas: lee {@code java/resources/cards2.csv} y construye el
 * catálogo (lista simple), el índice por nombre ({@code HashMap}), la línea de
 * evolución (árbol: base -> mejorada -> legendaria) y las habilidades
 * especiales (cola de prioridad). Una carta se identifica por su {@code uid},
 * que además nombra su imagen: {@code <uid>.jpeg} (por ejemplo
 * {@code CHR_01_INQUISIDOR.jpeg}).
 *
 * <p>Formato: CSV delimitado por comas, campos con comas/puntos y comas entre
 * comillas dobles, cabecera en la primera fila. Columnas soportadas (se buscan
 * por nombre de cabecera, por eso se aceptan sinónimos):
 *
 * <pre>
 *   card_uid / uid                       -> id
 *   numero_coleccion                     -> (referencia)
 *   categoria / grupo / tipo             -> PERSONAJE/CRIATURA -> CardType.CRIATURA,
 *                                            UTILIDAD -> HECHIZO, MEJORA -> EQUIPO
 *   nombre                               -> nombre visible
 *   coste_flux / coste / mana_cost       -> maná
 *   ataque_mod / ataque / atk            -> ataque base
 *   defensa_durabilidad / defensa / vida -> vida / durabilidad
 *   descripcion_efecto / habilidad       -> texto del efecto (se conserva siempre)
 *   elemento                             -> opcional; si falta, la carta es ELECTRICO
 *   especial                             -> opcional (si/no); marca prioridad alta
 *   evoluciona_de                        -> opcional; uid de la carta evolucionada superior
 * </pre>
 *
 * <p>En {@code Evoluciona_de} el padre debe aparecer ANTES que el hijo en el archivo
 * (la línea de evolución se construye de base -> mejorada -> legendaria).
 */
public final class CardDatabase {
    /** Una fila del fichero, validada y lista para convertirse en {@link Card}. */
    public record CardRow(
            String id,
            String name,
            CardType type,
            Element element,
            int manaCost,
            int attack,
            int health,
            boolean special,
            String ability,
            String evolvesFromId) {

        public Card toCard() {
            return new Card(id, name, element, type, manaCost, attack, health, special, ability);
        }

        public boolean isBase() {
            return evolvesFromId == null || evolvesFromId.isBlank();
        }
    }

    private CardDatabase() {
    }

    /** Lee la ficha desde el classpath o, si no está, desde {@code java/resources/cards2.csv}. */
    public static List<CardRow> load() throws IOException {
        InputStream in = CardDatabase.class.getResourceAsStream("/cards2.csv");
        if (in == null) in = new java.io.FileInputStream("java/resources/cards2.csv");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return parse(reader);
        }
    }

    /** Interpreta el CSV (cabecera en la primera fila; las líneas con {@code #} se ignoran). */
    public static List<CardRow> parse(BufferedReader reader) throws IOException {
        List<CardRow> rows = new ArrayList<>();
        String line;
        Map<String, Integer> header = null;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank() || line.trim().startsWith("#")) continue;
            List<String> fields = splitCsv(line);
            if (header == null) {
                header = indexHeader(fields);
                ensure(header, CARD_UID, "card_uid");
                ensure(header, NOMBRE, "nombre");
                ensure(header, CATEGORIA, "categoria");
                ensure(header, COSTE, "coste_flux");
                ensure(header, ATAQUE, "ataque_mod");
                ensure(header, DEFENSA, "defensa_durabilidad");
                continue;
            }
            rows.add(parseRow(fields, header));
        }
        return rows;
    }

    private static final String[] CARD_UID = {"card_uid", "uid", "id"};
    private static final String[] NOMBRE = {"nombre", "name"};
    private static final String[] CATEGORIA = {"categoria", "grupo", "tipo", "tipo_juego"};
    private static final String[] COSTE = {"coste_flux", "coste", "costo", "mana_cost", "mana"};
    private static final String[] ATAQUE = {"ataque_mod", "ataque", "atq", "attack"};
    private static final String[] DEFENSA = {"defensa_durabilidad", "defensa", "vida", "hp", "health"};
    private static final String[] DESCRIPCION = {"descripcion_efecto", "descripcion", "habilidad", "efecto"};
    private static final String[] ELEMENTO = {"elemento", "element"};
    private static final String[] ESPECIAL = {"especial"};
    private static final String[] EVOLUCIONA = {"evoluciona_de", "padre", "evoluciona"};

    private static Map<String, Integer> indexHeader(List<String> fields) {
        Map<String, Integer> header = new LinkedHashMap<>();
        for (int i = 0; i < fields.size(); i++) header.put(normalize(fields.get(i)), i);
        return header;
    }

    private static void ensure(Map<String, Integer> header, String[] aliases, String label) {
        if (columnIndex(header, aliases) < 0) throw new IllegalArgumentException(
                "Falta la columna '" + label + "' en la cabecera del CSV");
    }

    private static int columnIndex(Map<String, Integer> header, String[] aliases) {
        for (String alias : aliases) {
            Integer idx = header.get(alias);
            if (idx != null) return idx;
        }
        return -1;
    }

    private static String cell(List<String> fields, int idx) {
        return (idx >= 0 && idx < fields.size()) ? fields.get(idx).trim() : "";
    }

    private static CardRow parseRow(List<String> fields, Map<String, Integer> header) {
        String id = cell(fields, columnIndex(header, CARD_UID));
        String name = cell(fields, columnIndex(header, NOMBRE));
        if (id.isEmpty() || name.isEmpty()) throw new IllegalArgumentException(
                "card_uid y nombre no pueden estar vacíos en: " + fields);
        CardType type = parseType(cell(fields, columnIndex(header, CATEGORIA)));
        Element element = parseElement(cell(fields, columnIndex(header, ELEMENTO)));
        int cost = number(cell(fields, columnIndex(header, COSTE)), "coste", id);
        int attack = number(cell(fields, columnIndex(header, ATAQUE)), "ataque", id);
        int health = Math.max(0, number(cell(fields, columnIndex(header, DEFENSA)), "defensa", id));
        boolean special = parseYesNo(cell(fields, columnIndex(header, ESPECIAL)));
        String ability = cell(fields, columnIndex(header, DESCRIPCION));
        String parentId = cell(fields, columnIndex(header, EVOLUCIONA));
        return new CardRow(id, name, type, element, cost, attack, health, special, ability, parentId);
    }

    private static CardType parseType(String categoria) {
        return switch (categoria.toLowerCase(Locale.ROOT)) {
            case "personaje", "personajes", "criatura", "criaturas", "creature" -> CardType.CRIATURA;
            case "utilidad", "utilidades", "hechizo", "spell" -> CardType.HECHIZO;
            case "mejora", "mejoras", "equipo", "equipment" -> CardType.EQUIPO;
            default -> throw new IllegalArgumentException("Categoría no reconocida: '" + categoria
                    + "' (usa Personaje, Criatura, Utilidad o Mejora)");
        };
    }

    private static Element parseElement(String value) {
        String element = value.trim().toUpperCase(Locale.ROOT);
        if (element.isEmpty()) return Element.ELECTRICO;
        try {
            return Element.valueOf(element);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Elemento no reconocido: '" + value + "'");
        }
    }

    private static boolean parseYesNo(String value) {
        String v = value.trim().toLowerCase(Locale.ROOT);
        return switch (v) {
            case "", "no", "false", "0", "n" -> false;
            case "si", "sí", "true", "1", "s", "y" -> true;
            default -> throw new IllegalArgumentException("Especial debe ser si/no, se recibió: '" + value + "'");
        };
    }

    private static int number(String value, String field, String id) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + " inválido en la carta '" + id + "'");
        }
    }

    private static String normalize(String s) { return s == null ? "" : s.trim().toLowerCase(Locale.ROOT); }

    /** Divide una línea CSV respetando campos entre comillas dobles (comas o ';' internos). */
    static List<String> splitCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else inQuotes = false;
                } else cur.append(ch);
            } else if (ch == '"') {
                inQuotes = true;
            } else if (ch == ',') {
                out.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        out.add(cur.toString().trim());
        return out;
    }

    /**
     * Construye el catálogo, el índice por nombre y el árbol de evolución del
     * motor a partir de la ficha. Las cartas sin {@code evoluciona_de} son
     * bases bajo la raíz sintética; las demás cuelgan de su padre y se registran
     * al pie de su línea. El padre debe aparecer antes que su hijo en el archivo.
     */
    public static void apply(GameEngine engine, List<CardRow> rows) {
        Map<String, CardRow> byId = new LinkedHashMap<>();
        for (CardRow row : rows) byId.put(row.id(), row);
        for (CardRow row : rows) {
            Card card = row.toCard();
            if (row.isBase()) {
                engine.registerCard(card);
                engine.registerBaseEvolution(card);
            }
        }
        for (CardRow row : rows) {
            if (row.isBase()) continue;
            CardRow parent = byId.get(row.evolvesFromId());
            if (parent == null) throw new IllegalArgumentException("Evoluciona_de apunta al uid inexistente '"
                    + row.evolvesFromId() + "' (carta: " + row.name() + ")");
            engine.registerEvolution(parent.toCard(), row.toCard());
        }
    }
}
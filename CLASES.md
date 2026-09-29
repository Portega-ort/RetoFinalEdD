# Explicación de las clases más importantes — RiftForge TCG

Documento complementario al `README.md`, organizado en **tablas**: para cada clase
se indica qué es, qué métodos expone, cómo se usa en el juego, con qué estructura
se relaciona y qué complejidad tiene.

| Documento | Contenido | Formato |
|---|---|---|
| `README.md` | documentación general: reglas, estructura, ejecución, empaquetado | texto + diagramas |
| `CLASES.md` (este) | explicación clase por clase del código más relevante | tablas |

---

## 1. Mapa de paquetes

| Paquete | Responsabilidad | Clases principales | Depende de |
|---|---|---|---|
| `riftforge.app` | preparar la partida y arrancar | `Startup`, `Main` | `ui`, `ui.gui`, `data`, `engine`, `model`, `sort` |
| `riftforge.data` | leer y validar la ficha de cartas | `CardDatabase` (+ record `CardRow`) | `engine`, `model` |
| `riftforge.engine` | reglas de la jugada (no imprime nada) | `GameEngine` (+ record `TurnResult`) | `model`, `structures`, `sort` |
| `riftforge.engine.tests` | verificación automática | `GameEngineTest` | todo lo anterior |
| `riftforge.model` | TDAs del dominio | `Card`, `CardType`, `Element`, `Player`, `Rift`, `BattleEvent` | `structures` (solo `Player`) |
| `riftforge.sort` | ordenamiento propio (entregable) | `OwnSorter` | `model` |
| `riftforge.structures` | nodos y TDAs genéricos | `Node`, `SinglyLinkedList`, `DoublyLinkedList`, `CircularLinkedList`, `LinkedStack`, `CircularQueue`, `PriorityQueue`, `Tree` | nada |
| `riftforge.ui` | render del estado en consola | `ConsoleRenderer` | `engine`, `model`, `structures` |
| `riftforge.ui.gui` | interfaz gráfica Swing | `GameWindow`, `CardView`, `Theme`, `Typeface`, `ImageStore`, `BackgroundStore`, `Resources` | `engine`, `model`, `structures` |

**Regla de dependencias** (se mantiene en todo el proyecto):

| Nivel | Package | Puede usar | Prohibido |
|---|---|---|---|
| 1 | `structures`, `sort` | solo el JDK | cartas, jugadores, consola, GUI |
| 2 | `model` | `structures` | consola, GUI, ficheros |
| 3 | `engine` | `model`, `structures`, `sort` | `System.out`, Swing |
| 4 | `data` | `engine`, `model` | consola, GUI |
| 5 | `ui`, `ui.gui` | `engine`, `model`, `structures` | reglas de juego |
| 6 | `app` | todo | — |

Consecuencia práctica: las estructuras son **genéricas** (`SinglyLinkedList<T>`,
`LinkedStack<T>`, …) y se podrían reutilizar en cualquier otro proyecto sin
cambiar una línea.

---

## 2. Capa de dominio — `model`

### 2.1 `Card` — el TAD central

`model/Card.java`. Cumple el requisito de **datos primitivos, complejos y TAD**.

| Tipo de dato | Campo | Tipo Java | Nota |
|---|---|---|---|
| primitivo | `manaCost` | `int` | `final`, inmutable |
| primitivo | `attack` | `int` | `final`, inmutable (valor base) |
| primitivo | `health` | `int` | `final`, inmutable (valor base) |
| primitivo | `special` | `boolean` | prioridad alta en la cola de resolución |
| primitivo | `damageTaken` | `int` | **único campo mutable**: daño de combate |
| complejo | `name` | `String` | nombre visible, y también identidad |
| complejo | `uid` | `String` | nombra la imagen `<uid>.jpeg` |
| complejo | `specialAbility` | `String` | texto del efecto; nunca `null` |
| TAD / enum | `element` | `Element` | 6 valores cerrados |
| TAD / enum | `type` | `CardType` | 3 valores cerrados |

| Método | Qué hace | Complejidad |
|---|---|---|
| `Card(...)` ×3 constructores | valida nombre no vacío, `element`/`type` no nulos, valores ≥ 0, y que una carta `special` describa su habilidad | O(1) |
| `id()`, `name()`, `element()`, `type()`, `manaCost()`, `attack()`, `health()` | accesores | O(1) |
| `special()`, `specialAbility()` | prioridad y texto del efecto | O(1) |
| `attackWithBonus(Rift rift)` | `attack + attackBonus` **solo si** `element == rift.bonusElement()` | O(1) |
| `takeDamage(int amount)` | acumula daño (ignora negativos) | O(1) |
| `remainingHealth()` | `health - damageTaken` | O(1) |
| `isDestroyed()` | `remainingHealth() <= 0` | O(1) |
| `clearDamage()` | reinicia el daño al reciclar el cementerio | O(1) |
| `equals` / `hashCode` | identidad por nombre | O(k) en la longitud del nombre |
| `toString()` | una línea legible; añade `[ESPECIAL: …]` si aplica | O(1) |

| Decisión de diseño | Motivo |
|---|---|
| invariantes en el constructor, no en setters | no hay setters: el objeto es válido o no se construye |
| identidad = nombre | permite indexar el catálogo en un `HashMap<String, Card>` y buscar cartas en el árbol sin inventar un id |
| el daño vive en la carta, los valores base no se tocan | una carta dañada que vuelve al mazo solo necesita `clearDamage()` |
| el bono de grieta se calcula en la carta | la regla vive en el dominio, no en el motor |

### 2.2 `Element` y `CardType` — vocabulario cerrado

| Clase | Valores | Se usa en |
|---|---|---|
| `enum Element` | `FUEGO`, `AGUA`, `ELECTRICO`, `SOMBRA`, `DRAGON`, `VACIO` | una grieta por elemento; bonus de ataque |
| `enum CardType` | `CRIATURA`, `HECHIZO`, `EQUIPO` | comportamiento al resolver la carta |

| Propiedad | Detalle |
|---|---|
| Por qué `enum` | el compilador garantiza que no habrá valores fuera del dominio |
| Validación del CSV | `parseType` y `parseElement` (`data/CardDatabase.java:157-184`) lanzan `IllegalArgumentException` con el valor recibido si no reconocen la categoría o el elemento |
| Mapeo de categorías | `Personaje`/`Criatura` → `CRIATURA`; `Utilidad` → `HECHIZO`; `Mejora` → `EQUIPO` |
| Valor por defecto | elemento vacío → `ELECTRICO` |

### 2.3 `Rift` y `BattleEvent` — records inmutables

| Clase | Campos | Reglas / validación | Uso |
|---|---|---|---|
| `record Rift` | `name`, `bonusElement`, `attackBonus` | nombre no vacío, elemento no nulo, bono ≥ 0 | estado de la arena; rota en la lista circular |
| `record BattleEvent` | `turn`, `description` | ninguna (es un valor) | nodo del historial doble; `toString` = `"Turno N: descripción"` |

### 2.4 `Player` — el duelista (integra las estructuras propias)

| Campo | Estructura propia / tipo | Significado |
|---|---|---|
| `deck` | `LinkedStack<Card>` | mazo; se roba con `pop()` (tope) |
| `graveyard` | `LinkedStack<Card>` | cementerio; se descarta con `push()` |
| `field` | `SinglyLinkedList<Card>` | campo de batalla (solo criaturas) |
| `fieldAttackBonus` | `int` | +ATQ permanente aportado por las Mejoras equipadas |
| `shieldPool` | `int` | blindaje absorbente de las Mejoras |
| `mana` / `life` | `int` | recursos; maná con tope `MAX_MANA = 10` |
| `name` | `String` | validado no vacío |

| Método | Qué hace | Complejidad |
|---|---|---|
| `Player(name, mana, life)` | valida nombre no vacío y valores ≥ 0; recorta el maná a 10 | O(1) |
| `canPay(card)` / `pay(card)` | consulta y pago del coste; `pay` lanza `IllegalStateException` si no alcanza | O(1) |
| `restoreMana(amount)` | `min(10, mana + amount)` | O(1) |
| `receiveDamage(damage)` / `heal(amount)` | la vida nunca baja de 0 | O(1) |
| `isDefeated()` | `life == 0` → victoria del rival | O(1) |
| `buffFieldAttack(amount)` | +ATQ permanente al campo (efecto de las Mejoras) | O(1) |
| `addShield(amount)` | suma blindaje | O(1) |
| `absorb(damage)` | **consume blindaje y devuelve el daño que llega a la vida** | O(1) |

---

## 3. Estructuras propias — `structures`

Ninguna usa `LinkedList`, `Stack`, `Queue`, `Deque`, `PriorityQueue`, `TreeMap`
ni `TreeSet` de `java.util`. Todas comparten un nodo package-private:

| Nodo | Campo | Nota |
|---|---|---|
| `final class Node<T>` | `data`, `next`, `previous` | `final` y **no público**: nadie fuera de `structures` puede tocar los punteros, así la invariante de cada estructura queda protegida |

### 3.1 Tabla comparativa de las 6 TDAs

| TDA | Reemplaza a | Puntero(s) clave | Operación(es) | Complejidad | Dónde se usa en el juego |
|---|---|---|---|---|---|
| `SinglyLinkedList<T>` | `java.util.LinkedList` (simple) | `head` | `add` (al inicio), `find(Predicate)`, `remove(Predicate)`, `iterator()` | `add` O(1); `find`/`remove` O(n); recorrido O(n) | catálogo de cartas y campo de batalla de cada jugador |
| `DoublyLinkedList<T>` | `java.util.LinkedList` (doble) | `head` + `tail` | `addLast`, `forward()`, `backward()` | `addLast` O(1); recorridos O(n) | historial de jugadas (bitácora) |
| `CircularLinkedList<T>` | — | `current` (el final es el inicio) | `add` (al final lógico), `current()`, `rotate()` | `rotate` O(1); `add` O(n) busca la cola | las 6 grietas elementales |
| `LinkedStack<T>` | `java.util.Stack` | `top` | `push`, `pop`, `peek` | todo O(1) | deck y cementerio de cada jugador |
| `CircularQueue<T>` | `ArrayDeque` / `LinkedList` | `rear` con `rear.next` = frente | `enqueue`, `dequeue`, `peek`, `snapshot()` | O(1) | iniciativa de turnos |
| `PriorityQueue<T>` | `java.util.PriorityQueue` | `head`, `tail`, `firstNormal` | `enqueue(v, high)`, `dequeue`, `peek` | O(1) amortizado | efectos pendientes: especiales antes |
| `Tree<T>` + `TreeNode<T>` | colecciones de árbol | nodos con hijos en `DoublyLinkedList` | `setRoot`, `addChild`, `preorder`, `postorder`, `height`, `size` | O(n) por recorrido | línea de evolución de cartas |

### 3.2 Detalle de cada estructura

| TDA | Cómo funciona (decisión clave) | Comportamiento notable |
|---|---|---|
| `SinglyLinkedList<T>` | `add` enlaza el nuevo nodo al `head` (O(1)) | implementa `Iterable<T>` con un `Iterator` propio, así se recorre con `for-each` **sin exponer los nodos** |
| `DoublyLinkedList<T>` | mantiene `head` y `tail`; `addLast` enlaza por ambos extremos | `forward()` y `backward()` comparten el helper `traverse(start, forward)`: una sola routine con dos direcciones |
| `CircularLinkedList<T>` | un solo puntero `current`; no usa `null` como centinela | `add` busca el último nodo (`while (tail.next != current)`) para **respetar el orden de configuración**; `rotate()` avanza y devuelve el nuevo valor |
| `LinkedStack<T>` | `push`/`pop` sobre `top` | `pop`/`peek` lanzan `NoSuchElementException` si está vacía; la misma clase sirve para deck y cementerio, por eso el reciclaje `pop`→`push` es simétrico |
| `CircularQueue<T>` | `rear.next` siempre apunta al frente | **no hay final de ronda**: al desencolar el último elemento, `rear` queda `null`; `snapshot()` da una vista desde el frente sin alterar el ciclo |
| `PriorityQueue<T>` | puntero `firstNormal` que actúa de **frontera** entre el bloque de altas y el de normales | ver la tabla siguiente |
| `Tree<T>` | cada nodo guarda su dato + hijos en una `DoublyLinkedList` (estructura propia dentro de otra) | operación **recursiva** en 5 puntos (ver tabla siguiente) |

**`PriorityQueue<T>` — los tres casos de inserción de una prioridad alta:**

| Situación | Qué hace `enqueue(v, true)` | Coste |
|---|---|---|
| la lista es toda de prioridad alta (`firstNormal == null`) | añade al final | O(1) |
| la lista es toda normal (`firstNormal == head`) | antepone al `head` | O(1) |
| mezcla (`head`… altas … `firstNormal` … normales) | camina hasta la frontera e inserta justo antes | O(n) |

| Invariante | Formulación |
|---|---|
| bloques | `head … (antes de firstNormal)` = todas altas · `firstNormal … tail` = todas normales |
| efecto | las cartas `Especial` resuelven **antes** que las normales, sin importar cuándo se encolaron |
| Dentro de un nivel | el orden es **FIFO** (el último en encolar es el último en salir) |

**`Tree<T>` — las cinco operaciones recursivas:**

| Método | Estrategia recursiva | Uso en el juego | Complejidad |
|---|---|---|---|
| `find(nodo, valor)` | caso base `nodo == null`; compara; si no, busca en cada hijo | permite que `addChild` localice al padre en cualquier nivel | O(n) |
| `preorder(nodo, out)` | añade el nodo y luego recorre sus hijos | render de la línea de evolución (tecla `E` / botón *Evoluciones*) | O(n) |
| `postorder(nodo, out)` | recorre los hijos y luego añade el nodo | recorrido alternativo del árbol | O(n) |
| `height(nodo)` | `1 + max(alturas de los hijos)` | ranking: `height() == 3` ⇒ existe legendaria | O(n) |
| `size` | contador mantenido en `setRoot` / `addChild` | nº de cartas registradas | O(1) |

| Detalle de `Tree` | Valor |
|---|---|
| altura de un árbol de un solo nodo | `0` |
| altura de un árbol vacío | `-1` |
| raíz en el juego | sintética: la carta `"Árbol de Evolución"`, para agrupar todas las líneas |
| Complejidad de `addChild` | O(n) por el `find` recursivo del padre (aceptable: el árbol se arma una sola vez) |

---

## 4. Ordenamiento propio — `sort`

### `OwnSorter.insertionSortByCost(List<Card>)`

Entregable de **ordenamiento**: clasificación por inserción escrita a mano, sin
`Arrays.sort` ni `Collections.sort`.

```java
public static List<Card> insertionSortByCost(List<Card> input) {
    List<Card> cards = new ArrayList<>(input);           // 1) copia defensiva
    for (int i = 1; i < cards.size(); i++) {
        Card key = cards.get(i);
        int j = i - 1;
        while (j >= 0 && cards.get(j).manaCost() > key.manaCost()) {
            cards.set(j + 1, cards.get(j));             // 2) desplazar a la derecha
            j--;
        }
        cards.set(j + 1, key);                           // 3) insertar la clave
    }
    return cards;
}
```

| Paso | Qué hace | Por qué |
|---|---|---|
| 1 | copia la lista con `new ArrayList<>(input)` | el método **no muta** la entrada: quien llama (`GameEngine.catalogSortedByCost`, `Startup.splitDecks`) sigue viendo el catálogo en su orden original |
| 2 | el bucle externo toma cada posición `i` como **clave** (`key`) | hipótesis: las posiciones `0..i-1` ya están ordenadas |
| 3 | el bucle interno camina hacia atrás (`j`) mientras el elemento sea **más caro** que la clave y lo desplaza a `j+1` | los elementos mayores se apartan para dejar un hueco libre junto a la clave |
| 4 | coloca la clave en el hueco `j + 1` | queda ordenado `0..i` |
| 5 | devuelve la copia | el llamante recibe una lista nueva, nunca la original |

| Propiedad | Explicación |
|---|---|
| **Correctitud** | tras la iteración `i` el prefijo `0..i` está ordenado; caso base: la posición 0, trivialmente ordenada; al terminar, toda la lista lo está |
| **Estabilidad** | la comparación es estricta (`>` y no `>=`): una clave igual al anterior **no** lo sobrepasa, así que dos cartas del mismo coste conservan su orden de entrada |
| Peor caso | O(n²) tiempo (lista invertida) |
| Mejor caso | O(n) tiempo (lista ya ordenada: el `while` no se ejecuta) |
| Memoria extra | O(n) por la copia; el algoritmo en sí es O(1) (in-place sobre la copia) |
| **Motivación** | con mazos de decenas de cartas el O(n²) es irrelevante, y a cambio el algoritmo es trazable a mano, que vale más en un trabajo académico que la eficiencia de un sort de biblioteca |
| Reutilización | el mismo criterio ordena la vista de colección **y** equilibra la curva de coste al repartir los mazos |

---

## 5. Carga de datos — `data` (`CardDatabase`)

Convierte `cards2.csv` (40 cartas) en estructuras del motor. No es un simple
`split(",")`: resuelve cuatro problemas reales.

| Problema | Método | Solución |
|---|---|---|
| 1. Fichero no encontrado al empaquetar | `load()` | busca primero en el classpath (`/cards2.csv`, dentro del JAR) y si no cae a `java/resources/cards2.csv`: la app no depende del directorio de trabajo |
| 2. Cabecera variable | `indexHeader` + `ensure` + `columnIndex` | indexa las columnas por nombre normalizado y las busca por **sinónimos**; si falta una obligatoria, el error dice cuál |
| 3. CSV real (descripciones con comas) | `splitCsv` | parser con comillas dobles, escape `""` y separador `,` dentro de campos citados; ignora líneas en blanco y comentarios `#` |
| 4. Datos sucios | `parseType`, `parseElement`, `parseYesNo`, `number` | convierte y valida en el dominio; cualquier valor desconocido lanza `IllegalArgumentException` |

| Columna del CSV | Sinónimos aceptados | Destino | Obligatoria |
|---|---|---|---|
| `card_uid` | `uid`, `id` | `Card.id` (nombre de la imagen) | sí |
| `nombre` | `name` | `Card.name` | sí |
| `categoria` | `grupo`, `tipo`, `tipo_juego` | `CardType` | sí |
| `coste_flux` | `coste`, `costo`, `mana_cost`, `mana` | `Card.manaCost` | sí |
| `ataque_mod` | `ataque`, `atq`, `attack` | `Card.attack` | sí |
| `defensa_durabilidad` | `defensa`, `vida`, `hp`, `health` | `Card.health` | sí |
| `descripcion_efecto` | `descripcion`, `habilidad`, `efecto` | `Card.specialAbility` | no |
| `elemento` | `element` | `Card.element` (vacío → `ELECTRICO`) | no |
| `especial` | — | prioridad alta en la cola (si/no) | no |
| `evoluciona_de` | `padre`, `evoluciona` | `CardRow.evolvesFromId` | no |

| Miembro | Qué es / qué hace |
|---|---|
| `record CardRow` | una fila ya validada; `toCard()` la convierte en `Card` e `isBase()` indica si no tiene `evoluciona_de` |
| `apply(engine, rows)` | **dos pasadas obligatorias**: 1ª registra todas las bases (catálogo + nivel 1 del árbol); 2ª cuelga cada hijo de su padre con `registerEvolution` y verifica que el `evoluciona_de` exista. Así el orden de filas en el CSV es irrelevante |

---

## 6. Motor de reglas — `engine` (`GameEngine`)

Regla de diseño explícita: **el motor no imprime nada**; devuelve un
`record TurnResult` y cada interfaz lo formatea como quiera. Por eso el mismo
motor sirve para la consola y para la GUI.

### 6.1 Estructuras que contiene

| Campo | Tipo | Papel |
|---|---|---|
| `catalog` | `SinglyLinkedList<Card>` | las 40 cartas, recorribles |
| `catalogIndex` | `HashMap<String, Card>` | tabla hash: `findCard` en O(1) |
| `rifts` | `CircularLinkedList<Rift>` | 6 grietas; rotan cada `TURNS_PER_RIFT = 2` turnos |
| `turnQueue` | `CircularQueue<Player>` | iniciativa |
| `history` | `DoublyLinkedList<BattleEvent>` | bitácora, legible en ambos sentidos |
| `pendingEffects` | `PriorityQueue<Card>` | cola de efectos: las especiales salen primero |
| `evolutions` | `Tree<Card>` | líneas de evolución |
| `turn` | `int` | contador de turnos |

### 6.2 API del catálogo y del árbol

| Método | Qué hace | Complejidad |
|---|---|---|
| `registerCard(card)` | inserta en la lista simple **y** en el índice hash | O(1) |
| `findCard(name)` | busca en el hash; si falla, recorre la lista con `equalsIgnoreCase` (degradación elegante en vez de excepción) | O(1) / O(n) |
| `removeCard(name)` | borra de la lista y del índice | O(n) + O(1) |
| `catalog()` | `Iterable<Card>` para recorrer sin exponer nodos | — |
| `catalogSortedByCost()` | **copia** ordenada con `OwnSorter`; el catálogo original queda intacto | O(n²) |
| `registerBaseEvolution(base)` | nivel 1 de la línea bajo la raíz sintética | O(n) |
| `registerEvolution(parent, child)` | cuelga el hijo de su padre y lo da de alta en el catálogo | O(n) |
| `evolutionTree()` | devuelve el `Tree<Card>` | O(1) |

### 6.3 `playTurn(boolean wantsToPlay)` — los 8 pasos

| # | Paso | Estructura / método implicado | Detalle |
|---|---|---|---|
| 1 | Sacar al activo | `turnQueue.dequeue()` / `peek()` | el nuevo frente de la cola pasa a ser el objetivo; `turn++` |
| 2 | Restaurar maná | `Player.restoreMana(2)` | con tope de 10 dentro de `Player` |
| 3 | Robar | `draw()` → `recycleDeckIfNeeded()` + `deck.pop()` | si el deck está vacío, el cementerio pasa al deck con `pop`/`push` limpiando el daño (evita la inanición) |
| 4 | Decidir | `canPay` / `pay` / `graveyard.push` / `pendingEffects.enqueue(card, card.special())` | sin carta → se registra; no quiere jugar o no puede pagar → se descarta; si no, se paga y se encola el efecto |
| 5 | Resolver efectos | `resolvePendingEffects` → `resolveCard` (`switch` sobre `CardType` con `yield`) | `CRIATURA` va al campo · `HECHIZO` ejecuta `resolveUtility` · `EQUIPO` exige criatura en campo y da +ATQ permanente y blindaje |
| 5.b | Efecto de cada utilidad | `resolveUtility` (`switch` sobre el nombre) | hace lo que su nombre indica sobre maná, vida, blindaje o +ATQ; si una carta nueva no está mapeada, aplica un efecto de respaldo (+1 maná) documentado en el historial |
| 6 | Atacar | `attackPhase` | **bloqueo estricto** (ver tabla siguiente) |
| 7 | Registrar | `history.addLast(new BattleEvent(turn, message))` | un evento por turno en la lista doble |
| 8 | Cerrar turno | `turnQueue.enqueue(active)` + `rifts.rotate()` si `turn % 2 == 0` | devuelve la iniciativa y rota la grieta activa |

### 6.4 `attackPhase` — bloqueo estricto

| Situación del atacante | Qué hace | Concepto de regla |
|---|---|---|
| es la criatura recién jugada | no golpea (solo bloquea y recibe daño) | **fatiga de invocación** |
| hay criaturas rivales | golpea a la frontal; el daño se limita con `Math.min(atk, remainingHealth())` | la primera bloquea |
| el bloqueador muere | pasa a la siguiente posición; **el sobrante se pierde** (no hay trample) | bloqueo estricto |
| no quedan bloquearores | el daño se acumula en `face` y va a la cara del rival | daño directo |
| todo el daño acumulado | `target.absorb(face)` descuenta el blindaje y solo el resto toca la vida | el blindaje absorbe antes que la vida |
| primer atacante del turno | recibe el `+ATQ` de las mejoras equipadas (`bonusGastado`) | las mejoras potencian al atacante frontal |
| con `attackWithBonus(activeRift())` | suma el bono solo si el elemento coincide con el de la grieta | grieta activa |

**Salida del turno:** `record TurnResult(int number, Player active, Player target, Card drawn, Rift rift, String action)` — la interfaz no necesita volverse a preguntar el estado.

---

## 7. Arranque y reparto — `app`

### 7.1 `Startup`

| Constante | Valor | Significado |
|---|---|---|
| `STARTING_MANA` | 6 | maná inicial (permite invocar una unidad el primer turno) |
| `STARTING_LIFE` | 30 | vida inicial (margen con el bloqueo estricto) |

| Método | Qué hace | Complejidad |
|---|---|---|
| `newEngine()` | carga la ficha, arma el motor con `CardDatabase.apply` y añade las 6 grietas | O(n·m) |
| `addRifts(game)` | crea una grieta por elemento con bonos de 1 a 3 | O(1) |
| `shuffled(list)` | **Fisher–Yates propio** sobre una copia con `java.util.Random` (no usa `Collections.shuffle`) | O(n) |
| `splitDecks(rows)` | reparte las 40 cartas en dos mazos de 20 | O(n²) |

**`splitDecks` — reparto en dos fases:**

| Fase | Estrategia | Garantía |
|---|---|---|
| 1 | `evolutionChains` detecta las líneas (base → descendientes) y las asigna **enteras y alternadas** (A, B, A, B…) | ningún jugador se queda con la mitad de una evolución: la cadena completa queda en un solo mazo |
| 2 | las cartas sueltas se ordenan con `OwnSorter.insertionSortByCost` y se reparten alternando | ambos mazos tienen una curva de maná parecida |
| 3 | si alguna mitad pasa de 20, las sobrantes se compensan hacia la otra | mazos de exactamente 20/20 |

### 7.2 `Main`

| Aspecto | Detalle |
|---|---|
| Sin argumentos | abre la GUI: `SwingUtilities.invokeLater(GameWindow::new)` |
| Con `--console` | bucle de texto: pide los dos nombres, reparte y baraja, y por turno muestra el estado y lee la tecla |
| Teclas de consola | `J` jugar · `D` descartar · `C` catálogo · `H` historial · `E` evoluciones · `Q` reiniciar partida · `X` salir |
| Fin de partida | si `result.target().isDefeated()` anuncia al ganador, muestra el historial y ofrece reiniciar |

---

## 8. Interfaz — `ui`

Ninguna de estas clases toca reglas: solo leen el estado del motor y lo pintan.

| Clase | Qué hace | Detalle relevante |
|---|---|---|
| `ConsoleRenderer` | toda la salida de texto: `showStatus`, `showCatalog`, `showHistory`, `showTurn`, `showEvolutionTree` | `showHistory` recorre la lista doble en los dos sentidos; `showEvolutionTree` imprime con `"  ".repeat(depth)` y delega la recursión en `showNode` |
| `gui/GameWindow` | ventana Swing única: cabecera, zona rival, zona activa, bitácora, mano con la próxima carta en grande, botones *Catálogo / Historial / Evoluciones / Barajar / Reiniciar / Salir* | clase más grande del proyecto; delega todo lo visual |
| `gui/CardView` | pinta una carta: arte, nombre, tipo, elemento y chips de coste/ATQ/DEF | el chip DEF se pone ámbar y muestra `restante/base` cuando la criatura está dañada |
| `gui/Theme` | paleta, colores y estilos compartidos | — |
| `gui/Typeface` | tres roles tipográficos: `display` (títulos y números), `body` (interfaz y cartas), `mono` (bitácora) | respaldo a fuente de sistema si falta el archivo |
| `gui/ImageStore` | cachea las imágenes de cartas (`cards/<uid>.jpeg`) | — |
| `gui/BackgroundStore` | carga los fondos `board`, `table` y `log` | degradado por defecto si falta alguno |
| `gui/Resources` | abre recursos **primero del classpath** y cae a `java/resources` | mismo criterio que `CardDatabase`: el JAR es autocontenido |

---

## 9. Pruebas — `engine.tests` (`GameEngineTest`)

| Aspecto | Detalle |
|---|---|
| Framework | **ninguno**: no hay `@Test` en el proyecto. Es una clase con `main`, contadores `passed`/`failed` y el helper `check(condición, etiqueta)` que imprime `PASS`/`FAIL` |
| Código de salida | `System.exit(1)` si hubo fallos → sirve también como script de integración continua |
| Cobertura | 13 escenarios / 29 comprobaciones |
| Escenarios deterministas | `newGameWithHalves()` reparte las 40 cartas; `controlledGame()` arranca con mazos vacíos, para poder afirmar sobre un efecto sin depender del azar del baraje |

| # | Escenario | Qué comprueba |
|---|---|---|
| 1 | `testFicha40Cartas` | la ficha carga 40 cartas |
| 2 | `testRepartoEquilibrado` | mitades de 20/20, sin duplicados y juntas cubren las 40 |
| 3 | `testCadenasCompletasPorJugador` | cada cadena de evolución está completa en un solo mazo (XOR) |
| 4 | `testCriaturasAlCampo` | las criaturas jugadas llegan al campo y el duelo consume los mazos |
| 5 | `testFatigaInvocacion` | la criatura recién jugada entra al campo y **no** ataca ese turno |
| 6 | `testCombateEntreCriaturas` | bloqueo estricto: el bloqueador recibe el golpe; si muere, va al cementerio y el exceso **no** atraviesa la línea |
| 7 | `testDañoDirectoSinBloqueo` | campo rival vacío ⇒ el daño va directo a la vida |
| 8 | `testUtilidadSegunSuNombre` | "El Altar de Carga" devuelve +1 de Flux y la carta usada va al cementerio |
| 9 | `testMejoraEquipaYBlindajeAbsorbe` | la mejora no entra al campo, da +ATQ permanente y blindaje que absorbe antes que la vida |
| 10 | `testEspecialPrioridadCola` | en la `PriorityQueue` las especiales salen primero y el resto conserva el FIFO |
| 11 | `testOrdenamientoPropio` | el Insertion Sort propio ordena las 40 cartas por coste |
| 12 | `testArbolEvolucion` | `height() == 3` y las tres líneas completas están en el árbol |
| 13 | `testHistorialDoble` | un evento por turno y recorrido en las dos direcciones |

```bash
java -cp out riftforge.engine.tests.GameEngineTest
# Resultado: 29 pasan, 0 fallan.
```

---

## 10. Tabla resumen final

| Clase | Tipo de dato / TDA | Rol en el juego | Operación clave | Complejidad |
|---|---|---|---|---|
| `Card` | TAD (primitivos + complejos + enums) | la carta | `attackWithBonus`, `takeDamage` | O(1) |
| `Player` | dominio (2 pilas + 1 lista) | duelista | `absorb`, `restoreMana` | O(1) |
| `Rift` | record inmutable | grieta activa | — | O(1) |
| `BattleEvent` | record inmutable | jugada del historial | — | O(1) |
| `Element` / `CardType` | enumeraciones | vocabulario del dominio | `valueOf`, `switch` | O(1) |
| `SinglyLinkedList<T>` | lista simple | catálogo y campo | `add`, `find`, `remove` | add O(1); resto O(n) |
| `DoublyLinkedList<T>` | lista doble | historial | `addLast`, `forward`, `backward` | O(1) / O(n) |
| `CircularLinkedList<T>` | lista circular | 6 grietas | `rotate()` | O(1); `add` O(n) |
| `LinkedStack<T>` | pila LIFO | deck y cementerio | `push` / `pop` | O(1) |
| `CircularQueue<T>` | cola circular | iniciativa | `dequeue` / `enqueue` | O(1) |
| `PriorityQueue<T>` | cola de prioridad | efectos especiales primero | `enqueue(v, true)` | O(1) amortizado |
| `Tree<T>` | árbol n-ario | línea de evolución | `preorder`, `height` | O(n) |
| `OwnSorter` | ordenamiento propio | catálogo y reparto por coste | `insertionSortByCost` | O(n²) |
| `CardDatabase` | carga y validación de CSV | las 40 cartas | `load`, `apply` | O(n·m) |
| `GameEngine` | orquestador de reglas | el turno completo | `playTurn` | O(n) por turno |
| `Startup` | preparación de partida | reparto y baraje | `splitDecks`, `shuffled` | O(n²) |
| `ConsoleRenderer` / `GameWindow` | presentación | mostrar el estado | `showStatus` / `paint` | O(n) |

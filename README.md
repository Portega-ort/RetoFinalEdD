# RiftForge TCG

Implementación en Java de la especificación: un motor de cartas por turnos que usa TDAs enlazados propios (sin `java.util.LinkedList`, `Stack` ni `Queue`).

## Packages y responsabilidad

| Package | Responsabilidad | Clases principales |
|---|---|---|
| `riftforge.model` | Datos y reglas locales del dominio | `Card`, `Player`, `Rift`, `BattleEvent` |
| `riftforge.structures` | Nodos y TDAs genéricos | `SinglyLinkedList`, `DoublyLinkedList`, `CircularLinkedList`, `LinkedStack`, `CircularQueue` |
| `riftforge.engine` | Coordina una jugada; no imprime | `GameEngine` |
| `riftforge.ui` | Renderiza el estado en consola | `ConsoleRenderer` |
| `riftforge.app` | Crea la partida de ejemplo y arranca | `Main` |

La regla de dependencias es: `app → ui/engine → model/structures`. Las estructuras no conocen cartas, jugadores ni consola.

## Relación entre requisito y TDA

| TDA | Uso en el juego | Operación clave |
|---|---|---|
| Lista simple | Catálogo/binder | registrar, buscar y eliminar cartas |
| Lista doble | Historial de combate | recorrer hacia delante y atrás |
| Lista circular | Cuatro grietas elementales | `rotate()` pasa al siguiente terreno cada dos turnos |
| Dos pilas | Deck y cementerio de cada jugador | robar con `pop()`, descartar con `push()` |
| Cola circular | Iniciativa | sacar el frente y añadirlo al final conserva el ciclo de turnos |

## Flujo de `GameEngine.playTurn()`

1. Saca de la cola al jugador activo; el nuevo frente se convierte en objetivo.
2. Roba la carta del tope del deck. Si está vacío, mueve el cementerio al deck y vuelve a robar.
3. Si hay maná, calcula el ataque con el bono de la grieta, aplica daño y envía la carta al cementerio.
4. Agrega un `BattleEvent` al final de la lista doble.
5. Reencola al jugador activo; cada dos turnos rota la lista circular de grietas.

## Ejecutar

Desde la raíz del proyecto:

```bash
mkdir -p out
javac -d out $(find java/src -name '*.java')
java -cp out riftforge.app.Main
```

Al iniciar, se piden los nombres de los dos duelistas. En cada turno se muestra la carta a robar y puedes jugarla, descartarla o revisar el historial. `Q` termina la partida actual y comienza otra desde cero; `X` cierra el programa. Gana quien reduzca la vida rival a cero. Al terminar se imprime el orden que conserva la cola circular para la siguiente ronda.

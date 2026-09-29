# Conclusión reflexiva — RiftForge TCG

Este proyecto empezó como un encargo de estructuras de datos y terminó siendo,
para mí, una clase entera sobre cómo se piensa un sistema. La idea inicial era
simple: implementar a mano las estructuras que Java te da hechas y montar con
ellas un juego que las usara de verdad. Eso ya me parecía un buen ejercicio, pero
con el proyecto en marcha me di cuenta de que el valor no estaba en escribir las
clases, sino en defendirlas.

Lo primero que aprendí es que una estructura de datos no es un conjunto de
operaciones, es una **invariante**. Es posible que un `push`, un `pop` o un
`rotate` funcionen a la primera y que, aun así, la estructura esté mal. Me pasó
muchas veces. La lista circular de las grietas rota bien mientras el único
puntero apuntaba al nodo activo, pero en cuanto quise añadir una grieta al final
apareció un `NullPointerException` que no significaba que la lista estuviera rota,
significaba que yo estaba escribiendo el alta desde una posición que la lista nunca
prometió mantener. Lo mismo me pasó con la cola circular: uso `rear.next` como
frente en lugar de guardar dos punteros, lo cual es elegante y además evita tener
que fabricar un nodo centinela, pero exige que cada operación razone sobre lo que
queda detrás de `rear` en lugar de sobre una condición de “lista vacía”. Elegir la
opción más elegante no elimina el trabajo de demostrar que es correcta; solo
cambia en qué tienes que pensar.

El segundo aprendizaje tiene que ver con la prioridad. En principio, resolver
primero las cartas especiales era un problema de orden, y yo lo estaba estudiando
como un problema de orden. Lo que de verdad era era un problema de **estructura
de bloques**: la lista tiene que quedar dividida en dos tramos, uno de
prioridades altas y otro de normales, y el puntero `firstNormal` es exactamente
la frontera entre ambos. En cuanto lo vi así, los tres casos de inserción —todo
altas, todo normales, mezcla— dejaron de ser tres problemas y pasaron a ser tres
posiciones de la misma idea. Comprendí que muchas veces el algoritmo no es la
herramienta que falta, sino la forma que le falta a la estructura que ya tengo.

También cambié de opinión sobre el ordenamiento. Escribí un ordenamiento por
inserción con un peor caso de O(n²) sobre cuarenta cartas, un número que en
cualquier contexto profesional sería inaceptable. Y aun así fue la decisión
correcta, porque el objetivo no era procesar cuarenta cartas: era poder justificar
que el algoritmo es correcto, que es estable porque la comparación es estricta y
no usa `>=`, y que es legible de principio a fin. Ahí entendí algo que me
resultó incómodo y, a la vez, muy útil: la eficiencia no siempre es el objetivo,
y decir “usaría `Arrays.sort`” habría sido la respuesta fácil y la que menos
enseña.

Lo que más me sorprendió fue la arquitectura. La decisión de que el motor
de reglas no imprima nada y devuelva un
`record TurnResult` parecía un detalle de diseño y acabó siendo la que más tiempo
me ahorró. Gracias a ella la interfaz de consola y la ventana Swing no comparten
nada salvo la forma de pintar lo que reciben. No diseñé la primera interfaz
pensando en la segunda, y aun así las dos existen. Eso me enseñó que la
arquitectura que más te ahorra trabajo no es la más elegante, sino la que
hace que las decisiones difíciles se tomen una sola vez.

La parte de las pruebas fue una sorpresa. Escribí una batería de comprobaciones
sin ningún framework, con contadores y un método `check`, y la primera versión
falló más de lo que esperaba. No fallaron las estructuras: fallaron mis supuestos
sobre el reparto de cartas, sobre quién empieza el turno y sobre qué se
consideraba “el mazo vacío”. Esas pruebas no verificaron el código, verificaron
mis ideas sobre el código, y eso es precisamente lo que las hace útiles. También
me dejó una deuda pendiente que reconozco sin excusas: faltan los casos límite
del ordenamiento —lista vacía, un único elemento, lista ya ordenada, lista
invertida y costes duplicados— y las pruebas deberían haber sido casos de JUnit
desde el principio, no un `main` con contadores. Sé exactamente cómo arreglarlo, y
saberlo no es lo mismo que haberlo hecho.

Si tuviera que elegir los tres cambios que más me gustaría aplicar, serían
estos: modelar el efecto de las cartas de utilidad como un campo del
dominio en lugar de decidirlo por el nombre de la carta; poner un índice hash
para localizar el padre en el árbol y convertir ese `addChild` en O(1); y permitir
una semilla en el generador de números aleatorios, para poder reproducir una
partida que salió mal. Los tres son pequeños, y los tres nacen de la misma
conciencia: un sistema que no se puede reproducir ni explicar del todo todavía no
está terminado.

Me queda una idea que antes no tenía y que creo que resume el proyecto: un
programa bien hecho no es el que hace más cosas, sino el que **falla de forma
visible**. Validar los datos en el constructor en lugar de en los setters, no
publicar el nodo interno, y hacer que los tests devuelvan un código de salida
real, todo eso apunta en la misma dirección. Cuando algo falla, hay que saber
dónde mirar. Este trabajo me ha dejado esa costumbre, y creo que es lo más
duradero que me llevo de él.

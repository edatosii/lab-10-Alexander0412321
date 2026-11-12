package co.umariana.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * El árbol genealógico: un SUJETO (posición "1") con sus ASCENDIENTES (padres, abuelos... en Ahnentafel)
 * y sus DESCENDIENTES (hijos, nietos... en d'Aboville). Ver la clase Posicion.
 *
 * Son en realidad dos árboles unidos en la raíz:
 *
 *     ascendientes:  árbol BINARIO. Cada persona tiene como mucho un padre (2n) y una madre (2n + 1).
 *     descendientes: árbol GENERAL. Cada persona tiene una lista de hijos (1.1, 1.2, ...) ordenada por número.
 *
 * El árbol solo guarda a los miembros que EXISTEN: no hay "huecos" ni personas vacías. Puede faltar el padre
 * de alguien, o el hijo 1.1 aunque exista el 1.2.
 *
 * INSTRUCCIONES PARA EL ESTUDIANTE (Etapa 2)
 * ------------------------------------------
 * 1. Implemente el árbol con sus PROPIOS NODOS: una clase interna "Nodo" con la Persona, el nodo del
 *    padre, el nodo de la madre y la lista de hijos. NO se permite usar Map ni Set (las pruebas lo
 *    revisan): para buscar una posición recorra el árbol siguiendo su camino
 *      - Ahnentafel: los bits de n después del primer 1 dicen el camino (0 = padre, 1 = madre).
 *        Ej.: 5 = 101 -> desde el sujeto: padre (0), luego madre (1).
 *      - D'Aboville: cada número de la posición dice a qué hijo bajar. Ej.: 1.2.3 -> hijo 2, luego su hijo 3.
 *    Se permite usar List/ArrayList para la lista de hijos.
 * 2. Todos los errores de las reglas se informan con {@link ArbolInvalidoException} (un mensaje claro).
 *    Los datos inválidos de una Persona los detecta Persona.validar() (IllegalArgumentException).
 * 3. NO modifique las pruebas (src/test).
 *
 * REGLAS DEL ÁRBOL
 *   - La posición "1" (el sujeto) existe siempre y no se puede eliminar.
 *   - Una posición no puede repetirse.
 *   - Un ascendiente n solo se puede agregar si ya existe su hijo n / 2, y debe haber NACIDO ANTES
 *     (año de nacimiento menor) que ese hijo.
 *   - Un descendiente solo se puede agregar si ya existe su progenitor, y debe haber NACIDO DESPUÉS
 *     (año de nacimiento mayor) que su progenitor.
 */
public class ArbolGenealogico {

    /** Un miembro del árbol con sus enlaces: padre y madre (ascendientes) y la lista de hijos (descendientes). */
    private static class Nodo {
        final Persona persona;
        Nodo padre;
        Nodo madre;
        /** Hijos ordenados por su número de orden d'Aboville (de menor a mayor). */
        final List<Nodo> hijos = new ArrayList<>();

        Nodo(Persona persona) {
            this.persona = persona;
        }
    }

    /** El sujeto (posición "1"): la raíz de los dos árboles. */
    private final Nodo raiz;
    /** Cantidad de miembros (el sujeto incluido). */
    private int tamano;

    /**
     * Crea un árbol que solo tiene al sujeto.
     *
     * @throws IllegalArgumentException si la persona no es válida (Persona.validar)
     * @throws ArbolInvalidoException si su posición no es "1"
     */
    public ArbolGenealogico(Persona sujeto) {
        if (sujeto == null) {
            throw new ArbolInvalidoException("El sujeto es obligatorio.");
        }
        sujeto.validar();
        if (!Posicion.esSujeto(sujeto.getPosicion())) {
            throw new ArbolInvalidoException("El sujeto debe estar en la posición 1, no en la "
                    + sujeto.getPosicion() + ".");
        }
        raiz = new Nodo(sujeto);
        tamano = 1;
    }

    /** @return el sujeto (la persona de la posición "1") */
    public Persona sujeto() {
        return raiz.persona;
    }

    /** @return cuántos miembros tiene el árbol (el sujeto cuenta; un árbol nuevo tiene 1) */
    public int tamano() {
        return tamano;
    }

    /**
     * @return la persona de esa posición, o null si no existe en el árbol o si la posición no es válida
     */
    public Persona buscar(String posicion) {
        Nodo nodo = buscarNodo(posicion);
        return nodo == null ? null : nodo.persona;
    }

    /** @return true si hay alguien en esa posición (false también si la posición no es válida) */
    public boolean contiene(String posicion) {
        return buscarNodo(posicion) != null;
    }

    /**
     * Agrega una persona en la posición que ella misma trae (la usa el constructor del árbol a partir del texto).
     * El orden de llamada importa: primero debe existir la persona de la que depende (ver REGLAS DEL ÁRBOL).
     *
     * @return la misma persona
     * @throws IllegalArgumentException si la persona no es válida (Persona.validar)
     * @throws ArbolInvalidoException si la posición ya existe (o es "1"), si falta la persona de la que depende,
     *         o si los años de nacimiento no cumplen las reglas
     */
    public Persona agregar(Persona persona) {
        if (persona == null) {
            throw new ArbolInvalidoException("La persona es obligatoria.");
        }
        persona.validar();
        String posicion = persona.getPosicion();
        if (contiene(posicion)) {
            throw new ArbolInvalidoException("La posición " + posicion + " ya existe en el árbol.");
        }

        Nodo nuevo = new Nodo(persona);
        if (Posicion.esAscendiente(posicion)) {
            String posicionHijo = Posicion.posicionDelHijoEnLinea(posicion);
            Nodo hijo = buscarNodo(posicionHijo);
            if (hijo == null) {
                throw new ArbolInvalidoException("No se puede agregar la posición " + posicion
                        + ": falta su hijo/a en la posición " + posicionHijo + ".");
            }
            if (persona.getNacimiento() >= hijo.persona.getNacimiento()) {
                throw new ArbolInvalidoException("La posición " + posicion + " (" + persona.getNacimiento()
                        + ") debe haber nacido antes que su hijo/a " + posicionHijo
                        + " (" + hijo.persona.getNacimiento() + ").");
            }
            if (Integer.parseInt(posicion) % 2 == 0) {
                hijo.padre = nuevo;
            } else {
                hijo.madre = nuevo;
            }
        } else {
            String posicionProgenitor = Posicion.posicionDelProgenitor(posicion);
            Nodo progenitor = buscarNodo(posicionProgenitor);
            if (progenitor == null) {
                throw new ArbolInvalidoException("No se puede agregar la posición " + posicion
                        + ": falta su progenitor en la posición " + posicionProgenitor + ".");
            }
            if (persona.getNacimiento() <= progenitor.persona.getNacimiento()) {
                throw new ArbolInvalidoException("La posición " + posicion + " (" + persona.getNacimiento()
                        + ") debe haber nacido después que su progenitor " + posicionProgenitor
                        + " (" + progenitor.persona.getNacimiento() + ").");
            }
            insertarHijoOrdenado(progenitor, nuevo);
        }
        tamano++;
        return persona;
    }

    /**
     * Agrega el PADRE de la persona de esa posición, en la posición 2n (ver Posicion.posicionDelPadre).
     * Solo se pueden agregar padres al sujeto y a sus ascendientes.
     *
     * @return la persona creada
     * @throws ArbolInvalidoException si la posición no existe, si no es el sujeto ni un ascendiente, si ese padre
     *         ya existe o si no cumple las reglas del árbol
     * @throws IllegalArgumentException si los datos de la persona no son válidos
     */
    public Persona agregarPadre(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        exigirParaAscendientes(posicion);
        return agregarAscendiente(Posicion.posicionDelPadre(posicion), nombre, nacimiento, fallecimiento);
    }

    /**
     * Igual que agregarPadre, pero agrega la MADRE (posición 2n + 1).
     */
    public Persona agregarMadre(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        exigirParaAscendientes(posicion);
        return agregarAscendiente(Posicion.posicionDeLaMadre(posicion), nombre, nacimiento, fallecimiento);
    }

    /**
     * Agrega un HIJO a la persona de esa posición. Solo se pueden agregar hijos al sujeto y a sus descendientes.
     * Su posición es la del progenitor + "." + el número siguiente al MAYOR número de hijo que ya exista
     * (o 1 si todavía no tiene hijos). Ej.: si "1" tiene los hijos 1.1 y 1.3, el nuevo es 1.4.
     *
     * @return la persona creada
     * @throws ArbolInvalidoException si la posición no existe, si no es el sujeto ni un descendiente, o si no cumple
     *         las reglas del árbol
     * @throws IllegalArgumentException si los datos de la persona no son válidos
     */
    public Persona agregarHijo(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        Nodo progenitor = buscarNodo(posicion);
        if (progenitor == null) {
            throw new ArbolInvalidoException("La posición " + posicion + " no existe en el árbol.");
        }
        if (!Posicion.esSujeto(posicion) && !Posicion.esDescendiente(posicion)) {
            throw new ArbolInvalidoException("Solo se pueden agregar hijos al sujeto y a sus descendientes.");
        }
        int siguiente = 1;
        if (!progenitor.hijos.isEmpty()) {
            Nodo ultimo = progenitor.hijos.get(progenitor.hijos.size() - 1);
            siguiente = Posicion.orden(ultimo.persona.getPosicion()) + 1;
        }
        String nueva = posicion + "." + siguiente;
        if (siguiente > Posicion.MAX_ORDEN || nueva.length() > Posicion.MAX_LONGITUD) {
            throw new ArbolInvalidoException("No se pueden agregar más hijos a la posición " + posicion + ".");
        }
        return agregar(new Persona(nueva, nombre, nacimiento, fallecimiento));
    }

    /**
     * Elimina a la persona de esa posición y TODO lo que depende de ella:
     *   - si es un ascendiente: ella y todos sus propios ascendientes (sus padres, abuelos...);
     *   - si es un descendiente: ella y todos sus propios descendientes (hijos, nietos...).
     * Las demás posiciones NO cambian de número.
     *
     * @return cuántas personas se eliminaron (ella incluida)
     * @throws ArbolInvalidoException si la posición es "1" (el sujeto no se puede eliminar) o si no existe
     */
    public int eliminar(String posicion) {
        if (Posicion.esSujeto(posicion)) {
            throw new ArbolInvalidoException("El sujeto (posición 1) no se puede eliminar.");
        }
        Nodo nodo = buscarNodo(posicion);
        if (nodo == null) {
            throw new ArbolInvalidoException("La posición " + posicion + " no existe en el árbol.");
        }
        int eliminados;
        if (Posicion.esAscendiente(posicion)) {
            Nodo hijo = buscarNodo(Posicion.posicionDelHijoEnLinea(posicion));
            if (hijo.padre == nodo) {
                hijo.padre = null;
            } else {
                hijo.madre = null;
            }
            eliminados = contarAscendientes(nodo);
        } else {
            Nodo progenitor = buscarNodo(Posicion.posicionDelProgenitor(posicion));
            progenitor.hijos.remove(nodo);
            eliminados = contarDescendientes(nodo);
        }
        tamano -= eliminados;
        return eliminados;
    }

    /**
     * @return todos los miembros en este orden: el sujeto; luego los ascendientes por número Ahnentafel
     *         ascendente (2, 3, 4, 5...); luego los descendientes en PREORDEN (cada persona y después sus hijos:
     *         1.1, 1.1.1, 1.1.2, 1.2, ...). La lista es una copia nueva: modificarla no cambia el árbol.
     *         Este orden garantiza que cada persona aparece DESPUÉS de la que necesita (útil para guardar y cargar).
     */
    public List<Persona> miembros() {
        List<Persona> lista = new ArrayList<>();
        lista.add(raiz.persona);
        lista.addAll(ascendientes());
        lista.addAll(descendientes());
        return lista;
    }

    /** @return solo los ascendientes, por número Ahnentafel ascendente (sin el sujeto) */
    public List<Persona> ascendientes() {
        // Recorrido por niveles (BFS) del árbol binario: en cada generación, de izquierda (padre) a derecha
        // (madre), los números Ahnentafel quedan en orden ascendente.
        List<Persona> lista = new ArrayList<>();
        List<Nodo> cola = new ArrayList<>();
        cola.add(raiz);
        for (int i = 0; i < cola.size(); i++) {
            Nodo actual = cola.get(i);
            if (actual != raiz) {
                lista.add(actual.persona);
            }
            if (actual.padre != null) {
                cola.add(actual.padre);
            }
            if (actual.madre != null) {
                cola.add(actual.madre);
            }
        }
        return lista;
    }

    /** @return solo los descendientes, en preorden (sin el sujeto) */
    public List<Persona> descendientes() {
        List<Persona> lista = new ArrayList<>();
        for (Nodo hijo : raiz.hijos) {
            preorden(hijo, lista);
        }
        return lista;
    }

    // =====================================================================
    // Métodos auxiliares
    // =====================================================================

    /** Sigue el camino de la posición desde la raíz; null si no existe o si la posición no es válida. */
    private Nodo buscarNodo(String posicion) {
        if (Posicion.esAhnentafel(posicion)) {
            int n = Integer.parseInt(posicion);
            Nodo actual = raiz;
            // los bits de n después del primer 1, del más alto al más bajo: 0 = padre, 1 = madre
            for (int bit = Posicion.generacion(posicion) - 1; bit >= 0 && actual != null; bit--) {
                actual = ((n >> bit) & 1) == 0 ? actual.padre : actual.madre;
            }
            return actual;
        }
        if (Posicion.esDAboville(posicion)) {
            String[] partes = posicion.split("\\.");
            Nodo actual = raiz;
            for (int i = 1; i < partes.length && actual != null; i++) {
                actual = hijoConOrden(actual, Integer.parseInt(partes[i]));
            }
            return actual;
        }
        return null;
    }

    private static Nodo hijoConOrden(Nodo nodo, int orden) {
        for (Nodo hijo : nodo.hijos) {
            int o = Posicion.orden(hijo.persona.getPosicion());
            if (o == orden) {
                return hijo;
            }
            if (o > orden) {
                return null; // la lista está ordenada: ya no puede aparecer
            }
        }
        return null;
    }

    /** Inserta el hijo manteniendo la lista ordenada por número de orden. */
    private static void insertarHijoOrdenado(Nodo progenitor, Nodo nuevo) {
        int orden = Posicion.orden(nuevo.persona.getPosicion());
        int i = 0;
        while (i < progenitor.hijos.size()
                && Posicion.orden(progenitor.hijos.get(i).persona.getPosicion()) < orden) {
            i++;
        }
        progenitor.hijos.add(i, nuevo);
    }

    /** Comprueba que a la persona de esa posición se le puedan agregar padre o madre. */
    private void exigirParaAscendientes(String posicion) {
        if (!contiene(posicion)) {
            throw new ArbolInvalidoException("La posición " + posicion + " no existe en el árbol.");
        }
        if (!Posicion.esAhnentafel(posicion)) {
            throw new ArbolInvalidoException("Solo se pueden agregar padres al sujeto y a sus ascendientes.");
        }
    }

    private Persona agregarAscendiente(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        if (!Posicion.esAhnentafel(posicion)) {
            throw new ArbolInvalidoException("No se pueden agregar más de 10 generaciones de ascendientes.");
        }
        return agregar(new Persona(posicion, nombre, nacimiento, fallecimiento));
    }

    private static int contarAscendientes(Nodo nodo) {
        if (nodo == null) {
            return 0;
        }
        return 1 + contarAscendientes(nodo.padre) + contarAscendientes(nodo.madre);
    }

    private static int contarDescendientes(Nodo nodo) {
        int total = 1;
        for (Nodo hijo : nodo.hijos) {
            total += contarDescendientes(hijo);
        }
        return total;
    }

    private static void preorden(Nodo nodo, List<Persona> lista) {
        lista.add(nodo.persona);
        for (Nodo hijo : nodo.hijos) {
            preorden(hijo, lista);
        }
    }
}

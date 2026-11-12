package co.umariana.construccion;

import co.umariana.modelo.ArbolGenealogico;
import co.umariana.modelo.ArbolInvalidoException;
import co.umariana.modelo.Persona;
import co.umariana.modelo.Posicion;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Construye un árbol genealógico a partir de un texto: cada línea es una persona.
 *
 *     posicion;nombre;nacimiento;fallecimiento
 *
 * Ejemplo (el fallecimiento puede quedar vacío):
 *
 *     1;John Doe;2002;
 *     2;Henry Doe;1920;1990
 *     3;Maria Doe;1921;
 *     1.1;Timmy Doe;2021;
 *     1.2;Amy Doe;2026;
 *
 * INSTRUCCIONES PARA EL ESTUDIANTE (Etapa 3)
 * ------------------------------------------
 * Reemplace el "throw new UnsupportedOperationException" por su implementación. Esta clase usa lo que ya
 * hizo en las etapas anteriores (Posicion, Persona y ArbolGenealogico). NO modifique las pruebas (src/test).
 *
 * REGLAS
 *   1. Si el texto es null o no tiene ninguna línea con contenido: ArbolInvalidoException (mensaje: "El texto está vacío...").
 *   2. Las líneas pueden terminar en \n o en \r\n. Las líneas en blanco se ignoran, pero los números de línea
 *      que se informan cuentan TODAS las líneas del texto (la primera es la línea 1).
 *   3. Cada línea tiene 4 campos separados por ';' (el cuarto, el fallecimiento, puede estar vacío). Se acepta
 *      también la línea con solo 3 campos y sin el último ';'. Los campos se recortan (trim).
 *      Si hay menos de 3 o más de 4 campos: error.
 *   4. El nacimiento y el fallecimiento deben ser números enteros (el fallecimiento vacío es "no tiene").
 *   5. Cada persona debe ser válida (Persona.validar) y no puede haber dos líneas con la misma posición.
 *   6. Debe existir la persona "1" (el sujeto), pero puede estar en cualquier línea.
 *   7. Las líneas pueden venir en CUALQUIER orden. El constructor debe agregarlas al árbol en un orden en el
 *      que cada persona ya tenga a la que necesita (por ejemplo, primero los ascendientes por número Ahnentafel
 *      ascendente y los descendientes por generación). Las reglas del árbol (ver ArbolGenealogico) las valida el árbol.
 *   8. Todos los errores asociados a una línea se informan con ArbolInvalidoException y un mensaje que EMPIEZA con
 *      "Línea N: " (N es el número de línea de la persona con el problema), por ejemplo:
 *          "Línea 3: la posición 2 está repetida."
 *      Los errores que no son de una línea (texto vacío, falta el sujeto) no llevan ese prefijo.
 *   9. Solo se muestran los miembros que están en el texto: no se crean personas vacías para los que faltan.
 */
public final class ConstructorArbol {

    private ConstructorArbol() {
    }

    /** Una persona leída del texto junto con el número de línea en que apareció. */
    private record Linea(int numero, Persona persona) {
    }

    /**
     * Convierte el texto en un árbol (ver las reglas arriba).
     *
     * @throws ArbolInvalidoException si el texto no cumple las reglas
     */
    public static ArbolGenealogico construir(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ArbolInvalidoException("El texto está vacío: escriba al menos la persona 1 (el sujeto).");
        }

        // 1. Leer y validar cada línea (en el orden del texto).
        List<Linea> lineas = new ArrayList<>();
        String[] renglones = texto.split("\n", -1);
        for (int i = 0; i < renglones.length; i++) {
            String renglon = renglones[i].strip(); // quita también el \r de Windows
            if (renglon.isEmpty()) {
                continue;
            }
            int numero = i + 1;
            Persona persona = leerPersona(numero, renglon);
            for (Linea anterior : lineas) {
                if (anterior.persona().getPosicion().equals(persona.getPosicion())) {
                    throw errorDeLinea(numero, "la posición " + persona.getPosicion()
                            + " está repetida (ya aparece en la línea " + anterior.numero() + ").");
                }
            }
            lineas.add(new Linea(numero, persona));
        }

        // 2. Buscar al sujeto.
        Linea sujeto = null;
        for (Linea l : lineas) {
            if (Posicion.esSujeto(l.persona().getPosicion())) {
                sujeto = l;
            }
        }
        if (sujeto == null) {
            throw new ArbolInvalidoException("Falta el sujeto: debe existir una línea con la posición 1.");
        }

        // 3. Ordenar para que cada persona se agregue después de la que necesita:
        //    ascendientes por número Ahnentafel y luego descendientes por generación.
        List<Linea> ascendientes = new ArrayList<>();
        List<Linea> descendientes = new ArrayList<>();
        for (Linea l : lineas) {
            String p = l.persona().getPosicion();
            if (Posicion.esAscendiente(p)) {
                ascendientes.add(l);
            } else if (Posicion.esDescendiente(p)) {
                descendientes.add(l);
            }
        }
        ascendientes.sort(Comparator.comparingInt(l -> Integer.parseInt(l.persona().getPosicion())));
        descendientes.sort(Comparator.comparingInt(l -> Posicion.generacion(l.persona().getPosicion())));

        // 4. Construir el árbol (las reglas las valida el propio árbol).
        ArbolGenealogico arbol;
        try {
            arbol = new ArbolGenealogico(sujeto.persona());
        } catch (IllegalArgumentException e) {
            throw errorDeLinea(sujeto.numero(), e.getMessage());
        }
        List<Linea> orden = new ArrayList<>(ascendientes);
        orden.addAll(descendientes);
        for (Linea l : orden) {
            try {
                arbol.agregar(l.persona());
            } catch (IllegalArgumentException e) {
                throw errorDeLinea(l.numero(), e.getMessage());
            }
        }
        return arbol;
    }

    /** Convierte una línea (no vacía) en una Persona válida; si no se puede, error con el número de línea. */
    private static Persona leerPersona(int numero, String renglon) {
        String[] campos = renglon.split(";", -1);
        if (campos.length < 3 || campos.length > 4) {
            throw errorDeLinea(numero, "se esperaban 4 campos separados por ';' "
                    + "(posicion;nombre;nacimiento;fallecimiento) y hay " + campos.length + ".");
        }
        String posicion = campos[0].strip();
        String nombre = campos[1].strip();
        int nacimiento = leerAnio(numero, campos[2].strip(), "nacimiento");
        Integer fallecimiento = null;
        if (campos.length == 4 && !campos[3].isBlank()) {
            fallecimiento = leerAnio(numero, campos[3].strip(), "fallecimiento");
        }
        Persona persona = new Persona(posicion, nombre, nacimiento, fallecimiento);
        try {
            persona.validar();
        } catch (IllegalArgumentException e) {
            throw errorDeLinea(numero, e.getMessage());
        }
        return persona;
    }

    private static int leerAnio(int numero, String valor, String campo) {
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw errorDeLinea(numero, "el año de " + campo + " debe ser un número entero (se recibió \""
                    + valor + "\").");
        }
    }

    private static ArbolInvalidoException errorDeLinea(int numero, String mensaje) {
        return new ArbolInvalidoException("Línea " + numero + ": " + mensaje);
    }
}

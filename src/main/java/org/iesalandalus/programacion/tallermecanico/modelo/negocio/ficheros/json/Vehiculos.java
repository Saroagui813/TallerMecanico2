package org.iesalandalus.programacion.tallermecanico.modelo.negocio.ficheros.json;

import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.iesalandalus.programacion.tallermecanico.modelo.TallerMecanicoExcepcion;
import org.iesalandalus.programacion.tallermecanico.modelo.dominio.Vehiculo;
import org.iesalandalus.programacion.tallermecanico.modelo.negocio.IVehiculos;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Vehiculos implements IVehiculos {

    private static final String FICHERO_VEHICULOS = String.format("%s%s%s%s%s%s%s", "datos", File.separator, "ficheros", File.separator, "json", File.separator, "vehiculos.json");

    private static ObjectMapper mapper;
    private static Vehiculos instancia;

    private Vehiculos() {
        mapper = new ObjectMapper();
    }

    static Vehiculos getInstancia() {
        if (instancia == null) {
            instancia = new Vehiculos();
        }
        return instancia;
    }

    @Override
    public void comenzar() {

    }

    public List<Vehiculo> leer() {
        List<Vehiculo> vehiculos = new ArrayList<>();
        File fichero = new File(FICHERO_VEHICULOS);
        if (fichero.exists()) {
            try {
                vehiculos = mapper.readValue(fichero, new TypeReference<>() {
                });
            } catch (StreamWriteException e) {
                System.err.printf("Error de sintaxis en el fichero. %s%n", e.getMessage());
            } catch (DatabindException e) {
                System.err.printf("Error relacionado con el mapeo. %s%n", e.getMessage());
            } catch (IOException e) {
                System.err.printf("Error al leer el fichero. %s%n", e.getMessage());
            }
        } else {
            System.err.printf("El fichero %s no existe.%n", FICHERO_VEHICULOS);
        }
        return vehiculos;
    }

    @Override
    public void terminar() {

    }

    public void escribir(List<Vehiculo> vehiculos) {
        File fichero = new File(FICHERO_VEHICULOS);
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(fichero, vehiculos);
        } catch (StreamWriteException e) {
            System.err.printf("Error de sintaxis en el fichero. %s%n", e.getMessage());
        } catch (DatabindException e) {
            System.err.printf("Error relacionado con el mapeo. %s%n", e.getMessage());
        } catch (IOException e) {
            System.err.printf("Error al escribir el fichero. %s%n", e.getMessage());
        }
    }

    @Override
    public List<Vehiculo> get() {
        return leer();
    }

    @Override
    public void insertar(Vehiculo vehiculo) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(vehiculo, "No se puede insertar un vehículo nulo.");
        List<Vehiculo> lista = leer();
        if (lista.contains(vehiculo)) {
            throw new TallerMecanicoExcepcion("Ya existe un vehículo con esa matrícula.");
        } else  {
            lista.add(vehiculo);
            escribir(lista);
        }
    }

    @Override
    public Vehiculo buscar(Vehiculo vehiculo) {
        Objects.requireNonNull(vehiculo, "No se puede buscar un vehículo nulo.");
        List<Vehiculo> lista = leer();
        int indice = lista.indexOf(vehiculo);
        return (indice == -1) ? null : lista.get(indice);
    }

    @Override
    public void borrar(Vehiculo vehiculo) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(vehiculo, "No se puede borrar un vehículo nulo.");
        List<Vehiculo> lista = leer();
        Vehiculo vehiculo1 = buscar(vehiculo);
        if (vehiculo1 != null) {
            lista.remove(vehiculo1);
            escribir(lista);
        } else {
            throw new TallerMecanicoExcepcion("No existe ningún vehículo con esa matrícula.");
        }
    }
}


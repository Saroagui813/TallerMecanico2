package org.iesalandalus.programacion.tallermecanico.modelo.negocio.ficheros.json;

import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.iesalandalus.programacion.tallermecanico.modelo.TallerMecanicoExcepcion;
import org.iesalandalus.programacion.tallermecanico.modelo.dominio.Cliente;
import org.iesalandalus.programacion.tallermecanico.modelo.negocio.IClientes;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Clientes implements IClientes {

    private static final String FICHERO_CLIENTES = String.format("%s%s%s%s%s%s%s", "datos", File.separator, "ficheros", File.separator, "json", File.separator, "clientes.json");

    private ObjectMapper mapper;
    private static Clientes instancia;

    private Clientes() {
        mapper = new ObjectMapper();
    }

    static Clientes getInstancia() {
        if (instancia == null) {
            instancia = new Clientes();
        }
        return instancia;
    }

    @Override
    public void comenzar() {

    }

    public List<Cliente> leer() {
        List<Cliente> clientes = new ArrayList<>();
        File fichero = new File(FICHERO_CLIENTES);
        if (fichero.exists()) {
            try {
                clientes = mapper.readValue(fichero, new TypeReference<>() {
                });
            } catch (StreamWriteException e) {
                System.err.printf("Error de sintaxis en el fichero. %s%n", e.getMessage());
            } catch (DatabindException e) {
                System.err.printf("Error relacionado con el mapeo. %s%n", e.getMessage());
            } catch (IOException e) {
                System.err.printf("Error al leer el fichero. %s%n", e.getMessage());
            }
        } else {
            System.err.printf("El fichero %s no existe.%n", FICHERO_CLIENTES);
        }
        return clientes;
    }

    @Override
    public void terminar() {

    }

    public void escribir(List<Cliente> clientes) {
        File fichero = new File(FICHERO_CLIENTES);
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(fichero, clientes);
        } catch (StreamWriteException e) {
            System.err.printf("Error de sintaxis en el fichero. %s%n", e.getMessage());
        } catch (DatabindException e) {
            System.err.printf("Error relacionado con el mapeo. %s%n", e.getMessage());
        } catch (IOException e) {
            System.err.printf("Error al escribir el fichero. %s%n", e.getMessage());
        }
    }

    @Override
    public List<Cliente> get() {
        return leer();
    }

    @Override
    public void insertar(Cliente cliente) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(cliente, "No se puede insertar un cliente nulo.");
        List<Cliente> lista = leer();
        if (lista.contains(cliente)) {
            throw new TallerMecanicoExcepcion("Ya existe un cliente con ese DNI.");
        } else  {
            lista.add(cliente);
            escribir(lista);
        }
    }

    @Override
    public Cliente modificar(Cliente cliente, String nombre, String telefono) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(cliente, "No se puede modificar un cliente nulo.");
        List<Cliente> lista = leer();
        Cliente cliente1 = buscar(cliente);
        if (cliente1 != null) {
            cliente1.setNombre(nombre);
            cliente1.setTelefono(telefono);
            escribir(lista);
        } else {
            throw new TallerMecanicoExcepcion("No existe ningún cliente con ese DNI.");
        }
        return cliente1;
    }

    @Override
    public Cliente buscar(Cliente cliente) {
        Objects.requireNonNull(cliente, "No se puede buscar un cliente nulo.");
        List<Cliente> lista = leer();
        int indice = lista.indexOf(cliente);
        return (indice == -1) ? null : lista.get(indice);
    }

    @Override
    public void borrar(Cliente cliente) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(cliente, "No se puede borrar un cliente nulo.");
        List<Cliente> lista = leer();
        Cliente cliente1 = buscar(cliente);
        if (cliente1 != null) {
            lista.remove(cliente1);
            escribir(lista);
        } else {
            throw new TallerMecanicoExcepcion("No existe ningún cliente con ese DNI.");
        }
    }
}

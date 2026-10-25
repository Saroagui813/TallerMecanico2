package org.iesalandalus.programacion.tallermecanico.modelo.negocio.ficheros.json;

import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.iesalandalus.programacion.tallermecanico.modelo.TallerMecanicoExcepcion;
import org.iesalandalus.programacion.tallermecanico.modelo.dominio.*;
import org.iesalandalus.programacion.tallermecanico.modelo.negocio.ITrabajos;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

public class Trabajos implements ITrabajos {

    private static final String FICHERO_TRABAJOS = String.format("%s%s%s%s%s%s%s", "datos", File.separator, "ficheros", File.separator, "json", File.separator, "trabajos.json");
    private static final TypeReference<List<Trabajo>> TYPE_LIST_TRABAJO = new TypeReference<>() {};

    private static ObjectMapper mapper;
    private static Trabajos instancia;


    private Trabajos() {
        mapper = new ObjectMapper();
    }

    static Trabajos getInstancia() {
        if (instancia == null) {
            instancia = new Trabajos();
        }
        return instancia;
    }

    @Override
    public void comenzar() {

    }

    public List<Trabajo> leer() {
        List<Trabajo> trabajos = new ArrayList<>();
        File fichero = new File(FICHERO_TRABAJOS);
        if (fichero.exists()) {
            try {
                trabajos = mapper.readValue(fichero, TYPE_LIST_TRABAJO);
            } catch (StreamWriteException e) {
                System.err.printf("Error de sintaxis en el fichero. %s%n", e.getMessage());
            } catch (DatabindException e) {
                System.err.printf("Error relacionado con el mapeo. %s%n", e.getMessage());
            } catch (IOException e) {
                System.err.printf("Error al leer el fichero. %s%n", e.getMessage());
            }
        } else {
            System.err.printf("El fichero %s no existe.%n", FICHERO_TRABAJOS);
        }
        return trabajos;
    }

    public void escribir(List<Trabajo> trabajos) {
        File fichero = new File(FICHERO_TRABAJOS);
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(fichero, trabajos);
        } catch (StreamWriteException e) {
            System.err.printf("Error de sintaxis en el fichero. %s%n", e.getMessage());
        } catch (DatabindException e) {
            System.err.printf("Error relacionado con el mapeo. %s%n", e.getMessage());
        } catch (IOException e) {
            System.err.printf("Error al escribir el fichero. %s%n", e.getMessage());
        }
    }

    @Override
    public void terminar() {

    }

    @Override
    public List<Trabajo> get() {
        return leer();
    }

    @Override
    public List<Trabajo> get(Cliente cliente) {
        Objects.requireNonNull(cliente,"El cliente no puede ser nulo.");
        List<Trabajo> trabajosCliente = new ArrayList<>();
        List<Trabajo> trabajos = leer();
        for (Trabajo trabajo : trabajos) {
            if(trabajo.getCliente().equals(cliente)){
                trabajosCliente.add(trabajo);
            }
        }
        return trabajosCliente;
    }

    @Override
    public List<Trabajo> get(Vehiculo vehiculo) {
        Objects.requireNonNull(vehiculo,"El vehiculo no puede ser nulo.");
        List<Trabajo> trabajosVehiculo = new ArrayList<>();
        List<Trabajo> trabajos = leer();
        for (Trabajo trabajo : trabajos) {
            if(trabajo.getVehiculo().equals(vehiculo)){
                trabajosVehiculo.add(trabajo);
            }
        }
        return trabajosVehiculo;
    }

    @Override
    public Map<TipoTrabajo, Integer> getEstadisticasMensuales(LocalDate mes) {
        Objects.requireNonNull(mes, "El mes no puede ser nulo.");
        Map<TipoTrabajo, Integer> estadisticas = inicializarEstadisticas();
        List<Trabajo> trabajos = leer();
        for (Trabajo trabajo : trabajos) {
            LocalDate fecha = trabajo.getFechaInicio();
            if (fecha.getMonthValue() == mes.getMonthValue() && fecha.getYear() == mes.getYear()) {
                TipoTrabajo tipoTrabajo = TipoTrabajo.get(trabajo);
                estadisticas.put(tipoTrabajo, estadisticas.get(tipoTrabajo) + 1);
            }
        }
        return estadisticas;
    }

    private Map<TipoTrabajo, Integer> inicializarEstadisticas() {
        Map<TipoTrabajo, Integer> estadisticas = new EnumMap<>(TipoTrabajo.class);
        for (TipoTrabajo tipoTrabajo : TipoTrabajo.values()) {
            estadisticas.put(tipoTrabajo, 0);
        }
        return estadisticas;
    }

    @Override
    public void insertar(Trabajo trabajo) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(trabajo, "No se puede insertar un trabajo nulo.");
        List<Trabajo> trabajos = leer();
        if (trabajos.contains(trabajo)) {
            throw new TallerMecanicoExcepcion("El trabajo ya existe.");
        } else {
            trabajos.add(trabajo);
            escribir(trabajos);
        }
    }

    @Override
    public Trabajo anadirHoras(Trabajo trabajo, int horas) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(trabajo, "No puedo añadir horas a un trabajo nulo.");
        Trabajo trabajoEncontrado = getTrabajoAbierto(trabajo.getVehiculo());
        trabajoEncontrado.anadirHoras(horas);
        return trabajoEncontrado;
    }

    private Trabajo getTrabajoAbierto(Vehiculo vehiculo) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(vehiculo, "No puedo operar sobre un vehículo nulo.");
        Trabajo trabajoEncontrado = null;
        List<Trabajo> trabajos = leer();
        Iterator<Trabajo> iteradorTrabajos = trabajos.iterator();
        while (iteradorTrabajos.hasNext() && trabajoEncontrado == null) {
            Trabajo trabajo = iteradorTrabajos.next();
            if (trabajo.getVehiculo().equals(vehiculo) && !trabajo.estaCerrado()) {
                trabajoEncontrado = trabajo;
            }
        }
        if (trabajoEncontrado == null) {
            throw new TallerMecanicoExcepcion("No existe ningún trabajo abierto para dicho vehículo.");
        }
        return trabajoEncontrado;
    }

    @Override
    public Trabajo anadirPrecioMaterial(Trabajo trabajo, float precioMaterial) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(trabajo, "No puedo añadir precio del material a un trabajo nulo.");
        Trabajo trabajoEncontrado = getTrabajoAbierto(trabajo.getVehiculo());
        if (trabajoEncontrado instanceof Mecanico mecanico) {
            mecanico.anadirPrecioMaterial(precioMaterial);
        } else {
            throw new TallerMecanicoExcepcion("No se puede añadir precio al material para este tipo de trabajos.");
        }
        return trabajoEncontrado;
    }

    @Override
    public Trabajo cerrar(Trabajo trabajo, LocalDate fechaFin) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(trabajo, "No puedo cerrar un trabajo nulo.");
        Trabajo trabajoEncontrado = getTrabajoAbierto(trabajo.getVehiculo());
        trabajoEncontrado.cerrar(fechaFin);
        return trabajoEncontrado;
    }

    @Override
    public Trabajo buscar(Trabajo trabajo) {
        Objects.requireNonNull(trabajo, "No se puede buscar un trabajo nulo.");
        List<Trabajo> trabajos = leer();
        int indice = trabajos.indexOf(trabajo);
        return (indice == -1) ? null : trabajos.get(indice);
    }

    @Override
    public void borrar(Trabajo trabajo) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(trabajo, "No se puede borrar un trabajo nulo.");
        List<Trabajo> trabajos = leer();
        Trabajo trabajo1 = buscar(trabajo);
        if (trabajo1 != null) {
            trabajos.remove(trabajo1);
            escribir(trabajos);
        } else {
            throw new TallerMecanicoExcepcion("No existe ningún trabajo con esa matrícula.");
        }
    }
}

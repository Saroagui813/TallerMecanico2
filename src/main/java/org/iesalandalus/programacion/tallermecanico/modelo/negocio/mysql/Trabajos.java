package org.iesalandalus.programacion.tallermecanico.modelo.negocio.mysql;

import org.iesalandalus.programacion.tallermecanico.modelo.TallerMecanicoExcepcion;
import org.iesalandalus.programacion.tallermecanico.modelo.dominio.*;
import org.iesalandalus.programacion.tallermecanico.modelo.negocio.ITrabajos;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

public class Trabajos implements ITrabajos {

    private Connection conexion;
    private static Trabajos instancia;

    protected static Trabajos getInstancia() {
        if (instancia == null) {
            instancia = new Trabajos();
        }
        return instancia;
    }

    private Trabajos() {

    }

    @Override
    public void comenzar() {
        conexion = MySQL.getConexion();
    }

    @Override
    public void terminar() {
        MySQL.cerrarConexion();
    }

    @Override
    public List<Trabajo> get() {
        List<Trabajo> trabajos = new ArrayList<>();
        String consultaSQL = "SELECT * FROM trabajos";
        try {
            PreparedStatement sentencia = conexion.prepareStatement(consultaSQL);
            ResultSet resultado = sentencia.executeQuery();
            while (resultado.next()) {
                String dni = resultado.getString("dni");
                String matricula = resultado.getString("matricula");
                LocalDate fechaInicio = resultado.getDate("fecha_inicio").toLocalDate();
                String tipo = resultado.getString("tipo");
                int horas = resultado.getInt("horas");
                float precioMaterial = resultado.getFloat("precio_material");
                Date fechaFin = resultado.getDate("fecha_fin");

                Cliente cliente = Clientes.getInstancia().buscar(Cliente.get(dni));
                Vehiculo vehiculo = Vehiculos.getInstancia().buscar(Vehiculo.get(matricula));

                Trabajo trabajo;

                if (tipo.equalsIgnoreCase("REVISION")) {
                    trabajo = new Revision(cliente, vehiculo, fechaInicio);
                } else {
                    trabajo = new Mecanico(cliente, vehiculo, fechaInicio);
                }

                try {
                    if (horas > 0) {
                        trabajo.anadirHoras(horas);
                    }
                    if (trabajo instanceof Mecanico mecanico && precioMaterial > 0) {
                        mecanico.anadirPrecioMaterial(precioMaterial);
                    }
                    if (fechaFin != null) {
                        trabajo.cerrar(fechaFin.toLocalDate());
                    }

                } catch (TallerMecanicoExcepcion e) {
                    System.out.println("ERROR: " + e.getMessage());
                }

                trabajos.add(trabajo);
            }
            sentencia.close();
            resultado.close();
        } catch (SQLException e) {
            System.out.println("ERROR MySQL: "+ e.getMessage());
        }
        return trabajos;
    }

    @Override
    public List<Trabajo> get(Cliente cliente) {
        Objects.requireNonNull(cliente,"El cliente no puede ser nulo.");
        List<Trabajo> trabajosCliente = new ArrayList<>();
        List<Trabajo> trabajos = get();
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
        List<Trabajo> trabajosVehiculos = new ArrayList<>();
        List<Trabajo> trabajos = get();
        for (Trabajo trabajo : trabajos) {
            if (trabajo.getVehiculo().equals(vehiculo)) {
                trabajosVehiculos.add(trabajo);
            }
        }
        return trabajosVehiculos;
    }

    @Override
    public Map<TipoTrabajo, Integer> getEstadisticasMensuales(LocalDate mes) {
        Objects.requireNonNull(mes, "El mes no puede ser nulo.");

        return Map.of();
    }

    private Map<TipoTrabajo, Integer> inicializarEstadisticas() {
        return null;
    }

    @Override
    public void insertar(Trabajo trabajo) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(trabajo, "El trabajo no puede ser nulo.");
        int filas = 0;
        if (buscar(trabajo) != null) {
            throw new TallerMecanicoExcepcion("El trabajo ya existe.");
        } else {
            String consultaSQL = "INSERT INTO trabajos VALUES (?, ?, ?, ?, ?, ?, ?)";
            try {
                PreparedStatement sentencia = conexion.prepareStatement(consultaSQL);
                sentencia.setString(1, trabajo.getCliente().getDni());
                sentencia.setString(2, trabajo.getVehiculo().matricula());
                sentencia.setDate(3, Date.valueOf(trabajo.getFechaInicio()));
                sentencia.setDate(4, trabajo.estaCerrado() ? Date.valueOf(trabajo.getFechaFin()) : null);
                sentencia.setInt(5, trabajo.getHoras());
                sentencia.setString(6, TipoTrabajo.get(trabajo).name());

                if (trabajo instanceof Mecanico mecanico) {
                    sentencia.setFloat(7, mecanico.getPrecioMaterial());
                } else {
                    sentencia.setNull(7, Types.FLOAT);
                }

                filas = sentencia.executeUpdate();
                sentencia.close();
            } catch (SQLException e) {
                System.out.println("ERROR MySQL: "+ e.getMessage());
            }

            System.out.println("Filas insertadas: " + filas);
        }
    }

    private void comprobarTrabajo(Cliente cliente, Vehiculo vehiculo, LocalDate fechaInicio) {

    }

    @Override
    public Trabajo anadirHoras(Trabajo trabajo, int horas) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(trabajo, "No puedo añadir horas a un trabajo nulo.");
        if (buscar(trabajo) == null) {
            throw new TallerMecanicoExcepcion("El trabajo no existe.");
        }
        return null;
    }

    @Override
    public Trabajo anadirPrecioMaterial(Trabajo trabajo, float precioMaterial) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(trabajo, "No puedo añadir precio del material a un trabajo nulo.");

        return null;
    }

    @Override
    public Trabajo cerrar(Trabajo trabajo, LocalDate fechaFin) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(trabajo, "No puedo cerrar un trabajo nulo.");

        return null;
    }

    @Override
    public Trabajo buscar(Trabajo trabajo) {
        Objects.requireNonNull(trabajo, "No se puede buscar un trabajo nulo.");
        Trabajo encontrado = null;
        String consultaSQL = "SELECT * FROM trabajos WHERE dni = ? AND matricula = ? AND fecha_inicio = ?";
        PreparedStatement sentencia;
        try {
            sentencia = conexion.prepareStatement(consultaSQL);
            sentencia.setString(1, trabajo.getCliente().getDni());
            sentencia.setString(2, trabajo.getVehiculo().matricula());
            sentencia.setDate(3, Date.valueOf(trabajo.getFechaInicio()));
            ResultSet resultado = sentencia.executeQuery();
            if (resultado.next()) {
                String dni = resultado.getString("dni");
                String matricula = resultado.getString("matricula");
                LocalDate fechaInicio = resultado.getDate("fecha_inicio").toLocalDate();
                String tipo = resultado.getString("tipo");
                int horas = resultado.getInt("horas");
                float precioMaterial = resultado.getFloat("precio_material");
                Date fechaFin = resultado.getDate("fecha_fin");

                Cliente cliente = Clientes.getInstancia().buscar(Cliente.get(dni));
                Vehiculo vehiculo = Vehiculos.getInstancia().buscar(Vehiculo.get(matricula));

                if (tipo.equalsIgnoreCase("REVISION")) {
                    encontrado = new Revision(cliente, vehiculo, fechaInicio);
                } else {
                    encontrado = new Mecanico(cliente, vehiculo, fechaInicio);
                }

                try {
                    if (horas > 0) {
                        encontrado.anadirHoras(horas);
                    }
                    if (encontrado instanceof Mecanico mecanico && precioMaterial > 0) {
                        mecanico.anadirPrecioMaterial(precioMaterial);
                    }
                    if (fechaFin != null) {
                        encontrado.cerrar(fechaFin.toLocalDate());
                    }

                } catch (TallerMecanicoExcepcion e) {
                    System.out.println("ERROR: " + e.getMessage());
                }
            }
            sentencia.close();
            resultado.close();
        } catch (SQLException e) {
            System.out.println("ERROR MySQL: "+ e.getMessage());
        }
        return encontrado;
    }

    @Override
    public void borrar(Trabajo trabajo) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(trabajo, "No se puede borrar un trabajo nulo.");
    }
}

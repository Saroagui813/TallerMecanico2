package org.iesalandalus.programacion.tallermecanico.modelo.negocio.mysql;

import org.iesalandalus.programacion.tallermecanico.modelo.TallerMecanicoExcepcion;
import org.iesalandalus.programacion.tallermecanico.modelo.dominio.*;
import org.iesalandalus.programacion.tallermecanico.modelo.negocio.ITrabajos;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

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
                String dni = resultado.getString("cliente");
                String matricula = resultado.getString("vehiculo");
                LocalDate fechaInicio = resultado.getDate("fechaInicio").toLocalDate();
                String tipo = resultado.getString("tipo");
                int horas = resultado.getInt("horas");
                float precioMaterial = resultado.getFloat("precioMaterial");
                Date fechaFin = resultado.getDate("fechaFin");

                Cliente cliente = Clientes.getInstancia().buscar(Cliente.get(dni));
                Vehiculo vehiculo = Vehiculos.getInstancia().buscar(Vehiculo.get(matricula));

                Trabajo trabajo;

                if (tipo.equalsIgnoreCase(TipoTrabajo.REVISION.toString())) {
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
            System.out.println("ERROR MySQL: " + e.getMessage());
        }
        return trabajos;
    }

    @Override
    public List<Trabajo> get(Cliente cliente) {
        Objects.requireNonNull(cliente,"El cliente no puede ser nulo.");
        List<Trabajo> trabajosCliente;
        List<Trabajo> trabajos = get();
        trabajosCliente = trabajos.stream()
                .filter(trabajo -> trabajo.getCliente().equals(cliente))
                .collect(Collectors.toList());
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
        Map<TipoTrabajo, Integer> estadisticas = inicializarEstadisticas();
        String consultaSQL = "SELECT tipo, COUNT(*) AS total FROM trabajos WHERE MONTH(fechaInicio) = ? AND YEAR(fechaInicio) = ? GROUP BY tipo";
        try {
            PreparedStatement sentencia = conexion.prepareStatement(consultaSQL);
            sentencia.setInt(1, mes.getMonthValue());
            sentencia.setInt(2, mes.getYear());
            ResultSet resultado = sentencia.executeQuery();
            while (resultado.next()) {
                String tipo = resultado.getString("tipo");
                int total = resultado.getInt("total");

                if (tipo.equalsIgnoreCase(TipoTrabajo.REVISION.toString())) {
                    estadisticas.put(TipoTrabajo.REVISION, total);
                } else {
                    estadisticas.put(TipoTrabajo.MECANICO, total);
                }
            }
            sentencia.close();
            resultado.close();
        } catch (SQLException e) {
            System.out.println("ERROR MySQL: " + e.getMessage());
        }
        return estadisticas;
    }

    private Map<TipoTrabajo, Integer> inicializarEstadisticas() {
        Map<TipoTrabajo, Integer> estadisticas = new EnumMap<>(TipoTrabajo.class);
        estadisticas.put(TipoTrabajo.REVISION, 0);
        estadisticas.put(TipoTrabajo.MECANICO, 0);
        return estadisticas;
    }

    @Override
    public void insertar(Trabajo trabajo) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(trabajo, "El trabajo no puede ser nulo.");
        comprobarTrabajo(trabajo.getCliente(), trabajo.getVehiculo(), trabajo.getFechaInicio());
        int filas = 0;
        String consultaSQL = "INSERT INTO trabajos (cliente, vehiculo, fechaInicio, fechaFin, horas, tipo, precioMaterial) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement sentencia = conexion.prepareStatement(consultaSQL);
            sentencia.setString(1, trabajo.getCliente().getDni());
            sentencia.setString(2, trabajo.getVehiculo().matricula());
            sentencia.setDate(3, Date.valueOf(trabajo.getFechaInicio()));
            sentencia.setDate(4, trabajo.estaCerrado() ? Date.valueOf(trabajo.getFechaFin()) : null);
            sentencia.setInt(5, trabajo.getHoras());
            sentencia.setString(6, TipoTrabajo.get(trabajo).toString());

            if (trabajo instanceof Mecanico mecanico) {
                sentencia.setFloat(7, mecanico.getPrecioMaterial());
            } else {
                sentencia.setNull(7, Types.FLOAT);
            }

            filas = sentencia.executeUpdate();
            sentencia.close();
        } catch (SQLException e) {
            System.out.println("ERROR MySQL: " + e.getMessage());
        }

        System.out.println("Filas insertadas: " + filas);
    }

    private void comprobarTrabajo(Cliente cliente, Vehiculo vehiculo, LocalDate fechaInicio) throws TallerMecanicoExcepcion {
        Trabajo trabajoTemporal = new Mecanico(cliente, vehiculo, fechaInicio);
        if (buscar(trabajoTemporal) != null) {
            throw new TallerMecanicoExcepcion("El trabajo ya existe.");
        }
    }

    @Override
    public Trabajo anadirHoras(Trabajo trabajo, int horas) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(trabajo, "No puedo añadir horas a un trabajo nulo.");
        Trabajo trabajoBuscado = buscar(trabajo);
        if (trabajoBuscado == null) {
            throw new TallerMecanicoExcepcion("El trabajo no existe.");
        } else {
            trabajoBuscado.anadirHoras(horas);
            String consultaSQL = "UPDATE trabajos SET horas = ? WHERE cliente = ? AND vehiculo = ? AND fechaInicio = ?";
            PreparedStatement sentencia;
            try {
                sentencia = conexion.prepareStatement(consultaSQL);
                sentencia.setInt(1, trabajoBuscado.getHoras());
                sentencia.setString(2, trabajoBuscado.getCliente().getDni());
                sentencia.setString(3, trabajoBuscado.getVehiculo().matricula());
                sentencia.setDate(4, Date.valueOf(trabajoBuscado.getFechaInicio()));
                sentencia.executeUpdate();
                sentencia.close();
            } catch (SQLException e) {
                System.out.println("ERROR MySQL: " + e.getMessage());
            }
        }
        return buscar(trabajoBuscado);
    }

    @Override
    public Trabajo anadirPrecioMaterial(Trabajo trabajo, float precioMaterial) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(trabajo, "No puedo añadir precio del material a un trabajo nulo.");
        Trabajo trabajoBuscado = buscar(trabajo);
        if (trabajoBuscado == null) {
            throw new TallerMecanicoExcepcion("El trabajo no existe.");
        } else {
            if (trabajoBuscado instanceof Mecanico mecanico) {
                mecanico.anadirPrecioMaterial(precioMaterial);
                String consultaSQL = "UPDATE trabajos SET precioMaterial = ? WHERE cliente = ? AND vehiculo = ?  AND fechaInicio = ?";
                try {
                    PreparedStatement sentencia = conexion.prepareStatement(consultaSQL);
                    sentencia.setFloat(1, mecanico.getPrecioMaterial());
                    sentencia.setString(2, trabajo.getCliente().getDni());
                    sentencia.setString(3, trabajo.getVehiculo().matricula());
                    sentencia.setDate(4, Date.valueOf(trabajo.getFechaInicio()));
                    sentencia.executeUpdate();
                    sentencia.close();
                } catch (SQLException e) {
                    System.out.println("ERROR MySQL: " + e.getMessage());
                }
            } else {
                throw new TallerMecanicoExcepcion("No se puede añadir precio del material a un trabajo que no es mecánico.");
            }
        }
        return buscar(trabajoBuscado);
    }

    @Override
    public Trabajo cerrar(Trabajo trabajo, LocalDate fechaFin) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(trabajo, "No puedo cerrar un trabajo nulo.");
        Trabajo trabajoBuscado = buscar(trabajo);
        if (trabajoBuscado == null) {
            throw new TallerMecanicoExcepcion("El trabajo no existe.");
        } else {
            trabajoBuscado.cerrar(fechaFin);
            String consultaSQL = "UPDATE trabajos SET fechaFin = ? WHERE cliente = ? AND vehiculo = ? AND fechaInicio = ?";
            try {
                PreparedStatement sentencia = conexion.prepareStatement(consultaSQL);
                sentencia.setDate(1, Date.valueOf(fechaFin));
                sentencia.setString(2, trabajo.getCliente().getDni());
                sentencia.setString(3, trabajo.getVehiculo().matricula());
                sentencia.setDate(4, Date.valueOf(trabajo.getFechaInicio()));
                sentencia.executeUpdate();
                sentencia.close();
            } catch (SQLException e) {
                System.out.println("ERROR MySQL: " + e.getMessage());
            }
        }
        return buscar(trabajoBuscado);
    }

    @Override
    public Trabajo buscar(Trabajo trabajo) {
        Objects.requireNonNull(trabajo, "No se puede buscar un trabajo nulo.");
        Trabajo encontrado = null;
        String consultaSQL = "SELECT * FROM trabajos WHERE cliente = ? AND vehiculo = ? AND fechaInicio = ?";
        try {
            PreparedStatement sentencia = conexion.prepareStatement(consultaSQL);
            sentencia.setString(1, trabajo.getCliente().getDni());
            sentencia.setString(2, trabajo.getVehiculo().matricula());
            sentencia.setDate(3, Date.valueOf(trabajo.getFechaInicio()));
            ResultSet resultado = sentencia.executeQuery();
            if (resultado.next()) {
                String dni = resultado.getString("cliente");
                String matricula = resultado.getString("vehiculo");
                LocalDate fechaInicio = resultado.getDate("fechaInicio").toLocalDate();
                String tipo = resultado.getString("tipo");
                int horas = resultado.getInt("horas");
                float precioMaterial = resultado.getFloat("precioMaterial");
                Date fechaFin = resultado.getDate("fechaFin");

                Cliente cliente = Clientes.getInstancia().buscar(Cliente.get(dni));
                Vehiculo vehiculo = Vehiculos.getInstancia().buscar(Vehiculo.get(matricula));

                if (tipo.equalsIgnoreCase(TipoTrabajo.REVISION.toString())) {
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
            System.out.println("ERROR MySQL: " + e.getMessage());
        }
        return encontrado;
    }

    @Override
    public void borrar(Trabajo trabajo) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(trabajo, "No se puede borrar un trabajo nulo.");
        if (buscar(trabajo) == null) {
            throw new TallerMecanicoExcepcion("No existe ningún trabajo con esa matrícula.");
        } else {
            String consultaSQL = "DELETE FROM trabajos WHERE cliente = ? AND vehiculo = ? AND fechaInicio = ?";
            PreparedStatement sentencia;
            try {
                sentencia = conexion.prepareStatement(consultaSQL);
                sentencia.setString(1, trabajo.getCliente().getDni());
                sentencia.setString(2, trabajo.getVehiculo().matricula());
                sentencia.setDate(3, Date.valueOf(trabajo.getFechaInicio()));
                sentencia.executeUpdate();
                sentencia.close();
            } catch (SQLException e) {
                System.out.println("ERROR MySQL: "+ e.getMessage());
            }
        }
    }
}
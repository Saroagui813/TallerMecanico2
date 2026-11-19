package org.iesalandalus.programacion.tallermecanico.modelo.negocio.mysql;

import org.iesalandalus.programacion.tallermecanico.modelo.TallerMecanicoExcepcion;
import org.iesalandalus.programacion.tallermecanico.modelo.dominio.Vehiculo;
import org.iesalandalus.programacion.tallermecanico.modelo.negocio.IVehiculos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Vehiculos implements IVehiculos {

    private Connection conexion;
    private static Vehiculos instancia;

    static Vehiculos getInstancia() {
        if (instancia == null) {
            instancia = new Vehiculos();
        }
        return instancia;
    }

    private Vehiculos() {

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
    public List<Vehiculo> get() {
        List<Vehiculo> lista = new ArrayList<>();
        String consultaSQL = "SELECT * FROM vehiculos";
        try {
            PreparedStatement sentencia = conexion.prepareStatement(consultaSQL);
            ResultSet resultado = sentencia.executeQuery();
            while(resultado.next()) {
                Vehiculo vehiculo = new Vehiculo(
                resultado.getString(3),
                resultado.getString(2),
                resultado.getString(1)
                );
                lista.add(vehiculo);
            }
            sentencia.close();
            resultado.close();
        } catch (SQLException ex) {
            System.out.println("ERROR MySQL: " + ex.getMessage());
        }
        return lista;
    }

    @Override
    public void insertar(Vehiculo vehiculo) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(vehiculo, "No se puede insertar un vehiculo nulo.");
        int filas = 0;
        if (buscar(vehiculo) != null) {
            throw new TallerMecanicoExcepcion("Ya existe un vehiculo con esa matrícula.");
        } else {
            String consultaSQL = "INSERT INTO vehiculos VALUES (?, ?, ?)";
            PreparedStatement sentencia;
            try {
                sentencia = conexion.prepareStatement(consultaSQL);
                sentencia.setString(1, vehiculo.matricula());
                sentencia.setString(2, vehiculo.modelo());
                sentencia.setString(3, vehiculo.marca());

                filas = sentencia.executeUpdate();

                sentencia.close();

            } catch (SQLException e) {
                System.out.println("ERROR MySQL: " + e.getMessage());
            }

            System.out.println("Filas insertadas: " + filas);
        }
    }

    @Override
    public Vehiculo buscar(Vehiculo vehiculo) {
        Objects.requireNonNull(vehiculo, "No se puede buscar un vehiculo nulo.");
        Vehiculo encontrado = null;
        String consultaSQL = "SELECT * FROM vehiculos WHERE matricula = ?";
        try {
            PreparedStatement sentencia = conexion.prepareStatement(consultaSQL);
            sentencia.setString(1, vehiculo.matricula());
            ResultSet resultado = sentencia.executeQuery();
            if(resultado.next()) {
                encontrado = new Vehiculo(
                        resultado.getString(3),
                        resultado.getString(2),
                        resultado.getString(1)
                );
            }
            sentencia.close();
            resultado.close();
        } catch (SQLException e) {
            System.out.println("ERROR MySQL: " + e.getMessage());
        }
        return encontrado;
    }

    @Override
    public void borrar(Vehiculo vehiculo) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(vehiculo, "No se puede borrar un vehiculo nulo.");
        if (buscar(vehiculo) == null) {
            System.out.println("No existe el vehiculo con esa matricula.");
        } else {
            String consultaSQL = "DELETE FROM vehiculos WHERE matricula = ?";
            try {
                PreparedStatement sentencia = conexion.prepareStatement(consultaSQL);
                sentencia.setString(1, vehiculo.matricula());
                sentencia.executeUpdate();
                sentencia.close();
            } catch (SQLException e) {
                System.out.println("ERROR MySQL: " + e.getMessage());
            }
        }
    }
}

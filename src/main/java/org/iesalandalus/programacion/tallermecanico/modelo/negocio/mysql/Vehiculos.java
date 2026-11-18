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

    @Override
    public void comenzar() {
        conexion = MySQL.getConexion();
    }

    @Override
    public void terminar() {

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
                resultado.getString(1),
                resultado.getString(2),
                resultado.getString(3)
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
        return null;
    }

    @Override
    public void borrar(Vehiculo vehiculo) throws TallerMecanicoExcepcion {

    }
}

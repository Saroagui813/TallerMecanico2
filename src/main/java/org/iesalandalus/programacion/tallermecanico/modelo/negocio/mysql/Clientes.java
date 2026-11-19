package org.iesalandalus.programacion.tallermecanico.modelo.negocio.mysql;

import org.iesalandalus.programacion.tallermecanico.modelo.TallerMecanicoExcepcion;
import org.iesalandalus.programacion.tallermecanico.modelo.dominio.Cliente;
import org.iesalandalus.programacion.tallermecanico.modelo.negocio.IClientes;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Clientes implements IClientes {

    private Connection conexion;
    private static Clientes instancia;

    static Clientes getInstancia(){
        if (instancia == null) {
            instancia = new Clientes();
        }
        return instancia;
    }

    private Clientes() {

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
    public List<Cliente> get() {
        List<Cliente> lista = new ArrayList<>();
        String consultaSQL = "SELECT * FROM clientes";
        try {
            PreparedStatement sentencia = conexion.prepareStatement(consultaSQL);
            ResultSet resultado = sentencia.executeQuery();
            while (resultado.next()) {
                Cliente cliente = new Cliente(
                        resultado.getString(2),
                        resultado.getString(1),
                        resultado.getString(3)
                );
                lista.add(cliente);
            }
            sentencia.close();
            resultado.close();
        } catch (SQLException ex) {
            System.out.println("ERROR MySQL: " + ex.getMessage());
        }
        return lista;
    }

    @Override
    public void insertar(Cliente cliente) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(cliente, "No se puede insertar un cliente nulo.");
        int filas = 0;
        if (buscar(cliente) != null) {
            throw new TallerMecanicoExcepcion("Ya existe un cliente con ese DNI.");
        } else {
            String consultaSQL = "INSERT INTO clientes VALUES (?, ?, ?)";
            PreparedStatement sentencia;
            try {
                sentencia = conexion.prepareStatement(consultaSQL);
                sentencia.setString(1, cliente.getDni());
                sentencia.setString(2, cliente.getNombre());
                sentencia.setString(3, cliente.getTelefono());

                filas = sentencia.executeUpdate();

                sentencia.close();

            } catch (SQLException e) {
                System.out.println("ERROR MySQL: " + e.getMessage());
            }

            System.out.println("Filas insertadas: " + filas);
        }
    }

    @Override
    public Cliente modificar(Cliente cliente, String nombre, String telefono) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(cliente, "No se puede modificar un cliente nulo.");
        Cliente cBuscado = buscar(cliente);
        if (cBuscado == null) {
            throw new TallerMecanicoExcepcion("No existe ningún cliente con ese DNI.");
        }

        String nombreFinal = (nombre == null || nombre.isBlank()) ? cBuscado.getNombre() : nombre;
        String telefonoFinal = (telefono == null || telefono.isBlank()) ? cBuscado.getTelefono() : telefono;

        String consultaSQL = "UPDATE clientes SET nombre = ?, telefono = ? WHERE dni = ?";
        try {
            PreparedStatement sentencia = conexion.prepareStatement(consultaSQL);
            sentencia.setString(1, nombreFinal);
            sentencia.setString(2, telefonoFinal);
            sentencia.setString(3, cBuscado.getDni());
            sentencia.executeUpdate();
            sentencia.close();
        } catch (SQLException e) {
            System.out.println("ERROR MySQL: " + e.getMessage());
        }
        return buscar(cBuscado);
    }

    @Override
    public Cliente buscar(Cliente cliente) {
        Objects.requireNonNull(cliente, "No se puede buscar un cliente nulo.");
        Cliente encontrado = null;
        String consultaSQL = "SELECT * FROM clientes WHERE dni = ?";
        try {
            PreparedStatement sentencia = conexion.prepareStatement(consultaSQL);
            sentencia.setString(1, cliente.getDni());
            ResultSet resultado = sentencia.executeQuery();
            if (resultado.next()) {
                encontrado = new Cliente(
                        resultado.getString(2),
                        resultado.getString(1),
                        resultado.getString(3)
                );
            }
            sentencia.close();
            resultado.close();
        } catch (SQLException ex) {
            System.out.println("ERROR MySQL: " + ex.getMessage());
        }
        return encontrado;
    }

    @Override
    public void borrar(Cliente cliente) throws TallerMecanicoExcepcion {
        Objects.requireNonNull(cliente, "No se puede borrar un cliente nulo.");
        Cliente cBuscado = buscar(cliente);
        if (cBuscado == null) {
            throw new TallerMecanicoExcepcion("No existe ningún cliente con ese DNI.");
        }

        String consultaSQL = "DELETE FROM clientes WHERE dni = ?";
        try {
            PreparedStatement sentencia = conexion.prepareStatement(consultaSQL);
            sentencia.setString(1, cBuscado.getDni());
            sentencia.executeUpdate();
            sentencia.close();
        } catch (SQLException e) {
            System.out.println("ERROR MySQL: " + e.getMessage());
        }
    }
}

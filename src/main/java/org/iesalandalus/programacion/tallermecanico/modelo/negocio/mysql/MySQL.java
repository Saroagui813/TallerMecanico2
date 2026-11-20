package org.iesalandalus.programacion.tallermecanico.modelo.negocio.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQL {

    private static final String HOST = "";
    private static final String ESQUEMA = "tallerMecanico";
    private static final String USUARIO = "root";
    private static final String CONTRASENA = "root";

    private static Connection conexion;

    private MySQL() {

    }

    public static Connection getConexion() {
        if (conexion != null) {
            return conexion;
        }
        try {
            conexion = DriverManager.getConnection("jdbc:mysql://" + HOST + "/" + ESQUEMA, USUARIO, CONTRASENA);
            System.out.println("Conexión a MySQL realizada correctamente.");
        } catch (SQLException ex) {
            System.out.println("ERROR MySQL: " + ex);
        }
        return conexion;
    }

    public static void cerrarConexion() {
        try {
            if (conexion != null) {
                conexion.close();
                conexion = null;
                System.out.println("Conexión a MySQL cerrada correctamente.");
            }
        } catch (SQLException ex) {
            System.out.println("ERROR MySQL: "+ ex);
        }
    }

}

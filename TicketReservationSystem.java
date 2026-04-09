package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TicketReservationSystem {

	private static final String URL = "jdbc:mysql://localhost:3306/TicketReservation";
	private static final String USER = "root";
	private static final String PASSWORD = "";

	private static Connection connection;

	public static void main(String[] args) {
		try {
			//Class.forName("com.mysql.cj.jdbc.Driver");
			//connection = DriverManager.getConnection(URL, USER, PASSWORD);
			System.out.println("Connected to the database.");

			// Initialize tables and GUI
			//DatabaseMethods.initializeTables(connection);
			GuiMethods.GuiInitialize(connection);

			System.out.println("Next customer id: " + DatabaseMethods.getNumRows(connection, "customer"));
			System.out.println("Next event id: " + DatabaseMethods.getNumRows(connection, "event"));
			System.out.println("Next ticket id: " + DatabaseMethods.getNumRows(connection, "ticket"));
			System.out.println("Next reservation id: " + DatabaseMethods.getNumRows(connection, "reservation"));

			// Add a shutdown hook to close the connection
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				try {
					if (connection != null && !connection.isClosed()) {
						connection.close();
						System.out.println("Database connection closed.");
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}));

		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}

package db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

public class DatabaseMethods {
	public static void initializeTables(Connection connection) throws SQLException {
		String createCustomerTable = new String("CREATE TABLE IF NOT EXISTS Customer("
				+ "CustomerID INTEGER PRIMARY KEY,"
				+ "CardNumber BIGINT CHECK (CardNumber BETWEEN 1000000000000000 AND 9999999999999999)," + "CVV CHAR(3),"
				+ "ExpDate DATE," + "CardholderName VARCHAR(256)," + "Email VARCHAR(256) UNIQUE NOT NULL,"
				+ "FirstName VARCHAR(64) NOT NULL," + "LastName VARCHAR(64) NOT NULL" + ");");

		String createEventTable = new String("CREATE TABLE IF NOT EXISTS Event(" + "EventID INTEGER PRIMARY KEY,"
				+ "Name VARCHAR(256) NOT NULL," + "Description TEXT," + "Duration INTEGER NOT NULL,"
				+ "EventDate DATE NOT NULL," + "EventTime TIME NOT NULL,"
				+ "Capacity INTEGER NOT NULL CHECK (Capacity > 0),"
				+ "EventType VARCHAR(32) CHECK (EventType IN ('concert', 'conference', 'workshop', 'festival', 'stage play')), "
				+ "Refundable BOOLEAN NOT NULL, " + "Percentage INTEGER" + ");");

		String createReservationTable = new String("CREATE TABLE IF NOT EXISTS Reservation("
				+ "ReservationID INTEGER PRIMARY KEY,"
				+ "PaymentAmount DECIMAL(10,2) NOT NULL CHECK (PaymentAmount >= 0)," + "ReservationDate DATE NOT NULL,"
				+ "TicketCount INTEGER NOT NULL CHECK (TicketCount > 0)," + "CustomerID INTEGER," + "EventID INTEGER,"
				+ "FOREIGN KEY (CustomerID) REFERENCES Customer(CustomerID), "
				+ "FOREIGN KEY (EventID) REFERENCES Event(EventID));");

		String createTicketTable = new String("CREATE TABLE IF NOT EXISTS Ticket(" + "TicketID INTEGER PRIMARY KEY,"
				+ "Availability BOOLEAN NOT NULL,"
				+ "TicketType VARCHAR(32) NOT NULL CHECK (TicketType IN ('VIP', 'Reduced Fare', 'Standard')),"
				+ "Price DECIMAL(10,2) NOT NULL CHECK (Price > 0)," + "SeatRow CHAR(2) NOT NULL,"
				+ "SeatNumber INTEGER NOT NULL," + "EventID INTEGER," + "ReservationID INTEGER,"
				+ "FOREIGN KEY (EventID) REFERENCES Event(EventID) ON DELETE CASCADE,"
				+ "FOREIGN KEY (ReservationID) REFERENCES Reservation (ReservationID) ON DELETE SET NULL" + ");");

		try (Statement stmt = connection.createStatement()) {
			// Check and create Customer table
			if (!DatabaseMethods.isTableExists(connection, "Customer")) {
				stmt.execute(createCustomerTable);
				System.out.println("Customer table created successfully.");
			} else {
				System.out.println("Customer table already exists.");
			}

			// Check and create Event table
			if (!DatabaseMethods.isTableExists(connection, "Event")) {
				stmt.execute(createEventTable);
				System.out.println("Event table created successfully.");
			} else {
				System.out.println("Event table already exists.");
			}

			// Check and create Reservation table
			if (!DatabaseMethods.isTableExists(connection, "Reservation")) {
				stmt.execute(createReservationTable);
				System.out.println("Reservation table created successfully.");
			} else {
				System.out.println("Reservation table already exists.");
			}

			// Check and create Ticket table
			if (!DatabaseMethods.isTableExists(connection, "Ticket")) {
				stmt.execute(createTicketTable);
				System.out.println("Ticket table created successfully.");
			} else {
				System.out.println("Ticket table already exists.");
			}

			System.out.println("Table initialization completed.");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void insertCustomer(Connection connection, String email, String firstName, String lastName)
			throws SQLException {
		String query = "INSERT INTO Customer(CustomerId, CardNumber, CVV, ExpDate, CardholderName, Email, FirstName, LastName) VALUES(?, NULL, NULL, NULL, NULL, ?, ?, ?)";
		try (PreparedStatement stmt = connection.prepareStatement(query)) {
			stmt.setInt(1, DatabaseMethods.getNumRows(connection, "customer"));
			stmt.setString(2, email);
			stmt.setString(3, firstName);
			stmt.setString(4, lastName);
			stmt.executeUpdate();
			System.out.println("Customer inserted successfully.");
		}
	}

	public static void insertEvent(Connection connection, int eventId, String name, String description, int duration,
			String eventDate, String eventTime, int capacity, String eventType, int vipTickets, int normalTickets,
			int discountedTickets, double vipTicketPrice, double normalTicketPrice, double discountedTicketPrice,
			boolean Refundable, int Percentage) throws SQLException {
		// Insert event into Event table
		String query = "INSERT INTO Event(EventID, Name, Description, Duration, EventDate, EventTime, Capacity, EventType, Refundable, Percentage) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement stmt = connection.prepareStatement(query)) {
			stmt.setInt(1, eventId);
			stmt.setString(2, name);
			stmt.setString(3, description);
			stmt.setInt(4, duration);
			stmt.setDate(5, Date.valueOf(eventDate));
			stmt.setTime(6, Time.valueOf(eventTime));
			stmt.setInt(7, capacity);
			stmt.setString(8, eventType);
			stmt.setBoolean(9, Refundable);
			stmt.setInt(10, Percentage);
			stmt.executeUpdate();
			System.out.println("Event inserted successfully.");

			// Create tickets for the event
			// Pass the number of tickets for each type (VIP, Normal, Discounted) for the
			// createTicketsForEvent method
			createTicketsForEvent(connection, eventId, vipTickets, normalTickets, discountedTickets, vipTicketPrice,
					normalTicketPrice, discountedTicketPrice);
		}
	}

	public static void createTicketsForEvent(Connection connection, int eventId, int vipTickets, int normalTickets,
			int discountedTickets, double invipTicketPrice, double innormalTicketPrice, double indiscountedTicketPrice)
			throws SQLException {
		// SQL query to insert tickets into the Ticket table
		String ticketQuery = "INSERT INTO Ticket(TicketID, Availability, TicketType, Price, SeatRow, SeatNumber, EventID) VALUES(?, ?, ?, ?, ?, ?, ?)";

		// Set prices for each ticket type
		double vipTicketPrice = invipTicketPrice; // Price for VIP tickets
		double normalTicketPrice = innormalTicketPrice; // Price for normal tickets
		double discountedTicketPrice = indiscountedTicketPrice; // Price for discounted tickets

		try (PreparedStatement ticketStmt = connection.prepareStatement(ticketQuery)) {
			int currentTicketCount = DatabaseMethods.getNumRows(connection, "Ticket");
			int nextTicketId = currentTicketCount + 1;

			// Create VIP tickets first
			for (int seatNumber = 0; seatNumber < vipTickets; seatNumber++) {
				String seatRow = getSeatRow(seatNumber);
				int seatNumberInRow = seatNumber % 12; // Reset seat number to 0 after every 12 seats

				ticketStmt.setInt(1, nextTicketId);
				ticketStmt.setBoolean(2, true); // Set availability to true
				ticketStmt.setString(3, "VIP"); // Set ticket type to VIP
				ticketStmt.setDouble(4, vipTicketPrice); // Set ticket price (VIP)
				ticketStmt.setString(5, seatRow); // Set seat row (dynamically calculated)
				ticketStmt.setInt(6, seatNumberInRow); // Set the seat number
				ticketStmt.setInt(7, eventId); // Set the event ID

				ticketStmt.executeUpdate();
				nextTicketId++; // Increment the TicketID for the next ticket
			}

			// Create normal tickets next
			for (int seatNumber = 0; seatNumber < normalTickets; seatNumber++) {
				String seatRow = getSeatRow(seatNumber + vipTickets); // Adjust seat number by VIP count
				int seatNumberInRow = (seatNumber + vipTickets) % 12; // Reset seat number to 0 after every 12 seats

				ticketStmt.setInt(1, nextTicketId);
				ticketStmt.setBoolean(2, true); // Set availability to true
				ticketStmt.setString(3, "Standard"); // Set ticket type to Normal
				ticketStmt.setDouble(4, normalTicketPrice); // Set ticket price (Normal)
				ticketStmt.setString(5, seatRow); // Set seat row (dynamically calculated)
				ticketStmt.setInt(6, seatNumberInRow); // Set seat number
				ticketStmt.setInt(7, eventId); // Set the event ID

				ticketStmt.executeUpdate();
				nextTicketId++; // Increment the TicketID for the next ticket
			}

			// Finally, create discounted tickets
			for (int seatNumber = 0; seatNumber < discountedTickets; seatNumber++) {
				String seatRow = getSeatRow(seatNumber + vipTickets + normalTickets); // Adjust seat number by VIP and
																						// normal count
				int seatNumberInRow = (seatNumber + vipTickets + normalTickets) % 12; // Reset seat number to 0 after
																						// every 12 seats

				ticketStmt.setInt(1, nextTicketId);
				ticketStmt.setBoolean(2, true); // Set availability to true
				ticketStmt.setString(3, "Reduced Fare"); // Set ticket type to Discounted
				ticketStmt.setDouble(4, discountedTicketPrice); // Set ticket price (Discounted)
				ticketStmt.setString(5, seatRow); // Set seat row
				ticketStmt.setInt(6, seatNumberInRow); // Set seat number
				ticketStmt.setInt(7, eventId); // Set the event ID

				ticketStmt.executeUpdate();
				nextTicketId++; // Increment the TicketID for the next ticket
			}

			System.out.println(vipTickets + " VIP tickets, " + normalTickets + " normal tickets, and "
					+ discountedTickets + " discounted tickets created for event with EventID: " + eventId);
		} catch (SQLException ex) {
			System.err.println("Error creating tickets: " + ex.getMessage());
			throw ex;
		}
	}

	private static String getSeatRow(int seatNumber) {
		// Calculate row letter (A, B, C, etc.)
		char row = (char) ('A' + (seatNumber / 12)); // Rows A, B, C, etc., with 12 seats per row
		return String.valueOf(row);
	}

	public static void searchAvailableSeats(Connection connection, int eventId) throws SQLException {
		String query = "SELECT TicketID, SeatRow, SeatNumber, Price, TicketType FROM Ticket WHERE EventID = ? AND Availability = TRUE";
		try (PreparedStatement stmt = connection.prepareStatement(query)) {
			stmt.setInt(1, eventId);
			try (ResultSet rs = stmt.executeQuery()) {
				System.out.println("Available seats:");
				while (rs.next()) {
					System.out.printf("TicketID: %d, SeatRow: %s, SeatNumber: %d, Price: %.2f, TicketType: %s%n",
							rs.getInt("TicketID"), rs.getString("SeatRow"), rs.getInt("SeatNumber"),
							rs.getDouble("Price"), rs.getString("TicketType"));
				}
			}
		}
	}

	public static void bookTickets(Connection connection, double paymentAmount, String reservationDate, int ticketCount,
			int customerId, int eventId, int[] ticketIds) throws SQLException {
		// SQL queries
		String insertReservation = "INSERT INTO Reservation(ReservationID, PaymentAmount, ReservationDate, TicketCount, CustomerID, EventID) VALUES(?, ?, ?, ?, ?, ?)";
		String updateTickets = "UPDATE Ticket SET Availability = FALSE, ReservationID = ? WHERE TicketID = ?";

		int reservationId;

		try (PreparedStatement reservationStmt = connection.prepareStatement(insertReservation);
				PreparedStatement ticketStmt = connection.prepareStatement(updateTickets)) {

			// Get the current row count in the Reservation table and calculate the next
			// ReservationID
			reservationId = getNumRows(connection, "Reservation");
			System.out.println(reservationId);
			// Insert the reservation
			reservationStmt.setInt(1, reservationId); // Set the ReservationID
			reservationStmt.setDouble(2, paymentAmount);
			reservationStmt.setDate(3, Date.valueOf(reservationDate));
			reservationStmt.setInt(4, ticketCount);
			reservationStmt.setInt(5, customerId);
			reservationStmt.setInt(6, eventId);
			reservationStmt.executeUpdate();
			System.out.println("Reservation created successfully with ID: " + reservationId);

			// Update the tickets with the new ReservationID
			for (int ticketId : ticketIds) {
				System.out.println(
						"Setting reservationId = " + reservationId + " for ticket with ticketId = " + ticketId);
				ticketStmt.setInt(1, reservationId);
				ticketStmt.setInt(2, ticketId);
				ticketStmt.executeUpdate();
			}
			System.out.println("Tickets booked successfully.");
		} catch (SQLException e) {
			// Handle any SQL exception here
			System.err.println("Error booking tickets: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static ResultSet getAllReservations(Connection connection, String email) throws SQLException {
		String query = "SELECT ReservationID, CONCAT(c.FirstName, ' ', c.LastName) AS CustomerName, e.Name AS EventName, r.ReservationDate, r.TicketCount, r.PaymentAmount "
				+ "FROM Reservation r " + "JOIN Customer c ON r.CustomerID = c.CustomerID "
				+ "JOIN Event e ON r.EventID = e.EventID " + "WHERE c.Email = ?";

		PreparedStatement stmt = connection.prepareStatement(query);
		stmt.setString(1, email); // Use email parameter for filtering
		return stmt.executeQuery();
	}

	public static void cancelReservation(Connection connection, int reservationId) throws SQLException {
		String deleteReservation = "DELETE FROM Reservation WHERE ReservationID = ?";
		String updateTickets = "UPDATE Ticket SET Availability = TRUE, ReservationID = NULL WHERE ReservationID = ?";

		try (PreparedStatement deleteStmt = connection.prepareStatement(deleteReservation);
				PreparedStatement updateStmt = connection.prepareStatement(updateTickets)) {

			updateStmt.setInt(1, reservationId);
			updateStmt.executeUpdate();

			deleteStmt.setInt(1, reservationId);
			deleteStmt.executeUpdate();

			System.out.println("Reservation canceled successfully.");
		}
	}

	public static ResultSet getAllEvents(Connection connection) throws SQLException {
		String query = "SELECT EventID, Name, Description, Duration, EventDate, EventTime, Capacity, EventType FROM Event";
		PreparedStatement stmt = connection.prepareStatement(query);
		return stmt.executeQuery();
	}

	public static ResultSet getRefundInformation(Connection connection, int eventId) throws SQLException {
		String query = "SELECT r.CustomerID AS UserID, c.Email AS Email, r.PaymentAmount AS RefundAmount "
				+ "FROM Reservation r " + "JOIN Customer c ON r.CustomerID = c.CustomerID " + "WHERE r.EventID = ?";

		PreparedStatement stmt = connection.prepareStatement(query);
		stmt.setInt(1, eventId);
		return stmt.executeQuery();
	}

	public static void cancelEvent(Connection connection, int eventId) throws SQLException {
		String deleteReservations = "DELETE FROM Reservation WHERE EventID = ?";
		String deleteEvent = "DELETE FROM Event WHERE EventID = ?";

		try (PreparedStatement deleteReservationsStmt = connection.prepareStatement(deleteReservations);
				PreparedStatement deleteEventStmt = connection.prepareStatement(deleteEvent)) {

			deleteReservationsStmt.setInt(1, eventId);
			deleteReservationsStmt.executeUpdate();

			deleteEventStmt.setInt(1, eventId);
			deleteEventStmt.executeUpdate();

			System.out.println("Event canceled successfully.");
		}
	}

	public static int getNumRows(Connection connection, String table) throws SQLException {
		int nextId = -1;
		try {
			String countQuery = new String("SELECT COUNT(*) AS total FROM `" + table + "`");
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(countQuery);

			if (resultSet.next()) {
				int rowCount = resultSet.getInt("total");
				nextId = rowCount; // Calculate the next ID
			}
			resultSet.close();
			statement.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return nextId;
	}

	public static boolean isCustomerExists(Connection connection, String username) throws SQLException {
		String query = "SELECT COUNT(*) FROM Customer WHERE Email = ?";
		try (PreparedStatement stmt = connection.prepareStatement(query)) {
			stmt.setString(1, username);
			ResultSet rs = stmt.executeQuery();
			rs.next();
			return rs.getInt(1) > 0;
		}
	}

	public static String[] getEventNames(Connection connection, String sortBy, String sortOrder) throws SQLException {
		String query = "SELECT Name FROM Event";

		// Correctly handle sorting order
		String order = "ASC"; // Default is Ascending
		if ("Descending".equalsIgnoreCase(sortOrder)) {
			order = "DESC"; // If "Descending" is selected, change to DESC
		}

		// Sorting by the given criteria (sortBy and sortOrder)
		if ("Name".equals(sortBy)) {
			query += " ORDER BY Name " + order;
		} else if ("Availability".equals(sortBy)) {
			query += " ORDER BY (Capacity - (SELECT COUNT(*) FROM Ticket WHERE EventID = Event.EventID AND Availability = FALSE)) "
					+ order;
		} else if ("Date".equals(sortBy)) {
			query += " ORDER BY EventDate " + order;
		}

		try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
			List<String> names = new ArrayList<>();
			while (rs.next()) {
				names.add(rs.getString("Name"));
			}
			return names.toArray(new String[0]);
		}
	}

	public static Object[] getEventDetails(Connection connection, String eventName) throws SQLException {
		String query = "SELECT EventID, Name, Description, EventDate, EventTime FROM Event WHERE Name = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, eventName);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					return new Object[] { rs.getInt("EventID"), rs.getString("Name"), rs.getString("Description"),
							rs.getDate("EventDate"), rs.getTime("EventTime") };
				} else {
					throw new SQLException("Event not found.");
				}
			}
		}
	}

	public static String getEventDate(Connection connection, String eventName) throws SQLException {
		String eventDate = null;
		String query = "SELECT EventDate FROM event WHERE Name = ?"; // SQL query

		try (PreparedStatement statement = connection.prepareStatement(query)) {
			statement.setString(1, eventName); // Set the event name in the query

			ResultSet rs = statement.executeQuery(); // Execute query

			if (rs.next()) {
				eventDate = rs.getString("EventDate"); // Retrieve event date from the result set
			}
		}

		return eventDate; // Return the event date
	}

	public static int getAvailableTickets(Connection connection, int eventId) throws SQLException {
		String query = "SELECT COUNT(*) FROM Ticket WHERE EventID = ? AND Availability = 1";
		try (PreparedStatement pst = connection.prepareStatement(query)) {
			pst.setInt(1, eventId);
			ResultSet rs = pst.executeQuery();
			if (rs.next()) {
				return rs.getInt(1); // Return the count of available tickets
			}
		}
		return 0;
	}

	public static ResultSet getTicketsForEvent(Connection connection, int eventId) throws SQLException {
		String query = "SELECT TicketID, SeatRow, SeatNumber, Availability, TicketType FROM Ticket WHERE EventID = ?";
		PreparedStatement stmt = connection.prepareStatement(query);
		stmt.setInt(1, eventId);

		// Execute the query and return the result
		return stmt.executeQuery();
	}

	public static ResultSet getTicketPrice(Connection connection, int ticketId) throws SQLException {
		String query = "SELECT Price FROM Ticket WHERE TicketId = ?";
		PreparedStatement stmt = connection.prepareStatement(query);
		stmt.setInt(1, ticketId);

		// Execute the query and return the result
		return stmt.executeQuery();
	}

	public static ResultSet getTicketId(Connection connection, char ticketRow, int ticketColumn) throws SQLException {
		String query = "SELECT TicketID FROM Ticket WHERE SeatRow = ? AND SeatNumber = ?";
		PreparedStatement stmt = connection.prepareStatement(query);
		stmt.setInt(1, ticketRow);
		stmt.setInt(2, ticketColumn);
		// Execute the query and return the result
		return stmt.executeQuery();
	}

	public static int getCustomerIdByEmail(Connection connection, String email) throws SQLException {
		String query = "SELECT CustomerID FROM Customer WHERE Email = ?";
		try (PreparedStatement pst = connection.prepareStatement(query)) {
			pst.setString(1, email);
			ResultSet rs = pst.executeQuery();
			if (rs.next()) {
				return rs.getInt("CustomerID");
			} else {
				throw new SQLException("Customer not found with email: " + email);
			}
		}
	}

	public static String getCustomerCreditCardInfo(Connection connection, int customerId) throws SQLException {
		String query = "SELECT CardNumber, CVV, ExpDate, CardholderName FROM Customer WHERE CustomerID = ?";
		try (PreparedStatement stmt = connection.prepareStatement(query)) {
			stmt.setInt(1, customerId);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				String cardNumber = rs.getString("CardNumber");
				String CVV = rs.getString("CVV");
				String expDate = rs.getString("ExpDate");
				String cardholderName = rs.getString("CardholderName");
				if (cardNumber == null || expDate == null || cardholderName == null || CVV == null) {
					return null;
				}
				String[] parts = expDate.split("-");
				String year = parts[0].substring(2); // Extract last two digits of the year
				String month = parts[1]; // Extract the month
				expDate = month + "/" + year;

				return "Cardholder: " + cardholderName + "\nCard Number: " + cardNumber + "\nExpiry Date: " + expDate
						+ "\nCVV: " + CVV;
			} else {
				return null;
			}
		}
	}

	public static void updateCustomerCreditCardInfo(Connection connection, int customerId, String cardNumber,
			String CVV, String expDate, String cardholderName) throws SQLException {
		Date expDt;
		String query = "UPDATE Customer SET CardNumber = ?, CVV = ?, ExpDate = ?, CardholderName = ? WHERE CustomerID = ?";
		try (PreparedStatement stmt = connection.prepareStatement(query)) {
			String[] parts = expDate.split("/");
			String month = parts[0];
			String year = "20" + parts[1];
			expDate = year + "-" + month + "-01";
			expDt = Date.valueOf(expDate);
			stmt.setString(1, cardNumber);
			stmt.setString(2, CVV);
			stmt.setDate(3, expDt);
			stmt.setString(4, cardholderName);
			stmt.setInt(5, customerId);
			stmt.executeUpdate();
		}
	}

	public static ResultSet getEventSeatSummary(Connection connection) throws SQLException {
		String query = new String("SELECT e.EventID, e.Name,"
				+ "SUM(CASE WHEN t.Availability = TRUE THEN 1 ELSE 0 END) AS AvailableSeats, "
				+ "SUM(CASE WHEN t.Availability = FALSE THEN 1 ELSE 0 END) AS ReservedSeats "
				+ "FROM Event e JOIN Ticket t ON e.EventID = t.EventID " + "GROUP BY e.EventID, e.Name;");

		PreparedStatement stmt = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE,
				ResultSet.CONCUR_READ_ONLY);
		return stmt.executeQuery();
	}

	public static ResultSet getEventEarnings(Connection connection) throws SQLException {
		String query = new String("SELECT e.EventID, e.Name, " + "SUM(t.Price) AS Revenue "
				+ "FROM Event e JOIN Ticket t ON e.EventID = t.EventID " + "WHERE t.Availability = FALSE "
				+ "GROUP BY e.EventID, e.Name;");

		PreparedStatement stmt = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE,
				ResultSet.CONCUR_READ_ONLY);
		return stmt.executeQuery();
	}

	public static boolean isTableExists(Connection connection, String tableName) {
		boolean exists = false;

		try {
			// Get database metadata
			DatabaseMetaData metaData = connection.getMetaData();

			// Retrieve table metadata
			ResultSet tables = metaData.getTables(null, null, tableName, new String[] { "TABLE" });

			// Check if the table exists
			exists = tables.next();

			// Clean up resources
			tables.close();
		} catch (SQLException e) {
			System.err.println("Error checking if table exists: " + e.getMessage());
			e.printStackTrace();
		}

		return exists;
	}

	public static ResultSet getMostPopularEvent(Connection connection) throws SQLException {
		String query = new String("SELECT e.EventID, e.Name, " + "COUNT(r.ReservationID) AS TotalReservations "
				+ "FROM Event e JOIN Reservation r ON e.EventID = r.EventID " + "GROUP BY e.EventID, e.Name "
				+ "ORDER BY TotalReservations DESC " + "LIMIT 1;");

		PreparedStatement stmt = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE,
				ResultSet.CONCUR_READ_ONLY);
		return stmt.executeQuery();
	}

	public static ResultSet getEventWithMostRevenue(Connection connection, String start, String end)
			throws SQLException {
		String query = "SELECT e.EventID, e.Name, " + "SUM(t.Price) AS Revenue "
				+ "FROM Event e JOIN Ticket t ON e.EventID = t.EventID "
				+ "WHERE t.Availability = FALSE AND e.EventDate BETWEEN ? AND ? " + "GROUP BY e.EventID, e.Name "
				+ "ORDER BY Revenue DESC " + "LIMIT 1;";

		PreparedStatement stmt = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE,
				ResultSet.CONCUR_READ_ONLY);
		stmt.setDate(1, Date.valueOf(start));
		stmt.setDate(2, Date.valueOf(end));

		return stmt.executeQuery(); // Return the ResultSet directly
	}

	public static ResultSet getReservationsByTimePeriod(Connection connection, String start, String end)
			throws SQLException {
		String query = new String("SELECT "
				+ "r.ReservationID, r.ReservationDate, e.EventID, e.Name, c.CustomerID, CONCAT(c.FirstName, ' ', c.LastName) AS CustomerName, r.TicketCount, r.PaymentAmount "
				+ "FROM Reservation r JOIN Event e ON r.EventID = e.EventID "
				+ "JOIN Customer c ON r.CustomerID = c.CustomerID " + "WHERE r.ReservationDate BETWEEN ? AND ? "
				+ "ORDER BY r.ReservationDate;");

		PreparedStatement stmt = null;
		ResultSet rs = null;

		try {
			stmt = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			stmt.setDate(1, Date.valueOf(start));
			stmt.setDate(2, Date.valueOf(end));

			rs = stmt.executeQuery(); // Execute query and get the ResultSet

			return rs;
		} catch (SQLException ex) {
			throw new SQLException("Error executing query: " + ex.getMessage(), ex);
		}
	}

	public static ResultSet getTotalRevenueFromTicketsByEvent(Connection connection, String ticketType, int eventId)
			throws SQLException {
		// SQL Query for filtering by event
		String query = "SELECT e.EventID, e.Name, " + "SUM(t.Price) AS Revenue "
				+ "FROM Event e JOIN Ticket t ON e.EventID = t.EventID " + "WHERE t.Availability = FALSE "
				+ "AND t.TicketType IN (?, ?) " + "AND e.EventID = ? " // Filter by event name
				+ "GROUP BY e.EventID, e.Name";

		PreparedStatement stmt = connection.prepareStatement(query);
		stmt.setString(1, ticketType); // Set ticket type (VIP or Standard)
		stmt.setString(2, ticketType); // Set ticket type (VIP or Standard)
		stmt.setInt(3, eventId); // Set event name filter

		return stmt.executeQuery(); // Return the ResultSet
	}

	public static ResultSet getTotalRevenueFromTickets(Connection connection, String ticketType) throws SQLException {
		// SQL Query for all events
		String query = "SELECT e.EventID, e.Name, " + "SUM(t.Price) AS Revenue "
				+ "FROM Event e JOIN Ticket t ON e.EventID = t.EventID " + "WHERE t.Availability = FALSE "
				+ "AND t.TicketType IN (?, ?) " // Filter by ticket type (VIP or Standard)
				+ "GROUP BY e.EventID, e.Name";

		PreparedStatement stmt = connection.prepareStatement(query);
		stmt.setString(1, ticketType); // Set ticket type (VIP or Standard)
		stmt.setString(2, ticketType); // Set ticket type (VIP or Standard)

		return stmt.executeQuery(); // Return the ResultSet
	}

	// checks if the reservation is refundable. returns -1 if it is, or returns the
	// percentage that will be returned
	public static int checkIfRefundable(Connection connection, int EventID) throws SQLException {
		// SQL Query for all events
		String query = new String("SELECT Refundable, Percentage FROM Event WHERE EventID = ?");
		int Refundable = 1;
		int Percentage = 0;
		try (PreparedStatement stmt = connection.prepareStatement(query)) {
			stmt.setInt(1, EventID);

			ResultSet rs = stmt.executeQuery(); // Execute query

			if (rs.next()) {
				Refundable = rs.getInt("Refundable"); // Retrieve event date from the result set
				Percentage = rs.getInt("Percentage"); // Retrieve event date from the result set
			}
		}
		if (Refundable == 1) {
			return -1;
		} else {
			return Percentage;
		}
	}

	public static int getEventFromRes(Connection connection, int ReservationID) throws SQLException {
		// SQL Query for all events
		String query = new String("SELECT EventID FROM Reservation WHERE ReservationID = ?");
		int eventID = -1;
		try (PreparedStatement stmt = connection.prepareStatement(query)) {
			stmt.setInt(1, ReservationID);

			ResultSet rs = stmt.executeQuery(); // Execute query

			if (rs.next()) {
				eventID = rs.getInt("EventID"); // Retrieve event date from the result set
			}
		}

		return eventID; // Return the event date

	}
}

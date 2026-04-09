package db;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;

public class GuiMethods {
	static Connection connection;

	public static void GuiInitialize(Connection dbConnection) {
		connection = dbConnection;
		try {
			CreateLoginForm form = new CreateLoginForm();
			form.setSize(500, 200);
			form.setLocationRelativeTo(null);
			form.setVisible(true);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, e.getMessage());
		}
	}
}

class CreateLoginForm extends JFrame implements ActionListener {
	private static final long serialVersionUID = 1L;

	JButton loginButton;
	JPanel panel;
	JLabel userLabel;
	JTextField emailField;

	CreateLoginForm() {
		userLabel = new JLabel("Enter your email to log in, or enter 'admin' to login as admin:");

		emailField = new JTextField(15);
		emailField.setPreferredSize(new Dimension(200, 30));

		loginButton = new JButton("Login");
		loginButton.setPreferredSize(new Dimension(100, 40));

		panel = new JPanel(new GridLayout(3, 1));
		panel.add(userLabel);
		panel.add(emailField);
		panel.add(loginButton);

		add(panel, BorderLayout.CENTER);

		loginButton.addActionListener(this);
		setTitle("Login Form");
		setLocationRelativeTo(null); // Center the window
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Terminate the program when this window is closed
	}

	public void actionPerformed(ActionEvent ae) {
		String username = emailField.getText();

		try {
			if (username.equals("admin")) {
				JOptionPane.showMessageDialog(this, "Welcome, Admin!");
				AdminPage adminPage = new AdminPage(GuiMethods.connection);
				adminPage.setVisible(true);
				dispose();
			} else {
				if (DatabaseMethods.isCustomerExists(GuiMethods.connection, username)) {
					// User exists, show event list
					JOptionPane.showMessageDialog(this, "Welcome back, " + username + "!");
					EventListPage eventListPage = new EventListPage(GuiMethods.connection, username);

					eventListPage.setVisible(true);
					dispose();
				} else {
					// User doesn't exist, show registration form
					JOptionPane.showMessageDialog(this, "User not found. Please register.");
					CustomerForm customerForm = new CustomerForm(GuiMethods.connection, username);
					customerForm.setVisible(true);
					dispose();
				}
			}
		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
		}
	}
}

class CustomerForm extends JFrame implements ActionListener {
	private static final long serialVersionUID = 1L;

	JTextField emailField, firstNameField, lastNameField;
	JButton submitButton;
	Connection connection;
	String username;

	CustomerForm(Connection dbConnection, String username) {
		connection = dbConnection;
		this.username = username;

		setTitle("Customer Registration");
		setSize(400, 300);
		setLayout(new GridLayout(4, 2));
		setLocationRelativeTo(null);

		emailField = new JTextField(15);
		firstNameField = new JTextField(15);
		lastNameField = new JTextField(15);

		submitButton = new JButton("Submit");
		submitButton.addActionListener(this);

		add(new JLabel("First Name:"));
		add(firstNameField);
		add(new JLabel("Last Name:"));
		add(lastNameField);
		add(new JLabel("Email:"));
		add(emailField);
		add(submitButton);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Terminate the program when this window is closed
	}

	public void actionPerformed(ActionEvent ae) {
		try {
			DatabaseMethods.insertCustomer(connection, emailField.getText(), firstNameField.getText(),
					lastNameField.getText());
			JOptionPane.showMessageDialog(this, "Customer Registered Successfully!");
			EventListPage eventListPage = new EventListPage(connection, emailField.getText()); // Pass email here
			eventListPage.setVisible(true);
			dispose();
		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
		}
	}
}

class EventListPage extends JFrame {
	private static final long serialVersionUID = 1L;
	private Connection connection;
	private String username;
	private JPanel eventsPanel;
	private JLabel currentDateTimeLabel;
	private JComboBox<String> sortByComboBox;
	private JComboBox<String> sortOrderComboBox;

	EventListPage(Connection dbConnection, String username) {
		this.connection = dbConnection;
		this.setUsername(username);

		setTitle("Available Events");
		setSize(1500, 700);
		setLocationRelativeTo(null);
		setLayout(new BorderLayout());
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Add a panel at the top for the username, logout button, and current date/time
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BorderLayout());

		JLabel usernameLabel = new JLabel("Logged in as: " + username, JLabel.LEFT);
		usernameLabel.setFont(new Font("Arial", Font.BOLD, 14));

		JButton logoutButton = new JButton("Logout");
		logoutButton.setFont(new Font("Arial", Font.BOLD, 14));
		logoutButton.addActionListener(e -> {
			dispose();
			GuiMethods.GuiInitialize(connection);
		});

		// Create a label for the current date and time
		currentDateTimeLabel = new JLabel();
		currentDateTimeLabel.setFont(new Font("Arial", Font.BOLD, 16));
		currentDateTimeLabel.setHorizontalAlignment(JLabel.RIGHT);
		currentDateTimeLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

		// Sorting combo boxes
		sortByComboBox = new JComboBox<>(new String[] { "Name", "Availability", "Date" });
		sortOrderComboBox = new JComboBox<>(new String[] { "Ascending", "Descending" });

		// Action listeners to handle sorting
		sortByComboBox.addActionListener(e -> refreshEventList());
		sortOrderComboBox.addActionListener(e -> refreshEventList());

		// Add the components to the topPanel
		JPanel sortPanel = new JPanel();
		sortPanel.add(new JLabel("Sort by: "));
		sortPanel.add(sortByComboBox);
		sortPanel.add(new JLabel("Order: "));
		sortPanel.add(sortOrderComboBox);

		// Add "Show Most Popular Event" button
		JButton popularEventButton = new JButton("Show Most Popular Event");
		popularEventButton.setFont(new Font("Arial", Font.BOLD, 12));
		popularEventButton.addActionListener(e -> showMostPopularEvent());
		sortPanel.add(popularEventButton);

		JButton cancelReservationButton = new JButton("Cancel Reservations");
		cancelReservationButton.setFont(new Font("Arial", Font.BOLD, 12));
		cancelReservationButton.addActionListener(e -> cancelReservation());
		sortPanel.add(cancelReservationButton);

		topPanel.add(usernameLabel, BorderLayout.WEST);
		topPanel.add(logoutButton, BorderLayout.EAST);
		topPanel.add(currentDateTimeLabel, BorderLayout.CENTER);
		topPanel.add(sortPanel, BorderLayout.SOUTH);

		add(topPanel, BorderLayout.NORTH);

		// Create the panel for event buttons using GridBagLayout
		eventsPanel = new JPanel();
		eventsPanel.setLayout(new GridBagLayout());
		eventsPanel.setBackground(Color.WHITE);
		JScrollPane scrollPane = new JScrollPane(eventsPanel);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		add(scrollPane, BorderLayout.CENTER);

		// Start a timer to update the current date and time every second
		Timer timer = new Timer(1000, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateCurrentDateTime();
			}
		});
		timer.start();

		// Initially fetch and display events without sorting
		refreshEventList();
	}

	// Method to update the current date and time
	private void updateCurrentDateTime() {
		// Get the current date and time
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy | HH:mm:ss");
		String currentDateTime = dateFormat.format(Calendar.getInstance().getTime());

		// Update the label with the current date and time
		currentDateTimeLabel.setText(currentDateTime);
	}

	// Method to refresh the event list after sorting
	private void refreshEventList() {
		// Get selected sorting option
		String sortBy = (String) sortByComboBox.getSelectedItem();
		String sortOrder = (String) sortOrderComboBox.getSelectedItem();

		try {
			String[] events = DatabaseMethods.getEventNames(connection, sortBy, sortOrder);
			eventsPanel.removeAll();

			GridBagConstraints gbc = new GridBagConstraints();
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.insets = new Insets(5, 5, 5, 5);
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.anchor = GridBagConstraints.NORTHWEST;

			int col = 0;
			int row = 0;

			for (String event : events) {
				String eventDate = DatabaseMethods.getEventDate(connection, event);
				RoundedButton eventButton = new RoundedButton(event, eventDate);
				eventButton.setFont(new Font("Arial", Font.BOLD, 16));
				eventButton.setForeground(Color.WHITE);
				eventButton.setPreferredSize(new Dimension(200, 100));
				eventButton.setToolTipText("Click to view details for " + event);

				eventButton.addMouseListener(new java.awt.event.MouseAdapter() {
					@Override
					public void mouseEntered(java.awt.event.MouseEvent evt) {
						eventButton.setBackground(new Color(0, 105, 217));
					}

					@Override
					public void mouseExited(java.awt.event.MouseEvent evt) {
						eventButton.setBackground(new Color(0, 123, 255));
					}
				});

				eventButton.addActionListener(e -> showEventDetails(event));

				// Add the button to the grid
				gbc.gridx = col;
				gbc.gridy = row;

				eventsPanel.add(eventButton, gbc);

				col++;
				if (col >= 4) { // After every 4 buttons, move to the next row
					col = 0;
					row++;
				}
			}

			if (events.length == 0) {
				eventsPanel.add(new JLabel("There are no events scheduled at the moment.", JLabel.CENTER));
			}

			eventsPanel.revalidate();
			eventsPanel.repaint();

		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(this, "Error fetching events: " + ex.getMessage());
		}
	}

	private void showEventDetails(String eventName) {
		try {
			Object[] eventDetails = DatabaseMethods.getEventDetails(connection, eventName);
			int availableTickets = DatabaseMethods.getAvailableTickets(connection, (int) eventDetails[0]); // EventID

			EventDetailsDialog detailsDialog = new EventDetailsDialog(this.connection, this, eventDetails,
					availableTickets, this.username);
			detailsDialog.setVisible(true);
		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(this, "Error fetching event details: " + ex.getMessage());
		}
	}

	private void showMostPopularEvent() {
		try {
			ResultSet rs = DatabaseMethods.getMostPopularEvent(connection);

			if (rs.next()) { // Check if there is a result
				String message = String.format("Most Popular Event:\n\nEvent ID: %d\nName: %s\nTotal Reservations: %d",
						rs.getInt("EventID"), // Event ID
						rs.getString("Name"), // Event name
						rs.getInt("TotalReservations") // Total reservations
				);
				JOptionPane.showMessageDialog(this, message, "Most Popular Event", JOptionPane.INFORMATION_MESSAGE);
			} else {
				JOptionPane.showMessageDialog(this, "No events found.", "Most Popular Event",
						JOptionPane.INFORMATION_MESSAGE);
			}

			rs.close(); // Close the ResultSet after processing
		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(this, "Error fetching most popular event: " + ex.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void cancelReservation() {
		// Create the panel to display the table with all reservations
		try {
			// Fetch all reservations
			ResultSet rs = DatabaseMethods.getAllReservations(connection, this.username);

			// Create a table model to display the reservation data
			DefaultTableModel model = new DefaultTableModel();
			DefaultTableModel ResIDS = new DefaultTableModel();
			model.addColumn("Customer Name");
			model.addColumn("Event Name");
			model.addColumn("Reservation Date");
			model.addColumn("Ticket Count");
			model.addColumn("Payment Amount");

			ResIDS.addColumn("ResID");

			// Fill the table with data from the ResultSet
			while (rs.next()) {
				model.addRow(new Object[] { rs.getString("CustomerName"), rs.getString("EventName"),
						rs.getDate("ReservationDate"), rs.getInt("TicketCount"), rs.getDouble("PaymentAmount") });
				ResIDS.addRow(new Object[] { rs.getInt("ReservationID") });
			}

			// Create a JTable to display the reservation data
			JTable reservationTable = new JTable(model);
			reservationTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION); // Allow multiple row
																								// selection

			JScrollPane scrollPane = new JScrollPane(reservationTable);

			// Show the table in a dialog
			int option = JOptionPane.showConfirmDialog(this, scrollPane, "Cancel Reservations",
					JOptionPane.OK_CANCEL_OPTION);

			if (option == JOptionPane.OK_OPTION) {
				// Get selected rows
				int[] selectedRows = reservationTable.getSelectedRows();

				if (selectedRows.length > 0) {
					// Confirm cancellation
					int confirm = JOptionPane.showConfirmDialog(this,
							"Are you sure you want to cancel the selected reservation(s)?", "Confirm Cancellation",
							JOptionPane.YES_NO_OPTION);

					if (confirm == JOptionPane.YES_OPTION) {
						// Loop through the selected rows and cancel each reservation
						for (int row : selectedRows) {
							int reservationId = (int) ResIDS.getValueAt(row, 0); // Get the Reservation ID
							int eventId = (int) DatabaseMethods.getEventFromRes(connection, reservationId);

							// Check if event is refundable
							try {
								// Fetch the refund percentage from the database
								double refundPercentage = DatabaseMethods.checkIfRefundable(connection, eventId);
								double payment = (double) model.getValueAt(row, 4);
								double refundAmount;

								// Calculate the refund amount
								if (refundPercentage == -1) {
									refundAmount = payment; // Full payment refund
								} else {
									refundAmount = payment * (refundPercentage / 100); // Partial refund
								}

								String message;
								if (refundPercentage == -1) {
									message = String.format("You are eligible for a full refund of $%.2f.",
											refundAmount);
								} else {
									message = String.format(
											"You will receive a refund of $%.2f (%.2f%% of the original payment).",
											refundAmount, refundPercentage);
								}

								// Show the refund information in a dialog box
								JOptionPane.showMessageDialog(null, message, "Refund Information",
										JOptionPane.INFORMATION_MESSAGE);

							} catch (SQLException ex) {
								JOptionPane.showMessageDialog(null,
										"Error fetching refund information: " + ex.getMessage(), "Error",
										JOptionPane.ERROR_MESSAGE);
							}

							// Cancel the reservation
							DatabaseMethods.cancelReservation(connection, reservationId);
						}

						// Show success message
						JOptionPane.showMessageDialog(this,
								"Selected reservations canceled successfully and the refund will be added to your account shortly.");
					}
				} else {
					JOptionPane.showMessageDialog(this, "No reservations selected.");
				}
			}
		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(this, "Error fetching reservations: " + ex.getMessage());
		}
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
}

class EventDetailsDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	JLabel descriptionLabel, dateLabel, timeLabel, ticketsLabel;
	JButton reserveButton;
	int eventID;
	Connection connection;
	int currentCustomerId;
	double totalPaymentAmount;
	ResultSet ticketret;
	double ticketPrice;

	EventDetailsDialog(Connection dbconnection, JFrame parent, Object[] eventDetails, int availableTickets,
			String username) throws SQLException {
		super(parent, "Event Details", true);
		this.currentCustomerId = DatabaseMethods.getCustomerIdByEmail(dbconnection, username);
		setSize(500, 400);
		this.connection = dbconnection;
		setLocationRelativeTo(parent);
		setLayout(new BorderLayout(10, 10));
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		// Panel to display the event description with scroll support
		JTextArea descriptionTextArea = new JTextArea("Description: " + eventDetails[2]);
		descriptionTextArea.setWrapStyleWord(true);
		descriptionTextArea.setLineWrap(true);
		descriptionTextArea.setEditable(false);
		descriptionTextArea.setOpaque(false);
		descriptionTextArea.setFont(new Font("Arial", Font.PLAIN, 14));

		JScrollPane descriptionScrollPane = new JScrollPane(descriptionTextArea);
		descriptionScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		descriptionScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		descriptionScrollPane.setBorder(BorderFactory.createTitledBorder("Event Description"));

		// Panel to display other event details
		JPanel detailsPanel = new JPanel();
		detailsPanel.setLayout(new GridLayout(4, 1, 5, 5));
		detailsPanel.setBorder(BorderFactory.createTitledBorder(""));
		detailsPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

		JLabel nameLabel = new JLabel("Name: " + eventDetails[1]);
		JLabel dateLabel = new JLabel("Date: " + eventDetails[3]);
		JLabel timeLabel = new JLabel("Time: " + eventDetails[4]);
		JLabel ticketsLabel = new JLabel("Available Tickets: " + availableTickets);
		this.eventID = (int) eventDetails[0];

		detailsPanel.add(nameLabel);
		detailsPanel.add(dateLabel);
		detailsPanel.add(timeLabel);
		detailsPanel.add(ticketsLabel);

		// Reserve button
		reserveButton = new JButton("Reserve Ticket");
		reserveButton.setFont(new Font("Arial", Font.BOLD, 14));
		reserveButton.addActionListener(e -> makeReservation());

		// Add components to the dialog
		add(descriptionScrollPane, BorderLayout.CENTER); // Center for the description
		add(detailsPanel, BorderLayout.NORTH);
		add(reserveButton, BorderLayout.SOUTH); // Bottom for the reserve button
	}

	private void makeReservation() {
		try {
			// Fetch all tickets for the event
			ResultSet rs = DatabaseMethods.getTicketsForEvent(connection, eventID);

			// Create the grid panel for seats
			JPanel gridPanel = new JPanel();
			gridPanel.setLayout(new GridLayout(0, 12, 5, 5)); // Grid for seats

			// List for selected seats
			List<SeatSelection> selectedSeats = new ArrayList<>();
			totalPaymentAmount = 0;

			// Create a legend for seat types
			JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			legendPanel.add(createLegendLabel("VIP", Color.GREEN));
			legendPanel.add(createLegendLabel("Normal", Color.BLUE));
			legendPanel.add(createLegendLabel("Discounted", Color.RED));

			JLabel totalAmountLabel = new JLabel("Total Amount: $0.00");
			totalAmountLabel.setFont(new Font("Arial", Font.BOLD, 14));
			totalAmountLabel.setHorizontalAlignment(JLabel.RIGHT);

			// Stage panel
			JPanel stagePanel = new JPanel(new GridLayout(1, 1));
			JLabel stageLabel = new JLabel("STAGE", JLabel.CENTER);
			stageLabel.setFont(new Font("Arial", Font.BOLD, 16));
			stageLabel.setOpaque(true);
			stageLabel.setBackground(Color.DARK_GRAY);
			stageLabel.setForeground(Color.WHITE);
			stagePanel.add(stageLabel);

			// Iterate through tickets and create seat buttons/labels
			while (rs.next()) {
				int ticketId = rs.getInt("TicketID");
				String seatRow = rs.getString("SeatRow");
				int seatNumber = rs.getInt("SeatNumber");
				boolean availability = rs.getBoolean("Availability");
				String ticketType = rs.getString("TicketType");
				String seatLabel = seatRow + seatNumber;

				Color ticketColor = getColorByTicketType(ticketType);

				if (availability) {
					JButton seatButton = new JButton(seatLabel);
					seatButton.setBackground(ticketColor);
					seatButton.setOpaque(true);
					seatButton.setPreferredSize(new Dimension(60, 60));

					seatButton.addActionListener(e -> {
						try {
							ResultSet ticketPriceResult = DatabaseMethods.getTicketPrice(connection, ticketId);
							if (ticketPriceResult.next()) {
								double ticketPrice = ticketPriceResult.getDouble("Price");

								if (selectedSeats.stream().anyMatch(seat -> seat.getTicketId() == ticketId)) {
									selectedSeats.removeIf(seat -> seat.getTicketId() == ticketId);
									seatButton.setBackground(ticketColor);
									totalPaymentAmount -= ticketPrice;
								} else {
									selectedSeats.add(new SeatSelection(ticketId, seatLabel));
									seatButton.setBackground(Color.ORANGE);
									totalPaymentAmount += ticketPrice;
								}
								totalAmountLabel.setText(String.format("Total Amount: $%.2f", totalPaymentAmount));
							}
						} catch (SQLException ex) {
							ex.printStackTrace();
						}
					});

					gridPanel.add(seatButton);
				} else {
					JLabel seatLabelComponent = new JLabel(seatLabel, JLabel.CENTER);
					seatLabelComponent.setBackground(Color.GRAY);
					seatLabelComponent.setOpaque(true);
					seatLabelComponent.setPreferredSize(new Dimension(60, 60));
					gridPanel.add(seatLabelComponent);
				}
			}

			// Wrap the grid panel in a scroll pane
			JScrollPane scrollPane = new JScrollPane(gridPanel);
			scrollPane.setPreferredSize(new Dimension(800, 600));
			scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

			JPanel stageAndSeatsPanel = new JPanel(new BorderLayout());
			stageAndSeatsPanel.add(stagePanel, BorderLayout.NORTH); // Stage at the top
			stageAndSeatsPanel.add(scrollPane, BorderLayout.CENTER); // Scrollable seat grid below

			// Display all components in a confirmation dialog
			int option = JOptionPane.showConfirmDialog(this,
					new Object[] { legendPanel, stageAndSeatsPanel, totalAmountLabel }, "Select Seats",
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

			if (option == JOptionPane.OK_OPTION) {
				if (selectedSeats.isEmpty()) {
					JOptionPane.showMessageDialog(this, "No seats selected.");
				} else {
					// Prepare the confirmation message showing selected tickets and total payment
					// amount
					StringBuilder confirmationMessage = new StringBuilder("You selected the following seats:\n");
					for (SeatSelection seat : selectedSeats) {
						confirmationMessage.append(seat.getSeatLabel()).append(", ");
					}
					confirmationMessage.append("\nTotal Payment: $").append(totalPaymentAmount);

					int confirm = JOptionPane.showConfirmDialog(this, confirmationMessage.toString(),
							"Confirm Reservation", JOptionPane.YES_NO_OPTION);

					if (confirm == JOptionPane.YES_OPTION) {

						// Call the handleCreditCardInformation method to check for the card and ask the
						// user if they want to use it
						boolean proceedWithReservation = handleCreditCardInformation();

						if (proceedWithReservation) {
							// Proceed with the reservation
							int ticketCount = selectedSeats.size();
							int[] ticketIDs = new int[ticketCount];
							int index = 0;
							for (SeatSelection seat : selectedSeats) {
								ticketIDs[index] = seat.getTicketId();
								index++;
							}

							String date = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());

							// Call method to book tickets
							DatabaseMethods.bookTickets(connection, totalPaymentAmount, date, ticketCount,
									currentCustomerId, eventID, ticketIDs);

							JOptionPane.showMessageDialog(this, "Reservation successful!");
							dispose(); // Close the EventDetailsDialog
						} else {
							JOptionPane.showMessageDialog(this, "Reservation cancelled.");
						}
					}
				}
			}
		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(this, "Error fetching tickets: " + ex.getMessage());
		}
	}

// Utility method to get color based on ticket type
	private Color getColorByTicketType(String ticketType) {
		switch (ticketType) {
		case "VIP":
			return Color.GREEN;
		case "Reduced Fare":
			return Color.RED;
		default:
			return Color.BLUE;
		}
	}

// Utility to create legend labels
	private JPanel createLegendLabel(String text, Color color) {
		JPanel legendItem = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel colorBox = new JLabel();
		colorBox.setPreferredSize(new Dimension(20, 20));
		colorBox.setOpaque(true);
		colorBox.setBackground(color);

		legendItem.add(new JLabel(text + ": "));
		legendItem.add(colorBox);
		return legendItem;
	}

	private boolean handleCreditCardInformation() throws SQLException {
		// Fetch the stored credit card information
		String creditCardInfo = DatabaseMethods.getCustomerCreditCardInfo(connection, currentCustomerId);

		// If the user has a credit card on file
		if (creditCardInfo != null) {
			// Split the details into parts: Cardholder name, card number, expiry date, CVV
			String[] cardDetails = creditCardInfo.split("\n");
			String cardholderName = cardDetails[0].replace("Cardholder: ", "");
			String cardNumber = cardDetails[1].replace("Card Number: ", "");
			String expDate = cardDetails[2].replace("Expiry Date: ", "");

			// Mask the card number except for the last 4 digits
			String maskedCardNumber = "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);

			// Create the message to show the user the credit card info
			String message = "Credit Card Information:\n\n" + "Cardholder: " + cardholderName + "\n" + "Card Number: "
					+ maskedCardNumber + "\n" + "Expiry Date: " + expDate + "\n" + "CVV: ***\n\n"
					+ "Would you like to use this card for the reservation?";

			// Ask the user if they want to use this card
			int cardConfirm = JOptionPane.showConfirmDialog(this, message, "Use Stored Credit Card",
					JOptionPane.YES_NO_OPTION);

			// If the user confirms, return true to proceed with the reservation
			if (cardConfirm == JOptionPane.YES_OPTION) {
				return true; // Proceed with the reservation
			} else {
				// If the user clicks "No", ask them to enter new card details
				return promptForNewCardDetails();
			}

		} else {
			// If no card is found, ask the user if they want to provide one
			JOptionPane.showMessageDialog(this, "No credit card on file. Please enter your new credit card details.");
			return promptForNewCardDetails();
		}
	}

	// This method will prompt the user to enter new credit card details
	private boolean promptForNewCardDetails() throws SQLException {
		while (true) { // Keep asking the user for card details until valid input or they cancel
			JTextField cardNumberField = new JTextField(16);
			JTextField expDateField = new JTextField(5);
			JTextField CVVField = new JTextField(3);
			JTextField cardholderNameField = new JTextField(20);

			JPanel cardInfoPanel = new JPanel(new GridLayout(4, 2, 10, 10));
			cardInfoPanel.add(new JLabel("Card Number:"));
			cardInfoPanel.add(cardNumberField);
			cardInfoPanel.add(new JLabel("Expiry Date (MM/YY):"));
			cardInfoPanel.add(expDateField);
			cardInfoPanel.add(new JLabel("CVV:"));
			cardInfoPanel.add(CVVField);
			cardInfoPanel.add(new JLabel("Cardholder Name:"));
			cardInfoPanel.add(cardholderNameField);

			int option2 = JOptionPane.showConfirmDialog(this, cardInfoPanel, "Enter Credit Card Info",
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

			if (option2 == JOptionPane.OK_OPTION) {
				// Validate and store the credit card info
				String cardNumber = cardNumberField.getText().trim();
				String expDate = expDateField.getText().trim();
				String cardholderName = cardholderNameField.getText().trim();
				String CVV = CVVField.getText().trim();

				// Validate the credit card details
				if (cardNumber.isEmpty() || expDate.isEmpty() || cardholderName.isEmpty() || CVV.isEmpty()) {
					JOptionPane.showMessageDialog(this, "Please enter all the required fields.");
					continue; // Restart the loop
				}

				// Validate card number length and format
				if (!cardNumber.matches("\\d{16}")) {
					JOptionPane.showMessageDialog(this,
							"Invalid card number. Please enter a valid 16-digit card number.");
					continue; // Restart the loop
				}

				// Validate expiry date (MM/YY format)
				if (!expDate.matches("(0[1-9]|1[0-2])/([0-9]{2})")) {
					JOptionPane.showMessageDialog(this, "Invalid expiry date format. Use MM/YY.");
					continue; // Restart the loop
				}

				// Validate CVV format
				if (!CVV.matches("\\d{3}")) {
					JOptionPane.showMessageDialog(this,
							"Invalid CVV. Please enter the 3-digit security code located on the back of your credit card.");
					continue; // Restart the loop
				}

				// Store the credit card info in the database
				try {
					DatabaseMethods.updateCustomerCreditCardInfo(connection, currentCustomerId, cardNumber, CVV,
							expDate, cardholderName);
				} catch (SQLException ex) {
					JOptionPane.showMessageDialog(this, "Error saving credit card info: " + ex.getMessage());
					return false; // Exit the loop in case of database error
				}

				String creditCardInfo = DatabaseMethods.getCustomerCreditCardInfo(connection, currentCustomerId);

				String[] cardDetails = creditCardInfo.split("\n");
				String cardholderNametmp = cardDetails[0].replace("Cardholder: ", "");
				String cardNumbertmp = cardDetails[1].replace("Card Number: ", "");
				String expDatetmp = cardDetails[2].replace("Expiry Date: ", "");

				// Mask the card number except for the last 4 digits
				String maskedCardNumber = "**** **** **** " + cardNumber.substring(cardNumbertmp.length() - 4);

				// Create the message to show the user the credit card info
				String message = "Credit Card Information:\n\n" + "Cardholder: " + cardholderNametmp + "\n"
						+ "Card Number: " + maskedCardNumber + "\n" + "Expiry Date: " + expDatetmp + "\n"
						+ "CVV: ***\n\n" + "Would you like to use this card for the reservation?";

				// Ask the user if they want to use this card
				int cardConfirm2 = JOptionPane.showConfirmDialog(this, message, "Use Stored Credit Card",
						JOptionPane.YES_NO_OPTION);

				if (cardConfirm2 == JOptionPane.YES_OPTION) {
					return true; // Proceed with the reservation
				}
			} else {
				// User canceled the input dialog
				JOptionPane.showMessageDialog(this, "Reservation canceled.");
				return false; // Exit the loop if the user cancels
			}
		}
	}
}

class AdminPage extends JFrame {
	private static final long serialVersionUID = 1L;
	private Connection connection;

	// Fields for the form to create an event
	JTextField nameField;
	JTextArea descriptionField;
	JTextField durationField;
	JTextField eventDateField;
	JTextField eventTimeField;
	JTextField capacityField;
	JComboBox<String> eventTypeComboBox;
	JTextField vipTicketsField;
	JTextField normalTicketsField;
	JTextField standardfareTicketsField;
	JTextField vipTicketPriceField;
	JTextField standardTicketPriceField;
	JTextField reducedfareTicketPriceField;

	// AdminPage Constructor
	AdminPage(Connection dbConnection) {
		connection = dbConnection;

		setTitle("Admin Dashboard");
		setSize(600, 600);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Main panel with BorderLayout
		setLayout(new BorderLayout(10, 10));

		// Welcome Label
		JLabel welcomeLabel = new JLabel("Welcome to the Admin Dashboard", JLabel.CENTER);
		welcomeLabel.setFont(new Font("Arial", Font.BOLD, 20));
		add(welcomeLabel, BorderLayout.NORTH);

		// Create a panel for 5 options
		JPanel optionsPanel = new JPanel();
		optionsPanel.setLayout(new GridLayout(5, 1, 10, 10));
		optionsPanel.setBackground(Color.WHITE);

		// Create buttons for each option
		JButton createEventButton = new JButton("Create Event");
		JButton cancelEventButton = new JButton("Cancel Event");
		JButton seeEarningsButton = new JButton("See Earnings from Events");
		JButton showReservationsButton = new JButton("Show Reservations");
		JButton mostRevenueEventButton = new JButton("Event with Most Revenue in Time Range");
		JButton viewReservationsButton = new JButton("Show Reservations by Time Period");
		JButton totalRevenueButton = new JButton("Show Total Revenue by Ticket Type");

		// Add action listeners for each button
		mostRevenueEventButton.addActionListener(e -> viewEventWithMostRevenue(connection));
		viewReservationsButton.addActionListener(e -> viewReservationsByTimePeriod(connection));
		totalRevenueButton.addActionListener(e -> viewTotalRevenueFromTickets(connection, null));
		createEventButton.addActionListener(e -> showCreateEventForm());
		cancelEventButton.addActionListener(e -> cancelEvent());
		seeEarningsButton.addActionListener(e -> seeEarnings());
		showReservationsButton.addActionListener(e -> showReservations());

		// Add the buttons to the options panel
		optionsPanel.add(createEventButton);
		optionsPanel.add(cancelEventButton);
		optionsPanel.add(seeEarningsButton);
		optionsPanel.add(showReservationsButton);
		optionsPanel.add(mostRevenueEventButton);
		optionsPanel.add(viewReservationsButton);
		optionsPanel.add(totalRevenueButton);

		// Add the options panel to the center of the window
		add(optionsPanel, BorderLayout.CENTER);

		// Button panel at the bottom with Logout button
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));

		JButton logoutButton = new JButton("Logout");
		logoutButton.setFont(new Font("Arial", Font.BOLD, 14));
		logoutButton.addActionListener(e -> {
			dispose();
			CreateLoginForm loginForm = new CreateLoginForm();
			loginForm.setSize(500, 200);
			loginForm.setLocationRelativeTo(null);
			loginForm.setVisible(true);
		});
		buttonPanel.add(logoutButton);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	// Show form to create event
	private void showCreateEventForm() {
		// Create and show the form to create an event
		EventCreationForm eventForm = new EventCreationForm(connection);
		eventForm.setVisible(true);
	}

	private void cancelEvent() {
		try {
			// Fetch all events from the database
			ResultSet rs = DatabaseMethods.getAllEvents(connection);

			// Create a table model to display event details
			String[] columnNames = { "Event ID", "Event Name", "Description", "Duration (mins)", "Event Date",
					"Event Time", "Capacity", "Event Type" };
			DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);

			// Fill the table model with data
			while (rs.next()) {
				Object[] row = { rs.getInt("EventID"), // EventID (integer)
						rs.getString("Name"), rs.getString("Description"), rs.getInt("Duration"), // Duration (integer)
						rs.getDate("EventDate"), rs.getTime("EventTime"), rs.getInt("Capacity"), // Capacity (integer)
						rs.getString("EventType") };
				tableModel.addRow(row);
			}

			// Create a JTable to display the events with selection mode set to multiple
			// rows
			JTable eventTable = new JTable(tableModel);
			eventTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

			// Make the table cells resize dynamically based on the window size
			eventTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

			// Create a JScrollPane to wrap the JTable and allow scrolling
			JScrollPane scrollPane = new JScrollPane(eventTable);

			// Set preferred size for the scrollPane to allow resizing
			scrollPane.setPreferredSize(new Dimension(600, 400));

			// Create a panel with BorderLayout for better resizing behavior
			JPanel panel = new JPanel(new BorderLayout());
			panel.add(scrollPane, BorderLayout.CENTER);

			// Show a dialog with the table of events
			int option = JOptionPane.showConfirmDialog(this, panel, "Select Events to Cancel",
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

			// If the user selects events and clicks OK
			if (option == JOptionPane.OK_OPTION) {
				int[] selectedRows = eventTable.getSelectedRows();
				if (selectedRows.length > 0) {
					// Before canceling the event, fetch refund information
					StringBuilder refundInfo = new StringBuilder("Refund Information:\n");

					for (int row : selectedRows) {
						int eventId = (int) tableModel.getValueAt(row, 0); // Get EventID (first column in table)

						// Fetch reservation information for the selected event
						ResultSet refundResult = DatabaseMethods.getRefundInformation(connection, eventId);
						while (refundResult.next()) {
							int userId = refundResult.getInt("UserID");
							String email = refundResult.getString("Email");
							double refundedAmount = refundResult.getDouble("RefundAmount");

							// Display user refund information
							refundInfo.append("User ID: ").append(userId).append(" | Email: ").append(email)
									.append(" | Refunded Amount: $").append(refundedAmount).append("\n");
						}
					}

					// Show the refund information to the admin
					int confirmRefund = JOptionPane.showConfirmDialog(this, refundInfo.toString(), "Confirm Refunds",
							JOptionPane.YES_NO_OPTION);

					// If the admin confirms the refund, proceed with event cancellation
					if (confirmRefund == JOptionPane.YES_OPTION) {
						// Proceed with the event cancellation
						for (int row : selectedRows) {
							int eventId = (int) tableModel.getValueAt(row, 0);
							// Call method to cancel the event and refund (as a separate operation)
							DatabaseMethods.cancelEvent(connection, eventId);
						}
						JOptionPane.showMessageDialog(this,
								"Selected events canceled and refunds processed successfully.");
					}
				} else {
					JOptionPane.showMessageDialog(this, "No events selected.");
				}
			}
		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(this, "Error fetching or canceling events: " + ex.getMessage());
		}
	}

	// Show earnings from events
	private void seeEarnings() {
		try {
			// Fetch event earnings by calling the method from DatabaseMethods
			ResultSet rs = DatabaseMethods.getEventEarnings(connection);

			// StringBuilder to hold the result
			StringBuilder earningsSummary = new StringBuilder("Event Earnings:\n");
			int eventId = -1;
			String eventName = "";
			double revenue = -1;

			// Iterate through the ResultSet and append the results to the StringBuilder
			while (rs.next()) {
				eventId = rs.getInt("EventID");
				eventName = rs.getString("Name");
				revenue = rs.getDouble("Revenue");

				// Format the earnings information
				earningsSummary
						.append(String.format("Event: %s (ID: %d) - Revenue: $%.2f\n", eventName, eventId, revenue));
			}

			if (eventName.equals("")) {
				JOptionPane.showMessageDialog(this, "There are no earnings from events.", "Event Earnings",
						JOptionPane.INFORMATION_MESSAGE);
			} else {
				// Show the earnings in a message dialog
				JOptionPane.showMessageDialog(this, earningsSummary.toString(), "Event Earnings",
						JOptionPane.INFORMATION_MESSAGE);
			}
		} catch (SQLException ex) {
			// Handle SQL exceptions
			JOptionPane.showMessageDialog(this, "Error fetching event earnings: " + ex.getMessage());
		}
	}

	// Show all reservations
	private void showReservations() {
		try {
			// Fetch seat summary data for all events
			ResultSet rs = DatabaseMethods.getEventSeatSummary(connection);

			// Define column names for the table
			String[] columnNames = { "Event ID", "Event Name", "Available Seats", "Reserved Seats" };

			// Create a table model to hold the data
			DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);

			// Populate the table model with data from the ResultSet
			while (rs.next()) {
				Object[] row = { rs.getInt("EventID"), rs.getString("Name"), rs.getInt("AvailableSeats"),
						rs.getInt("ReservedSeats") };
				tableModel.addRow(row);
			}

			// Create a JTable to display the data
			JTable reservationsTable = new JTable(tableModel);
			JScrollPane scrollPane = new JScrollPane(reservationsTable);

			// Allow resizing of the dialog for better visibility
			scrollPane.setPreferredSize(new Dimension(600, 400));

			// Show the table in a dialog
			JOptionPane.showMessageDialog(this, scrollPane, "Event Seat Summary", JOptionPane.INFORMATION_MESSAGE);

		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(this, "Error fetching reservations: " + ex.getMessage());
		}
	}

	public static void viewEventWithMostRevenue(Connection connection) {
		// Create a new dialog for displaying the event details
		JDialog dialog = new JDialog();
		dialog.setTitle("Event with Most Revenue");
		dialog.setSize(800, 400);
		dialog.setLayout(new BorderLayout(10, 10));

		// Create a panel for the input fields (start date, end date) and the search
		// button
		JPanel inputPanel = new JPanel();
		inputPanel.setLayout(new FlowLayout());

		// Start Date Field
		JLabel startDateLabel = new JLabel("Start Date (yyyy-mm-dd): ");
		JTextField startDateField = new JTextField(10);
		inputPanel.add(startDateLabel);
		inputPanel.add(startDateField);

		// End Date Field
		JLabel endDateLabel = new JLabel("End Date (yyyy-mm-dd): ");
		JTextField endDateField = new JTextField(10);
		inputPanel.add(endDateLabel);
		inputPanel.add(endDateField);

		// Search Button
		JButton searchButton = new JButton("Search");
		inputPanel.add(searchButton);

		// Panel for displaying the results
		JPanel resultPanel = new JPanel();
		resultPanel.setLayout(new BorderLayout());

		// Add the input panel at the top
		dialog.add(inputPanel, BorderLayout.NORTH);

		// Add the result panel at the center
		dialog.add(resultPanel, BorderLayout.CENTER);

		// Action listener for the Search button
		searchButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String startDate = startDateField.getText();
				String endDate = endDateField.getText();

				if (startDate.isEmpty() || endDate.isEmpty()) {
					JOptionPane.showMessageDialog(dialog, "Please enter both start and end dates.");
					return;
				}

				try {
					// Call the method to fetch the event with the most revenue for the given date
					// range
					ResultSet rs = DatabaseMethods.getEventWithMostRevenue(connection, startDate, endDate);

					// Clear the previous results in the result panel
					resultPanel.removeAll();

					// Create a text area to display the results
					JTextArea resultTextArea = new JTextArea();
					resultTextArea.setEditable(false); // Make it read-only
					resultTextArea.setLineWrap(true);
					resultTextArea.setWrapStyleWord(true);

					// Create a string builder to build the result text
					StringBuilder resultText = new StringBuilder();

					if (rs.next()) {
						String eventName = rs.getString("Name");
						double revenue = rs.getDouble("Revenue");
						resultText.append("Event: ").append(eventName).append("\n");
						resultText.append("Revenue: $").append(String.format("%.2f", revenue)).append("\n");
					} else {
						resultText.append("No events found in this date range.");
					}

					// Set the result text in the text area
					resultTextArea.setText(resultText.toString());

					// Add the text area to a scroll pane
					JScrollPane scrollPane = new JScrollPane(resultTextArea);
					scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

					// Add the scroll pane to the result panel
					resultPanel.add(scrollPane, BorderLayout.CENTER);

					// Revalidate and repaint the dialog to display the new result
					dialog.revalidate();
					dialog.repaint();

				} catch (SQLException ex) {
					JOptionPane.showMessageDialog(dialog, "Error fetching event with most revenue: " + ex.getMessage());
				}
			}
		});

		// Center the dialog
		dialog.setLocationRelativeTo(null);

		// Make the dialog visible
		dialog.setVisible(true);
	}

	public static void viewReservationsByTimePeriod(Connection connection) {
		// Create a new dialog for displaying the reservations
		JDialog dialog = new JDialog();
		dialog.setTitle("Reservations by Time Period");
		dialog.setSize(800, 400);
		dialog.setLayout(new BorderLayout(10, 10));

		// Create a panel for the input fields (start date, end date) and the search
		// button
		JPanel inputPanel = new JPanel();
		inputPanel.setLayout(new FlowLayout());

		// Start Date Field
		JLabel startDateLabel = new JLabel("Start Date (yyyy-mm-dd): ");
		JTextField startDateField = new JTextField(10);
		inputPanel.add(startDateLabel);
		inputPanel.add(startDateField);

		// End Date Field
		JLabel endDateLabel = new JLabel("End Date (yyyy-mm-dd): ");
		JTextField endDateField = new JTextField(10);
		inputPanel.add(endDateLabel);
		inputPanel.add(endDateField);

		// Search Button
		JButton searchButton = new JButton("Search");
		inputPanel.add(searchButton);

		// Panel for displaying the results
		JPanel resultPanel = new JPanel();
		resultPanel.setLayout(new BorderLayout());

		// Add the input panel at the top
		dialog.add(inputPanel, BorderLayout.NORTH);

		// Add the result panel at the center
		dialog.add(resultPanel, BorderLayout.CENTER);

		// Action listener for the Search button
		searchButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String startDate = startDateField.getText();
				String endDate = endDateField.getText();

				if (startDate.isEmpty() || endDate.isEmpty()) {
					JOptionPane.showMessageDialog(dialog, "Please enter both start and end dates.");
					return;
				}

				try {
					// Call the method to fetch reservations for the given date range
					ResultSet rs = DatabaseMethods.getReservationsByTimePeriod(connection, startDate, endDate);

					// Clear the previous results in the result panel
					resultPanel.removeAll();

					// Create column names for the JTable
					String[] columnNames = { "Reservation ID", "Customer Name", "Event", "Reservation Date",
							"Ticket Count", "Payment Amount" };

					// Create a table model to hold the data
					DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);

					// Loop through the ResultSet and add reservation data to the table model
					while (rs.next()) {
						Object[] row = { rs.getString("ReservationID"), rs.getString("CustomerName"),
								rs.getString("Name"), rs.getString("ReservationDate"), rs.getInt("TicketCount"),
								rs.getDouble("PaymentAmount") };
						tableModel.addRow(row);
					}

					// Create the JTable with the model
					JTable reservationTable = new JTable(tableModel);

					// Set the JTable to be scrollable
					reservationTable.setFillsViewportHeight(true);
					JScrollPane tableScrollPane = new JScrollPane(reservationTable);

					// Add the table to the result panel
					resultPanel.add(tableScrollPane, BorderLayout.CENTER);

					// Revalidate and repaint the dialog to display the new result
					dialog.revalidate();
					dialog.repaint();

				} catch (SQLException ex) {
					JOptionPane.showMessageDialog(dialog, "Error fetching reservations: " + ex.getMessage());
				}
			}
		});

		// Center the dialog
		dialog.setLocationRelativeTo(null);

		// Make the dialog visible
		dialog.setVisible(true);
	}

	public static void viewTotalRevenueFromTickets(Connection connection, JFrame parent) {
		// Create a dialog window for user input
		JDialog dialog = new JDialog(parent, "Revenue Filter", true);
		dialog.setSize(600, 200);
		dialog.setLayout(new BorderLayout(10, 10));

		// Panel for ticket type and event filter selection
		JPanel inputPanel = new JPanel();
		inputPanel.setLayout(new FlowLayout());

		// Ticket Type Dropdown
		JLabel ticketTypeLabel = new JLabel("Select Ticket Type:");
		String[] ticketTypes = { "VIP", "Standard" };
		JComboBox<String> ticketTypeComboBox = new JComboBox<>(ticketTypes);
		inputPanel.add(ticketTypeLabel);
		inputPanel.add(ticketTypeComboBox);

		// Event Filter Dropdown
		JLabel eventFilterLabel = new JLabel("Filter by Event:");
		String[] eventOptions = { "All Events", "Specific Event" };
		JComboBox<String> eventFilterComboBox = new JComboBox<>(eventOptions);
		inputPanel.add(eventFilterLabel);
		inputPanel.add(eventFilterComboBox);

		// Search Button
		JButton searchButton = new JButton("Search");
		inputPanel.add(searchButton);

		// Panel to show the results (Initially empty)
		JPanel resultPanel = new JPanel();
		resultPanel.setLayout(new BorderLayout());

		// Add the input and result panels to the dialog
		dialog.add(inputPanel, BorderLayout.NORTH);
		dialog.add(resultPanel, BorderLayout.CENTER);

		// Action for Search Button
		searchButton.addActionListener(e -> {
			String ticketType = (String) ticketTypeComboBox.getSelectedItem();
			String eventFilter = (String) eventFilterComboBox.getSelectedItem();

			if ("Specific Event".equals(eventFilter)) {
				// If filtering by specific event, ask for EventID
				String eventIDStr = JOptionPane.showInputDialog(parent, "Enter EventID for the specific event:");
				if (eventIDStr != null && !eventIDStr.trim().isEmpty()) {
					try {
						int eventID = Integer.parseInt(eventIDStr);
						// Fetch revenue for the specific event
						ResultSet rs = DatabaseMethods.getTotalRevenueFromTicketsByEvent(connection, ticketType,
								eventID);
						displayRevenueResult(rs, ticketType, resultPanel);
					} catch (NumberFormatException ex) {
						JOptionPane.showMessageDialog(parent, "Invalid Event ID. Please enter a valid integer.");
					} catch (SQLException ex) {
						JOptionPane.showMessageDialog(parent, "Error fetching revenue data: " + ex.getMessage());
					}
				}
			} else {
				// Fetch revenue for all events
				try {
					ResultSet rs = DatabaseMethods.getTotalRevenueFromTickets(connection, ticketType);
					displayRevenueResult(rs, ticketType, resultPanel);
				} catch (SQLException ex) {
					JOptionPane.showMessageDialog(parent, "Error fetching revenue data: " + ex.getMessage());
				}
			}
		});

		dialog.setLocationRelativeTo(parent); // Center the dialog
		dialog.setVisible(true); // Show the dialog
	}

	private static void displayRevenueResult(ResultSet rs, String ticketType, JPanel resultPanel) throws SQLException {
		// Clear previous results
		resultPanel.removeAll();

		// Create a text area to display the results
		JTextArea textArea = new JTextArea();
		textArea.setEditable(false);
		StringBuilder sb = new StringBuilder();

		if (rs.next()) {
			double totalRevenue = rs.getDouble("Revenue");
			sb.append("Total Revenue from ").append(ticketType).append(" tickets: ").append(totalRevenue).append("\n");
		} else {
			sb.append("No data found for the selected filters.");
		}

		textArea.setText(sb.toString());
		resultPanel.add(new JScrollPane(textArea), BorderLayout.CENTER);
		resultPanel.revalidate();
		resultPanel.repaint();
	}
}

class RoundedButton extends JButton {
	private static final long serialVersionUID = 1L;
	private String eventDate;

	public RoundedButton(String eventName, String eventDate) {
		super(eventName);
		this.setEventDate(eventDate);
		setContentAreaFilled(false);
		setFocusPainted(false);
		setBorderPainted(false);
		setLayout(new BorderLayout());

		JLabel dateLabel = new JLabel(eventDate);
		dateLabel.setFont(new Font("Arial", Font.PLAIN, 12));
		dateLabel.setForeground(Color.WHITE);
		add(dateLabel, BorderLayout.NORTH); // Place event date at the bottom right
	}

	@Override
	protected void paintComponent(Graphics g) {
		if (getModel().isPressed()) {
			g.setColor(new Color(0, 105, 217)); // Darker color when pressed
		} else if (getModel().isRollover()) {
			g.setColor(new Color(0, 105, 217)); // Change color on hover
		} else {
			g.setColor(new Color(0, 123, 255)); // Default background color
		}

		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30); // Draw rounded corners

		super.paintComponent(g); // Paint the text and other components
	}

	public String getEventDate() {
		return eventDate;
	}

	public void setEventDate(String eventDate) {
		this.eventDate = eventDate;
	}
}

class SeatSelection {
	private int ticketId;
	private String seatLabel;

	public SeatSelection(int ticketId, String seatLabel) {
		this.ticketId = ticketId;
		this.seatLabel = seatLabel;
	}

	public int getTicketId() {
		return ticketId;
	}

	public String getSeatLabel() {
		return seatLabel;
	}
}

class EventCreationForm extends JDialog {
	private static final long serialVersionUID = 1L;
	private Connection connection;

	private JTextField nameField;
	private JTextArea descriptionField;
	private JTextField durationField;
	private JTextField eventDateField;
	private JTextField eventTimeField;
	private JTextField capacityField;
	private JComboBox<String> eventTypeComboBox;
	private JTextField vipTicketsField;
	private JTextField standardTicketsField;
	private JTextField reducedfareTicketsField;
	private JTextField vipTicketPriceField;
	private JTextField standardTicketPriceField;
	private JTextField reducedfareTicketPriceField;
	private JTextField refundPercentageField;
	JComboBox<String> refundableComboBox;

	EventCreationForm(Connection dbConnection) {
		connection = dbConnection;

		setTitle("Create Event");
		setSize(600, 600); // Increased height for better layout
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		// Main panel with BorderLayout
		setLayout(new BorderLayout(10, 10));

		// Welcome Label
		JLabel welcomeLabel = new JLabel("Enter details to create event", JLabel.CENTER);
		welcomeLabel.setFont(new Font("Arial", Font.BOLD, 20));
		add(welcomeLabel, BorderLayout.NORTH);

		// Create a scrollable panel for the form
		JPanel formPanel = new JPanel();
		formPanel.setLayout(new GridLayout(16, 2, 10, 10));
		formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Event form fields
		formPanel.add(new JLabel("Event Name:"));
		nameField = new JTextField();
		formPanel.add(nameField);

		formPanel.add(new JLabel("Event Description:"));
		descriptionField = new JTextArea(3, 20);
		descriptionField.setWrapStyleWord(true);
		descriptionField.setLineWrap(true);
		formPanel.add(new JScrollPane(descriptionField));

		formPanel.add(new JLabel("Duration (mins):"));
		durationField = new JTextField();
		formPanel.add(durationField);

		formPanel.add(new JLabel("Event Date (YYYY-MM-DD):"));
		eventDateField = new JTextField();
		formPanel.add(eventDateField);

		formPanel.add(new JLabel("Event Time (HH:MM:SS):"));
		eventTimeField = new JTextField();
		formPanel.add(eventTimeField);

		formPanel.add(new JLabel("Capacity:"));
		capacityField = new JTextField();
		formPanel.add(capacityField);

		formPanel.add(new JLabel("Event Type:"));
		String[] eventTypes = { "Concert", "Conference", "Workshop", "Festival", "Stage Play" };
		eventTypeComboBox = new JComboBox<>(eventTypes);
		formPanel.add(eventTypeComboBox);

		formPanel.add(new JLabel("VIP Tickets:"));
		vipTicketsField = new JTextField();
		formPanel.add(vipTicketsField);

		formPanel.add(new JLabel("Standard Tickets:"));
		standardTicketsField = new JTextField();
		formPanel.add(standardTicketsField);

		formPanel.add(new JLabel("Reduced Fare Tickets:"));
		reducedfareTicketsField = new JTextField();
		formPanel.add(reducedfareTicketsField);

		// Add price fields for each ticket type
		formPanel.add(new JLabel("VIP Ticket Price:"));
		vipTicketPriceField = new JTextField();
		formPanel.add(vipTicketPriceField);

		formPanel.add(new JLabel("Standard Ticket Price:"));
		standardTicketPriceField = new JTextField();
		formPanel.add(standardTicketPriceField);

		formPanel.add(new JLabel("Reduced Fare Ticket Price:"));
		reducedfareTicketPriceField = new JTextField();
		formPanel.add(reducedfareTicketPriceField);

		formPanel.add(new JLabel("Fully Refundable:"));
		String[] refundableOptions = { "Yes", "No" };
		refundableComboBox = new JComboBox<>(refundableOptions);
		formPanel.add(refundableComboBox);

		formPanel.add(new JLabel("Refund Percentage without % (if applicable):"));
		refundPercentageField = new JTextField();
		formPanel.add(refundPercentageField);

		// Add the form panel to the center of the window
		JScrollPane scrollPane = new JScrollPane(formPanel);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		add(scrollPane, BorderLayout.CENTER);

		// Button panel at the bottom with Submit and Logout buttons
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));

		JButton submitButton = new JButton("Create Event");
		submitButton.setFont(new Font("Arial", Font.BOLD, 14));
		submitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				createEvent();
			}
		});
		buttonPanel.add(submitButton);

		add(buttonPanel, BorderLayout.SOUTH);
	}

	private void createEvent() {
		try {
			String name = nameField.getText();
			String description = descriptionField.getText();
			int duration = Integer.parseInt(durationField.getText());
			String eventDate = eventDateField.getText();
			String eventTime = eventTimeField.getText();
			int capacity = Integer.parseInt(capacityField.getText());
			String eventType = (String) eventTypeComboBox.getSelectedItem(); // Get selected event type from the combo
																				// box

			// Get the number of tickets of each type
			int vipTickets = Integer.parseInt(vipTicketsField.getText());
			int normalTickets = Integer.parseInt(standardTicketsField.getText());
			int discountedTickets = Integer.parseInt(reducedfareTicketsField.getText());

			// Get the prices for each ticket type
			double vipTicketPrice = Double.parseDouble(vipTicketPriceField.getText());
			double normalTicketPrice = Double.parseDouble(standardTicketPriceField.getText());
			double discountedTicketPrice = Double.parseDouble(reducedfareTicketPriceField.getText());

			// Validate that the sum of tickets matches the capacity
			int totalTickets = vipTickets + normalTickets + discountedTickets;
			if (totalTickets != capacity) {
				JOptionPane.showMessageDialog(this,
						"The total number of tickets (VIP + Normal + Discounted) must equal the event capacity.");
				return;
			}

			// Validate inputs (ensure fields are not empty)
			if (name.isEmpty() || description.isEmpty() || eventDate.isEmpty() || eventTime.isEmpty()
					|| eventType.isEmpty()) {
				JOptionPane.showMessageDialog(this, "All fields must be filled out.");
				return;
			}

			int eventId = DatabaseMethods.getNumRows(connection, "Event");
			int refundPercentage = 100;
			boolean isRefundable = refundableComboBox.getSelectedItem().equals("Yes");

			if (!isRefundable) {
				refundPercentage = Integer.parseInt(refundPercentageField.getText().trim());
			}

			// Call the DatabaseMethods.insertEvent to insert the event into the database
			DatabaseMethods.insertEvent(connection, eventId, name, description, duration, eventDate, eventTime,
					capacity, eventType, vipTickets, normalTickets, discountedTickets, vipTicketPrice,
					normalTicketPrice, discountedTicketPrice, isRefundable, refundPercentage);

			// Show success message
			JOptionPane.showMessageDialog(this, "Event created successfully!");

			// Clear the form
			clearForm();

		} catch (SQLException ex) {
			JOptionPane.showMessageDialog(this, "Error creating event: " + ex.getMessage());
		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(this,
					"Please enter valid numbers for duration, capacity, and ticket counts.");
		}
	}

	// Method to clear the form after event creation
	private void clearForm() {
		nameField.setText("");
		descriptionField.setText("");
		durationField.setText("");
		eventDateField.setText("");
		eventTimeField.setText("");
		capacityField.setText("");
		eventTypeComboBox.setSelectedIndex(0); // Reset the event type dropdown
		vipTicketsField.setText("");
		standardTicketsField.setText("");
		reducedfareTicketsField.setText("");
		vipTicketPriceField.setText("");
		standardTicketPriceField.setText("");
		reducedfareTicketPriceField.setText("");
	}
}

package nl.utwente.ing.model.persistentmodel;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * The DatabaseConnection class.
 * Consists of methods that allow the application to set up a connection to the database and use this connection.
 *
 * @author Daan Kooij
 */
public class DatabaseConnection {

    private static Connection connection;

    /**
     * Method used to set up the connection to the SQLite database.
     * Furthermore, this method calls the createTables method, which initializes the tables of the database if necessary.
     * After using this method, the connection is stored in the connection field.
     *
     * @param databaseName The filename of the SQLite database that will be connected to.
     */
    public static void setUp(String databaseName) {
        try {
            String driver = "org.sqlite.JDBC";
            Class.forName(driver);
            String databaseURL = "jdbc:sqlite:" + databaseName;
            connection = DriverManager.getConnection(databaseURL);
            createTables();
            System.out.println("Setting up database connection complete");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error connecting to database");
        }
    }

    /**
     * Method used to create tables in the database if they do not yet exist.
     * If this method is called when the database is empty, the appropriate tables will be created in the database.
     * If this method is called when the database already contains the tables that this method tries to create,
     * nothing will change.
     */
    private static void createTables() {
        try {
            connection.setAutoCommit(false);
            Statement statement = connection.createStatement();
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS User_Table(\n" +
                            "  user_id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                            "  session_id TEXT,\n" +
                            "  highest_transaction_id BIGINT,\n" +
                            "  highest_category_id BIGINT\n," +
                            "  highest_saving_goal_id BIGINT,\n" +
                            "  highest_category_rule_id BIGINT,\n" +
                            "  highest_payment_request_id BIGINT,\n" +
                            "  system_time_millis BIGINT\n" +
                            ");"
            );
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS Transaction_Table(\n" +
                            "  user_id INTEGER,\n" +
                            "  transaction_id BIGINT,\n" +
                            "  date DATETIME,\n" +
                            "  amount FLOAT,\n" +
                            "  description TEXT,\n" +
                            "  external_iban TEXT,\n" +
                            "  type TEXT,\n" +
                            "  FOREIGN KEY(user_id) REFERENCES User_Table(user_id),\n" +
                            "  PRIMARY KEY(user_id, transaction_id)\n" +
                            ");"
            );
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS Category_Table(\n" +
                            "  user_id INTEGER,\n" +
                            "  category_id BIGINT,\n" +
                            "  name TEXT,\n" +
                            "  FOREIGN KEY(user_id) REFERENCES User_Table(user_id),\n" +
                            "  PRIMARY KEY(user_id, category_id)\n" +
                            ");"
            );
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS Transaction_Category(\n" +
                            "  user_id INTEGER,\n" +
                            "  transaction_id BIGINT,\n" +
                            "  category_id BIGINT,\n" +
                            "  FOREIGN KEY(user_id) REFERENCES User_Table(user_id),\n" +
                            "  FOREIGN KEY(transaction_id) REFERENCES Transaction_Table(transaction_id),\n" +
                            "  FOREIGN KEY(category_id) REFERENCES Category_Table(category_id),\n" +
                            "  PRIMARY KEY(user_id, transaction_id, category_id)\n" +
                            ");"
            );
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS CategoryRule_Table(\n" +
                            "  user_id INTEGER,\n" +
                            "  category_rule_id BIGINT,\n" +
                            "  description TEXT,\n" +
                            "  iban TEXT,\n" +
                            "  type TEXT,\n" +
                            "  category_id BIGINT,\n" +
                            "  apply_on_history BOOLEAN,\n" +
                            "  FOREIGN KEY(user_id) REFERENCES User_Table(user_id),\n" +
                            "  FOREIGN KEY(category_id) REFERENCES Category_Table(category_id),\n" +
                            "  PRIMARY KEY(user_id, category_rule_id)\n" +
                            ");"
            );
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS BalanceHistory_Table(\n" +
                            "  user_id INTEGER,\n" +
                            "  time_stamp_millis BIGINT,\n" +
                            "  open float ,\n" +
                            "  close float ,\n" +
                            "  volume float ,\n" +
                            "  FOREIGN KEY(user_id) REFERENCES User_Table(user_id),\n" +
                            "  PRIMARY KEY(user_id, time_stamp_millis)\n" +
                            ");"
            );
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS SavingGoal_Table(\n" +
                            "  user_id INTEGER,\n" +
                            "  saving_goal_id BIGINT,\n" +
                            "  name TEXT,\n" +
                            "  goal float,\n" +
                            "  save_per_month float,\n" +
                            "  min_balance_required float,\n" +
                            "  balance float,\n" +
                            "  FOREIGN KEY(user_id) REFERENCES User_Table(user_id),\n" +
                            "  PRIMARY KEY(user_id, saving_goal_id)\n" +
                            ");"
            );
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS PaymentRequest_Table(\n" +
                            "  user_id INTEGER,\n" +
                            "  payment_request_id BIGINT,\n" +
                            "  description TEXT,\n" +
                            "  due_date TEXT ,\n" +
                            "  amount float ,\n" +
                            "  number_of_requests BIGINT ,\n" +
                            "  filled BOOLEAN ,\n" +
                            "  FOREIGN KEY(user_id) REFERENCES User_Table(user_id),\n" +
                            "  PRIMARY KEY(user_id, payment_request_id)\n" +
                            ");"
            );
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS PaymentRequest_Transaction(\n" +
                            "  user_id INTEGER,\n" +
                            "  transaction_id BIGINT,\n" +
                            "  payment_request_id BIGINT,\n" +
                            "  FOREIGN KEY(user_id) REFERENCES User_Table(user_id),\n" +
                            "  FOREIGN KEY(transaction_id) REFERENCES Transaction_Table(transaction_id),\n" +
                            "  FOREIGN KEY(payment_request_id) REFERENCES PaymentRequest_Table(payment_request_id),\n" +
                            "  PRIMARY KEY(user_id, payment_request_id, transaction_id)\n" +
                            ");"
            );
            statement.close();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            System.err.println("Error creating tables");
        }
    }

    /**
     * Method used to retrieve the connection to the database.
     *
     * @return The Connection to the database.
     */
    public static Connection getDatabaseConnection() {
        return connection;
    }

}

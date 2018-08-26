package nl.utwente.ing.model.persistentmodel;

import nl.utwente.ing.model.bean.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * The CustomORM class.
 * Serves as a connection between the PersistentModel class and the SQL database.
 * Contains methods that translate Java statements to SQL queries and updates.
 *
 * @author Daan Kooij
 */
public class CustomORM {

    private Connection connection;


    private static final String INCREASE_HIGHEST_TRANSACTION_ID =
            "UPDATE User_Table\n" +
                    "SET highest_transaction_id = highest_transaction_id + 1\n" +
                    "WHERE user_id = ?;";
    private static final String GET_HIGHEST_TRANSACTION_ID =
            "SELECT highest_transaction_id\n" +
                    "FROM User_Table\n" +
                    "WHERE user_id = ?;";
    private static final String CREATE_TRANSACTION =
            "INSERT INTO Transaction_Table (user_id, transaction_id, date, amount, description, external_iban, type)\n" +
                    "VALUES (?, ?, ?, ?, ?, ?, ?);";
    private static final String GET_TRANSACTION =
            "SELECT transaction_id, date, amount, description, external_iban, type\n" +
                    "FROM Transaction_Table\n" +
                    "WHERE user_id = ?\n" +
                    "AND transaction_id = ?;";
    private static final String UPDATE_TRANSACTION_DATE =
            "UPDATE Transaction_Table\n" +
                    "SET date = ?\n" +
                    "WHERE user_id = ?\n" +
                    "AND transaction_id = ?;";
    private static final String UPDATE_TRANSACTION_AMOUNT =
            "UPDATE Transaction_Table\n" +
                    "SET amount = ?\n" +
                    "WHERE user_id = ?\n" +
                    "AND transaction_id = ?;";
    private static final String UPDATE_TRANSACTION_DESCRIPTION =
            "UPDATE Transaction_Table\n" +
                    "SET description = ?\n" +
                    "WHERE user_id = ?\n" +
                    "AND transaction_id = ?;";
    private static final String UPDATE_TRANSACTION_EXTERNAL_IBAN =
            "UPDATE Transaction_Table\n" +
                    "SET external_iban = ?\n" +
                    "WHERE user_id = ?\n" +
                    "AND transaction_id = ?;";
    private static final String UPDATE_TRANSACTION_TYPE =
            "UPDATE Transaction_Table\n" +
                    "SET type = ?\n" +
                    "WHERE user_id = ?\n" +
                    "AND transaction_id = ?;";
    private static final String DELETE_TRANSACTION =
            "DELETE FROM Transaction_Table\n" +
                    "WHERE user_id = ?\n" +
                    "AND transaction_id = ?;\n";
    private static final String GET_TRANSACTIONS =
            "SELECT transaction_id, date, amount, description, external_iban, type\n" +
                    "FROM Transaction_Table\n" +
                    "WHERE user_id = ?\n" +
                    "LIMIT ?\n" +
                    "OFFSET ?;";
    private static final String GET_ALL_TRANSACTIONS =
            "SELECT transaction_id, date, amount, description, external_iban, type\n" +
                    "FROM Transaction_Table\n" +
                    "WHERE user_id = ?;";
    private static final String GET_TRANSACTIONS_BY_CATEGORY =
            "SELECT t.transaction_id, t.date, t.amount, t.description, t.external_iban, t.type\n" +
                    "FROM Transaction_Table t, Category_Table c, Transaction_Category tc\n" +
                    "WHERE t.transaction_id = tc.transaction_id\n" +
                    "AND tc.category_id = c.category_id\n" +
                    "AND t.user_id = tc.user_id\n" +
                    "AND tc.user_id = c.user_id\n" +
                    "AND t.user_id = ?\n" +
                    "AND c.name = ?\n" +
                    "LIMIT ?\n" +
                    "OFFSET ?;";
    private static final String INCREASE_HIGHEST_CATEGORY_ID =
            "UPDATE User_Table\n" +
                    "SET highest_category_id = highest_category_id + 1\n" +
                    "WHERE user_id = ?;";
    private static final String GET_HIGHEST_CATEGORY_ID =
            "SELECT highest_category_id\n" +
                    "FROM User_Table\n" +
                    "WHERE user_id = ?;";
    private static final String CREATE_CATEGORY =
            "INSERT INTO Category_Table (user_id, category_id, name)\n" +
                    "VALUES (?, ?, ?);";
    private static final String GET_CATEGORY =
            "SELECT category_id, name\n" +
                    "FROM Category_Table\n" +
                    "WHERE user_id = ?\n" +
                    "AND category_id = ?;";
    private static final String UPDATE_CATEGORY_NAME =
            "UPDATE Category_Table\n" +
                    "SET name = ?\n" +
                    "WHERE user_id = ?\n" +
                    "AND category_id = ?;";
    private static final String DELETE_CATEGORY =
            "DELETE FROM Category_Table\n" +
                    "WHERE user_id = ?\n" +
                    "AND category_id = ?;";
    private static final String GET_CATEGORIES =
            "SELECT category_id, name\n" +
                    "FROM Category_Table\n" +
                    "WHERE user_id = ?\n" +
                    "LIMIT ?\n" +
                    "OFFSET ?;";
    private static final String LINK_TRANSACTION_TO_CATEGORY =
            "INSERT INTO Transaction_Category (user_id, transaction_id, category_id)\n" +
                    "VALUES (?, ?, ?);";
    private static final String UNLINK_TRANSACTION_FROM_CATEGORY =
            "DELETE FROM Transaction_Category\n" +
                    "WHERE user_id = ?\n" +
                    "AND transaction_id = ?\n" +
                    "AND category_id = ?;";
    private static final String UNLINK_TRANSACTION_FROM_ALL_CATEGORIES =
            "DELETE FROM Transaction_Category\n" +
                    "WHERE user_id = ?\n" +
                    "AND transaction_id = ?;";
    private static final String UNLINK_CATEGORY_FROM_ALL_TRANSACTIONS =
            "DELETE FROM Transaction_Category\n" +
                    "WHERE user_id = ?\n" +
                    "AND category_id = ?;";
    private static final String GET_CATEGORY_ID_BY_TRANSACTION_ID =
            "SELECT tc.category_id\n" +
                    "FROM Transaction_Table t, Transaction_Category tc\n" +
                    "WHERE t.transaction_id = tc.transaction_id\n" +
                    "AND t.user_id = tc.user_id\n" +
                    "AND t.user_id = ?\n" +
                    "AND t.transaction_id = ?;";
    private static final String CREATE_NEW_USER =
            "INSERT INTO User_Table (session_id, highest_transaction_id, highest_category_id, highest_saving_goal_id, highest_category_rule_id, system_time_millis)\n" +
                    "VALUES (?, 0, 0, 0, 0, 0);";
    private static final String GET_USER_ID =
            "SELECT user_id\n" +
                    "FROM User_Table\n" +
                    "WHERE session_id = ?;";
    private static final String GET_CATEGORYRULES =
            "SELECT category_rule_id, description, iban, type, category_id, apply_on_history\n" +
                    "FROM CategoryRule_Table\n" +
                    "WHERE user_id = ?;";
    private static final String GET_CATEGORYRULE =
            "SELECT description, iban, type, category_id, apply_on_history\n" +
                    "FROM CategoryRule_Table\n" +
                    "WHERE user_id = ?\n" +
                    "AND category_rule_id = ?;";
    private static final String INCREASE_HIGHEST_CATEGORYRULE_ID =
            "UPDATE User_Table\n" +
                    "SET highest_category_rule_id = highest_category_rule_id + 1\n" +
                    "WHERE user_id = ?;";
    private static final String GET_HIGHEST_CATEGORYRULE_ID =
            "SELECT highest_category_rule_id\n" +
                    "FROM User_Table\n" +
                    "WHERE user_id = ?;";
    private static final String CREATE_CATEGORYRULE =
            "INSERT INTO CategoryRule_Table (user_id, category_rule_id, description, iban, type, category_id, apply_on_history)\n" +
                    "VALUES (?, ?, ?, ?, ?, ?, ?);";
    private static final String UPDATE_CATEGORYRULE_DESCRIPTION =
            "UPDATE CategoryRule_Table\n" +
                    "SET description = ?\n" +
                    "WHERE user_id = ?\n" +
                    "AND category_rule_id = ?;";
    private static final String UPDATE_CATEGORYRULE_IBAN =
            "UPDATE CategoryRule_Table\n" +
                    "SET iban = ?\n" +
                    "WHERE user_id = ?\n" +
                    "AND category_rule_id = ?;";
    private static final String UPDATE_CATEGORYRULE_TYPE =
            "UPDATE CategoryRule_Table\n" +
                    "SET type = ?\n" +
                    "WHERE user_id = ?\n" +
                    "AND category_rule_id = ?;";
    private static final String UPDATE_CATEGORYRULE_CATEGORYID =
            "UPDATE CategoryRule_Table\n" +
                    "SET category_id = ?\n" +
                    "WHERE user_id = ?\n" +
                    "AND category_rule_id = ?;";
    private static final String DELETE_CATEGORYRULE =
            "DELETE FROM CategoryRule_Table\n" +
                    "WHERE user_id = ?\n" +
                    "AND category_rule_id = ?;";
    private static final String GET_BALANCE_HISTORY_POINTS_IN_RANGE =
            "SELECT open, close, volume, time_stamp_millis\n" +
                    "FROM BalanceHistory_Table\n" +
                    "WHERE user_id = ?\n" +
                    "AND time_stamp_millis >= ?\n" +
                    "AND time_stamp_millis < ?\n" +
                    "ORDER BY time_stamp_millis ASC;";
    private static final String GET_PREVIOUS_BALANCE_HISTORY_POINT_CLOSE =
            "SELECT close, time_stamp_millis\n" +
                    "FROM BalanceHistory_Table\n" +
                    "WHERE user_id = ?\n" +
                    "AND time_stamp_millis < ?\n" +
                    "ORDER BY time_stamp_millis DESC\n" +
                    "LIMIT 1;";
    private static final String CREATE_BALANCE_HISTORY_POINT =
            "INSERT INTO BalanceHistory_Table (user_id, time_stamp_millis, open, close, volume)\n" +
                    "VALUES (?, ?, ?, ?, ?);";
    private static final String UPDATE_BALANCE_HISTORY_POINT =
            "UPDATE BalanceHistory_Table\n" +
                    "SET open = ?, close = ?, volume = ?\n" +
                    "WHERE user_id = ?\n" +
                    "AND time_stamp_millis = ?;";
    private static final String GET_FUTURE_BALANCE_HISTORY_POINTS =
            "SELECT time_stamp_millis, open, close, volume\n" +
                    "FROM BalanceHistory_Table\n" +
                    "WHERE user_id = ?\n" +
                    "AND time_stamp_millis > ?\n;";
    private static final String INCREASE_HIGHEST_SAVING_GOAL_ID =
            "UPDATE User_Table\n" +
                    "SET highest_saving_goal_id = highest_saving_goal_id + 1\n" +
                    "WHERE user_id = ?;";
    private static final String GET_HIGHEST_SAVING_GOAL_ID =
            "SELECT highest_saving_goal_id\n" +
                    "FROM User_Table\n" +
                    "WHERE user_id = ?;";
    private static final String GET_ALL_SAVING_GOALS =
            "SELECT saving_goal_id, name, goal, save_per_month, min_balance_required, balance\n" +
                    "FROM SavingGoal_Table\n" +
                    "WHERE user_id = ?\n" +
                    "ORDER BY saving_goal_id ASC;";
    private static final String CREATE_SAVING_GOAL =
            "INSERT INTO SavingGoal_Table (user_id, saving_goal_id, name, goal, save_per_month, min_balance_required, balance)\n" +
                    "VALUES (?, ?, ?, ?, ?, ?, ?);";
    private static final String GET_SAVING_GOAL =
            "SELECT name, goal, save_per_month, min_balance_required, balance\n" +
                    "FROM SavingGoal_Table\n" +
                    "WHERE user_id = ?\n" +
                    "AND saving_goal_id = ?;";
    private static final String DELETE_SAVING_GOAL =
            "DELETE FROM SavingGoal_Table\n" +
                    "WHERE user_id = ?\n" +
                    "AND saving_goal_id = ?;";
    private static final String SET_CURRENT_TIME_MILLIS =
            "UPDATE User_Table\n" +
                    "SET system_time_millis = ?\n" +
                    "WHERE user_id = ?;";
    private static final String GET_CURRENT_TIME_MILLIS =
            "SELECT system_time_millis\n" +
                    "FROM User_Table\n" +
                    "WHERE user_id = ?;";
    private static final String CHECK_IF_BALANCE_HISTORY_POINT_EXISTS =
            "SELECT close\n" +
                    "FROM BalanceHistory_Table\n" +
                    "WHERE user_id = ?\n" +
                    "AND time_stamp_millis = ?;";
    private static final String UPDATE_SAVING_GOAL_BALANCE =
            "UPDATE SavingGoal_Table\n" +
                    "SET balance = ?\n" +
                    "WHERE user_id = ?\n" +
                    "AND saving_goal_id = ?;";


    /**
     * The constructor of CustomORM.
     * Sets the connection field to the connection parameter.
     *
     * @param connection The database connection.
     */
    public CustomORM(Connection connection) {
        this.connection = connection;
    }

    /**
     * Method used to increase the highestTransactionID field of a certain user by one in the database.
     *
     * @param userID The id of the user whose highestTransactionID field should be increased.
     */
    public void increaseHighestTransactionID(int userID) {
        try {
            PreparedStatement statement = connection.prepareStatement(INCREASE_HIGHEST_TRANSACTION_ID);
            statement.setInt(1, userID);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to retrieve the highestTransactionID field of a certain user from the database.
     *
     * @param userID The id of the user whose highestTransactionID field should be retrieved.
     * @return The value of the highestTransactionID field of the user with userID.
     */
    public long getHighestTransactionID(int userID) {
        long highestTransactionID = -1;
        try {
            PreparedStatement statement = connection.prepareStatement(GET_HIGHEST_TRANSACTION_ID);
            statement.setInt(1, userID);
            ResultSet rs = statement.executeQuery();
            highestTransactionID = rs.getLong(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return highestTransactionID;
    }

    /**
     * Method used to insert a Transaction into the database.
     *
     * @param userID        The id of the user to which this new Transaction will belong.
     * @param transactionID The transactionID of the to be inserted Transaction.
     * @param date          The date of the to be inserted Transaction.
     * @param amount        The amount of the to be inserted Transaction.
     * @param externalIBAN  The externalIBAN of the to be inserted Transaction.
     * @param type          The type of the to be inserted Transaction.
     */
    public void createTransaction(int userID, long transactionID, String date, float amount, String description, String externalIBAN,
                                  String type) {
        try {
            PreparedStatement statement = connection.prepareStatement(CREATE_TRANSACTION);
            statement.setInt(1, userID);
            statement.setLong(2, transactionID);
            statement.setString(3, date);
            statement.setFloat(4, amount);
            statement.setString(5, description);
            statement.setString(6, externalIBAN);
            statement.setString(7, type);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to retrieve a Transaction from the database.
     *
     * @param userID        The id of the user from which a Transaction should be retrieved.
     * @param transactionID The id of the to be retrieved Transaction.
     * @return A Transaction object containing data retrieved from the database.
     */
    public Transaction getTransaction(int userID, long transactionID) {
        Transaction transaction = null;
        try {
            PreparedStatement statement = connection.prepareStatement(GET_TRANSACTION);
            statement.setInt(1, userID);
            statement.setLong(2, transactionID);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                String date = resultSet.getString(2);
                float amount = resultSet.getFloat(3);
                String description = resultSet.getString(4);
                String externalIBAN = resultSet.getString(5);
                String type = resultSet.getString(6);
                transaction = new Transaction(transactionID, date, amount, description, externalIBAN, type);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transaction;
    }

    /**
     * Method used to change the date of a Transaction in the database.
     *
     * @param date          The new date of the Transaction.
     * @param userID        The id of the user whose Transaction with transactionID will be updated.
     * @param transactionID The id of the to be updated Transaction.
     */
    public void updateTransactionDate(String date, int userID, long transactionID) {
        try {
            PreparedStatement statement = connection.prepareStatement(UPDATE_TRANSACTION_DATE);
            statement.setString(1, date);
            statement.setInt(2, userID);
            statement.setLong(3, transactionID);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to change the amount of a Transaction in the database.
     *
     * @param amount        The new amount of the Transaction.
     * @param userID        The id of the user whose Transaction with transactionID will be updated.
     * @param transactionID The id of the to be updated Transaction.
     */
    public void updateTransactionAmount(float amount, int userID, long transactionID) {
        try {
            PreparedStatement statement = connection.prepareStatement(UPDATE_TRANSACTION_AMOUNT);
            statement.setFloat(1, amount);
            statement.setInt(2, userID);
            statement.setLong(3, transactionID);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to change the description of a Transaction in the database.
     *
     * @param description   The new description of the Transaction.
     * @param userID        The id of the user whose Transaction with transactionID will be updated.
     * @param transactionID The id of the to be updated Transaction.
     */
    public void updateTransactionDescription(String description, int userID, long transactionID) {
        try {
            PreparedStatement statement = connection.prepareStatement(UPDATE_TRANSACTION_DESCRIPTION);
            statement.setString(1, description);
            statement.setInt(2, userID);
            statement.setLong(3, transactionID);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to change the externalIBAN of a Transaction in the database.
     *
     * @param externalIBAN  The new externalIBAN of the Transaction.
     * @param userID        The id of the user whose Transaction with transactionID will be updated.
     * @param transactionID The id of the to be updated Transaction.
     */
    public void updateTransactionExternalIBAN(String externalIBAN, int userID, long transactionID) {
        try {
            PreparedStatement statement = connection.prepareStatement(UPDATE_TRANSACTION_EXTERNAL_IBAN);
            statement.setString(1, externalIBAN);
            statement.setInt(2, userID);
            statement.setLong(3, transactionID);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to change the type of a Transaction in the database.
     *
     * @param type          The new type of the Transaction.
     * @param userID        The id of the user whose Transaction with transactionID will be updated.
     * @param transactionID The id of the to be updated Transaction.
     */
    public void updateTransactionType(String type, int userID, long transactionID) {
        try {
            PreparedStatement statement = connection.prepareStatement(UPDATE_TRANSACTION_TYPE);
            statement.setString(1, type);
            statement.setInt(2, userID);
            statement.setLong(3, transactionID);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to delete a Transaction from the database.
     *
     * @param userID        The id of the user whose Transaction with transactionID will be deleted.
     * @param transactionID The id of the to be deleted Transaction.
     */
    public void deleteTransaction(int userID, long transactionID) {
        try {
            PreparedStatement statement = connection.prepareStatement(DELETE_TRANSACTION);
            statement.setInt(1, userID);
            statement.setLong(2, transactionID);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to retrieve a batch of Transaction objects belonging to a certain user from the database.
     *
     * @param userID The id of the user to who the to be retrieved Transaction objects belong.
     * @param limit  The (maximum) amount of Transaction objects to be retrieved.
     * @param offset The starting index to retrieve Transaction objects.
     * @return An ArrayList of Transaction objects.
     */
    public ArrayList<Transaction> getTransactions(int userID, int limit, int offset) {
        ArrayList<Transaction> transactions = new ArrayList<>();
        try {
            PreparedStatement statement = connection.prepareStatement(GET_TRANSACTIONS);
            statement.setInt(1, userID);
            statement.setInt(2, limit);
            statement.setInt(3, offset);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                long transactionID = resultSet.getLong(1);
                String date = resultSet.getString(2);
                float amount = resultSet.getFloat(3);
                String description = resultSet.getString(4);
                String externalIBAN = resultSet.getString(5);
                String type = resultSet.getString(6);
                transactions.add(new Transaction(transactionID, date, amount, description, externalIBAN, type));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactions;
    }

    /**
     * Method used to retrieve all Transaction objects belonging to a certain user from the database.
     *
     * @param userID The id of the user to who the to be retrieved Transaction objects belong.
     * @return An ArrayList of Transaction objects.
     */
    public ArrayList<Transaction> getAllTransactions(int userID) {
        ArrayList<Transaction> transactions = new ArrayList<>();
        try {
            PreparedStatement statement = connection.prepareStatement(GET_ALL_TRANSACTIONS);
            statement.setInt(1, userID);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                long transactionID = resultSet.getLong(1);
                String date = resultSet.getString(2);
                float amount = resultSet.getFloat(3);
                String description = resultSet.getString(4);
                String externalIBAN = resultSet.getString(5);
                String type = resultSet.getString(6);
                transactions.add(new Transaction(transactionID, date, amount, description, externalIBAN, type));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactions;
    }

    /**
     * Method used to retrieve a batch of Transaction objects
     * belonging to a certain user and category from the database.
     *
     * @param userID       The id of the user to who the to be retrieved Transaction objects belong.
     * @param categoryName The name of the Category to which the retrieved Transaction objects belong.
     * @param limit        The (maximum) amount of Transaction objects to be retrieved.
     * @param offset       The starting index to retrieve Transaction objects.
     * @return An ArrayList of Transaction objects.
     */
    public ArrayList<Transaction> getTransactionsByCategory(int userID, String categoryName, int limit, int offset) {
        ArrayList<Transaction> transactions = new ArrayList<>();
        try {
            PreparedStatement statement = connection.prepareStatement(GET_TRANSACTIONS_BY_CATEGORY);
            statement.setInt(1, userID);
            statement.setString(2, categoryName);
            statement.setInt(3, limit);
            statement.setInt(4, offset);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                long transactionID = resultSet.getLong(1);
                String date = resultSet.getString(2);
                float amount = resultSet.getFloat(3);
                String description = resultSet.getString(4);
                String externalIBAN = resultSet.getString(5);
                String type = resultSet.getString(6);
                transactions.add(new Transaction(transactionID, date, amount, description, externalIBAN, type));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactions;
    }

    /**
     * Method used to increase the highestCategoryID field of a certain user by one in the database.
     *
     * @param userID The id of the user whose highestCategoryID field should be increased.
     */
    public void increaseHighestCategoryID(int userID) {
        try {
            PreparedStatement statement = connection.prepareStatement(INCREASE_HIGHEST_CATEGORY_ID);
            statement.setInt(1, userID);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to retrieve the highestCategoryID field of a certain user from the database.
     *
     * @param userID The id of the user whose highestCategoryID field should be retrieved.
     * @return The value of the highestCategoryID field of the user with userID.
     */
    public long getHighestCategoryID(int userID) {
        long highestCategoryID = -1;
        try {
            PreparedStatement statement = connection.prepareStatement(GET_HIGHEST_CATEGORY_ID);
            statement.setInt(1, userID);
            ResultSet rs = statement.executeQuery();
            highestCategoryID = rs.getLong(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return highestCategoryID;
    }

    /**
     * Method used to insert a new Category into the database.
     *
     * @param userID     The id of the user to which this new Category will belong.
     * @param categoryID The categoryID of the to be inserted Category.
     * @param name       The name of the to be inserted Category.
     */
    public void createCategory(int userID, long categoryID, String name) {
        try {
            PreparedStatement statement = connection.prepareStatement(CREATE_CATEGORY);
            statement.setInt(1, userID);
            statement.setLong(2, categoryID);
            statement.setString(3, name);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to retrieve a Category from the database.
     *
     * @param userID     The id of the user from which a Category should be retrieved.
     * @param categoryID The id of the to be retrieved Category.
     * @return A Category object containing data retrieved from the database.
     */
    public Category getCategory(int userID, long categoryID) {
        Category category = null;
        try {
            PreparedStatement statement = connection.prepareStatement(GET_CATEGORY);
            statement.setInt(1, userID);
            statement.setLong(2, categoryID);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                String name = resultSet.getString(2);
                category = new Category(categoryID, name);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return category;
    }

    /**
     * Method used to update the name of a Category in the database.
     *
     * @param name       The new name of the to be updated Category.
     * @param userID     The id of the user whose Category with categoryID will be updated.
     * @param categoryID The id of the to be updated Category.
     */
    public void updateCategoryName(String name, int userID, long categoryID) {
        try {
            PreparedStatement statement = connection.prepareStatement(UPDATE_CATEGORY_NAME);
            statement.setString(1, name);
            statement.setInt(2, userID);
            statement.setLong(3, categoryID);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to delete a Category from the database.
     *
     * @param userID     The id of the user whose Category with categoryID will me deleted.
     * @param categoryID The id of the to be deleted Category.
     */
    public void deleteCategory(int userID, long categoryID) {
        try {
            PreparedStatement statement = connection.prepareStatement(DELETE_CATEGORY);
            statement.setInt(1, userID);
            statement.setLong(2, categoryID);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to retrieve a batch of Category objects belonging to a certain user from the database.
     *
     * @param userID The id of the user to who the to be retrieved Category objects belong.
     * @param limit  The (maximum) amount of Category objects to be retrieved.
     * @param offset The starting index to retrieve Category objects.
     * @return An ArrayList of Category objects.
     */
    public ArrayList<Category> getCategories(int userID, int limit, int offset) {
        ArrayList<Category> categories = new ArrayList<>();
        try {
            PreparedStatement statement = connection.prepareStatement(GET_CATEGORIES);
            statement.setInt(1, userID);
            statement.setInt(2, limit);
            statement.setInt(3, offset);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int categoryID = resultSet.getInt(1);
                String name = resultSet.getString(2);
                categories.add(new Category(categoryID, name));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categories;
    }

    /**
     * Method used to link a Transaction to a Category in the database.
     *
     * @param userID        The id of the user to who the to be linked Transaction and Category objects belong.
     * @param transactionID The id of the Transaction that will be linked to a Category.
     * @param categoryID    The id of the Category that will be linked to a Transaction.
     */
    public void linkTransactionToCategory(int userID, long transactionID, long categoryID) {
        try {
            PreparedStatement statement = connection.prepareStatement(LINK_TRANSACTION_TO_CATEGORY);
            statement.setInt(1, userID);
            statement.setLong(2, transactionID);
            statement.setLong(3, categoryID);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to unlink a Transaction from a Category in the database.
     *
     * @param userID        The id of the user to who the to be unlinked Transaction and Category objects belong.
     * @param transactionID The id of the Transaction that will be unlinked from a Category.
     * @param categoryID    The id of the Category from which the Transaction will be unlinked.
     */
    public void unlinkTransactionFromCategory(int userID, long transactionID, long categoryID) {
        try {
            PreparedStatement statement = connection.prepareStatement(UNLINK_TRANSACTION_FROM_CATEGORY);
            statement.setInt(1, userID);
            statement.setLong(2, transactionID);
            statement.setLong(3, categoryID);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to unlink a Transaction from all Category objects in the database.
     *
     * @param userID        The id of the user to who the to be unlinked Transaction object belongs.
     * @param transactionID The id of the Transaction that will be unlinked from all Category objects in the database.
     */
    public void unlinkTransactionFromAllCategories(int userID, long transactionID) {
        try {
            PreparedStatement statement = connection.prepareStatement(UNLINK_TRANSACTION_FROM_ALL_CATEGORIES);
            statement.setInt(1, userID);
            statement.setLong(2, transactionID);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to unlink a Category from all Transaction objects in the database.
     *
     * @param userID     The id of the user to who the to be unlinked Category object belongs.
     * @param categoryID The id of the Category that will be unlinked from all Transaction objects in the database.
     */
    public void unlinkCategoryFromAllTransactions(int userID, long categoryID) {
        try {
            PreparedStatement statement = connection.prepareStatement(UNLINK_CATEGORY_FROM_ALL_TRANSACTIONS);
            statement.setInt(1, userID);
            statement.setLong(2, categoryID);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to retrieve the id of the Category that is linked to a certain Transaction from the database.
     *
     * @param userID        The id of the user who is the owner of the Transaction object with transactionID.
     * @param transactionID The id of the Transaction from which the linked Category id will be retrieved.
     * @return The id of the Category that is linked to the Transaction.
     */
    public long getCategoryIDByTransactionID(int userID, long transactionID) {
        long categoryID = -1;
        try {
            PreparedStatement statement = connection.prepareStatement(GET_CATEGORY_ID_BY_TRANSACTION_ID);
            statement.setInt(1, userID);
            statement.setLong(2, transactionID);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                categoryID = resultSet.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categoryID;
    }

    /**
     * Method used to add a new User with sessionID in the database.
     *
     * @param sessionID The sessionID of the to be created User.
     */
    public void createNewUser(String sessionID) {
        try {
            PreparedStatement statement = connection.prepareStatement(CREATE_NEW_USER);
            statement.setString(1, sessionID);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to retrieve the userID of the user with sessionID from the database.
     *
     * @param sessionID The sessionID of the User whose userID will be retrieved.
     * @return The userID of the user with sessionID.
     */
    public int getUserID(String sessionID) {
        int userID = -1;
        try {
            PreparedStatement statement = connection.prepareStatement(GET_USER_ID);
            statement.setString(1, sessionID);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                userID = resultSet.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return userID;
    }

    /**
     * Method used to retrieve a batch of CategoryRule objects belonging to a certain user from the database.
     *
     * @param userID The id of the user to who the to be retrieved Category objects belong.
     * @return An ArrayList of CategoryRule objects.
     */
    public ArrayList<CategoryRule> getCategoryRules(int userID) {
        ArrayList<CategoryRule> categoryRules = new ArrayList<>();
        try {
            PreparedStatement statement = connection.prepareStatement(GET_CATEGORYRULES);
            statement.setInt(1, userID);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                long categoryRuleID = resultSet.getInt(1);
                String description = resultSet.getString(2);
                String iBAN = resultSet.getString(3);
                String type = resultSet.getString(4);
                long categoryID = resultSet.getLong(5);
                boolean applyOnHistory = resultSet.getBoolean(6);
                categoryRules.add(new CategoryRule(categoryRuleID, description, iBAN, type, categoryID, applyOnHistory));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categoryRules;
    }

    /**
     * Method used to retrieve a CategoryRule from the database.
     *
     * @param userID         The id of the user from which a CategoryRule should be retrieved.
     * @param categoryRuleID The id of the to be retrieved CategoryRule.
     * @return A CategoryRule object containing data retrieved from the database.
     */
    public CategoryRule getCategoryRule(int userID, long categoryRuleID) {
        CategoryRule categoryRule = null;
        try {
            PreparedStatement statement = connection.prepareStatement(GET_CATEGORYRULE);
            statement.setInt(1, userID);
            statement.setLong(2, categoryRuleID);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                String description = resultSet.getString(1);
                String iBAN = resultSet.getString(2);
                String type = resultSet.getString(3);
                long categoryID = resultSet.getLong(4);
                boolean applyOnHistory = resultSet.getBoolean(5);
                categoryRule = new CategoryRule(categoryRuleID, description, iBAN, type, categoryID, applyOnHistory);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categoryRule;
    }

    /**
     * Method used to increase the highest ID of CategoryRules by one.
     *
     * @param userID The ID of the user.
     */
    public void increaseHighestCategoryRuleID(int userID) {
        try {
            PreparedStatement statement = connection.prepareStatement(INCREASE_HIGHEST_CATEGORYRULE_ID);
            statement.setInt(1, userID);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to retrieve the highest ID of CategoryRules.
     *
     * @param userID The ID of the user.
     * @return The highest ID of CategoryRules.
     */
    public long getHighestCategoryRuleID(int userID) {
        long highestCategoryRuleID = -1;
        try {
            PreparedStatement statement = connection.prepareStatement(GET_HIGHEST_CATEGORYRULE_ID);
            statement.setInt(1, userID);
            ResultSet rs = statement.executeQuery();
            highestCategoryRuleID = rs.getLong(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return highestCategoryRuleID;
    }

    /**
     * Method used to insert a new CategoryRule into the database.
     *
     * @param userID         The id of the user to which this new CategoryRule will belong.
     * @param categoryID     The ID of the to be inserted CategoryRule.
     * @param description    The description of the to be inserted CategoryRule.
     * @param iBan           The iban of the to be inserted CategoryRule.
     * @param type           The type of the to be inserted CategoryRule.
     * @param categoryID     The categoryID of the to be inserted CategoryRule.
     * @param applyOnHistory Whether the categoryRule applies to old transactions or not.
     */
    public void createCategoryRule(int userID, long categoryRuleID, String description, String iBan, String type,
                                   long categoryID, boolean applyOnHistory) {
        try {
            PreparedStatement statement = connection.prepareStatement(CREATE_CATEGORYRULE);
            statement.setInt(1, userID);
            statement.setLong(2, categoryRuleID);
            statement.setString(3, description);
            statement.setString(4, iBan);
            statement.setString(5, type);
            statement.setLong(6, categoryID);
            statement.setBoolean(7, applyOnHistory);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to update the description of a CategoryRule.
     *
     * @param description    The new description.
     * @param userID         The ID of the user.
     * @param categoryRuleID The ID of the categoryRule.
     */
    public void updateCategoryRuleDescription(String description, int userID, Long categoryRuleID) {
        try {
            PreparedStatement statement = connection.prepareStatement(UPDATE_CATEGORYRULE_DESCRIPTION);
            statement.setString(1, description);
            statement.setInt(2, userID);
            statement.setLong(3, categoryRuleID);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to update the iban of a CategoryRule.
     *
     * @param iBan           The new iban.
     * @param userID         The ID of the user.
     * @param categoryRuleID The ID of the categoryRule.
     */
    public void updateCategoryRuleIBAN(String iBan, int userID, Long categoryRuleID) {
        try {
            PreparedStatement statement = connection.prepareStatement(UPDATE_CATEGORYRULE_IBAN);
            statement.setString(1, iBan);
            statement.setInt(2, userID);
            statement.setLong(3, categoryRuleID);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to update the type of a CategoryRule.
     *
     * @param type           The new type.
     * @param userID         The ID of the user.
     * @param categoryRuleID The ID of the categoryRule.
     */
    public void updateCategoryRuleType(String type, int userID, Long categoryRuleID) {
        try {
            PreparedStatement statement = connection.prepareStatement(UPDATE_CATEGORYRULE_TYPE);
            statement.setString(1, type);
            statement.setInt(2, userID);
            statement.setLong(3, categoryRuleID);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to update the categoryID of a CategoryRule.
     *
     * @param categoryID     The new categoryID.
     * @param userID         The ID of the user.
     * @param categoryRuleID The ID of the categoryRule.
     */
    public void updateCategoryRuleCategory(Long categoryID, int userID, Long categoryRuleID) {
        try {
            PreparedStatement statement = connection.prepareStatement(UPDATE_CATEGORYRULE_CATEGORYID);
            statement.setLong(1, categoryID);
            statement.setInt(2, userID);
            statement.setLong(3, categoryRuleID);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to remove a CategoryRule from the database.
     *
     * @param userID         The ID of the user which the CategoryRule belongs to.
     * @param categoryRuleID The ID of the to be removed CategoryRule.
     */
    public void deleteCategoryRule(int userID, long categoryRuleID) {
        try {
            PreparedStatement statement = connection.prepareStatement(DELETE_CATEGORYRULE);
            statement.setInt(1, userID);
            statement.setLong(2, categoryRuleID);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to get all the balance history points between the specified start and end time.
     *
     * @param userID               The specified ID of the user.
     * @param startTimestampMillis The start time of the desired range of points.
     * @param endTimestampMillis   The end time of the desired range of points.
     * @return All the balance history points in the desired range.
     */
    public ArrayList<BalanceHistoryPoint> getBalanceHistoryPointsInRange(int userID, long startTimestampMillis, long endTimestampMillis) {
        ArrayList<BalanceHistoryPoint> balanceHistoryPoints = new ArrayList<>();
        try {
            PreparedStatement statement = connection.prepareStatement(GET_BALANCE_HISTORY_POINTS_IN_RANGE);
            statement.setInt(1, userID);
            statement.setLong(2, startTimestampMillis);
            statement.setLong(3, endTimestampMillis);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                float open = resultSet.getFloat(1);
                float close = resultSet.getFloat(2);
                float volume = resultSet.getFloat(3);
                long timeStampMillis = resultSet.getLong(4);
                balanceHistoryPoints.add(new BalanceHistoryPoint(open, close, volume, timeStampMillis));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return balanceHistoryPoints;
    }

    /**
     * Method used to retrieve the balance close value of the balance history point that was the last before the to be added
     * point that starts with the specified timestamp.
     *
     * @param userID          The specified ID of the user.
     * @param timestampMillis The timestamp of the to be added balance history point.
     * @return The close value of the balance history point that was the last before the specified time.
     */
    public float getPreviousBalanceHistoryPointClose(int userID, long timestampMillis) {
        float close = 0;
        try {
            PreparedStatement statement = connection.prepareStatement(GET_PREVIOUS_BALANCE_HISTORY_POINT_CLOSE);
            statement.setInt(1, userID);
            statement.setLong(2, timestampMillis);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                close = resultSet.getFloat(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return close;
    }

    /**
     * Method used to create a balance history point in the database.
     *
     * @param userID              The ID of the specified user.
     * @param balanceHistoryPoint The data to be inserted in the database.
     */
    public void createBalanceHistoryPoint(int userID, BalanceHistoryPoint balanceHistoryPoint) {
        try {
            PreparedStatement statement = connection.prepareStatement(CREATE_BALANCE_HISTORY_POINT);
            statement.setInt(1, userID);
            statement.setLong(2, balanceHistoryPoint.getTimeStamp());
            statement.setFloat(3, balanceHistoryPoint.getOpen());
            statement.setFloat(4, balanceHistoryPoint.getClose());
            statement.setFloat(5, balanceHistoryPoint.getVolume());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to update a balance history point in the database.
     *
     * @param userID The ID of the specified user.
     * @param b      The balance history point that needs to be updated, its timestamp must already exist in the database.
     */
    public void updateBalanceHistoryPoint(int userID, BalanceHistoryPoint b) {
        try {
            PreparedStatement statement = connection.prepareStatement(UPDATE_BALANCE_HISTORY_POINT);
            statement.setFloat(1, b.getOpen());
            statement.setFloat(2, b.getClose());
            statement.setFloat(3, b.getVolume());
            statement.setInt(4, userID);
            statement.setLong(5, b.getTimeStamp());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to retrieve the balance history points that are further in time then the given timestamp
     *
     * @param userID          The ID of the specified user.
     * @param timestampMillis The timestamp of the balance history point before the points to return.
     * @return All balance history points that occur after the timestamp given.
     */
    public ArrayList<BalanceHistoryPoint> getFutureBalanceHistoryPoints(int userID, long timestampMillis) {
        ArrayList<BalanceHistoryPoint> balanceHistoryPoints = new ArrayList<>();
        try {
            PreparedStatement statement = connection.prepareStatement(GET_FUTURE_BALANCE_HISTORY_POINTS);
            statement.setInt(1, userID);
            statement.setLong(2, timestampMillis);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                long timeStamp = resultSet.getLong(1);
                float open = resultSet.getFloat(2);
                float low = resultSet.getFloat(3);
                float volume = resultSet.getFloat(4);
                balanceHistoryPoints.add(new BalanceHistoryPoint(open, low, volume, timeStamp));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return balanceHistoryPoints;
    }

    /**
     * Method used to retrieve the savinggoals of a user.
     *
     * @param userID    The ID of the specified user.
     * @return  All savinggoals of the specified user.
     */
    public ArrayList<SavingGoal> getSavingGoals(int userID) {
        ArrayList<SavingGoal> savingGoals = new ArrayList<>();
        try {
            PreparedStatement statement = connection.prepareStatement(GET_ALL_SAVING_GOALS);
            statement.setInt(1, userID);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                long savingGoalID = resultSet.getLong(1);
                String name = resultSet.getString(2);
                float goal = resultSet.getFloat(3);
                float savePerMonth = resultSet.getFloat(4);
                float minBalanceRequired = resultSet.getFloat(5);
                float balance = resultSet.getFloat(6);
                savingGoals.add(new SavingGoal(savingGoalID, name, goal, savePerMonth, minBalanceRequired, balance));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return savingGoals;
    }

    /**
     * Method used to increase the highest savinggoal ID of the specified user.
     *
     * @param userID    The ID of the specified user.
     */
    public void increaseHighestSavingGoalID(int userID) {
        try {
            PreparedStatement statement = connection.prepareStatement(INCREASE_HIGHEST_SAVING_GOAL_ID);
            statement.setInt(1, userID);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to retrieve the highest savinggoal ID of the specified user.
     *
     * @param userID    The ID of the specified user.
     * @return  The highest savinggoal ID.
     */
    public long getHighestSavingGoalID(int userID) {
        long highestSavingGoalID = -1;
        try {
            PreparedStatement statement = connection.prepareStatement(GET_HIGHEST_SAVING_GOAL_ID);
            statement.setInt(1, userID);
            ResultSet rs = statement.executeQuery();
            highestSavingGoalID = rs.getLong(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return highestSavingGoalID;
    }

    /**
     * Method used to create a savinggoal for the specified user.
     *
     * @param userID                The ID of the specified user.
     * @param savingGoalID          The ID of the to be created savinggoal.
     * @param name                  The name of the to be created savinggoal.
     * @param goal                  The goal of the to be created savinggoal.
     * @param savePerMonth          The amount to be saved per month of the to be created savinggoal.
     * @param minBalanceRequired    The minimal balance that the user needs to have for the savinggoal
     *                              to save money.
     */
    public void createSavingGoal(int userID, long savingGoalID, String name, float goal, float savePerMonth, float minBalanceRequired) {
        try {
            PreparedStatement statement = connection.prepareStatement(CREATE_SAVING_GOAL);
            statement.setInt(1, userID);
            statement.setLong(2, savingGoalID);
            statement.setString(3, name);
            statement.setFloat(4, goal);
            statement.setFloat(5, savePerMonth);
            statement.setFloat(6, minBalanceRequired);
            statement.setFloat(7, 0);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to get the savinggoal with specified ID.
     *
     * @param userID        The ID of the specified user.
     * @param savingGoalID  The ID of the to be retrieved savinggoal.
     * @return  The savinggoal with savingGoalID.
     */
    public SavingGoal getSavingGoal(int userID, long savingGoalID) {
        SavingGoal savingGoal = null;
        try {
            PreparedStatement statement = connection.prepareStatement(GET_SAVING_GOAL);
            statement.setInt(1, userID);
            statement.setLong(2, savingGoalID);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                String name = resultSet.getString(1);
                float goal = resultSet.getFloat(2);
                float saverPerMonth = resultSet.getFloat(3);
                float minBalanceRequired = resultSet.getFloat(4);
                float balance = resultSet.getFloat(5);
                savingGoal = new SavingGoal(savingGoalID, name, goal, saverPerMonth, minBalanceRequired, balance);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return savingGoal;
    }

    /**
     * Method used to delete the specified savinggoal.
     *
     * @param userID        The ID of the specified user.
     * @param savingGoalID  The ID of the to be deleted savinggoal.
     */
    public void deleteSavingGoal(int userID, long savingGoalID) {
        try {
            PreparedStatement statement = connection.prepareStatement(DELETE_SAVING_GOAL);
            statement.setInt(1, userID);
            statement.setLong(2, savingGoalID);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    /**
     * Method used to retrieve the current system time of the specified user.
     *
     * @param userID    The ID of the specified user.
     * @return
     */
    public long getCurrentTimeMillis(int userID) {
        long currentTimeMillis = -1;
        try {
            PreparedStatement statement = connection.prepareStatement(GET_CURRENT_TIME_MILLIS);
            statement.setInt(1, userID);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                currentTimeMillis = resultSet.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return currentTimeMillis;
    }


    /**
     * Method used to store the current time of the specified user.
     *
     * @param userID                    The ID of the specified user.
     * @param currentTimestampMillis    The current system time of the specified user.
     */
    public void setCurrentTimeMillis(int userID, long currentTimestampMillis) {
        try {
            PreparedStatement statement = connection.prepareStatement(SET_CURRENT_TIME_MILLIS);
            statement.setLong(1, currentTimestampMillis);
            statement.setInt(2, userID);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method used to check whether a balanceHistoryPoint with the specified timestamp exists.
     *
     * @param userID                            The ID of the specified user.
     * @param savingGoalTransactionTimeMillis   The timestamp of a new transaction to be posted.
     * @return
     */
    public boolean balanceHistoryPointExists(int userID, long savingGoalTransactionTimeMillis) {
        boolean bhpExists = false;
        try {
            PreparedStatement statement = connection.prepareStatement(CHECK_IF_BALANCE_HISTORY_POINT_EXISTS);
            statement.setInt(1, userID);
            statement.setLong(2, savingGoalTransactionTimeMillis);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                bhpExists = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bhpExists;
    }

    /**
     * Method used to update the balance of a savinggoal.
     *
     * @param userID        The ID of the specified user.
     * @param savingGoalID  The ID of the to be updated savinggoal.
     * @param newBalance    The new balance of the savinggoal.
     */
    public void updateSavingGoalBalance(int userID, long savingGoalID, float newBalance) {
        try {
            PreparedStatement statement = connection.prepareStatement(UPDATE_SAVING_GOAL_BALANCE);
            statement.setFloat(1, newBalance);
            statement.setInt(2, userID);
            statement.setLong(3, savingGoalID);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

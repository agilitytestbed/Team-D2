package nl.utwente.ing.model.persistentmodel;

import nl.utwente.ing.exception.InvalidSessionIDException;
import nl.utwente.ing.exception.ResourceNotFoundException;
import nl.utwente.ing.model.Model;
import nl.utwente.ing.model.bean.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

/**
 * The PersistentModel class, an implementation of the Model interface.
 * Implements the methods specified in the Model interface using persistent storage methods, meaning that the data
 * stored using the persistent model will exist over multiple executions of the application.
 *
 * @author Daan Kooij
 */
public class PersistentModel implements Model {

    private Connection connection;
    private CustomORM customORM;

    /**
     * The constructor of PersistentModel.
     * Retrieves the database connection from the DatabaseConnection class and initializes a CustomORM object.
     */
    public PersistentModel() {
        this.connection = DatabaseConnection.getDatabaseConnection();
        this.customORM = new CustomORM(connection);
    }

    /**
     * Method used to retrieve the transactions belonging to a certain user.
     *
     * @param sessionID    The sessionID of the user.
     * @param categoryName The category to be filtered on (empty String if no filter).
     * @param limit        The maximum amount of transactions to be fetched.
     * @param offset       The starting index to fetch transactions.
     * @return An ArrayList of Transaction belonging to the user with sessionID.
     */
    public ArrayList<Transaction> getTransactions(String sessionID, String categoryName, int limit, int offset)
            throws InvalidSessionIDException {
        int userID = this.getUserID(sessionID);
        ArrayList<Transaction> transactions;
        if (categoryName.equals("")) {
            transactions = customORM.getTransactions(userID, limit, offset);
        } else {
            transactions = customORM.getTransactionsByCategory(userID, categoryName, limit, offset);
        }
        for (Transaction transaction : transactions) {
            this.populateCategory(userID, transaction);
        }
        return transactions;
    }

    /**
     * Method used to create a new Transaction for a certain user.
     *
     * @param sessionID    The sessionID of the user.
     * @param date         The date of the to be created Transaction.
     * @param amount       The amount of the to be created Transaction.
     * @param description  The description of the to be created Transaction.
     * @param externalIBAN The external IBAN of the to be created Transaction.
     * @param type         The type of the to be created Transaction.
     * @param categoryID   The categoryID of the Category that will be assigned to the to be created Transaction
     *                     (0 if no Category).
     * @return The Transaction created by this method.
     */
    public Transaction postTransaction(String sessionID, String date, float amount, String description, String externalIBAN, String type,
                                       long categoryID)
            throws InvalidSessionIDException, ResourceNotFoundException {
        int userID = this.getUserID(sessionID);
        Transaction transaction = null;
        try {
            connection.setAutoCommit(false);
            customORM.increaseHighestTransactionID(userID);
            long transactionID = customORM.getHighestTransactionID(userID);
            connection.commit();
            connection.setAutoCommit(true);
            customORM.createTransaction(userID, transactionID, date, amount, description, externalIBAN, type);
            transaction = customORM.getTransaction(userID, transactionID);
            if (categoryID > 0) {
                this.assignCategoryToTransaction(sessionID, transactionID, categoryID);
            } else {
                ArrayList<CategoryRule> categoryRules = customORM.getCategoryRules(userID);
                if (categoryRules.size() > 0) {
                    categoryRules.sort(Comparator.comparing(CategoryRule::getCategory_id));
                    for (int i = 0; i < categoryRules.size(); i++) {
                        if (transactionMatchesCategoryRule(transaction, categoryRules.get(i))) {
                            assignCategoryToTransaction(sessionID, transactionID, categoryRules.get(i).getCategory_id());
                            break;
                        }
                    }
                }
            }
            setBalanceHistoryPoint(date, amount, type, userID);
            this.populateCategory(userID, transaction);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transaction;
    }

    /**
     * Method used to create a balance history point in the database.
     *
     * @param date      The date of the transaction.
     * @param amount    The amount of the transaction.
     * @param type      The type of the transaction (deposit or withdrawal).
     * @param userID    The ID of the specified user.
     */
    private void setBalanceHistoryPoint(String date, float amount, String type, int userID) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        dateFormat.setLenient(false);
        long timestampMillis = -1;
        try {
            timestampMillis = dateFormat.parse(date.trim()).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        float volume = amount;
        if (type.equals("withdrawal")) {
            amount = -amount;
        }
        float open = customORM.getPreviousBalanceHistoryPointClose(userID, timestampMillis);
        float close = open + amount;
        BalanceHistoryPoint b = new BalanceHistoryPoint(open, close, volume, timestampMillis);
        customORM.createBalanceHistoryPoint(userID, b);
        fixFutureBalanceHistoryPoints(userID, timestampMillis, amount);
    }

    /**
     * Method used to update all balance history points that are already in the database, but record history after the
     * currently added balance history point.
     *
     * @param userID            The ID of the specified user.
     * @param timestampMillis   The time stamp in milliseconds of the current transaction.
     * @param amount            The amount of the current transaction.
     */
    private void fixFutureBalanceHistoryPoints(int userID, long timestampMillis, float amount) {
        ArrayList<BalanceHistoryPoint> futureBalanceHistoryPoints = customORM.getFutureBalanceHistoryPoints(userID, timestampMillis);
        if (futureBalanceHistoryPoints.size() > 0) {
            for (BalanceHistoryPoint b : futureBalanceHistoryPoints) {
                b.setOpen(b.getOpen() + amount);
                b.setClose(b.getClose() + amount);
                customORM.updateBalanceHistoryPoint(userID, b);
            }
        }
    }

    /**
     * Method used to retrieve a certain Transaction of a certain user.
     *
     * @param sessionID     The sessionID of the user.
     * @param transactionID The transactionID of the Transaction that will be retrieved.
     * @return The Transaction with transactionID belonging to the user with sessionID.
     */
    public Transaction getTransaction(String sessionID, long transactionID)
            throws InvalidSessionIDException, ResourceNotFoundException {
        int userID = this.getUserID(sessionID);
        Transaction transaction = customORM.getTransaction(userID, transactionID);
        this.populateCategory(userID, transaction);
        if (transaction != null) {
            return transaction;
        } else {
            throw new ResourceNotFoundException();
        }
    }

    /**
     * Method used to update a certain Transaction of a certain user.
     *
     * @param sessionID     The sessionID of the user.
     * @param transactionID The transactionID of the Transaction that will be updated.
     * @param date          The new date of the to be updated Transaction.
     * @param amount        The new amount of the to be updated Transaction.
     * @param externalIBAN  The new external IBAN of the to be updated Transaction.
     * @param type          The new type of the to be updated Transaction.
     * @param categoryID    The new categoryID of the Category that will be assigned to the to be updated Transaction
     *                      (0 if no Category).
     * @return The Transaction updated by this method.
     */
    public Transaction putTransaction(String sessionID, long transactionID, String date, float amount,
                                      String description, String externalIBAN, String type, long categoryID)
            throws InvalidSessionIDException, ResourceNotFoundException {
        int userID = this.getUserID(sessionID);
        Transaction transaction = customORM.getTransaction(userID, transactionID);
        if (transaction != null) {
            if (date != null && !date.equals("")) {
                customORM.updateTransactionDate(date, userID, transactionID);
            }
            if (amount != 0) {
                customORM.updateTransactionAmount(amount, userID, transactionID);
            }
            if (description != null) {
                customORM.updateTransactionDescription(description, userID, transactionID);
            }
            if (externalIBAN != null && !externalIBAN.equals("")) {
                customORM.updateTransactionExternalIBAN(externalIBAN, userID, transactionID);
            }
            if (type != null && !type.equals("")) {
                customORM.updateTransactionType(type, userID, transactionID);
            }
            if (categoryID != 0) {
                this.assignCategoryToTransaction(sessionID, transactionID, categoryID);
            }
            transaction = customORM.getTransaction(userID, transactionID);
            this.populateCategory(userID, transaction);
            return transaction;
        } else {
            throw new ResourceNotFoundException();
        }
    }

    /**
     * Method used to delete a certain Transaction of a certain user.
     *
     * @param sessionID     The sessionID of the user.
     * @param transactionID The transactionID of the Transaction that will be deleted.
     */
    public void deleteTransaction(String sessionID, long transactionID)
            throws InvalidSessionIDException, ResourceNotFoundException {
        int userID = this.getUserID(sessionID);
        Transaction transaction = customORM.getTransaction(userID, transactionID);
        if (transaction != null) {
            customORM.unlinkTransactionFromAllCategories(userID, transactionID);
            customORM.deleteTransaction(userID, transactionID);
        } else {
            throw new ResourceNotFoundException();
        }
    }

    /**
     * Method used to assign a Category to a Transaction.
     * Currently, first all other assigned Category objects to Transaction are unassigned.
     *
     * @param sessionID     The sessionID of the user.
     * @param transactionID The transactionID of the Transaction to which the Category will be assigned.
     * @param categoryID    The categoryID of the Category which will be assigned to the Transaction.
     * @return The Transaction to which the Category is assigned.
     */
    public Transaction assignCategoryToTransaction(String sessionID, long transactionID, long categoryID)
            throws InvalidSessionIDException, ResourceNotFoundException {
        int userID = this.getUserID(sessionID);
        Transaction transaction = customORM.getTransaction(userID, transactionID);
        if (transaction != null) {
            Category category = customORM.getCategory(userID, categoryID);
            if (category != null) {
                customORM.unlinkTransactionFromAllCategories(userID, transactionID);
                customORM.linkTransactionToCategory(userID, transactionID, categoryID);
                transaction.setCategory(category);
                return transaction;
            } else {
                throw new ResourceNotFoundException();
            }
        } else {
            throw new ResourceNotFoundException();
        }
    }

    /**
     * Method used to retrieve the categories belonging to a certain user.
     *
     * @param sessionID The sessionID of the user.
     * @param limit     The maximum amount of categories to be fetched.
     * @param offset    The starting index to fetch categories.
     * @return An ArrayList of Category belonging to the user with sessionID.
     */
    public ArrayList<Category> getCategories(String sessionID, int limit, int offset)
            throws InvalidSessionIDException {
        int userID = this.getUserID(sessionID);
        return customORM.getCategories(userID, limit, offset);
    }

    /**
     * Method used to create a new Category for a certain user.
     *
     * @param sessionID The sessionID of the user.
     * @param name      The name of the to be created category.
     * @return The Category created by this method.
     */
    public Category postCategory(String sessionID, String name) throws InvalidSessionIDException {
        int userID = this.getUserID(sessionID);
        Category category = null;
        try {
            connection.setAutoCommit(false);
            customORM.increaseHighestCategoryID(userID);
            long categoryID = customORM.getHighestCategoryID(userID);
            connection.commit();
            connection.setAutoCommit(true);
            customORM.createCategory(userID, categoryID, name);
            category = customORM.getCategory(userID, categoryID);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return category;
    }

    /**
     * Method used to retrieve a certain Category of a certain user.
     *
     * @param sessionID  The sessionID of the user.
     * @param categoryID The categoryID of the Category that will be retrieved.
     * @return The Category with categoryID belonging to the user with sessionID.
     */
    public Category getCategory(String sessionID, long categoryID)
            throws InvalidSessionIDException, ResourceNotFoundException {
        int userID = this.getUserID(sessionID);
        Category category = customORM.getCategory(userID, categoryID);
        if (category != null) {
            return category;
        } else {
            throw new ResourceNotFoundException();
        }
    }

    /**
     * Method used to update a certain Category of a certain user.
     *
     * @param sessionID  The sessionID of the user.
     * @param categoryID The categoryID of the Category that will be updated.
     * @param name       The new name of the to be updated Category.
     * @return The Category updated by this method.
     */
    public Category putCategory(String sessionID, long categoryID, String name)
            throws InvalidSessionIDException, ResourceNotFoundException {
        int userID = this.getUserID(sessionID);
        Category category = customORM.getCategory(userID, categoryID);
        if (category != null) {
            if (name != null && !name.equals("")) {
                customORM.updateCategoryName(name, userID, categoryID);
            }
            category = customORM.getCategory(userID, categoryID);
        } else {
            throw new ResourceNotFoundException();
        }
        return category;
    }

    /**
     * Method used to remove a certain Category of a certain user.
     *
     * @param sessionID  The sessionID of the user.
     * @param categoryID The categoryID of the Category that will be deleted.
     */
    public void deleteCategory(String sessionID, long categoryID)
            throws InvalidSessionIDException, ResourceNotFoundException {
        int userID = this.getUserID(sessionID);
        Category category = customORM.getCategory(userID, categoryID);
        if (category != null) {
            customORM.unlinkCategoryFromAllTransactions(userID, categoryID);
            customORM.deleteCategory(userID, categoryID);
        } else {
            throw new ResourceNotFoundException();
        }
    }

    /**
     * Method used to retrieve all the CategoryRules of a certain user.
     *
     * @param sessionID The sessionID of the user.
     * @return A list of all CategoryRules of the user.
     * @throws InvalidSessionIDException
     */
    public ArrayList<CategoryRule> getCategoryRules(String sessionID) throws InvalidSessionIDException {
        int userID = this.getUserID(sessionID);
        ArrayList<CategoryRule> categoryRules = customORM.getCategoryRules(userID);
        return categoryRules;
    }

    /**
     * Method used to create a CategoryRule for a certain user.
     * If the applyOnHistory boolean is true, this will check for all Transactions if the rule should be applied to it.
     *
     * @param sessionID      The sessionID of the user.
     * @param description    The description of the to be created CategoryRule.
     * @param iBan           The Iban of the to be created CategoryRule.
     * @param type           The type of the to be created CategoryRule.
     * @param categoryID     The category ID of the to be created CategoryRule.
     * @param applyOnHistory Whether the rule should be applied to already existing transactions of the user.
     * @return The created categoryRule.
     * @throws InvalidSessionIDException
     * @throws ResourceNotFoundException
     */
    public CategoryRule postCategoryRule(String sessionID, String description, String iBan, String type, long categoryID,
                                         boolean applyOnHistory) throws InvalidSessionIDException, ResourceNotFoundException {
        int userID = this.getUserID(sessionID);
        CategoryRule categoryRule = null;
        try {
            // if category with categoryID doesnt exist, throw resourcenotfoundexception.
            if (categoryID <= 0 && categoryID > customORM.getHighestCategoryID(userID)) {
                throw new ResourceNotFoundException();
            }
            connection.setAutoCommit(false);
            customORM.increaseHighestCategoryRuleID(userID);
            long categoryRuleID = customORM.getHighestCategoryRuleID(userID);
            connection.commit();
            connection.setAutoCommit(true);
            customORM.createCategoryRule(userID, categoryRuleID, description, iBan, type, categoryID, applyOnHistory);
            categoryRule = customORM.getCategoryRule(userID, categoryRuleID);
            if (applyOnHistory) {
                ArrayList<Transaction> transactions = customORM.getAllTransactions(userID);
                for (Transaction t : transactions) {
                    if (transactionMatchesCategoryRule(t, categoryRule)) {
                        assignCategoryToTransaction(sessionID, t.getID(), categoryRuleID);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categoryRule;
    }

    /**
     * Method used to check if a Transaction belongs to a CategoryRule.
     *
     * @param t The Transaction that is checked.
     * @param c The CategoryRule that is checked.
     * @return true if the Transaction matches the CategoryRule, false otherwise.
     */
    public boolean transactionMatchesCategoryRule(Transaction t, CategoryRule c) {
        boolean match = false;
        if (t.getType().contains(c.getType())) {
            if (t.getDescription().contains(c.getDescription()) && t.getExternalIBAN().contains(c.getiBAN())) {
                match = true;
            }
        }
        return match;
    }

    /**
     * Method used to retrieve a specific CategoryRule of a user.
     *
     * @param sessionID      The sessionID of the user.
     * @param categoryRuleID The ID of the CategoryRule.
     * @return The CategoryRule with the ID.
     * @throws InvalidSessionIDException
     * @throws ResourceNotFoundException
     */
    public CategoryRule getCategoryRule(String sessionID, Long categoryRuleID) throws InvalidSessionIDException, ResourceNotFoundException {
        int userID = this.getUserID(sessionID);
        CategoryRule categoryRule = customORM.getCategoryRule(userID, categoryRuleID);
        if (categoryRule != null) {
            return categoryRule;
        } else {
            throw new ResourceNotFoundException();
        }
    }

    /**
     * Method used to update a certain CategoryRule of a certain User.
     *
     * @param sessionID      The sessionID of the to be updated CategoryRule.
     * @param categoryRuleID The CategoryRule ID of the to be updated CategoryRule.
     * @param description    The description of the to be updated CategoryRule.
     * @param iBan           The iban of the to be updated CategoryRule.
     * @param type           The type of the to be updated CategoryRule.
     * @param categoryID     The category ID of the to be updated CategoryRule.
     * @return The updated CategoryRule.
     * @throws InvalidSessionIDException
     * @throws ResourceNotFoundException
     */
    public CategoryRule putCategoryRule(String sessionID, Long categoryRuleID, String description, String iBan, String type,
                                        Long categoryID)
            throws InvalidSessionIDException, ResourceNotFoundException {
        int userID = this.getUserID(sessionID);
        CategoryRule categoryRule = customORM.getCategoryRule(userID, categoryRuleID);
        if (categoryRule != null) {
            if (description != null) {
                customORM.updateCategoryRuleDescription(description, userID, categoryRuleID);
            }
            if (iBan != null) {
                customORM.updateCategoryRuleIBAN(iBan, userID, categoryRuleID);
            }
            if (type != null) {
                customORM.updateCategoryRuleType(type, userID, categoryRuleID);
            }
            if (categoryID != null && categoryID > 0) {
                customORM.updateCategoryRuleCategory(categoryID, userID, categoryRuleID);
            }
            categoryRule = customORM.getCategoryRule(userID, categoryRuleID);
        } else {
            throw new ResourceNotFoundException();
        }
        return categoryRule;
    }

    /**
     * Method used to delete a CategoryRule from a specific user.
     *
     * @param sessionID      The sessionID of the user.
     * @param categoryRuleID The categoryRule ID of the to be deleted CategoryRule.
     * @throws InvalidSessionIDException
     * @throws ResourceNotFoundException
     */
    public void deleteCategoryRule(String sessionID, long categoryRuleID) throws InvalidSessionIDException, ResourceNotFoundException {
        int userID = this.getUserID(sessionID);
        CategoryRule categoryRule = customORM.getCategoryRule(userID, categoryRuleID);
        if (categoryRule != null) {
            customORM.deleteCategoryRule(userID, categoryRuleID);
        } else {
            throw new ResourceNotFoundException();
        }
    }

    /**
     * Method used to create and retrieve a new Session.
     *
     * @return A new Session.
     */
    public Session getSession() {
        /*
        In an exceptionally rare case, it can happen that there will be two users with the same sessionID.
        This only happens when two sessionIDs are generated at exactly the same time and they are the same.
         */
        String sessionID = "";
        boolean unique = false;
        while (!unique) {
            sessionID = UUID.randomUUID().toString();
            if (customORM.getUserID(sessionID) == -1) {
                unique = true;
            }
        }
        customORM.createNewUser(sessionID);
        return new Session(sessionID);
    }

    /**
     * Method used to populate a Transaction object with a Category object.
     *
     * @param transaction The Transaction object that will be populated by a Category object.
     */
    private void populateCategory(int userID, Transaction transaction) {
        if (transaction != null) {
            long categoryID = customORM.getCategoryIDByTransactionID(userID, transaction.getID());
            Category category = customORM.getCategory(userID, categoryID);
            transaction.setCategory(category);
        }
    }

    /**
     * Method used to retrieve the userID belonging to a certain sessionID.
     *
     * @param sessionID The sessionID from which the belonging userID will be retrieved.
     * @return The userID belonging to sessionID.
     * @throws InvalidSessionIDException
     */
    private int getUserID(String sessionID) throws InvalidSessionIDException {
        int userID = customORM.getUserID(sessionID);
        if (userID == -1) {
            throw new InvalidSessionIDException();
        }
        return userID;
    }

    /**
     * Method used to retrieve balance history over the specified intervals.
     *
     * @param sessionID       The sessionID of the to be retrieved intervals.
     * @param intervalsNumber The number of intervals to be retrieved.
     * @param intervalTime    The type of the to be retrieved intervals.
     * @return The specified intervals.
     * @throws InvalidSessionIDException
     */
    public ArrayList<Interval> getIntervals(String sessionID, int intervalsNumber, String intervalTime) throws InvalidSessionIDException {
        int userID = getUserID(sessionID);
        ArrayList<Interval> intervals = new ArrayList<>();
        Calendar c = new GregorianCalendar();
        c.setTimeInMillis(Instant.now().getEpochSecond());
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);

        // interval from 11 to 12 o'clock, then start is 11, end is 12. Same with other intervalTimes.
        if (intervalTime.equals("hour")) {
            c.add(Calendar.HOUR_OF_DAY, 1);
            for (int i = 0; i < intervalsNumber; i++) {
                long endIntervalTimeMillis = c.getTimeInMillis();
                c.add(Calendar.HOUR_OF_DAY, -1);
                long startIntervalTimeMillis = c.getTimeInMillis();
                intervals.add(calculateInterval(userID, startIntervalTimeMillis, endIntervalTimeMillis));
            }

        } else if (intervalTime.equals("day")) {
            c.set(Calendar.HOUR, 0);
            c.add(Calendar.DAY_OF_YEAR, 1);
            for (int i = 0; i < intervalsNumber; i++) {
                long endIntervalTimeMillis = c.getTimeInMillis();
                c.add(Calendar.DAY_OF_YEAR, -1);
                long startIntervalTimeMillis = c.getTimeInMillis();
                intervals.add(calculateInterval(userID, startIntervalTimeMillis, endIntervalTimeMillis));
            }

        } else if (intervalTime.equals("week")) {
            c.set(Calendar.HOUR, 0);
            c.set(Calendar.DAY_OF_WEEK, 0);
            c.add(Calendar.WEEK_OF_YEAR, 1);
            for (int i = 0; i < intervalsNumber; i++) {
                long endIntervalTimeMillis = c.getTimeInMillis();
                c.add(Calendar.WEEK_OF_YEAR, -1);
                long startIntervalTimeMillis = c.getTimeInMillis();
                intervals.add(calculateInterval(userID, startIntervalTimeMillis, endIntervalTimeMillis));
            }

        } else if (intervalTime.equals("month")) {
            c.set(Calendar.HOUR, 0);
            c.set(Calendar.DAY_OF_MONTH, 0);
            c.add(Calendar.MONTH, 1);
            for (int i = 0; i < intervalsNumber; i++) {
                long endIntervalTimeMillis = c.getTimeInMillis();
                c.add(Calendar.MONTH, -1);
                long startIntervalTimeMillis = c.getTimeInMillis();
                intervals.add(calculateInterval(userID, startIntervalTimeMillis, endIntervalTimeMillis));
            }

        } else if (intervalTime.equals("year")) {
            c.set(Calendar.HOUR, 0);
            c.set(Calendar.DAY_OF_YEAR, 0);
            c.set(Calendar.MONTH, 0);
            c.add(Calendar.YEAR, 1);
            for (int i = 0; i < intervalsNumber; i++) {
                long endIntervalTimeMillis = c.getTimeInMillis();
                c.add(Calendar.YEAR, -1);
                long startIntervalTimeMillis = c.getTimeInMillis();
                intervals.add(calculateInterval(userID, startIntervalTimeMillis, endIntervalTimeMillis));
            }
        }

        return intervals;
    }

    /**
     * Method used to calculate values for an interval between start en end time.
     *
     * @param userID                    The ID of the specified user.
     * @param startIntervalTimeMillis   The start time of the interval.
     * @param endIntervalTimeMillis     The end time of the interval.
     * @return  The balancehistory between start and end time, in an interval object.
     */
    public Interval calculateInterval(int userID, long startIntervalTimeMillis, long endIntervalTimeMillis) {
        ArrayList<BalanceHistoryPoint> balanceHistoryPoints = customORM.getBalanceHistoryPointsInRange(userID, startIntervalTimeMillis, endIntervalTimeMillis);
        float open;
        float close;
        float high = 0;
        float low = 0;
        float volume = 0;
        long timeStampSeconds = startIntervalTimeMillis / 1000;
        if (balanceHistoryPoints.size() > 0) {
            open = balanceHistoryPoints.get(0).getOpen();
            close = balanceHistoryPoints.get(balanceHistoryPoints.size() - 1).getClose();
            high = open;
            low = open;
            for (BalanceHistoryPoint b : balanceHistoryPoints) {
                // Assuming that the arraylist is sorted by time_stamp_millis ASCENDING by SQL statement retrieving the data.
                if (b.getClose() > high) {
                    high = b.getClose();
                }
                if (b.getClose() < low) {
                    low = b.getClose();
                }
                volume += b.getVolume();
            }
        } else {
            close = customORM.getPreviousBalanceHistoryPointClose(userID, startIntervalTimeMillis);
            open = close;
        }
        Interval interval = new Interval(open, close, high, low, volume, timeStampSeconds);
        return interval;
    }
}

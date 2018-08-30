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

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        dateFormat.setLenient(false);
        long transactionTimestampMillis = -1;
        try {
            transactionTimestampMillis = dateFormat.parse(date.trim()).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        long previousTimeMillis = customORM.getCurrentTimeMillis(userID);
        if (previousTimeMillis < transactionTimestampMillis) {
            updateSavingGoals(userID, transactionTimestampMillis, previousTimeMillis, externalIBAN);
        }

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

            if (previousTimeMillis < transactionTimestampMillis && type.equals("deposit")) {
                updatePaymentRequests(userID, amount, transactionID);

            }

            setBalanceHistoryPoint(transactionTimestampMillis, amount, type, userID);
            this.populateCategory(userID, transaction);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transaction;
    }

    /**
     * Method used to update the payment requests when a transaction comes in.
     *
     * @param user_id       The ID of the user.
     * @param amount        The amount of the transaction.
     * @param transactionID The ID of the transaction.
     */
    private void updatePaymentRequests(int user_id, float amount, long transactionID) {
        ArrayList<PaymentRequest> paymentRequests = customORM.getOpenPaymentRequests(user_id);
        for (PaymentRequest p : paymentRequests) {
            if (p.getAmount() == amount) {
                customORM.linkTransactionToPaymentRequest(user_id, transactionID, p.getId());
                if (customORM.paymentRequestIsFilled(user_id, p.getId())) {
                    customORM.updatePaymentRequestFilled(user_id, p.getId(), true);
                }
                break;
            }
        }
    }

    /**
     * Method for determining if the first of the month has passed between the current to be created transaction and
     * the current stored system time in the database. If the first has passed, all the savinggoals will be checked if
     * they should create a transaction to save money on their balance.
     *
     * @param userID
     * @param currentTimestampMillis
     * @param previousTimeMillis
     * @param externalIBAN
     */
    private void updateSavingGoals(int userID, long currentTimestampMillis, long previousTimeMillis, String externalIBAN) {
        customORM.setCurrentTimeMillis(userID, currentTimestampMillis);

        Calendar currentCal = new GregorianCalendar();
        currentCal.setTimeInMillis(currentTimestampMillis);

        Calendar previousCal = new GregorianCalendar();
        previousCal.setTimeInMillis(previousTimeMillis);

        int monthsDiff = currentCal.get(Calendar.MONTH) - previousCal.get(Calendar.MONTH);
        int yearsInBetween = currentCal.get(Calendar.YEAR) - previousCal.get(Calendar.YEAR);
        monthsDiff += yearsInBetween * 12;

        // Assuming the SQL statement sorted it by saving_goal_id ASC (so in order of creation)
        ArrayList<SavingGoal> savingGoals = customORM.getSavingGoals(userID);

        if (monthsDiff > 0 && savingGoals.size() > 0 && customORM.getTransactions(userID, 1, 0).size() > 0) {
            previousCal.set(Calendar.MILLISECOND, 0);
            previousCal.set(Calendar.SECOND, 0);
            previousCal.set(Calendar.MINUTE, 0);
            previousCal.set(Calendar.HOUR, 0);
            previousCal.set(Calendar.DAY_OF_MONTH, 0);
            previousCal.add(Calendar.MONTH, 1);

            for (int i = 0; i < monthsDiff; i++) {
                for (SavingGoal s : savingGoals) {
                    float previousClose = customORM.getPreviousBalanceHistoryPointClose(userID, currentTimestampMillis);
                    if (s.getBalance() < s.getGoal() && previousClose > s.getMinBalanceRequired()) {
                        try {
                            connection.setAutoCommit(false);
                            customORM.increaseHighestTransactionID(userID);
                            long transactionID = customORM.getHighestTransactionID(userID);
                            connection.commit();
                            connection.setAutoCommit(true);

                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                            String date = null;
                            boolean noDateFound = true;
                            long savingGoalTransactionTimeStamp = -1;

                            while (noDateFound) {
                                long savingGoalTransactionTimeMillis = previousCal.getTimeInMillis();
                                if (!customORM.balanceHistoryPointExists(userID, savingGoalTransactionTimeMillis)) {
                                    date = dateFormat.format(previousCal.getTime());
                                    savingGoalTransactionTimeStamp = savingGoalTransactionTimeMillis;
                                    noDateFound = false;
                                }
                                previousCal.add(Calendar.MILLISECOND, 1);
                            }

                            float amount = s.getSavePerMonth();
                            String description = "Saving money for goal: " + s.getName();
                            String type = "withdrawal";

                            customORM.createTransaction(userID, transactionID, date, amount, description, externalIBAN, type);
                            setBalanceHistoryPoint(savingGoalTransactionTimeStamp, amount, type, userID);

                            float newBalance = s.getBalance() + amount;
                            s.setBalance(newBalance);
                            customORM.updateSavingGoalBalance(userID, s.getId(), newBalance);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }
                previousCal.set(Calendar.MILLISECOND, 0);
                previousCal.add(Calendar.MONTH, 1);
            }
        }
    }


    /**
     * Method used to create a balance history point in the database.
     *
     * @param timestampMillis The timestamp in milliseconds of the transaction.
     * @param amount          The amount of the transaction.
     * @param type            The type of the transaction (deposit or withdrawal).
     * @param userID          The ID of the specified user.
     */
    private void setBalanceHistoryPoint(long timestampMillis, float amount, String type, int userID) {
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
     * @param userID          The ID of the specified user.
     * @param timestampMillis The time stamp in milliseconds of the current transaction.
     * @param amount          The amount of the current transaction.
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
        c.setTimeInMillis(Instant.now().getEpochSecond() * 1000);
        c.set(Calendar.MILLISECOND, 0);
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
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.add(Calendar.DAY_OF_YEAR, 1);
            for (int i = 0; i < intervalsNumber; i++) {
                long endIntervalTimeMillis = c.getTimeInMillis();
                c.add(Calendar.DAY_OF_YEAR, -1);
                long startIntervalTimeMillis = c.getTimeInMillis();
                intervals.add(calculateInterval(userID, startIntervalTimeMillis, endIntervalTimeMillis));
            }

        } else if (intervalTime.equals("week")) {
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.DAY_OF_WEEK, 1);
            c.add(Calendar.WEEK_OF_YEAR, 1);
            for (int i = 0; i < intervalsNumber; i++) {
                long endIntervalTimeMillis = c.getTimeInMillis();
                c.add(Calendar.WEEK_OF_YEAR, -1);
                long startIntervalTimeMillis = c.getTimeInMillis();
                intervals.add(calculateInterval(userID, startIntervalTimeMillis, endIntervalTimeMillis));
            }

        } else if (intervalTime.equals("month")) {
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.DAY_OF_MONTH, 1);
            c.add(Calendar.MONTH, 1);
            for (int i = 0; i < intervalsNumber; i++) {
                long endIntervalTimeMillis = c.getTimeInMillis();
                c.add(Calendar.MONTH, -1);
                long startIntervalTimeMillis = c.getTimeInMillis();
                intervals.add(calculateInterval(userID, startIntervalTimeMillis, endIntervalTimeMillis));
            }

        } else if (intervalTime.equals("year")) {
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.DAY_OF_YEAR, 1);
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
     * @param userID                  The ID of the specified user.
     * @param startIntervalTimeMillis The start time of the interval.
     * @param endIntervalTimeMillis   The end time of the interval.
     * @return The balancehistory between start and end time, in an interval object.
     */
    public Interval calculateInterval(int userID, long startIntervalTimeMillis, long endIntervalTimeMillis) {
        ArrayList<BalanceHistoryPoint> balanceHistoryPoints = customORM.getBalanceHistoryPointsInRange(userID, startIntervalTimeMillis, endIntervalTimeMillis);
        float open;
        float close;
        float high;
        float low;
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
            high = close;
            low = close;
        }
        Interval interval = new Interval(open, close, high, low, volume, timeStampSeconds);
        return interval;
    }

    /**
     * Method used to retrieve the savinggoals of a user.
     *
     * @param sessionID The sessionID of the specified user.
     * @return All savinggoals of the specified user.
     * @throws InvalidSessionIDException
     */
    public ArrayList<SavingGoal> getSavingGoals(String sessionID) throws InvalidSessionIDException {
        int userID = getUserID(sessionID);
        ArrayList<SavingGoal> savingGoals = customORM.getSavingGoals(userID);
        return savingGoals;
    }


    /**
     * Method used to create a new savinggoal for the specified user.
     *
     * @param sessionID          The sessionID of the specified user.
     * @param name               The name of the to be created savinggoal.
     * @param goal               The goal of the to be created savinggoal.
     * @param savePerMonth       The amount to be saved per month of the to be created savinggoal.
     * @param minBalanceRequired The minimal balance the user should have for the to be created savingoal to set money aside.
     * @return The created savinggoal.
     * @throws InvalidSessionIDException
     */
    public SavingGoal postSavingGoal(String sessionID, String name, float goal, float savePerMonth, float minBalanceRequired)
            throws InvalidSessionIDException {
        int userID = getUserID(sessionID);
        SavingGoal savingGoal = null;
        try {
            connection.setAutoCommit(false);
            customORM.increaseHighestSavingGoalID(userID);
            long savingGoalID = customORM.getHighestSavingGoalID(userID);
            connection.commit();
            connection.setAutoCommit(true);
            customORM.createSavingGoal(userID, savingGoalID, name, goal, savePerMonth, minBalanceRequired);
            savingGoal = customORM.getSavingGoal(userID, savingGoalID);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return savingGoal;
    }

    /**
     * Method used to delete a savingoal of the specified user.
     *
     * @param sessionID    The sessionID of the user.
     * @param savingGoalID The ID of the to be deleted savinggoal.
     * @throws InvalidSessionIDException
     * @throws ResourceNotFoundException
     */
    public void deleteSavingGoal(String sessionID, long savingGoalID) throws InvalidSessionIDException, ResourceNotFoundException {
        int userID = getUserID(sessionID);
        SavingGoal savingGoal = customORM.getSavingGoal(userID, savingGoalID);
        if (savingGoal != null) {
            long currentTimeStamp = System.currentTimeMillis();
            try {
                connection.setAutoCommit(false);
                customORM.increaseHighestTransactionID(userID);
                long transactionID = customORM.getHighestTransactionID(userID);
                connection.commit();
                connection.setAutoCommit(true);

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                String date = dateFormat.format(new Date(currentTimeStamp));
                float amount = savingGoal.getBalance();
                String description = "Saving goal: " + savingGoal.getName() + " has been met and deleted";
                String externalIBAN = "internal transaction";
                String type = "deposit";

                customORM.createTransaction(userID, transactionID, date, amount, description, externalIBAN, type);
                setBalanceHistoryPoint(currentTimeStamp, amount, type, userID);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            customORM.deleteSavingGoal(userID, savingGoalID);

        } else {
            throw new ResourceNotFoundException();
        }
    }

    /**
     * Method used to retrieve all the payment requests of a user.
     *
     * @param sessionID     The sessionID of the user.
     * @return  A list of all the payment requests belonging to the specified user.
     * @throws InvalidSessionIDException
     */
    @Override
    public ArrayList<PaymentRequest> getPaymentRequests(String sessionID) throws InvalidSessionIDException {
        int user_id = getUserID(sessionID);
        ArrayList<PaymentRequest> paymentRequests = customORM.getPaymentRequests(user_id);
        return paymentRequests;
    }

    /**
     * Method used to create a new payment request for a user.
     *
     * @param sessionID             The sessionID of the user.
     * @param description           The description of the to be created payment request.
     * @param due_date              The due date of the to be created payment request.
     * @param amount                The amount of the to be created payment request.
     * @param number_of_requests    The number of requests of the to be created payment request.
     * @return  The newly created payment request.
     * @throws InvalidSessionIDException
     */
    @Override
    public PaymentRequest postPaymentRequest(String sessionID, String description, String due_date, float amount, long number_of_requests) throws InvalidSessionIDException {
        int user_id = getUserID(sessionID);
        PaymentRequest paymentRequest = null;
        try {
            connection.setAutoCommit(false);
            customORM.increaseHighestPaymentRequestID(user_id);
            long paymentRequestID = customORM.getHighestPaymentRequestID(user_id);
            connection.commit();
            connection.setAutoCommit(true);
            customORM.createPaymentRequest(user_id, paymentRequestID, description, due_date, amount, number_of_requests);

            paymentRequest = new PaymentRequest(paymentRequestID, description, due_date, amount, number_of_requests, false, new ArrayList<>());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return paymentRequest;
    }
}

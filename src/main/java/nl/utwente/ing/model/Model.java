package nl.utwente.ing.model;

import nl.utwente.ing.exception.InvalidSessionIDException;
import nl.utwente.ing.exception.ResourceNotFoundException;
import nl.utwente.ing.model.bean.*;

import java.util.ArrayList;

/**
 * The Model interface.
 * Consists of method specifications to facilitate the controller part of the application to interact with
 * transactions and categories.
 *
 * @author Daan Kooij
 */
public interface Model {

    /**
     * Method used to retrieve the transactions belonging to a certain user.
     *
     * @param sessionID    The sessionID of the user.
     * @param categoryName The category to be filtered on (empty String if no filter).
     * @param limit        The maximum amount of transactions to be fetched.
     * @param offset       The starting index to fetch transactions.
     * @return An ArrayList of Transaction belonging to the user with sessionID.
     */
    ArrayList<Transaction> getTransactions(String sessionID, String categoryName, int limit, int offset)
            throws InvalidSessionIDException;

    /**
     * Method used to create a new Transaction for a certain user.
     *
     * @param sessionID    The sessionID of the user.
     * @param date         The date of the to be created Transaction.
     * @param amount       The amount of the to be created Transaction.
     * @param description  The description of the to be created Transaction
     * @param externalIBAN The external IBAN of the to be created Transaction.
     * @param type         The type of the to be created Transaction.
     * @param categoryID   The categoryID of the Category that will be assigned to the to be created Transaction
     *                     (0 if no Category).
     * @return The Transaction created by this method.
     */
    Transaction postTransaction(String sessionID, String date, float amount, String description, String externalIBAN, String type,
                                long categoryID) throws InvalidSessionIDException, ResourceNotFoundException;

    /**
     * Method used to retrieve a certain Transaction of a certain user.
     *
     * @param sessionID     The sessionID of the user.
     * @param transactionID The transactionID of the Transaction that will be retrieved.
     * @return The Transaction with transactionID belonging to the user with sessionID.
     */
    Transaction getTransaction(String sessionID, long transactionID)
            throws InvalidSessionIDException, ResourceNotFoundException;

    /**
     * Method used to update a certain Transaction of a certain user.
     *
     * @param sessionID     The sessionID of the user.
     * @param transactionID The transactionID of the Transaction that will be updated.
     * @param date          The new date of the to be updated Transaction.
     * @param amount        The new amount of the to be updated Transaction.
     * @param description   The new description of the to be updated Transaction.
     * @param externalIBAN  The new external IBAN of the to be updated Transaction.
     * @param type          The new type of the to be updated Transaction.
     * @param categoryID    The new categoryID of the Category that will be assigned to the to be updated Transaction
     *                      (0 if no Category).
     * @return The Transaction updated by this method.
     */
    Transaction putTransaction(String sessionID, long transactionID, String date, float amount, String description,
                               String externalIBAN, String type, long categoryID)
            throws InvalidSessionIDException, ResourceNotFoundException;

    /**
     * Method used to delete a certain Transaction of a certain user.
     *
     * @param sessionID     The sessionID of the user.
     * @param transactionID The transactionID of the Transaction that will be deleted.
     */
    void deleteTransaction(String sessionID, long transactionID)
            throws InvalidSessionIDException, ResourceNotFoundException;

    /**
     * Method used to assign a Category to a Transaction.
     * Currently, first all other assigned Category objects to Transaction are unassigned.
     *
     * @param sessionID     The sessionID of the user.
     * @param transactionID The transactionID of the Transaction to which the Category will be assigned.
     * @param categoryID    The categoryID of the Category which will be assigned to the Transaction.
     * @return The Transaction to which the Category is assigned.
     */
    Transaction assignCategoryToTransaction(String sessionID, long transactionID, long categoryID)
            throws InvalidSessionIDException, ResourceNotFoundException;

    /**
     * Method used to retrieve the categories belonging to a certain user.
     *
     * @param sessionID The sessionID of the user.
     * @param limit     The maximum amount of categories to be fetched.
     * @param offset    The starting index to fetch categories.
     * @return An ArrayList of Category belonging to the user with sessionID.
     */
    ArrayList<Category> getCategories(String sessionID, int limit, int offset) throws InvalidSessionIDException;

    /**
     * Method used to create a new Category for a certain user.
     *
     * @param sessionID The sessionID of the user.
     * @param name      The name of the to be created category.
     * @return The Category created by this method.
     */
    Category postCategory(String sessionID, String name) throws InvalidSessionIDException;

    /**
     * Method used to retrieve a certain Category of a certain user.
     *
     * @param sessionID  The sessionID of the user.
     * @param categoryID The categoryID of the Category that will be retrieved.
     * @return The Category with categoryID belonging to the user with sessionID.
     */
    Category getCategory(String sessionID, long categoryID) throws InvalidSessionIDException, ResourceNotFoundException;

    /**
     * Method used to update a certain Category of a certain user.
     *
     * @param sessionID  The sessionID of the user.
     * @param categoryID The categoryID of the Category that will be updated.
     * @param name       The new name of the to be updated Category.
     * @return The Category updated by this method.
     */
    Category putCategory(String sessionID, long categoryID, String name)
            throws InvalidSessionIDException, ResourceNotFoundException;

    /**
     * Method used to remove a certain Category of a certain user.
     *
     * @param sessionID  The sessionID of the user.
     * @param categoryID The categoryID of the Category that will be deleted.
     */
    void deleteCategory(String sessionID, long categoryID) throws InvalidSessionIDException, ResourceNotFoundException;

    /**
     * Method used to retrieve all the CategoryRules of a certain user.
     *
     * @param sessionID The sessionID of the user.
     * @return A list of all CategoryRules of the user.
     * @throws InvalidSessionIDException
     */
    ArrayList<CategoryRule> getCategoryRules(String sessionID) throws InvalidSessionIDException;

    /**
     * Method used to create a CategoryRule for a certain user.
     *
     * @param sessionID         The sessionID of the user.
     * @param description       The description of the to be created CategoryRule.
     * @param iBan              The Iban of the to be created CategoryRule.
     * @param type              The type of the to be created CategoryRule.
     * @param categoryID        The category ID of the to be created CategoryRule.
     * @param applyOnHistory    Whether the rule should be applied to already existing transactions of the user.
     * @return  The created categoryRule.
     * @throws InvalidSessionIDException
     * @throws ResourceNotFoundException
     */
    CategoryRule postCategoryRule(String sessionID, String description, String iBan, String type, long categoryID,
                                  boolean applyOnHistory) throws InvalidSessionIDException, ResourceNotFoundException;

    /**
     * Method used to retrieve a specific CategoryRule of a user.
     *
     * @param sessionID         The sessionID of the user.
     * @param categoryRuleID    The ID of the CategoryRule.
     * @return  The CategoryRule with the ID.
     * @throws InvalidSessionIDException
     * @throws ResourceNotFoundException
     */
    CategoryRule getCategoryRule(String sessionID, Long categoryRuleID) throws InvalidSessionIDException,
            ResourceNotFoundException;

    /**
     * Method used to update a certain CategoryRule of a certain User.
     *
     * @param sessionID         The sessionID of the to be updated CategoryRule.
     * @param categoryRuleID    The CategoryRule ID of the to be updated CategoryRule.
     * @param description       The description of the to be updated CategoryRule.
     * @param iBan              The iban of the to be updated CategoryRule.
     * @param type              The type of the to be updated CategoryRule.
     * @param categoryID        The category ID of the to be updated CategoryRule.
     * @return  The updated CategoryRule.
     * @throws InvalidSessionIDException
     * @throws ResourceNotFoundException
     */
    CategoryRule putCategoryRule(String sessionID, Long categoryRuleID, String description, String iBan, String type,
                                 Long categoryID)
            throws InvalidSessionIDException, ResourceNotFoundException;

    /**
     * Method used to delete a CategoryRule from a specific user.
     *
     * @param sessionID         The sessionID of the user.
     * @param categoryRuleID    The categoryRule ID of the to be deleted CategoryRule.
     * @throws InvalidSessionIDException
     * @throws ResourceNotFoundException
     */
    void deleteCategoryRule(String sessionID, long categoryRuleID) throws InvalidSessionIDException, ResourceNotFoundException;

    /**
     * Method used to retrieve balance history over the specified intervals.
     *
     * @param sessionID         The sessionID of the to be retrieved intervals.
     * @param intervalsNumber   The number of intervals to be retrieved.
     * @param intervalTime      The type of the to be retrieved intervals.
     * @return  The specified intervals.
     * @throws InvalidSessionIDException
     */
    ArrayList<Interval> getIntervals(String sessionID, int intervalsNumber, String intervalTime) throws InvalidSessionIDException;

    ArrayList<SavingGoal> getSavingGoals(String sessionID) throws InvalidSessionIDException;

    SavingGoal postSavingGoal(String sessionID, String name, float goal, float savePerMonth, float minBalanceRequired)
            throws InvalidSessionIDException;

    void deleteSavingGoal(String sessionID, long savingGoalID) throws InvalidSessionIDException, ResourceNotFoundException;

    /**
     * Method used to create and retrieve a new Session.
     *
     * @return A new Session.
     */
    Session getSession();

    ArrayList<PaymentRequest> getPaymentRequests(String sessionID) throws InvalidSessionIDException;

    PaymentRequest postPaymentRequest(String sessionID, String description, String due_date, float amount, long number_of_requests) throws InvalidSessionIDException;

    ArrayList<Message> getMessages(String sessionID) throws InvalidSessionIDException;

    void setMessageToRead(String sessionID, long messageIDLong) throws ResourceNotFoundException, InvalidSessionIDException;
}

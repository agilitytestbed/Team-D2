package nl.utwente.ing.model.bean;

/**
 * The CategoryRule class.
 * Used to store information about a CategoryRule.
 *
 * @author Sam Witt
 */
public class CategoryRule {

    private long id;
    private String description;
    private String iBAN;
    private String type;
    private long category_id;
    private boolean applyOnHistory;

    /**
     * An empty constructor of CategoryRule.
     * Used by the Spring framework.
     */
    public CategoryRule() {

    }

    /**
     * A constructor of CategoryRule.
     *
     * @param id                The id of the to be created CategoryRule.
     * @param description       The description of the to be created CategoryRule.
     * @param iBAN              The IBAN of the to be created CategoryRule.
     * @param type              The type of the to be created CategoryRule.
     * @param category_id       The category id of the to be created CategoryRule.
     * @param applyOnHistory    The setting of apply on history of the to be created CategoryRule.
     */
    public CategoryRule(long id, String description, String iBAN, String type, long category_id, boolean applyOnHistory) {
        this.id = id;
        this.description = description;
        this.iBAN = iBAN;
        this.type = type;
        this.category_id = category_id;
        this.applyOnHistory = applyOnHistory;
    }

    /**
     * Method used to retrieve the ID of CategoryRule.
     *
     * @return The ID of CategoryRule.
     */
    public long getId() {
        return id;
    }

    /**
     * Method used to update the ID of CategoryRule
     *
     * @param id The new id of CategoryRule.
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Method used to retrieve the description of CategoryRule.
     *
     * @return The description of the CategoryRule.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Method used to update the description of CategoryRule.
     *
     * @param description The new description of CategoryRule.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Method used to retrieve the IBAN of CategoryRule.
     *
     * @return The IBAN of CategoryRule.
     */
    public String getiBAN() {
        return iBAN;
    }

    /**
     * Method used to update the IBAN of CategoryRule.
     *
     * @param iBAN The new IBAN of CategoryRule.
     */
    public void setiBAN(String iBAN) {
        this.iBAN = iBAN;
    }

    /**
     * Method used to retrieve the type of CategoryRule.
     *
     * @return The type of CategoryRule.
     */
    public String getType() {
        return type;
    }

    /**
     * Method used to update the type of CategoryRule.
     *
     * @param type The new type of CategoryRule.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Method used to retrieve the category ID of CategoryRule.
     *
     * @return The category ID of CategoryRule.
     */
    public long getCategory_id() {
        return category_id;
    }

    /**
     * Method used to update the category ID of CategoryRule.
     *
     * @param category_id The new category ID of CategoryRule.
     */
    public void setCategory_id(long category_id) {
        this.category_id = category_id;
    }

    /**
     * Method used to check if CategoryRule applies on history.
     *
     * @return Whether CategoryRule applies on history.
     */
    public boolean getApplyOnHistory() {
        return applyOnHistory;
    }

    /**
     * Method used to update apply on history of CategoryRule.
     *
     * @param applyOnHistory The new setting of apply on history of CategoryRule.
     */
    public void setApplyOnHistory(boolean applyOnHistory) {
        this.applyOnHistory = applyOnHistory;
    }
}

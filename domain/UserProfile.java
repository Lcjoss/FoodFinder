package FoodFinder.domain;

public class UserProfile {
    private int uid;
    private String username;
    private String password;
    // Stored preferences (for example, as comma‐separated strings)
    private String selectedCuisines;
    private String selectedMealTypes;
    private String selectedRestrictions;
    private String selectedFoodItems;

    public UserProfile(String username, String password) {
        // In a real application uid would be auto‐generated in the database
        this.username = username;
        this.password = password;
    }

    // Getters and setters
    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getSelectedCuisines() {
        return selectedCuisines;
    }

    public void setSelectedCuisines(String selectedCuisines) {
        this.selectedCuisines = selectedCuisines;
    }

    public String getSelectedMealTypes() {
        return selectedMealTypes;
    }

    public void setSelectedMealTypes(String selectedMealTypes) {
        this.selectedMealTypes = selectedMealTypes;
    }

    public String getSelectedRestrictions() {
        return selectedRestrictions;
    }

    public void setSelectedRestrictions(String selectedRestrictions) {
        this.selectedRestrictions = selectedRestrictions;
    }

    public String getSelectedFoodItems() {
        return selectedFoodItems;
    }

    public void setSelectedFoodItems(String selectedFoodItems) {
        this.selectedFoodItems = selectedFoodItems;
    }
}

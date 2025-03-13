package FoodFinder.session;

import FoodFinder.domain.UserProfile;

public class UserSession {
    private static UserProfile currentUser;

    public static void setCurrentUser(UserProfile user) {
        currentUser = user;
    }

    public static UserProfile getCurrentUser() {
        return currentUser;
    }
}

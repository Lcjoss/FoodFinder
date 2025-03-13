package FoodFinder.domain;

public class AdminProfile {
    private int adminId;
    private String username;
    private String password;

    public AdminProfile(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public int getAdminId() {
        return adminId;
    }

    public void setAdminId(int adminId) {
        this.adminId = adminId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}

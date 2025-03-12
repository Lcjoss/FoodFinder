package FoodFinder.domain;

import java.util.List;

public class Menu {
    public int id;
    public String type;
    public List<MenuItem> items;

    public Menu(int id, String type, List<MenuItem> items) {
        this.id = id;
        this.type = type;
        this.items = items;
    }
}

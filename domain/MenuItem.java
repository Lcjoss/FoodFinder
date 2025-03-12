package FoodFinder.domain;

import java.util.List;

public class MenuItem {
    public String name;
    public String recipe;
    public List<String> allergens;

    public MenuItem(String name, String recipe, List<String> allergens) {
        this.name = name;
        this.recipe = recipe;
        this.allergens = allergens;
    }
}

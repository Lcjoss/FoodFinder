package FoodFinder.domain;

public class Restaurant {
    public int id;
    public String name;
    public String cuisine;
    public String price;
    public String rating;
    public double lat;
    public double lon;

    public Restaurant(int id, String name, String cuisine, String price, String rating, double lat, double lon) {
        this.id = id;
        this.name = name;
        this.cuisine = cuisine;
        this.price = price;
        this.rating = rating;
        this.lat = lat;
        this.lon = lon;
    }

    @Override
    public String toString() {
        return name;
    }
}

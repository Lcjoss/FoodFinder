package FoodFinder.map;

import FoodFinder.domain.Restaurant;
import org.jxmapviewer.viewer.DefaultWaypoint;
import org.jxmapviewer.viewer.GeoPosition;

public class RestaurantWaypoint extends DefaultWaypoint {
    public Restaurant restaurant;

    public RestaurantWaypoint(Restaurant restaurant) {
        super(new GeoPosition(restaurant.lat, restaurant.lon));
        this.restaurant = restaurant;
    }
}

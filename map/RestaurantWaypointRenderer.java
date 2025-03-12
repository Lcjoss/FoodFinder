package FoodFinder.map;

import org.jxmapviewer.viewer.WaypointRenderer;
import org.jxmapviewer.JXMapViewer;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Color;
import java.awt.geom.Point2D;

public class RestaurantWaypointRenderer implements WaypointRenderer<RestaurantWaypoint> {
    @Override
    public void paintWaypoint(Graphics2D g, JXMapViewer map, RestaurantWaypoint wp) {
        Point2D pt = map.getTileFactory().geoToPixel(wp.getPosition(), map.getZoom());
        int x = (int) pt.getX();
        int y = (int) pt.getY();
        int radius = 10;
        g.setColor(new Color(0, 0, 139)); // dark blue
        g.fillOval(x - radius, y - radius, 2 * radius, 2 * radius);
        g.setColor(Color.WHITE);
        Font font = g.getFont().deriveFont(Font.BOLD, 16f);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        String text = "$";
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getAscent();
        g.drawString(text, x - textWidth / 2, y + textHeight / 2 - 2);
    }
}

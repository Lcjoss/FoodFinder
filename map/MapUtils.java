package FoodFinder.map;

import FoodFinder.domain.Restaurant;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.viewer.WaypointPainter;
import javax.swing.ToolTipManager;
import java.awt.event.MouseAdapter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MapUtils {
    public static JXMapViewer createMapViewer(List<Restaurant> restaurants) {
        JXMapViewer mapViewer = new JXMapViewer();
        mapViewer.setTileFactory(new DefaultTileFactory(new OSMTileFactoryInfo()));
        GeoPosition center = new GeoPosition(35.2704, -120.6631);
        mapViewer.setZoom(5);
        mapViewer.setAddressLocation(center);

        ToolTipManager.sharedInstance().setInitialDelay(0);
        ToolTipManager.sharedInstance().registerComponent(mapViewer);

        Set<RestaurantWaypoint> restaurantWaypoints = new HashSet<>();
        for (Restaurant res : restaurants) {
            restaurantWaypoints.add(new RestaurantWaypoint(res));
        }
        WaypointPainter<RestaurantWaypoint> waypointPainter = new WaypointPainter<>();
        waypointPainter.setWaypoints(restaurantWaypoints);
        waypointPainter.setRenderer(new RestaurantWaypointRenderer());
        mapViewer.setOverlayPainter(waypointPainter);

        MouseAdapter mia = new PanMouseInputListener(mapViewer);
        mapViewer.addMouseListener(mia);
        mapViewer.addMouseMotionListener(mia);
        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCursor(mapViewer));

        return mapViewer;
    }
}

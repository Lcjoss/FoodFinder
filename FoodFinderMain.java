package FoodFinder;

import FoodFinder.ui.FoodFinderFrame;

public class FoodFinderMain {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            FoodFinderFrame frame = new FoodFinderFrame();
            frame.setVisible(true);
        });
    }
}

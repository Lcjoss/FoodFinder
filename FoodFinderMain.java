package FoodFinder;

import FoodFinder.ui.FoodFinderFrame;
import FoodFinder.ui.MainFrame;
import com.sun.tools.javac.Main;

public class FoodFinderMain {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}

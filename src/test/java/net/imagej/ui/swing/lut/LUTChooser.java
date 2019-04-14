package net.imagej.ui.swing.lut;

import net.imagej.ImageJ;
import net.imagej.lut.LUTSelector;
import net.imglib2.display.ColorTable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.command.Command;

@Plugin(type=Command.class, menuPath = "Test>Choose LUT")
public class LUTChooser implements Command {

    @Parameter
    ColorTable ct;

    @Override
    public void run() {

    }

    public static void main (String... args)  {

        // Creates an IJ instance
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        ij.command().run(LUTSelector.class,true);

    }
}

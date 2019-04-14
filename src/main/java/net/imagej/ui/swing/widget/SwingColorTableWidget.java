/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2016 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package net.imagej.ui.swing.widget;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ChangeListener;

import net.imagej.display.ColorTables;
import net.imagej.lut.LUTService;
import net.imagej.widget.ColorTableWidget;
import net.imglib2.display.ColorTable;

import org.apache.commons.lang3.StringUtils;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.swing.widget.SwingInputWidget;
import org.scijava.widget.InputWidget;
import org.scijava.widget.WidgetModel;

/**
 * Swing implementation of ColorTable (LookUpTable, LUT) chooser widget.
 *
 * @author Nicolas Chiaruttini
 * @author Barry DeZonia
 */
@Plugin(type = InputWidget.class)
public class SwingColorTableWidget extends SwingInputWidget<ColorTable>
	implements ActionListener, ColorTableWidget<JPanel>
{

	// -- fields --

	private BufferedImage image; // Image of the current selected ColorTable

	private JButton choose; // JButton triggering the display of the Dialog to choose a ColorTable

	private ColorTable colorTable; // Current ColorTable selected

	final public static ColorTable DEFAULT_COLOR_TABLE = ColorTables.GRAYS;

	// -- Look Up Table (ColorTable) Service

	@Parameter
	LUTService lutService;

	// -- constructors --

	public SwingColorTableWidget() {
		image = new BufferedImage(256, 20, BufferedImage.TYPE_INT_RGB);
	}

	// -- ActionListener method --

	@Override
	public void actionPerformed(final ActionEvent e) {
		final ColorTable choice = selectColorTable();
		if (choice == null) return;
		colorTable = choice;
		updateModel();
		refreshWidget();
	}

	// -- InputWidget methods --

	@Override
	public ColorTable getValue() {
		return colorTable;
	}

	// -- WrapperPlugin methods --

	@Override
	public void set(final WidgetModel model) {
		super.set(model);

		getComponent().setLayout(new BoxLayout(getComponent(), BoxLayout.X_AXIS));
		choose = new JButton() {

			@Override
			public Dimension getMaximumSize() {
				return getPreferredSize();
			}
		};
		setToolTip(choose);
		getComponent().add(choose);
		choose.addActionListener(this);

		refreshWidget();
	}

	// -- Typed methods --

	@Override
	public boolean supports(final WidgetModel model) {
		return super.supports(model) && model.isType(ColorTable.class);
	}

	// -- AbstractUIInputWidget methods ---

	@Override
	public void doRefresh() {
		colorTable = (ColorTable) get().getValue();

		if (colorTable==null) {
			colorTable = DEFAULT_COLOR_TABLE;
		}
		fillImage(colorTable);
		choose.setIcon(new ImageIcon(image));
		choose.repaint();
	}

	// -- helpers --

	private void fillImage(ColorTable cTable) {
		for (int x = 0; x < 256; x++) {
			int r = cTable.get(0, x) & 0xff;
			int g = cTable.get(1, x) & 0xff;
			int b = cTable.get(2, x) & 0xff;
			int rgb = (r << 16) | (g << 8) | b;
			for (int y = 0; y < 20; y++) {
				image.setRGB(x, y, rgb);
			}
		}
	}

	//----- Helper class

	public ColorTable selectColorTable() {

		colorTable = (ColorTable) get().getValue();

		if (colorTable==null) {
			colorTable = DEFAULT_COLOR_TABLE;
		}

		JLabel lutPreview = new JLabel();

		ButtonGroup group = new ButtonGroup();
		JPanel radioPanel = new JPanel(new GridLayout(0, 1));

		fillImage(colorTable);
		lutPreview.setIcon(new ImageIcon(image));

		// Needs a Change Listener because focus change with keys do not work with ActionEvent
		ChangeListener lutChanged = evt -> {
			JRadioButton jrb = (JRadioButton) evt.getSource();
			ButtonModel bm = jrb.getModel();
			if (bm.isSelected()) {
				try {
					String lutName = jrb.getActionCommand();
					colorTable = lutService.loadLUT(lutService.findLUTs().get(lutName));
					fillImage(colorTable);
					lutPreview.setIcon(new ImageIcon(image));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};

		// Get lut keys into a list
		List<String> keys = new ArrayList<>(lutService.findLUTs().keySet());
		// Sort lut keys by alphabetical order
		keys.sort(String::compareTo);
		// Then by categories : count the number of / in keys
		keys.sort(Comparator.comparingInt(s -> StringUtils.countMatches(s, "/")));

		for (String lutKey: keys) {
			JRadioButton lutButton = new JRadioButton(lutKey.substring(0,lutKey.length()-4)); // Removes ".lut" in the display
			lutButton.setActionCommand(lutKey);
			lutButton.addChangeListener(lutChanged);
			group.add(lutButton);
			radioPanel.add(lutButton);
		}

		radioPanel.add(lutPreview);

		int result = JOptionPane.showConfirmDialog(null, radioPanel, "Choose a Color Table", JOptionPane.PLAIN_MESSAGE);
		if ((result == JOptionPane.OK_OPTION)&&(group.getSelection()!=null)) {
			try {
				String lutKey = group.getSelection().getActionCommand();
				if (lutService.findLUTs().containsKey(lutKey)) {
					return lutService.loadLUT(lutService.findLUTs().get(lutKey));
				} else {
					System.err.println("Could not find lut with key:"+lutKey);
					return colorTable; // ok not to return null ? Problem with reopening window
				}
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			System.out.println("User canceled / closed the dialog, result = " + result);
			return colorTable; // Is this ok not to return null ?
		}


	}
}

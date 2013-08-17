package at.jku.aig2qbf.visualizer;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.concurrent.CountDownLatch;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import at.jku.aig2qbf.Util;
import at.jku.aig2qbf.Util.FileExtension;
import at.jku.aig2qbf.component.Tree;
import at.jku.aig2qbf.parser.Parser;

public class TreeVisualizer {
	public static boolean CLOSE_ON_EXIT = false;

	public static void DisplayTree(Tree tree) {
		DisplayTree(tree, "Visualize!");
	}

	public static void DisplayTree(Tree tree, String title) {
		if (tree == null) {
			throw new RuntimeException("Tree must not be null");
		}

		if (title == null) {
			title = "Undefined title";
		}

		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		final CountDownLatch waitForSignal = new CountDownLatch(1);

		TreeFrame frame = new TreeFrame(tree, title, screenSize.width, screenSize.height);

		if (CLOSE_ON_EXIT) {
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		}
		else {
			frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		}

		frame.setBounds(0, 0, screenSize.width, screenSize.height);
		frame.setVisible(true);

		frame.addWindowListener(new WindowListener() {

			@Override
			public void windowOpened(WindowEvent arg0) {
				if (CLOSE_ON_EXIT) {
					waitForSignal.countDown();
				}
			}

			@Override
			public void windowIconified(WindowEvent arg0) {

			}

			@Override
			public void windowDeiconified(WindowEvent arg0) {

			}

			@Override
			public void windowDeactivated(WindowEvent arg0) {

			}

			@Override
			public void windowClosing(WindowEvent arg0) {
				waitForSignal.countDown();
			}

			@Override
			public void windowClosed(WindowEvent arg0) {

			}

			@Override
			public void windowActivated(WindowEvent arg0) {

			}
		});

		try {
			waitForSignal.await();
		}
		catch (InterruptedException e) {

		}
	}

	public static void main(String[] args) {
		JFrame filechooserComponent = new JFrame();

		JFileChooser chooser = new JFileChooser(new File("./"));
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		int option = chooser.showOpenDialog(filechooserComponent);

		if (option == JFileChooser.APPROVE_OPTION) {
			String filename = chooser.getSelectedFile().getAbsolutePath();

			Parser parser = Util.GetParser(filename);

			Tree tree = parser.parse(filename);

			TreeVisualizer.DisplayTree(tree, filename);
		}
	}
}

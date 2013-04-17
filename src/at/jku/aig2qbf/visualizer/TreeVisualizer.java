package at.jku.aig2qbf.visualizer;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.concurrent.CountDownLatch;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import at.jku.aig2qbf.component.Tree;
import at.jku.aig2qbf.parser.AAG;
import at.jku.aig2qbf.parser.AIG;
import at.jku.aig2qbf.parser.Parser;
import at.jku.aig2qbf.parser.Parser.Extension;

public class TreeVisualizer {
	public static void DisplayTree(Tree tree, String title) {
		if(tree == null) {
			throw new RuntimeException("Tree must not be null");
		}
		
		if(title == null) {
			title = "Undefined title";
		}
		
		final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		
		final CountDownLatch waitForSignal = new CountDownLatch(1);
		
		TreeFrame frame = new TreeFrame(tree, title, screenSize.width, screenSize.height);

	    frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
	    frame.setBounds(0, 0, screenSize.width, screenSize.height);
	    frame.setVisible(true);
	    
	    frame.addWindowListener(new WindowListener() {
			
			@Override
			public void windowOpened(WindowEvent arg0) {
				
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
		} catch (InterruptedException e) {
			
		}
	}
	
	public static void main(String[] args) {
		JFrame filechooserComponent = new JFrame();
		
		JFileChooser chooser = new JFileChooser(new File("./"));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int option = chooser.showOpenDialog(filechooserComponent);
		
		if(option == JFileChooser.APPROVE_OPTION) {
			String filename = chooser.getSelectedFile().getAbsolutePath();
			
			Extension extension = Parser.getExtension(filename);
			
			Tree tree = null;
			
			if (extension == null) {
				throw new RuntimeException(String.format("Unknown extension for filename \"%s\"", filename));
			}
			else {
				switch (extension) {
					case AAG:
						tree = new AAG().parse(filename);
					break;
					case AIG:
						tree = new AIG().parse(filename);
					break;
					default:
					break;
				}
			}
			
			TreeVisualizer.DisplayTree(tree, filename);
		}
	}
}

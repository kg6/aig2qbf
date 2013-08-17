package at.jku.aig2qbf.visualizer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import at.jku.aig2qbf.component.Tree;

@SuppressWarnings("serial")
public class TreeFrame extends JFrame {
	public TreeFrame(Tree tree, String title, int width, int height) {
		super(title);

		this.addWindowListener(new TreeFrameWindowListener());

		final JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BorderLayout());
		setContentPane(contentPanel);

		final DrawingPanel drawingPanel = new DrawingPanel(tree);
		this.addComponentListener(drawingPanel);

		final JScrollPane scrollPane = new JScrollPane(drawingPanel);
		contentPanel.add(scrollPane, BorderLayout.CENTER);

		JButton saveButton = new JButton("Save image to file");
		saveButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser fileChooser = drawingPanel.getOutputFileChooser();

				if (fileChooser.showSaveDialog(TreeFrame.this) != JFileChooser.APPROVE_OPTION) {
					return;
				}

				if (!drawingPanel.saveBackgroundImageToFile(fileChooser.getSelectedFile())) {
					JOptionPane.showMessageDialog(TreeFrame.this, "Unable to write the file to the storage location.", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		contentPanel.add(saveButton, BorderLayout.PAGE_END);

		setPreferredSize(new Dimension(width, height));

		pack();
	}

	private class TreeFrameWindowListener implements WindowListener {

		@Override
		public void windowOpened(WindowEvent e) {
			TreeFrame.this.repaint();
		}

		@Override
		public void windowClosing(WindowEvent e) {

		}

		@Override
		public void windowClosed(WindowEvent e) {

		}

		@Override
		public void windowIconified(WindowEvent e) {

		}

		@Override
		public void windowDeiconified(WindowEvent e) {

		}

		@Override
		public void windowActivated(WindowEvent e) {
			TreeFrame.this.repaint();
		}

		@Override
		public void windowDeactivated(WindowEvent e) {

		}
	}
}

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.print.*;
import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import javax.swing.text.*;
import javax.swing.undo.*;

public class Main {

	JFrame app = new JFrame("Untitled - Notepad");
	JMenuBar menuBar = new JMenuBar();
	JTextArea editor = new JTextArea();
	JScrollPane scrollPane = new JScrollPane(editor);
	File openedFile = null;
	String fileName = "Untitled";
	JFileChooser fileChooser;
	static Font systemFont = new Font("Segoe UI", Font.PLAIN, 12);
	JPanel statusPane = new JPanel(), infoPane = new JPanel();
	JLabel lineCol = new JLabel("Ln 1, Col 1"), zoomPercentLabel = new JLabel("100%");
	UndoManager undoManager = new UndoManager();
	static String clipboard = "";
	JLabel searchedResult = new JLabel();
	String eventQueue = "Exit";

	Main() {
		app.setSize(1010, 710);
		app.setIconImage(new ImageIcon(Objects.requireNonNull(Main.class.getResource("Logo.png"))).getImage());
		app.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		try {
			String data = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
			if (data != null)  clipboard = data;
		} catch (Exception ignore) {}
		if (openedFile != null)  fileChooser = new JFileChooser(openedFile.getAbsolutePath());
		else  fileChooser = new JFileChooser();
		fileChooser.setFileFilter(new FileNameExtensionFilter("Text Documents", "txt"));

		editor.setFont(new Font("Consolas", Font.PLAIN, 15));
		editor.setSelectedTextColor(Color.white);
		editor.setBorder(new MatteBorder(0, 4, 0, 4, Color.white));

		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.getHorizontalScrollBar().setEnabled(false);
		scrollPane.getVerticalScrollBar().setEnabled(false);
		scrollPane.setBorder(null);

		editor.addKeyListener(new KeyAdapter() {
			public void keyTyped(KeyEvent e) {
				if (!e.isControlDown()) {
					app.setTitle("*" + fileName + " - Notepad");
					if (app.getTitle().equals("*Untitled - Notepad") && editor.getText().equals("")
							&& e.getKeyCode() == KeyEvent.VK_BACK_SPACE)
						app.setTitle(fileName + " - Notepad");
				}
				if (editor.getSize().width > app.getWidth())  scrollPane.getHorizontalScrollBar().setEnabled(true);
				if (editor.getSize().height > app.getHeight())  scrollPane.getVerticalScrollBar().setEnabled(true);
				if (editor.getSize().width <= app.getWidth())  scrollPane.getHorizontalScrollBar().setEnabled(false);
				if (editor.getSize().height <= app.getHeight())  scrollPane.getVerticalScrollBar().setEnabled(false);
			}
		});
		editor.getDocument().addUndoableEditListener(e -> undoManager.addEdit(e.getEdit()));
		editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_DOWN_MASK), "none");
		editor.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK), "undo");
		editor.getActionMap().put("undo", new AbstractAction() {
			public void actionPerformed(ActionEvent e) { undoManager.undo(); }
		});
		editor.addCaretListener(e -> {
			int caretPos = editor.getCaretPosition();
			int rowNum = (caretPos == 0) ? 1 : 0;
			for (int offset = caretPos; offset > 0;) {
				try { offset = Utilities.getRowStart(editor, offset) - 1; }
				catch (BadLocationException ignored) {}
				rowNum++;
			}
			int offset = 0;
			try {
				offset = Utilities.getRowStart(editor, caretPos);
			} catch (BadLocationException ignored) {}
			lineCol.setText("Ln " + rowNum + ", Col " + (caretPos - offset + 1));

			if (editor.getText().equals("")) {
				menuBar.getMenu(1).getMenuComponent(8).setEnabled(false);
				menuBar.getMenu(1).getMenuComponent(9).setEnabled(false);
				menuBar.getMenu(1).getMenuComponent(10).setEnabled(false);
			}
			if (!editor.getText().equals("")) {
				menuBar.getMenu(1).getItem(0).setEnabled(true);
				menuBar.getMenu(1).getMenuComponent(8).setEnabled(true);
				menuBar.getMenu(1).getMenuComponent(9).setEnabled(true);
				menuBar.getMenu(1).getMenuComponent(10).setEnabled(true);
			}
			if (editor.getSelectedText() == null) {
				menuBar.getMenu(1).getMenuComponent(2).setEnabled(false);
				menuBar.getMenu(1).getMenuComponent(3).setEnabled(false);
				menuBar.getMenu(1).getMenuComponent(5).setEnabled(false);
				menuBar.getMenu(1).getMenuComponent(7).setEnabled(false);
			}
			if (editor.getSelectedText() != null) {
				menuBar.getMenu(1).getMenuComponent(2).setEnabled(true);
				menuBar.getMenu(1).getMenuComponent(3).setEnabled(true);
				menuBar.getMenu(1).getMenuComponent(5).setEnabled(true);
				menuBar.getMenu(1).getMenuComponent(7).setEnabled(true);
			}
		});
		editor.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				if (fileName.equals("Untitled") && editor.getText().equals(""))  app.setTitle(fileName + " - Notepad");
				else  app.setTitle("*" + fileName + " - Notepad");
			}
			public void removeUpdate(DocumentEvent e) {
				if (fileName.equals("Untitled") && editor.getText().equals(""))  app.setTitle(fileName + " - Notepad");
				else  app.setTitle("*" + fileName + " - Notepad");
			}
			public void changedUpdate(DocumentEvent e) {
				if (fileName.equals("Untitled") && editor.getText().equals(""))  app.setTitle(fileName + " - Notepad");
				else  app.setTitle("*" + fileName + " - Notepad");
			}
		});

		statusPane.setLayout(new BorderLayout());
		JLabel separationLine = new JLabel(new ImageIcon(Objects.requireNonNull(Main.class.getResource("SeparationLine.png"))));
		separationLine.setBounds(0, 0, Integer.MAX_VALUE, 1);
		statusPane.add(separationLine, "North");
		searchedResult.setFont(systemFont);
		statusPane.add(searchedResult);

		infoPane.setLayout(null);
		infoPane.setBackground(Color.decode("#F0F0F0"));
		infoPane.setPreferredSize(new Dimension(431, 22));
		infoPane.setMinimumSize(new Dimension(431, 22));
		infoPane.setMaximumSize(new Dimension(431, 22));

		lineCol.setBounds(10, 4, 120, 12);
		infoPane.add(lineCol);
		zoomPercentLabel.setBounds(147, 4, 40, 12);
		infoPane.add(zoomPercentLabel);
		JLabel lineEnding = new JLabel("Windows (CRLF)");
		lineEnding.setBounds(197, 4, 108, 12);
		infoPane.add(lineEnding);
		JLabel unicode = new JLabel("UTF-8");
		unicode.setBounds(318, 4, 100, 12);
		infoPane.add(unicode);
		JLabel sizingHandle = new JLabel(new ImageIcon(Objects.requireNonNull(Main.class.getResource("SizingHandle.png"))));
		sizingHandle.setBounds(418, 9, 14, 14);
		infoPane.add(sizingHandle);

		for (Component c : infoPane.getComponents()) {
			c.setForeground(Color.black);
			c.setFont(systemFont);
		}

		JLabel infoPaneBlueprint = new JLabel(new ImageIcon(Objects.requireNonNull(Main.class.getResource("StatusPaneBlueprint.png"))));
		infoPaneBlueprint.setBounds(0, 0, 431, 22);
		infoPane.add(infoPaneBlueprint);
		statusPane.add(infoPane, BorderLayout.EAST);

		app.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				app.repaint();
				if (editor.getSize().width > app.getWidth())  scrollPane.getHorizontalScrollBar().setEnabled(true);
				if (editor.getSize().height > app.getHeight())  scrollPane.getVerticalScrollBar().setEnabled(true);
				if (editor.getSize().width <= app.getWidth())  scrollPane.getHorizontalScrollBar().setEnabled(false);
				if (editor.getSize().height <= app.getHeight())  scrollPane.getVerticalScrollBar().setEnabled(false);
			}
			public void componentMoved(ComponentEvent evt) {
				app.repaint();
			}
		});
		app.addWindowStateListener(e -> {
			app.repaint();
			sizingHandle.setVisible(e.getNewState() != Frame.MAXIMIZED_BOTH);
		});
		app.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (app.getTitle().charAt(0) == '*') {
					eventQueue = "Exit";
					askToSaveDialog();
				}
				else  app.dispose();
			}
		});
		app.add(scrollPane);
		app.add(statusPane, "South");

		menuBar.setBorder(new MatteBorder(0, 0, 2, 0, Color.decode("#F0F0F0")));
		fileMenu(); editMenu(); formatMenu(); viewMenu(); helpMenu();
		app.setJMenuBar(menuBar);
		app.setVisible(true);
		aboutDialog();
		System.gc();
	}

	void openFile() {
		fileChooser.setDialogTitle("Open");
		int userSelection = fileChooser.showOpenDialog(app);
		if (userSelection == JFileChooser.APPROVE_OPTION) {
			openedFile = fileChooser.getSelectedFile();
			try {
				Scanner myReader = new Scanner(openedFile);
				StringBuilder content = new StringBuilder();
				while (myReader.hasNextLine())  content.append(myReader.nextLine()).append("\n");
				myReader.close();
				editor.setText(content.toString());
			} catch (Exception ignored) {}
			fileName = openedFile.getName();
			app.setTitle(fileName + " - Notepad");
			undoManager.discardAllEdits();
			undo.setEnabled(false);
			eventQueue = "Exit";
		}
		System.gc();
	}

	void saveFile() {
		if (fileName.equals("Untitled"))
			saveFileAs();
		else {
			PrintWriter writer;
			try {
				writer = new PrintWriter(openedFile);
				writer.print("");
				writer.close();
			} catch (Exception ignored) {}

			FileWriter fileToSave;
			try {
				fileToSave = new FileWriter(openedFile.getAbsoluteFile());
				fileToSave.write(editor.getText());
				fileToSave.close();
			} catch (Exception ignored) {}
			app.setTitle(fileName + " - Notepad");
			eventQueue = "Exit";
		}
		System.gc();
	}

	void saveFileAs() {
		fileChooser.setDialogTitle("Save As");
		int userSelection = fileChooser.showSaveDialog(app);
		if (userSelection == JFileChooser.APPROVE_OPTION) {
			FileWriter fileToSave;
			try {
				openedFile = fileChooser.getSelectedFile();
				String path = openedFile.getAbsolutePath();
				if (!path.contains("."))  fileToSave = new FileWriter(openedFile.getAbsolutePath() + ".txt");
				else  fileToSave = new FileWriter(openedFile.getAbsolutePath());
				fileToSave.write(editor.getText());
				fileToSave.close();
			} catch (IOException ignored) {}

			if (!openedFile.getName().contains("."))  fileName = openedFile.getName() + ".txt";
			else  fileName = openedFile.getAbsolutePath().substring(openedFile.getAbsolutePath().lastIndexOf("\\") + 1);
			app.setTitle(fileName + " - Notepad");
			undoManager.discardAllEdits();
			undo.setEnabled(false);
		}
		System.gc();
	}

	void askToSaveDialog() {
		JDialog dialog = new JDialog(app, "Blocking Dialog", true);
		dialog.setTitle("Notepad");
		dialog.setSize(367, 142);
		dialog.setResizable(false);
		dialog.setLayout(null);
		dialog.setLocationRelativeTo(null);

		JPanel contentPane = new JPanel();
		contentPane.setBounds(0, 0, 367, 64);
		contentPane.setBackground(Color.white);
		contentPane.setLayout(null);

		JLabel question = new JLabel("Do you want to save changes to " + fileName + "?");
		question.setFont(new Font("Segoe UI", Font.PLAIN, 16));
		question.setForeground(Color.decode("#003399"));
		question.setBounds(9, 10, 367, 25);
		contentPane.add(question);

		JLabel separationLine = new JLabel(new ImageIcon(Objects.requireNonNull(Main.class.getResource("SeparationLine.png"))));
		separationLine.setBounds(0, 63, 367, 1);
		contentPane.add(separationLine);

		JButton saveBut = new JButton("Save");
		saveBut.setFocusPainted(false);
		saveBut.setFont(systemFont);
		saveBut.setBounds(99, 73, 69, 23);
		saveBut.addActionListener(e -> {
			switch (eventQueue) {
				case "Exit" -> {
					dialog.dispose();
					saveFile();
					if (openedFile != null)  app.dispose();
				}
				case "New" -> {
					dialog.dispose();
					saveFile();
					if (app.getTitle().charAt(0) != '*') {
						editor.setText("");
						fileName = "Untitled";
						openedFile = null;
						app.setTitle(fileName + " - Notepad");
					}
					eventQueue = "Exit";
				}
				case "Open" -> {
					dialog.dispose();
					saveFile();
					if (openedFile != null)  openFile();
					eventQueue = "Exit";
				}
			}
		});
		dialog.add(saveBut);

		JButton notSaveBut = new JButton("Not Save");
		notSaveBut.setFocusPainted(false);
		notSaveBut.setFont(systemFont);
		notSaveBut.setBounds(175, 73, 91, 23);
		notSaveBut.addActionListener(e -> {
			switch (eventQueue) {
				case "Exit" -> {
					dialog.setVisible(false);
					app.dispose();
				}
				case "New" -> {
					dialog.dispose();
					editor.setText("");
					fileName = "Untitled";
					app.setTitle(fileName + " - Notepad");
					eventQueue = "Exit";
				}
				case "Open" -> {
					dialog.dispose();
					openFile();
					eventQueue = "Exit";
				}
			}
		});
		dialog.add(notSaveBut);

		JButton cancelBut = new JButton("Cancel");
		cancelBut.setFocusPainted(false);
		cancelBut.setFont(systemFont);
		cancelBut.setBounds(272, 73, 70, 23);
		cancelBut.addActionListener(e -> dialog.dispose());
		dialog.add(cancelBut);

		JTextField temp = new JTextField();
		temp.addKeyListener(new KeyAdapter() {
			int buttonNum = 1;
			public void keyReleased(KeyEvent e) {
				if (buttonNum > 1 && e.getKeyCode() == KeyEvent.VK_LEFT)  buttonNum--;
				if (buttonNum < 3 && e.getKeyCode() == KeyEvent.VK_RIGHT)  buttonNum++;

				if (buttonNum == 1)  dialog.getRootPane().setDefaultButton(saveBut);
				else if (buttonNum == 2)  dialog.getRootPane().setDefaultButton(notSaveBut);
				else  dialog.getRootPane().setDefaultButton(cancelBut);
			}
		});
		dialog.getRootPane().setDefaultButton(saveBut);
		dialog.add(temp);
		dialog.add(contentPane);

		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dialog.dispose();
				System.gc();
			}
		});
		dialog.setVisible(true);
	}

	boolean isFindBoxFocused = true, isFindButClicked = false;
	static boolean isMatchCase = false, isWrap = false;
	String direction = "up", searchedWord = null;
	void findDialog() {
		JDialog dialog = new JDialog(app);
		dialog.setTitle("Find");
		dialog.setSize(370, 159);
		dialog.setResizable(false);
		dialog.setLayout(null);
		dialog.setLocationRelativeTo(app);

		JLabel findTitle = new JLabel("Find what:");
		findTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
		findTitle.setBounds(6, 15, 70, 15);
		dialog.add(findTitle);

		JTextField findBox = new JTextField();
		findBox.setBounds(72, 11, 191, 21);
		findBox.requestFocus();
		if (editor.getSelectedText() != null)  findBox.setText(editor.getSelectedText());
		else if (searchedWord != null)  findBox.setText(searchedWord);
		findBox.setSelectionStart(0);
		findBox.setSelectionEnd(findBox.getText().length());
		findBox.setFont(new Font("Segoe UI", Font.PLAIN, 11));
		findBox.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				findBox.setBorder(new CompoundBorder(new LineBorder(Color.decode("#0078D7"), 1),
						new MatteBorder(0, 2, 0, 0, Color.white)));
				isFindBoxFocused = true;
			}
			public void focusLost(FocusEvent e) {
				findBox.setBorder(new CompoundBorder(new LineBorder(Color.gray, 1),
						new MatteBorder(0, 2, 0, 0, Color.white)));
				isFindBoxFocused = false;
			}
		});
		findBox.addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent e) {
				if (!isFindBoxFocused)
					findBox.setBorder(new CompoundBorder(new LineBorder(Color.black, 1),
							new MatteBorder(0, 2, 0, 0, Color.white)));
			}
			public void mouseExited(MouseEvent e) {
				if (!isFindBoxFocused)
					findBox.setBorder(new CompoundBorder(new LineBorder(Color.gray, 1),
							new MatteBorder(0, 2, 0, 0, Color.white)));
			}
		});
		dialog.add(findBox);

		JCheckBox matchCase = new JCheckBox("Match case");
		matchCase.setFont(new Font("Segoe UI", Font.PLAIN, 11));
		matchCase.setFocusPainted(false);
		if (isMatchCase)  matchCase.setSelected(true);
		matchCase.setVerticalTextPosition(SwingConstants.BOTTOM);
		matchCase.setBounds(2, 70, 158, 18);
		matchCase.addChangeListener(e -> isMatchCase = matchCase.isSelected());
		dialog.add(matchCase);

		JCheckBox wrapAround = new JCheckBox("Wrap around");
		wrapAround.setFont(new Font("Segoe UI", Font.PLAIN, 11));
		wrapAround.setFocusPainted(false);
		if (isWrap)  wrapAround.setSelected(true);
		wrapAround.setVerticalTextPosition(SwingConstants.BOTTOM);
		wrapAround.setBounds(2, 94, 158, 18);
		wrapAround.addChangeListener(e -> isWrap = wrapAround.isSelected());
		dialog.add(wrapAround);

		JRadioButton upDirection = new JRadioButton("Up");
		upDirection.setFont(new Font("Segoe UI", Font.PLAIN, 11));
		upDirection.setBounds(163, 59, 45, 25);
		upDirection.setFocusPainted(false);
		if (direction.equals("up"))  upDirection.setSelected(true);
		upDirection.setVerticalTextPosition(SwingConstants.BOTTOM);
		dialog.add(upDirection);

		JRadioButton downDirection = new JRadioButton("Down");
		downDirection.setFont(new Font("Segoe UI", Font.PLAIN, 11));
		downDirection.setBounds(204, 59, 58, 25);
		downDirection.setFocusPainted(false);
		if (direction.equals("down"))  downDirection.setSelected(true);
		downDirection.setVerticalTextPosition(SwingConstants.BOTTOM);
		dialog.add(downDirection);

		upDirection.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				upDirection.setSelected(true);
				downDirection.setSelected(false);
				direction = "up";
			}
		});
		downDirection.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				downDirection.setSelected(true);
				upDirection.setSelected(false);
				direction = "down";
			}
		});

		JLabel directionLabel = new JLabel("Direction");
		directionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
		directionLabel.setBounds(170, 40, 50, 15);
		dialog.add(directionLabel);

		JLabel directionBlueprint = new JLabel(new ImageIcon(Objects.requireNonNull(Main.class.getResource("DirectionBlueprint.png"))));
		directionBlueprint.setBounds(161, 44, 102, 43);
		dialog.add(directionBlueprint);

		JButton findBut = new JButton("Find Next");
		findBut.setFocusPainted(false);
		findBut.setFont(systemFont);
		findBut.setMargin(new Insets(0,0,0,0));
		findBut.setBounds(273, 8, 75, 23);
		findBut.addActionListener(e -> findNext(findBox.getText()));
		dialog.add(findBut);
		dialog.getRootPane().setDefaultButton(findBut);

		JButton cancelBut = new JButton("Cancel");
		cancelBut.setFocusPainted(false);
		cancelBut.setFont(systemFont);
		cancelBut.setBounds(273, 37, 75, 23);
		cancelBut.addActionListener(e -> dialog.dispose());
		dialog.add(cancelBut);

		findBox.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				if (findBox.getText().equals(""))  findBut.setEnabled(false);
				if (!findBox.getText().equals(""))  findBut.setEnabled(true);
				if (e.getKeyCode() == KeyEvent.VK_UP)  dialog.getRootPane().setDefaultButton(findBut);
				if (e.getKeyCode() == KeyEvent.VK_DOWN)  dialog.getRootPane().setDefaultButton(cancelBut);
			}
		});
		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				searchedWord = findBox.getText();
				dialog.dispose();
				System.gc();
			}
		});
		dialog.setVisible(true);
	}

	int index = -1;
	void findNext(String target) {
		ArrayList<Integer> occurrences = new ArrayList<>();
		for (int i = 0; i <= editor.getText().length() - target.length(); i++) {
			if (editor.getText().startsWith(target, i))  occurrences.add(i);
		}
		if (occurrences.size() == 0)
			JOptionPane.showMessageDialog(app, "Cannot find \"" + target + "\"", "Notepad", JOptionPane.INFORMATION_MESSAGE);
		else {
			if (direction.equals("up"))  index++;
			else if (direction.equals("down"))  index--;

			if (isWrap) {
				if (index >= occurrences.size() || index < 0) {
					if (direction.equals("up"))  index = 0;
					else  index = occurrences.size() - 1;
				}
				editor.setSelectionStart(occurrences.get(index));
				editor.setSelectionEnd(occurrences.get(index) + target.length());
			} else {
				if (index >= 0 && index < occurrences.size()) {
					editor.setSelectionStart(occurrences.get(index));
					editor.setSelectionEnd(occurrences.get(index) + target.length());
				}
				if (index >= occurrences.size() || index < 0) {
					JOptionPane.showMessageDialog(app, "Cannot find \"" + target + "\"",
							"Notepad", JOptionPane.INFORMATION_MESSAGE);
					if (direction.equals("up"))  index = occurrences.size() - 1;
					else  index = 0;
				}
			}

			if (direction.equals("up") && index == 0)  searchedResult.setText("  Found next from the top");
			if (direction.equals("down") && index == occurrences.size() - 1)  searchedResult.setText("  Found next from the bottom");
			if (index != 0 && index != occurrences.size() - 1)  searchedResult.setText("");
		}
	}

	boolean isReplaceBoxFocused = false;
	static String replacedWord = null;
	int clickCount = 0;
	void replaceDialog() {
		JDialog dialog = new JDialog(app);
		dialog.setTitle("Replace");
		dialog.setSize(370, 191);
		dialog.setResizable(false);
		dialog.setLayout(null);
		dialog.setLocationRelativeTo(app);

		JLabel findTitle = new JLabel("Find what:");
		findTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
		findTitle.setBounds(6, 12, 70, 15);
		dialog.add(findTitle);

		JTextField findBox = new JTextField();
		findBox.setSelectionStart(0);
		findBox.setSelectionEnd(findBox.getText().length());
		findBox.setFont(new Font("Segoe UI", Font.PLAIN, 11));
		isFindBoxFocused = true;
		findBox.requestFocus();
		findBox.setBounds(84, 11, 177, 20);
		findBox.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				findBox.setBorder(new CompoundBorder(new LineBorder(Color.decode("#0078D7"), 1),
						new MatteBorder(0, 2, 0, 0, Color.white)));
				isFindBoxFocused = true;
			}
			public void focusLost(FocusEvent e) {
				findBox.setBorder(new CompoundBorder(new LineBorder(Color.gray, 1),
						new MatteBorder(0, 2, 0, 0, Color.white)));
				isFindBoxFocused = false;
			}
		});
		findBox.addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent e) {
				if (!isFindBoxFocused)
					findBox.setBorder(new CompoundBorder(new LineBorder(Color.black, 1),
							new MatteBorder(0, 2, 0, 0, Color.white)));
			}
			public void mouseReleased(MouseEvent e) {
				isReplaceBoxFocused = false;
				clickCount++;
			}
			public void mouseExited(MouseEvent e) {
				if (!isFindBoxFocused)
					findBox.setBorder(new CompoundBorder(new LineBorder(Color.gray, 1),
							new MatteBorder(0, 2, 0, 0, Color.white)));
			}
		});
		dialog.add(findBox);

		JLabel replaceTitle = new JLabel("Replace with:");
		replaceTitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
		replaceTitle.setBounds(6, 40, 96, 15);
		dialog.add(replaceTitle);

		JTextField replaceBox = new JTextField();
		if (replacedWord != null)  replaceBox.setText(replacedWord);
		replaceBox.setFont(new Font("Segoe UI", Font.PLAIN, 11));
		replaceBox.setBounds(84, 39, 177, 20);
		replaceBox.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				replaceBox.setBorder(new CompoundBorder(new LineBorder(Color.decode("#0078D7"), 1),
						new MatteBorder(0, 2, 0, 0, Color.white)));
				isReplaceBoxFocused = true;
			}
			public void focusLost(FocusEvent e) {
				replaceBox.setBorder(new CompoundBorder(new LineBorder(Color.gray, 1),
						new MatteBorder(0, 2, 0, 0, Color.white)));
				isReplaceBoxFocused = false;
			}
		});
		replaceBox.addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent e) {
				if (!isReplaceBoxFocused)
					replaceBox.setBorder(new CompoundBorder(new LineBorder(Color.black, 1),
							new MatteBorder(0, 2, 0, 0, Color.white)));
			}
			public void mouseReleased(MouseEvent e) {
				isFindBoxFocused = false;
			}
			public void mouseExited(MouseEvent e) {
				if (!isReplaceBoxFocused)
					replaceBox.setBorder(new CompoundBorder(new LineBorder(Color.gray, 1),
							new MatteBorder(0, 2, 0, 0, Color.white)));
			}
		});
		dialog.add(replaceBox);

		JCheckBox matchCase = new JCheckBox("Match case");
		matchCase.setFont(new Font("Segoe UI", Font.PLAIN, 11));
		matchCase.setFocusPainted(false);
		if (isMatchCase)  matchCase.setSelected(true);
		matchCase.setVerticalTextPosition(SwingConstants.BOTTOM);
		matchCase.setBounds(2, 100, 158, 18);
		matchCase.addChangeListener(e -> isMatchCase = matchCase.isSelected());
		dialog.add(matchCase);

		JCheckBox wrapAround = new JCheckBox("Wrap around");
		wrapAround.setFont(new Font("Segoe UI", Font.PLAIN, 11));
		wrapAround.setFocusPainted(false);
		if (isWrap)  wrapAround.setSelected(true);
		wrapAround.setVerticalTextPosition(SwingConstants.BOTTOM);
		wrapAround.setBounds(2, 124, 158, 18);
		wrapAround.addChangeListener(e -> isWrap = wrapAround.isSelected());
		dialog.add(wrapAround);

		JButton findBut = new JButton("Find Next");
		findBut.setFocusPainted(false);
		findBut.setFont(systemFont);
		findBut.setMargin(new Insets(0,0,0,0));
		findBut.setBounds(270, 8, 75, 23);
		isFindButClicked = false;
		clickCount = 0;
		findBut.addActionListener(e -> {
			clickCount++;
			findNext(findBox.getText());
		});
		dialog.add(findBut);
		dialog.getRootPane().setDefaultButton(findBut);

		JButton replaceBut = new JButton("Replace");
		replaceBut.setFocusPainted(false);
		replaceBut.setFont(systemFont);
		replaceBut.setMargin(new Insets(0,0,0,0));
		replaceBut.setBounds(270, 35, 75, 23);
		replaceBut.addActionListener(e -> {
			if (clickCount != 0)  replace(findBox.getText(), replaceBox.getText());
			if (clickCount == 0) {
				editor.setSelectionStart(editor.getText().indexOf(findBox.getText()));
				editor.setSelectionEnd(editor.getSelectionStart() + findBox.getText().length());
				clickCount++;
			}
		});
		dialog.add(replaceBut);

		JButton replaceAllBut = new JButton("Replace All");
		replaceAllBut.setFocusPainted(false);
		replaceAllBut.setFont(systemFont);
		replaceAllBut.setMargin(new Insets(0,0,0,0));
		replaceAllBut.setBounds(270, 62, 75, 23);
		replaceAllBut.addActionListener(e -> {
			app.setTitle("*" + fileName + " - Notepad");
			editor.setText(editor.getText().replaceAll(findBox.getText(), replaceBox.getText()));
		});
		dialog.add(replaceAllBut);

		JButton cancelBut = new JButton("Cancel");
		cancelBut.setFocusPainted(false);
		cancelBut.setFont(systemFont);
		cancelBut.setBounds(270, 89, 75, 23);
		cancelBut.addActionListener(e -> dialog.dispose());
		dialog.add(cancelBut);

		findBut.setEnabled(false);
		replaceBut.setEnabled(false);
		replaceAllBut.setEnabled(false);
		if (editor.getSelectedText() != null) {
			findBox.setText(editor.getSelectedText());
			findBut.setEnabled(true);
			replaceBut.setEnabled(true);
			replaceAllBut.setEnabled(true);
		} else if (searchedWord != null) {
			findBox.setText(searchedWord);
			findBut.setEnabled(true);
			replaceBut.setEnabled(true);
			replaceAllBut.setEnabled(true);
		}
		findBox.addKeyListener(new KeyAdapter() {
			int buttonNum = 1;
			public void keyReleased(KeyEvent e) {
				if (findBox.getText().equals("")) {
					findBut.setEnabled(false);
					replaceBut.setEnabled(false);
					replaceAllBut.setEnabled(false);
				}
				if (!findBox.getText().equals("")) {
					findBut.setEnabled(true);
					replaceBut.setEnabled(true);
					replaceAllBut.setEnabled(true);
				}

				if (findBut.isEnabled()) {
					if (buttonNum > 1 && e.getKeyCode() == KeyEvent.VK_UP)
						buttonNum--;
					if (buttonNum < 4 && e.getKeyCode() == KeyEvent.VK_DOWN)
						buttonNum++;

					if (buttonNum == 1)
						dialog.getRootPane().setDefaultButton(findBut);
					else if (buttonNum == 2)
						dialog.getRootPane().setDefaultButton(replaceBut);
					else if (buttonNum == 3)
						dialog.getRootPane().setDefaultButton(replaceAllBut);
					else
						dialog.getRootPane().setDefaultButton(cancelBut);
				}
			}
		});
		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (direction.equals("up"))
					index = 0;
				if (direction.equals("down")) {
					int count = 0;
					for (int i = 0; i < editor.getText().length() - findBox.getText().length(); i++) {
						if (editor.getText().startsWith(findBox.getText(), i))
							count++;
					}
					index = count - 1;
				}
				clickCount = 0;
				searchedWord = findBox.getText();
				replacedWord = replaceBox.getText();

				dialog.dispose();
				System.gc();
			}
		});
		dialog.setVisible(true);
	}

	void replace(String target, String alternative) {
		ArrayList<Integer> occurrences = new ArrayList<>();
		for (int i = 0; i <= editor.getText().length() - target.length(); i++) {
			if (editor.getText().startsWith(target, i))  occurrences.add(i);
		}
		if (occurrences.size() == 0) {
			JOptionPane.showMessageDialog(app, "Cannot find \"" + target + "\"", "Notepad", JOptionPane.INFORMATION_MESSAGE);
		}
		else {
			if (index != -1) {
				if (direction.equals("up"))  index--;
				else if (direction.equals("down"))  index++;
			}
			findNext(target);
			String previous = editor.getText().substring(0, editor.getSelectionStart());
			String next = editor.getText().substring(editor.getSelectionEnd());
			editor.setText(previous + alternative + next);
			if (direction.equals("up"))  index--;
			else if (direction.equals("down"))  index++;
			findNext(target);
			if (direction.equals("up"))  index--;
			else if (direction.equals("down"))  index++;
		}
	}

	boolean isLineNumFocused = true;
	static int line = 1;
	final char[] wrongChars = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q',
			'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '`', '~', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')',
			'-', '_', '=', '+', '[', '{', ']', '}', '\\', '|', ';', ':', '\'', '\"', ',', '<', '.', '>', '/', '?'};
	void goToDialog() {
		JDialog dialog = new JDialog(app, "Blocking Dialog", true);
		dialog.setTitle("Go To Line");
		dialog.setSize(265, 137);
		dialog.setResizable(false);
		dialog.setLayout(null);
		dialog.setLocationRelativeTo(app);

		JLabel title = new JLabel("Line number:");
		title.setFont(systemFont);
		title.setBounds(11, 10, 80, 12);
		dialog.add(title);

		JTextField lineNum = new JTextField();
		lineNum.setText(String.valueOf(line));
		lineNum.setFont(systemFont);
		lineNum.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				lineNum.setBorder(new CompoundBorder(new LineBorder(Color.decode("#0078D7"), 1),
						new MatteBorder(0, 2, 0, 0, Color.white)));
				isFindBoxFocused = true;
			}
			public void focusLost(FocusEvent e) {
				lineNum.setBorder(new CompoundBorder(new LineBorder(Color.gray, 1),
						new MatteBorder(0, 2, 0, 0, Color.white)));
				isFindBoxFocused = false;
			}
		});
		lineNum.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE)  dialog.dispose();
				for (char c : wrongChars) {
					if (e.getKeyChar() == c) {
						for (char c2 : wrongChars)  lineNum.setText(lineNum.getText().replace(Character.toString(c2), ""));
						JOptionPane.showMessageDialog(app, "Invalid Input: '" + e.getKeyChar() + "'.",
								"Unrecognizable Characters", JOptionPane.ERROR_MESSAGE);
						for (char c2 : wrongChars)  lineNum.setText(lineNum.getText().replace(Character.toString(c2), ""));
					}
				}
			}
		});
		lineNum.addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent e) {
				if (!isLineNumFocused)
					lineNum.setBorder(new CompoundBorder(new LineBorder(Color.black, 1),
							new MatteBorder(0, 2, 0, 0, Color.white)));
			}
			public void mouseExited(MouseEvent e) {
				if (!isLineNumFocused)
					lineNum.setBorder(new CompoundBorder(new LineBorder(Color.gray, 1),
							new MatteBorder(0, 2, 0, 0, Color.white)));
			}
		});
		lineNum.setBounds(11, 29, 227, 23);
		dialog.add(lineNum);

		JButton goToBut = new JButton("Go To");
		goToBut.setFont(systemFont);
		goToBut.setFocusPainted(false);
		dialog.getRootPane().setDefaultButton(goToBut);
		goToBut.setBounds(83, 63, 75, 23);
		goToBut.addActionListener(e -> {
			if (Integer.parseInt(lineNum.getText()) - 1 < editor.getDocument().getDefaultRootElement().getElementCount()) {
				editor.setCaretPosition(editor.getDocument().getDefaultRootElement()
						.getElement(Integer.parseInt(lineNum.getText()) - 1).getStartOffset());
				dialog.dispose();
			} else
				JOptionPane.showMessageDialog(app, "The line number is beyond the total number of lines",
						"Notepad - Goto Line", JOptionPane.INFORMATION_MESSAGE);
		});
		dialog.add(goToBut);

		JButton cancelBut = new JButton("Cancel");
		cancelBut.setFont(systemFont);
		cancelBut.setFocusPainted(false);
		cancelBut.setBounds(164, 63, 75, 23);
		cancelBut.addActionListener(e ->  {
			if (!lineNum.getText().equals(""))  line = Integer.parseInt(lineNum.getText());
			dialog.dispose();
		});
		dialog.add(cancelBut);

		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (!lineNum.getText().equals(""))  line = Integer.parseInt(lineNum.getText());
				dialog.dispose();
				System.gc();
			}
		});
		dialog.setVisible(true);
	}

	int scriptIndex = 0;
	void fontDialog() {
		JDialog dialog = new JDialog(app, "Blocking Dialog", true);
		dialog.setTitle("Font");
		dialog.setSize(441, 478);
		dialog.setResizable(false);
		dialog.setLayout(null);
		dialog.setLocationRelativeTo(app);
		Font font = editor.getFont();

		JLabel fontNameLabel = new JLabel("Font:");
		fontNameLabel.setFont(systemFont);
		fontNameLabel.setBounds(12, 13, 172, 15);
		dialog.add(fontNameLabel);

		JTextField fontNameEntry = new JTextField(font.getFontName());
		fontNameEntry.setFont(systemFont);
		fontNameEntry.setBorder(new CompoundBorder(new LineBorder(Color.decode("#0078D7"), 1),
				new MatteBorder(0, 5, 0, 0, Color.white)));
		fontNameEntry.setSelectionStart(0);
		fontNameEntry.setSelectionEnd(fontNameEntry.getText().length());
		fontNameEntry.setBounds(12, 30, 172, 22);
		dialog.add(fontNameEntry);

		String[] origFontNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		String[] fontNames = new String[origFontNames.length];
		for (int i = 0; i < origFontNames.length; i++)
			fontNames[i] = "<html><body style=\"font-size:15;font-family:" + origFontNames[i] + ";\">" + origFontNames[i] + "</html>";

		JList<String> fontNameList = new JList<>(fontNames);
		for (int i = 0; i < origFontNames.length; i++) {
			if (origFontNames[i].equals(font.getFontName())) {
				fontNameList.setSelectedIndex(i);
				break;
			}
		}
		fontNameList.setFocusable(false);
		fontNameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JScrollPane fontNameSelectionPane = new JScrollPane(fontNameList);
		fontNameList.ensureIndexIsVisible(fontNameList.getSelectedIndex());
		fontNameSelectionPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		fontNameSelectionPane.setBounds(12, 52, 172, 118);
		dialog.add(fontNameSelectionPane);


		JLabel fontStyleLabel = new JLabel("Font style:");
		fontStyleLabel.setFont(systemFont);
		fontStyleLabel.setBounds(201, 13, 129, 15);
		dialog.add(fontStyleLabel);

		JTextField fontStyleEntry = new JTextField();
		fontStyleEntry.setFont(systemFont);
		fontStyleEntry.setBorder(new CompoundBorder(new LineBorder(Color.decode("#0078D7"), 1),
				new MatteBorder(0, 5, 0, 0, Color.white)));
		fontStyleEntry.setSelectionStart(0);
		fontStyleEntry.setSelectionEnd(fontNameEntry.getText().length());
		fontStyleEntry.setBounds(201, 30, 129, 22);
		dialog.add(fontStyleEntry);

		String style = font.toString();
		for (int i = 0; i < 4; i++) {
			try {
				style = font.toString().substring(style.indexOf("style"));
				style = style.substring(6, style.indexOf(","));
			} catch (Exception ignore) {}

			style = switch (style) {
				case "plain" -> "Regular";
				case "italic" -> "Italic";
				case "bold" -> "Bold";
				case "bold italic" -> "Bold Italic";
				default -> style;
			};
		}
		fontStyleEntry.setText(style);

		JPanel fontStyleList = new JPanel();
		fontStyleList.setBackground(Color.white);
		fontStyleList.setBorder(new LineBorder(Color.gray, 1));
		fontStyleList.setLayout(new BoxLayout(fontStyleList, BoxLayout.Y_AXIS));

		JLabel regularBut = new JLabel("Regular");
		JLabel italicBut = new JLabel("Italic");
		JLabel boldBut = new JLabel("Bold");
		JLabel boldItalicBut = new JLabel("Bold Italic");
		fontStyleList.add(regularBut);
		fontStyleList.add(italicBut);
		fontStyleList.add(boldBut);
		fontStyleList.add(boldItalicBut);
		fontStyleList.setBounds(201, 52, 129, 118);

		for (int i = 0; i < fontStyleList.getComponentCount(); i++) {
			fontStyleList.getComponent(i).setFont(new Font(font.getFontName(), Font.BOLD + Font.ITALIC, 15));
			fontStyleList.getComponent(i).setBackground(Color.white);
			((JLabel) fontStyleList.getComponent(i)).setOpaque(true);
			fontStyleList.getComponent(i).setFocusable(false);
			fontStyleList.getComponent(i).setPreferredSize(new Dimension(1000, 21));
			fontStyleList.getComponent(i).setMinimumSize(new Dimension(1000, 21));
			fontStyleList.getComponent(i).setMaximumSize(new Dimension(1000, 21));
		}
		switch (style) {
			case "Regular" -> {
				regularBut.setForeground(Color.white);
				regularBut.setBackground(Color.decode("#0078D7"));
				regularBut.requestFocus();
			}
			case "Italic" -> {
				italicBut.setForeground(Color.white);
				italicBut.setBackground(Color.decode("#0078D7"));
				italicBut.requestFocus();
			}
			case "Bold" -> {
				boldBut.setForeground(Color.white);
				boldBut.setBackground(Color.decode("#0078D7"));
				boldBut.requestFocus();
			}
			default -> {
				boldItalicBut.setForeground(Color.white);
				boldItalicBut.setBackground(Color.decode("#0078D7"));
				boldItalicBut.requestFocus();
			}
		}
		dialog.add(fontStyleList);


		JLabel fontSizeLabel = new JLabel("Size:");
		fontSizeLabel.setFont(systemFont);
		fontSizeLabel.setBounds(347, 13, 64, 15);
		dialog.add(fontSizeLabel);

		JTextField fontSizeEntry = new JTextField(Integer.toString(font.getSize() - 4));
		fontSizeEntry.setFont(systemFont);
		fontSizeEntry.setBorder(new CompoundBorder(new LineBorder(Color.decode("#0078D7"), 1),
				new MatteBorder(0, 5, 0, 0, Color.white)));
		fontSizeEntry.setSelectionStart(0);
		fontSizeEntry.setSelectionEnd(fontSizeEntry.getText().length());
		fontSizeEntry.setBounds(347, 30, 64, 22);
		dialog.add(fontSizeEntry);

		String[] fontSizes = new String[16];
		fontSizes[0] = "8";
		fontSizes[1] = "9";
		fontSizes[2] = "10";
		fontSizes[3] = "11";
		for (int i = 4; i < 13; i++)
			fontSizes[i] = Integer.toString(12 + 2 * (i - 4));
		fontSizes[13] = "36";
		fontSizes[14] = "48";
		fontSizes[15] = "72";

		JList<String> fontSizeList = new JList<>(fontSizes);
		for (int i = 0; i < 16; i++) {
			if (Integer.toString(font.getSize() - 4).equals(fontSizes[i])) {
				fontSizeList.setSelectedIndex(i);
				break;
			}
		}
		fontSizeList.setFont(systemFont);
		fontSizeList.setFocusable(false);
		fontSizeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JScrollPane fontSizeSelectionPane = new JScrollPane(fontSizeList);
		fontSizeList.ensureIndexIsVisible(fontSizeList.getSelectedIndex());
		fontSizeSelectionPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		fontSizeSelectionPane.setBounds(347, 52, 64, 118);
		dialog.add(fontSizeSelectionPane);


		JLabel fontSampleBlueprint = new JLabel(new ImageIcon(Objects.requireNonNull(Main.class.getResource("FontSampleBlueprint.png"))));
		fontSampleBlueprint.setBounds(201, 185, 210, 77);
		dialog.add(fontSampleBlueprint);
		JLabel fontSampleTitle = new JLabel("Sample");
		fontSampleTitle.setFont(systemFont);
		fontSampleTitle.setBounds(209, 181, 50, 15);
		dialog.add(fontSampleTitle);

		JPanel fontSamplePane = new JPanel();
		fontSamplePane.setLayout(new GridBagLayout());
		JLabel fontSample = new JLabel("AaBbYyZz");
		fontSample.setFont(font);
		fontSamplePane.add(fontSample);
		fontSamplePane.setBounds(202, 196, 206, 65);
		dialog.add(fontSamplePane);

		JLabel scriptTitle = new JLabel("Script");
		scriptTitle.setFont(systemFont);
		scriptTitle.setBounds(202, 271, 210, 15);
		dialog.add(scriptTitle);

		String[] scripts = {"Western", "Greek", "Turkish", "Baltic", "Central European", "Cyrillic", "Vietnamese"};
		JComboBox<String> scriptSelection = new JComboBox<>(scripts);
		scriptSelection.setSelectedIndex(scriptIndex);
		if (Objects.requireNonNull(scriptSelection.getSelectedItem()).equals("Western"))  fontSample.setText("AaBbYyZz");
		else if (scriptSelection.getSelectedItem().equals("Greek"))  fontSample.setText("AaBbAαBβ");
		else if (scriptSelection.getSelectedItem().equals("Turkish"))  fontSample.setText("AaBbĞğŞş");
		else if (scriptSelection.getSelectedItem().equals("Baltic"))  fontSample.setText("AaBbYyZz");
		else if (scriptSelection.getSelectedItem().equals("Central European"))  fontSample.setText("AaBbÁáÔô");
		else if (scriptSelection.getSelectedItem().equals("Cyrillic"))  fontSample.setText("AaBbƂƃфɸ");
		else if (scriptSelection.getSelectedItem().equals("Vietnamese"))  fontSample.setText("AaBbƠơƯư");

		scriptSelection.setEditable(false);
		scriptSelection.setFont(systemFont);
		scriptSelection.setFocusable(false);
		scriptSelection.setBounds(202, 290, 210, 23);
		scriptSelection.addActionListener(e -> {
			if (Objects.requireNonNull(scriptSelection.getSelectedItem()).equals("Western"))  fontSample.setText("AaBbYyZz");
			else if (scriptSelection.getSelectedItem().equals("Greek"))  fontSample.setText("AaBbAαBβ");
			else if (scriptSelection.getSelectedItem().equals("Turkish"))  fontSample.setText("AaBbĞğŞş");
			else if (scriptSelection.getSelectedItem().equals("Baltic"))  fontSample.setText("AaBbYyZz");
			else if (scriptSelection.getSelectedItem().equals("Central European"))  fontSample.setText("AaBbÁáÔô");
			else if (scriptSelection.getSelectedItem().equals("Cyrillic"))  fontSample.setText("AaBbƂƃфɸ");
			else if (scriptSelection.getSelectedItem().equals("Vietnamese"))  fontSample.setText("AaBbƠơƯư");
		});
		dialog.add(scriptSelection);

		fontNameEntry.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				for (int i = 0; i < origFontNames.length; i++) {
					if (origFontNames[i].toLowerCase().compareTo(fontNameEntry.getText().toLowerCase()) >= 0) {
						fontNameList.ensureIndexIsVisible(i + 10);
						fontNameList.ensureIndexIsVisible(i);
						break;
					}
				}
				for (int i = 0; i < origFontNames.length; i++) {
					if (fontNameEntry.getText().equalsIgnoreCase(origFontNames[i])) {
						fontNameList.setSelectedIndex(i);
						fontNameList.ensureIndexIsVisible(origFontNames.length - 1);
						fontNameList.ensureIndexIsVisible(fontNameList.getSelectedIndex());
						break;
					}
					else {
						try { fontNameList.clearSelection(); } catch (Exception ignore) {}
					}
				}
				if (e.getKeyCode() == KeyEvent.VK_ENTER)  dialog.dispose();
			}
		});
		fontStyleEntry.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				for (; true; ) {
					if (fontStyleEntry.getText().equalsIgnoreCase("Regular")) {
						regularBut.setForeground(Color.white);
						regularBut.setBackground(Color.decode("#0078D7"));
						italicBut.setForeground(Color.black);
						italicBut.setBackground(Color.white);
						boldBut.setForeground(Color.black);
						boldBut.setBackground(Color.white);
						boldItalicBut.setForeground(Color.black);
						boldItalicBut.setBackground(Color.white);
						fontStyleEntry.setSelectionStart(0);
						fontStyleEntry.setSelectionEnd(fontStyleEntry.getText().length());
						break;
					}
					else if (fontStyleEntry.getText().equalsIgnoreCase("Italic")) {
						regularBut.setForeground(Color.black);
						regularBut.setBackground(Color.white);
						italicBut.setForeground(Color.white);
						italicBut.setBackground(Color.decode("#0078D7"));
						boldBut.setForeground(Color.black);
						boldBut.setBackground(Color.white);
						boldItalicBut.setForeground(Color.black);
						boldItalicBut.setBackground(Color.white);
						fontStyleEntry.setSelectionStart(0);
						fontStyleEntry.setSelectionEnd(fontStyleEntry.getText().length());
						break;
					}
					else if (fontStyleEntry.getText().equalsIgnoreCase("Bold")) {
						regularBut.setForeground(Color.black);
						regularBut.setBackground(Color.white);
						italicBut.setForeground(Color.black);
						italicBut.setBackground(Color.white);
						boldBut.setForeground(Color.white);
						boldBut.setBackground(Color.decode("#0078D7"));
						boldItalicBut.setForeground(Color.black);
						boldItalicBut.setBackground(Color.white);
						fontStyleEntry.setSelectionStart(0);
						fontStyleEntry.setSelectionEnd(fontStyleEntry.getText().length());
						break;
					}
					else if (fontStyleEntry.getText().equalsIgnoreCase("Bold Italic")) {
						regularBut.setForeground(Color.black);
						regularBut.setBackground(Color.white);
						italicBut.setForeground(Color.black);
						italicBut.setBackground(Color.white);
						boldBut.setForeground(Color.black);
						boldBut.setBackground(Color.white);
						boldItalicBut.setForeground(Color.white);
						boldItalicBut.setBackground(Color.decode("#0078D7"));
						fontStyleEntry.setSelectionStart(0);
						fontStyleEntry.setSelectionEnd(fontStyleEntry.getText().length());
						break;
					}
					else {
						regularBut.setForeground(Color.black);
						regularBut.setBackground(Color.white);
						italicBut.setForeground(Color.black);
						italicBut.setBackground(Color.white);
						boldBut.setForeground(Color.black);
						boldBut.setBackground(Color.white);
						boldItalicBut.setForeground(Color.black);
						boldItalicBut.setBackground(Color.white);
						fontStyleEntry.setSelectionStart(fontStyleEntry.getText().length());
						fontStyleEntry.setSelectionEnd(fontStyleEntry.getText().length());
						break;
					}
				}
				if (e.getKeyCode() == KeyEvent.VK_ENTER)  dialog.dispose();
			}
		});
		fontSizeEntry.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				for (char c : wrongChars) {
					if (!fontSizeEntry.getText().equals("") && e.getKeyChar() != c) {
						for (int i = 0; i < fontSizes.length; i++) {
							if (Integer.parseInt(fontSizes[i]) >= Integer.parseInt(fontSizeEntry.getText())) {
								fontSizeList.ensureIndexIsVisible(fontSizes.length - 1);
								fontSizeList.ensureIndexIsVisible(i);
								break;
							}
						}
					}
					else {
						try { fontSizeList.clearSelection(); }
						catch (Exception ignore) {}
					}
				}
				for (int i = 0; i < fontSizes.length; i++) {
					if (fontSizeEntry.getText().equalsIgnoreCase(fontSizes[i])) {
						fontSizeList.setSelectedIndex(i);
						fontSizeList.ensureIndexIsVisible(origFontNames.length - 1);
						fontSizeList.ensureIndexIsVisible(fontNameList.getSelectedIndex());
						break;
					}
					else {
						try { fontSizeList.clearSelection(); }
						catch (Exception ignore) {}
					}
				}
				if (e.getKeyCode() == KeyEvent.VK_ENTER)  dialog.dispose();
			}
		});

		fontNameList.addListSelectionListener(e -> {
			String fontName = origFontNames[fontNameList.getSelectedIndex()];
			boolean isValid = false;
			for (String origFontName : origFontNames) {
				if (origFontName.equals(fontName)) {
					isValid = true;
					break;
				}
			}
			if (isValid) {
				fontNameEntry.setText(fontName);
				fontNameEntry.setSelectionStart(0);
				fontNameEntry.setSelectionEnd(fontNameEntry.getText().length());
				fontNameEntry.requestFocus();

				regularBut.setFont(new Font(fontName, Font.PLAIN, font.getSize()));
				italicBut.setFont(new Font(fontName, Font.ITALIC, font.getSize()));
				boldBut.setFont(new Font(fontName, Font.BOLD, font.getSize()));
				boldItalicBut.setFont(new Font(fontName, Font.BOLD + Font.ITALIC, font.getSize()));
				fontSample.setFont(new Font(fontName, fontSample.getFont().getStyle(), fontSample.getFont().getSize()));
			}
		});

		regularBut.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				regularBut.setForeground(Color.white);
				regularBut.setBackground(Color.decode("#0078D7"));
				italicBut.setForeground(Color.black);
				italicBut.setBackground(Color.white);
				boldBut.setForeground(Color.black);
				boldBut.setBackground(Color.white);
				boldItalicBut.setForeground(Color.black);
				boldItalicBut.setBackground(Color.white);
				fontStyleEntry.setText("Regular");
				fontStyleEntry.setSelectionStart(0);
				fontStyleEntry.setSelectionEnd(fontStyleEntry.getText().length());
				fontStyleEntry.requestFocus();
				fontSample.setFont(new Font(fontSample.getFont().getFontName().replace(" Bold", "")
						.replace( " Italic", ""), Font.PLAIN, fontSample.getFont().getSize()));
			}
		});
		italicBut.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				regularBut.setForeground(Color.black);
				regularBut.setBackground(Color.white);
				italicBut.setForeground(Color.white);
				italicBut.setBackground(Color.decode("#0078D7"));
				boldBut.setForeground(Color.black);
				boldBut.setBackground(Color.white);
				boldItalicBut.setForeground(Color.black);
				boldItalicBut.setBackground(Color.white);
				fontStyleEntry.setText("Italic");
				fontStyleEntry.setSelectionStart(0);
				fontStyleEntry.setSelectionEnd(fontStyleEntry.getText().length());
				fontStyleEntry.requestFocus();
				fontSample.setFont(new Font(fontSample.getFont().getFontName().replace(" Bold", ""),
						Font.PLAIN + Font.ITALIC, fontSample.getFont().getSize()));
			}
		});
		boldBut.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				regularBut.setForeground(Color.black);
				regularBut.setBackground(Color.white);
				italicBut.setForeground(Color.black);
				italicBut.setBackground(Color.white);
				boldBut.setForeground(Color.white);
				boldBut.setBackground(Color.decode("#0078D7"));
				boldItalicBut.setForeground(Color.black);
				boldItalicBut.setBackground(Color.white);
				fontStyleEntry.setText("Bold");
				fontStyleEntry.setSelectionStart(0);
				fontStyleEntry.setSelectionEnd(fontStyleEntry.getText().length());
				fontStyleEntry.requestFocus();
				fontSample.setFont(new Font(fontSample.getFont().getFontName().replace( " Italic", ""), Font.BOLD, fontSample.getFont().getSize()));
			}
		});
		boldItalicBut.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				regularBut.setForeground(Color.black);
				regularBut.setBackground(Color.white);
				italicBut.setForeground(Color.black);
				italicBut.setBackground(Color.white);
				boldBut.setForeground(Color.black);
				boldBut.setBackground(Color.white);
				boldItalicBut.setForeground(Color.white);
				boldItalicBut.setBackground(Color.decode("#0078D7"));
				fontStyleEntry.setText("Bold Italic");
				fontStyleEntry.setSelectionStart(0);
				fontStyleEntry.setSelectionEnd(fontStyleEntry.getText().length());
				fontStyleEntry.requestFocus();
				fontSample.setFont(new Font(fontSample.getFont().getFontName(), Font.BOLD + Font.ITALIC, fontSample.getFont().getSize()));
			}
		});

		fontSizeList.addListSelectionListener(e -> {
			String fontSize = fontSizes[fontSizeList.getSelectedIndex()];
			boolean isValid = false;
			for (String size : fontSizes) {
				if (size.equals(fontSize)) {
					isValid = true;
					break;
				}
			}
			if (isValid) {
				fontSizeEntry.setText(fontSize);
				fontSizeEntry.setSelectionStart(0);
				fontSizeEntry.setSelectionEnd(fontSizeEntry.getText().length());
				fontSizeEntry.requestFocus();
				fontSample.setFont(new Font(fontSample.getFont().getFontName(), fontSample.getFont().getStyle(),
						Integer.parseInt(fontSize) + 4));
			}
		});

		JLabel showMoreFonts = new JLabel("<html><body style=\"font-size:12;font-family:Segoe UI;\"><u>Show more fonts</u></html>");
		showMoreFonts.setForeground(Color.decode("#0078D7"));
		showMoreFonts.setBounds(12, 371, 200, 18);
		showMoreFonts.addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent e) {
				showMoreFonts.setCursor(new Cursor(Cursor.HAND_CURSOR));
			}
			public void mouseClicked(MouseEvent e) {
				try { Desktop.getDesktop().open(new File ("C:\\Windows\\Fonts")); }
				catch (Exception ignore) {}
			}
			public void mouseExited(MouseEvent e) {
				showMoreFonts.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
		});
		dialog.add(showMoreFonts);

		JButton okBut = new JButton("OK");
		okBut.setFont(systemFont);
		okBut.setFocusPainted(false);
		app.getRootPane().setDefaultButton(okBut);
		okBut.setVerticalAlignment(SwingConstants.TOP);
		okBut.setBounds(247, 403, 79, 26);
		okBut.addActionListener(e -> {
			Font f = fontSample.getFont();
			editor.setFont(new Font(f.getFontName(), f.getStyle(), f.getSize()));
			scriptIndex = scriptSelection.getSelectedIndex();
			dialog.dispose();
		});
		dialog.add(okBut);

		JButton cancelBut = new JButton("Cancel");
		cancelBut.setFont(systemFont);
		cancelBut.setFocusPainted(false);
		cancelBut.setVerticalAlignment(SwingConstants.TOP);
		cancelBut.setBounds(333, 403, 79, 26);
		cancelBut.addActionListener(e -> dialog.dispose());
		dialog.add(cancelBut);

		dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dialog.dispose();
				System.gc();
			}
		});

		dialog.setVisible(true);
	}

	void fileMenu() {
		JMenu fileMenu = new JMenu(" File ");
		fileMenu.setBorder(null);
		PrinterJob printer = PrinterJob.getPrinterJob();

		JMenuItem newFile = new JMenuItem("New");
		newFile.setAccelerator(KeyStroke.getKeyStroke("control N"));
		newFile.addActionListener(e -> {
			if (app.getTitle().charAt(0) == '*') {
				eventQueue = "New";
				askToSaveDialog();
			}
			else {
				editor.setText("");
				fileName = "Untitled";
				openedFile = null;
				app.setTitle(fileName + " - Notepad");
			}
		});
		fileMenu.add(newFile);

		JMenuItem newWindow = new JMenuItem("New Window          ");
		newWindow.setAccelerator(KeyStroke.getKeyStroke("control shift N"));
		newWindow.addActionListener(e -> new Main());
		fileMenu.add(newWindow);

		JMenuItem openFile = new JMenuItem("Open...");
		openFile.setAccelerator(KeyStroke.getKeyStroke("control O"));
		openFile.addActionListener(e -> {
			if (app.getTitle().charAt(0) == '*') {
				eventQueue = "Open";
				askToSaveDialog();
			}
			else  openFile();
		});
		fileMenu.add(openFile);

		JMenuItem saveFile = new JMenuItem("Save...");
		saveFile.setAccelerator(KeyStroke.getKeyStroke("control S"));
		saveFile.addActionListener(e -> {
			if (fileName.equals("Untitled"))  saveFileAs();
			else  saveFile();
		});
		fileMenu.add(saveFile);

		JMenuItem saveFileAs = new JMenuItem("Save As...");
		saveFileAs.setAccelerator(KeyStroke.getKeyStroke("control shift S"));
		saveFileAs.addActionListener(e -> saveFileAs());
		fileMenu.add(saveFileAs);
		fileMenu.addSeparator();

		JMenuItem pageSetup = new JMenuItem("Page Setup...");
		pageSetup.addActionListener(e -> printer.pageDialog(printer.defaultPage()));
		fileMenu.add(pageSetup);

		JMenuItem printDialog = new JMenuItem("Print...");
		printDialog.setAccelerator(KeyStroke.getKeyStroke("control P"));
		printDialog.addActionListener(e -> {
			PrintRequestAttributeSet printerDialog = new HashPrintRequestAttributeSet();
			printerDialog.add(DialogTypeSelection.NATIVE);
			printerDialog.add(new DialogOwner(app));
			printer.printDialog(printerDialog);
		});
		fileMenu.add(printDialog);
		fileMenu.addSeparator();

		JMenuItem exitWindow = new JMenuItem("Exit");
		exitWindow.addActionListener(e -> app.dispatchEvent(new WindowEvent(app, WindowEvent.WINDOW_CLOSING)));
		fileMenu.add(exitWindow);

		for (int i = 0; i < fileMenu.getMenuComponentCount(); i++) {
			try { ((JMenuItem) fileMenu.getMenuComponent(i)).setMargin(new Insets(-1, -5, 1, 0)); }
			catch (Exception ignore) {}
		}
		menuBar.add(fileMenu);
	}

	JMenuItem undo = new JMenuItem("Undo");
	void editMenu() {
		JMenu editMenu = new JMenu(" Edit ");
		editMenu.setBorder(null);

		undo.setAccelerator(KeyStroke.getKeyStroke("control Z"));
		undo.setEnabled(false);
		undo.addActionListener(e -> {
			try { undoManager.undo(); }
			catch (CannotUndoException ignored) {}
		});
		editMenu.add(undo);
		editMenu.addSeparator();

		JMenuItem cut = new JMenuItem("Cut");
		cut.setAccelerator(KeyStroke.getKeyStroke("control X"));
		cut.setEnabled(false);
		cut.addActionListener(e -> {
			String text = editor.getSelectedText();
			if (text != null) {
				editor.setText(editor.getText().replace(text, ""));
				clipboard = text;
			}
		});
		editMenu.add(cut);

		JMenuItem copy = new JMenuItem("Copy");
		copy.setAccelerator(KeyStroke.getKeyStroke("control C"));
		copy.setEnabled(false);
		copy.addActionListener(e -> {
			String text = editor.getSelectedText();
			if (text != null)  clipboard = text;
		});
		editMenu.add(copy);

		JMenuItem paste = new JMenuItem("Paste");
		paste.setAccelerator(KeyStroke.getKeyStroke("control V"));
		paste.addActionListener(e -> {
			String text = editor.getSelectedText();
			if (text == null)  editor.setText(editor.getText() + clipboard);
			else  editor.setText(editor.getText().replace(text, "") + clipboard);
		});
		editMenu.add(paste);

		JMenuItem del = new JMenuItem("Delete");
		del.setAccelerator(KeyStroke.getKeyStroke((char) KeyEvent.VK_DELETE));
		del.setEnabled(false);
		del.addActionListener(e -> {
			String text = editor.getSelectedText();
			if (text != null)  editor.setText(editor.getText().replace(text, ""));
		});
		editMenu.add(del);
		editMenu.addSeparator();

		JMenuItem searchOnline = new JMenuItem("Search with Bing...               ");
		searchOnline.setAccelerator(KeyStroke.getKeyStroke("control E"));
		searchOnline.setEnabled(false);
		searchOnline.addActionListener(e -> {
			String text = editor.getSelectedText();
			if (text != null) {
				try { Desktop.getDesktop().browse(new URI("https://www.bing.com/search?q="
							+ URLEncoder.encode(text, StandardCharsets.UTF_8))); }
				catch (IOException | URISyntaxException ignore) {}
			}
		});
		editMenu.add(searchOnline);

		JMenuItem find = new JMenuItem("Find...");
		find.setAccelerator(KeyStroke.getKeyStroke("control F"));
		find.setEnabled(false);
		find.addActionListener(e -> findDialog());
		editMenu.add(find);

		JMenuItem findNext = new JMenuItem("Find Next");
		findNext.setAccelerator(KeyStroke.getKeyStroke("F3"));
		findNext.setEnabled(false);
		findNext.addActionListener(e -> {
			direction = "up";
			if (searchedWord == null)  findDialog();
			else  findNext(searchedWord);
		});
		editMenu.add(findNext);

		JMenuItem findPrevious = new JMenuItem("Find Previous");
		findPrevious.setAccelerator(KeyStroke.getKeyStroke("shift F3"));
		findPrevious.setEnabled(false);
		findPrevious.addActionListener(e -> {
			direction = "down";
			if (searchedWord == null)  findDialog();
			else  findNext(searchedWord);
		});
		editMenu.add(findPrevious);

		JMenuItem replace = new JMenuItem("Replace...");
		replace.setAccelerator(KeyStroke.getKeyStroke("control H"));
		replace.addActionListener(e -> replaceDialog());
		editMenu.add(replace);

		JMenuItem goTo = new JMenuItem("Go To...");
		goTo.setAccelerator(KeyStroke.getKeyStroke("control G"));
		goTo.addActionListener(e -> goToDialog());
		editMenu.add(goTo);
		editMenu.addSeparator();

		JMenuItem selectAll = new JMenuItem("Select All");
		selectAll.setAccelerator(KeyStroke.getKeyStroke("control A"));
		selectAll.addActionListener(e -> {
			editor.setSelectionStart(0);
			editor.setSelectionEnd(editor.getText().length());
		});
		editMenu.add(selectAll);

		JMenuItem timeDate = new JMenuItem("Time/Date");
		timeDate.setAccelerator(KeyStroke.getKeyStroke("F5"));
		timeDate.addActionListener(e -> editor.setText(editor.getText() + DateTimeFormatter.ofPattern("HH:mm:ss yyyy-MM-dd")
				.format(LocalDateTime.now())));
		editMenu.add(timeDate);

		for (int i = 0; i < editMenu.getMenuComponentCount(); i++) {
			try { ((JMenuItem) editMenu.getMenuComponent(i)).setMargin(new Insets(-1, -5, 1, 0)); }
			catch (Exception ignore) {}
		}
		menuBar.add(editMenu);
	}

	void formatMenu() {
		JMenu formatMenu = new JMenu(" Format ");
		formatMenu.setBorder(null);

		JCheckBoxMenuItem wrapWord = new JCheckBoxMenuItem("Word Wrap          ");
		wrapWord.addActionListener(e -> {
			if (wrapWord.getState()) {
				editor.setLineWrap(true);
				editor.setWrapStyleWord(true);
				scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			}
			else {
				editor.setLineWrap(false);
				editor.setWrapStyleWord(false);
				scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
			}
			infoPane.setVisible(infoPane.isVisible());
		});
		wrapWord.setMargin(new Insets(-1, -5, 1, 0));
		formatMenu.add(wrapWord);

		JMenuItem font = new JMenuItem("Font...");
		font.addActionListener(e -> fontDialog());
		font.setMargin(new Insets(-1, -5, 1, 0));
		formatMenu.add(font);

		menuBar.add(formatMenu);
	}

	void viewMenu() {
		JMenu viewMenu = new JMenu(" View ");
		viewMenu.setBorder(null);

		JMenu zoom = new JMenu("Zoom");
		zoom.setMargin(new Insets(-1, -5, 1, 0));

		JMenuItem zoomIn = new JMenuItem("Zoom In");
		zoomIn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, KeyEvent.CTRL_DOWN_MASK));
		zoomIn.setMargin(new Insets(-1, -5, 1, 0));
		zoomIn.addActionListener(e -> {
			Font font = editor.getFont();
			int zoomPercent = Integer.parseInt(zoomPercentLabel.getText()
					.substring(0, zoomPercentLabel.getText().length() - 1));
			if (zoomPercent < 500) {
				editor.setFont(new Font(font.getFontName(), font.getStyle(), font.getSize() + 2));
				zoomPercentLabel.setText((zoomPercent + 10) + "%");
			}
		});
		zoom.add(zoomIn);

		JMenuItem zoomOut = new JMenuItem("Zoom Out");
		zoomOut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, KeyEvent.CTRL_DOWN_MASK));
		zoomOut.setMargin(new Insets(-1, -5, 1, 0));
		zoomOut.addActionListener(e -> {
			Font font = editor.getFont();
			int zoomPercent = Integer.parseInt(zoomPercentLabel.getText()
					.substring(0, zoomPercentLabel.getText().length() - 1));
			if (zoomPercent > 50) {
				editor.setFont(new Font(font.getFontName(), font.getStyle(), font.getSize() - 2));
				zoomPercentLabel.setText((zoomPercent - 10) + "%");
			}
		});
		zoom.add(zoomOut);

		JMenuItem restoreZoom = new JMenuItem("Restore Default Zoom          ");
		restoreZoom.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, KeyEvent.CTRL_DOWN_MASK));
		restoreZoom.setMargin(new Insets(-1, -5, 1, 0));
		restoreZoom.addActionListener(e -> {
			Font font = editor.getFont();
			int zoomPercent = Integer.parseInt(zoomPercentLabel.getText().substring(0, zoomPercentLabel.getText().length() - 1));
			editor.setFont(new Font(font.getFontName(), font.getStyle(), font.getSize() - (zoomPercent - 100) / 5));
			zoomPercentLabel.setText("100%");
		});
		zoom.add(restoreZoom);
		viewMenu.add(zoom);

		editor.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, KeyEvent.CTRL_DOWN_MASK), "zoomIn");
		editor.getActionMap().put("zoomIn", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				Font font = editor.getFont();
				int zoomPercent = Integer.parseInt(zoomPercentLabel.getText().substring(0, zoomPercentLabel.getText().length() - 1));
				if (zoomPercent < 500) {
					editor.setFont(new Font(font.getFontName(), font.getStyle(), font.getSize() + 2));
					zoomPercentLabel.setText((zoomPercent + 10) + "%");
				}
			}
		});
		editor.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, KeyEvent.CTRL_DOWN_MASK), "zoomOut");
		editor.getActionMap().put("zoomOut", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				Font font = editor.getFont();
				int zoomPercent = Integer.parseInt(zoomPercentLabel.getText().substring(0, zoomPercentLabel.getText().length() - 1));
				if (zoomPercent > 50) {
					editor.setFont(new Font(font.getFontName(), font.getStyle(), font.getSize() - 2));
					zoomPercentLabel.setText((zoomPercent - 10) + "%");
				}
			}
		});

		JCheckBoxMenuItem hideStatusPane = new JCheckBoxMenuItem("Status Bar            ");
		hideStatusPane.setMargin(new Insets(-1, -5, 1, 0));
		hideStatusPane.setSelected(true);
		hideStatusPane.addChangeListener(e -> statusPane.setVisible(hideStatusPane.isSelected()));
		viewMenu.add(hideStatusPane);
		menuBar.add(viewMenu);
	}

	void helpMenu() {
		JMenu helpMenu = new JMenu(" Help ");
		helpMenu.setBorder(null);

		JMenuItem help = new JMenuItem("View Help");
		help.setMargin(new Insets(-1, -5, 1, 0));
		help.addActionListener(e -> {
			try { Desktop.getDesktop().browse(new URI("https://www.bing.com/search?q=get+help+with+notepad+in+windows+10&"
					+ "filters=guid:%224466414-en-dia%22%20lang:%22en%22&form=T00032&ocid=HelpPane-BingIA")); }
			catch (Exception ignore) {}
		});
		helpMenu.add(help);

		JMenuItem feedback = new JMenuItem("Send Feedback");
		feedback.setMargin(new Insets(-1, -5, 1, 0));
		feedback.addActionListener(e -> {
			try { Runtime.getRuntime().exec("cmd /c start feedback-hub:"); }
			catch (Exception ignore) {}
		});
		helpMenu.add(feedback);

		JMenuItem version = new JMenuItem("About Notepad              ");
		version.setMargin(new Insets(-1, -5, 1, 0));
		version.addActionListener(e -> aboutDialog());
		helpMenu.add(version);

		menuBar.add(helpMenu);
	}

	void aboutDialog() {
		JDialog dialog = new JDialog(app, "Blocking Dialog", true);
		dialog.setTitle("About Notepad");
		dialog.setSize(471, 462);
		dialog.setResizable(false);
		dialog.setLayout(null);
		dialog.setLocationRelativeTo(app);

		JLabel logo = new JLabel(new ImageIcon(Objects.requireNonNull(Main.class.getResource("Logo.png"))));
		logo.setBounds(-10, 27, 256, 256);
		dialog.add(logo);

		JLabel name = new JLabel("Notepad");
		name.setFont(new Font("Century Gothic", Font.BOLD, 32));
		name.setForeground(Color.gray);
		name.setBounds(47, 295, 236, 40);
		dialog.add(name);

		JPanel description = new JPanel();
		description.setLayout(new BoxLayout(description, BoxLayout.Y_AXIS));
		description.add(new JLabel("Name: Henry's Notepad"));
		description.add(new JLabel("Version: 1.0"));
		description.add(new JLabel("Release Date: August 6th, 2021"));
		description.add(new JLabel("Product ID: 1001-123456789"));
		description.add(new JLabel("Development Stage: Beta"));
		description.add(new JLabel("Developer: Henry Zhao"));
		description.add(new JLabel("Programming Language: Java"));
		description.add(new JLabel("Built with: Intellij IDEA"));
		for (int i = 0; i < description.getComponentCount(); i++) {
			description.getComponent(i).setFont(new Font("Segoe UI", Font.PLAIN, 14));
			description.getComponent(i).setForeground(Color.gray);
		}
		description.setBounds(245, 54, 220, 180);
		dialog.add(description);

		JLabel sourceCode = new JLabel("Source codes are on");
		sourceCode.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		sourceCode.setBounds(245, 238, 130, 15);
		dialog.add(sourceCode);

		JLabel github = new JLabel("<html><u>Github</u></html>");
		github.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		github.setForeground(Color.decode("#0078D7"));
		github.setBounds(373, 236, 42, 18);
		github.addMouseListener(new MouseAdapter() {
			public void mouseEntered(MouseEvent e) {
				github.setCursor(new Cursor(Cursor.HAND_CURSOR));
			}
			public void mouseClicked(MouseEvent e) {
				try {
					Desktop.getDesktop().browse(new URI("https://github.com/HenryZhao2020/Windows-Notepad"));
				} catch (Exception ignore) {}
			}
			public void mouseExited(MouseEvent e) {
				github.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
		});
		dialog.add(github);

		JLabel copyright = new JLabel("Microsoft Windows Notepad software and its user interface are protected by");
		copyright.setFont(systemFont);
		copyright.setForeground(Color.gray);
		copyright.setBounds(22, 364, 450, 15);
		dialog.add(copyright);

		copyright = new JLabel("trademark and other pending or exiting intellectual property rights in the");
		copyright.setFont(systemFont);
		copyright.setForeground(Color.gray);
		copyright.setBounds(22, 380, 450, 15);
		dialog.add(copyright);

		copyright = new JLabel("United States and other countries/regions");
		copyright.setFont(systemFont);
		copyright.setForeground(Color.gray);
		copyright.setBounds(22, 396, 450, 15);
		dialog.add(copyright);

		dialog.setVisible(true);
	}

	public static void main(String[] args) {
		try { UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel"); }
		catch (Exception ignored) {}
		new Main();
	}

}

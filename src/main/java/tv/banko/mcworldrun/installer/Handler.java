/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tv.banko.mcworldrun.installer;

import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import tv.banko.mcworldrun.installer.mod.ModInstaller;
import tv.banko.mcworldrun.installer.util.ArgumentParser;
import tv.banko.mcworldrun.installer.util.InstallerProgress;
import tv.banko.mcworldrun.installer.util.Utils;

public abstract class Handler implements InstallerProgress {
	protected static final int HORIZONTAL_SPACING = 4;
	protected static final int VERTICAL_SPACING = 6;

    public JButton buttonInstall;

	public JTextField installLocation;
	public JButton selectFolderButton;
	public JLabel statusLabel;

	private JPanel pane;


	public abstract String name();

	public abstract void install();

	public abstract void installCli(ArgumentParser args) throws Exception;

	public abstract String cliHelp();

	//this isnt great, but works
	public void setupPane1(JPanel pane, GridBagConstraints c, InstallerGui installerGui) {
	}

	public void setupPane2(JPanel pane, GridBagConstraints c, InstallerGui installerGui) {
	}

	public JPanel makePanel(InstallerGui installerGui) {
		pane = new JPanel(new GridBagLayout());
		pane.setBorder(new EmptyBorder(4, 4, 4, 4));

		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(VERTICAL_SPACING, HORIZONTAL_SPACING, VERTICAL_SPACING, HORIZONTAL_SPACING);
		c.gridx = c.gridy = 0;

		setupPane1(pane, c, installerGui);

		addRow(pane, c, "prompt.select.location",
				installLocation = new JTextField(20),
				selectFolderButton = new JButton());
		selectFolderButton.setText("...");
		selectFolderButton.setPreferredSize(new Dimension(installLocation.getPreferredSize().height, installLocation.getPreferredSize().height));
		selectFolderButton.addActionListener(e -> InstallerGui.selectInstallLocation(() -> installLocation.getText(), s -> installLocation.setText(s)));

		setupPane2(pane, c, installerGui);

		addRow(pane, c, "prompt.mod.version", new JLabel(ModInstaller.getModVersion()));

		addRow(pane, c, null, statusLabel = new JLabel(""));

		addLastRow(pane, c, null,
				buttonInstall = new JButton(Utils.BUNDLE.getString("prompt.install")));
		buttonInstall.addActionListener(e -> {
			buttonInstall.setEnabled(false);
			install();
		});

		statusLabel.setText(Utils.BUNDLE.getString("prompt.ready.install"));
		return pane;
	}

	protected LoaderVersion queryLoaderVersion() {
		return new LoaderVersion(ModInstaller.getLoaderVersion());
	}

	@Override
	public void updateProgress(String text) {
		statusLabel.setText(text);
		statusLabel.setForeground(UIManager.getColor("Label.foreground"));
	}

	public static String buildEditorPaneStyle() {
		JLabel label = new JLabel();
		Font font = label.getFont();
		Color color = label.getBackground();
		return String.format(Locale.ENGLISH,
				"font-family:%s;font-weight:%s;font-size:%dpt;background-color: rgb(%d,%d,%d);",
				font.getFamily(), (font.isBold() ? "bold" : "normal"), font.getSize(), color.getRed(), color.getGreen(), color.getBlue()
		);
	}

	@Override
	public void error(Throwable throwable) {
		StringWriter sw = new StringWriter(800);

		try (PrintWriter pw = new PrintWriter(sw)) {
			throwable.printStackTrace(pw);
		}

		String st = sw.toString().trim();
		System.err.println(st);

		String html = String.format("<html><body style=\"%s\">%s</body></html>",
				buildEditorPaneStyle(),
				st.replace(System.lineSeparator(), "<br>").replace("\t", "&ensp;"));
		JEditorPane textPane = new JEditorPane("text/html", html);
		textPane.setEditable(false);

		statusLabel.setText(throwable.getLocalizedMessage());
		statusLabel.setForeground(Color.RED);

		JOptionPane.showMessageDialog(pane,
				textPane,
				Utils.BUNDLE.getString("prompt.exception.occurrence"),
				JOptionPane.ERROR_MESSAGE);
	}

	protected void addRow(Container parent, GridBagConstraints c, String label, Component... components) {
		addRow(parent, c, false, label, components);
	}

	protected void addLastRow(Container parent, GridBagConstraints c, String label, Component... components) {
		addRow(parent, c, true, label, components);
	}

	protected static Component createSpacer() {
		return Box.createRigidArea(new Dimension(4, 0));
	}

	private void addRow(Container parent, GridBagConstraints c, boolean last, String label, Component... components) {
		if (label != null) {
			c.gridwidth = 1;
			c.anchor = GridBagConstraints.LINE_END;
			c.fill = GridBagConstraints.NONE;
			c.weightx = 0;
			parent.add(new JLabel(Utils.BUNDLE.getString(label)), c);
			c.gridx++;
			c.anchor = GridBagConstraints.LINE_START;
			c.fill = GridBagConstraints.HORIZONTAL;
		} else {
			c.gridwidth = 2;
			if (last) c.weighty = 1;
			c.anchor = last ? GridBagConstraints.PAGE_START : GridBagConstraints.CENTER;
			c.fill = GridBagConstraints.NONE;
		}

		c.weightx = 1;

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

		for (Component comp : components) {
			panel.add(comp);
		}

		parent.add(panel, c);

		c.gridy++;
		c.gridx = 0;
	}

	protected String getGameVersion() {
		return ModInstaller.getMinecraftVersion();
	}

	protected String getLoaderVersion() {
		return ModInstaller.getLoaderVersion();
	}
}

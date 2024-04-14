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

package tv.banko.gamersedition.installer.client;

import java.awt.*;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;

import tv.banko.gamersedition.installer.Handler;
import tv.banko.gamersedition.installer.InstallerGui;
import tv.banko.gamersedition.installer.LoaderVersion;
import tv.banko.gamersedition.installer.mod.JavaInstaller;
import tv.banko.gamersedition.installer.mod.ModInstaller;
import tv.banko.gamersedition.installer.util.ArgumentParser;
import tv.banko.gamersedition.installer.util.InstallerProgress;
import tv.banko.gamersedition.installer.util.NoopCaret;
import tv.banko.gamersedition.installer.util.Utils;

import net.fabricmc.installer.launcher.MojangLauncherHelperWrapper;

public class ClientHandler extends Handler {

	@Override
	public String name() {
		return "Client";
	}

	@Override
	public void install() {
		if (MojangLauncherHelperWrapper.isMojangLauncherOpen()) {
			showLauncherOpenMessage();
			return;
		}

		doInstall();
	}

	private void doInstall() {
		String gameVersion = "1.20.4";
		LoaderVersion loaderVersion = queryLoaderVersion();
		if (loaderVersion == null) return;

		System.out.println("Installing");

		new Thread(() -> {
			try {
				updateProgress(new MessageFormat(Utils.BUNDLE.getString("progress.installing")).format(new Object[]{loaderVersion.name}));
				Path mcPath = Paths.get(installLocation.getText());

				if (!Files.exists(mcPath)) {
					throw new RuntimeException(Utils.BUNDLE.getString("progress.exception.no.launcher.directory"));
				}

				final ProfileInstaller profileInstaller = new ProfileInstaller(mcPath);
				ProfileInstaller.LauncherType launcherType;

				List<ProfileInstaller.LauncherType> types = profileInstaller.getInstalledLauncherTypes();

				if (types.isEmpty()) {
					throw new RuntimeException(Utils.BUNDLE.getString("progress.exception.no.launcher.profile"));
				} else if (types.size() == 1) {
					launcherType = types.get(0);
				} else {
					launcherType = showLauncherTypeSelection();

					if (launcherType == null) {
						// canceled
						statusLabel.setText(Utils.BUNDLE.getString("prompt.ready.install"));
						return;
					}
				}

				String profileName = ClientInstaller.install(mcPath, gameVersion, loaderVersion, this);

				if (launcherType == null) {
					throw new RuntimeException(Utils.BUNDLE.getString("progress.exception.no.launcher.profile"));
				}

				profileInstaller.setupProfile(profileName, gameVersion, launcherType);

				SwingUtilities.invokeLater(() -> showInstalledMessage(loaderVersion.name, gameVersion, mcPath.resolve("mods")));
			} catch (Exception e) {
				error(e);
			} finally {
				buttonInstall.setEnabled(true);
			}
		}).start();
	}

	private void showInstalledMessage(String loaderVersion, String gameVersion, Path modsDirectory) {
		StringBuilder content = new StringBuilder();
		content.append("<html><body style=\"").append(buildEditorPaneStyle()).append("\">");
		content.append(new MessageFormat(Utils.BUNDLE.getString("prompt.install.successful.fabric"))
				.format(new Object[]{loaderVersion, gameVersion}));

		if (JavaInstaller.INSTALLED) {
			content.append("<br>").append(Utils.BUNDLE.getString("prompt.install.successful.java"));
		}

		content.append("<br>").append(String.format(Utils.BUNDLE.getString("prompt.install.successful.mod"),
						ModInstaller.getModVersion()))
				.append("<br>").append(Utils.BUNDLE.getString("prompt.install.successful.info"))
				.append("</body></html>");

		JEditorPane pane = new JEditorPane("text/html", content.toString());
		pane.setBackground(new Color(0, 0, 0, 0));
		pane.setEditable(false);
		pane.setCaret(new NoopCaret());

		pane.addHyperlinkListener(e -> {
			try {
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					if (e.getDescription().equals("fabric://mods")) {
						Desktop.getDesktop().open(modsDirectory.toFile());
					} else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
						Desktop.getDesktop().browse(e.getURL().toURI());
					} else {
						throw new UnsupportedOperationException("Failed to open " + e.getURL().toString());
					}
				}
			} catch (Throwable throwable) {
				error(throwable);
			}
		});

		final Image iconImage = Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemClassLoader().getResource("icon.png"));
		JOptionPane.showMessageDialog(
				null,
				pane,
				Utils.BUNDLE.getString("prompt.install.successful.title"),
				JOptionPane.INFORMATION_MESSAGE,
				new ImageIcon(iconImage.getScaledInstance(64, 64, Image.SCALE_DEFAULT))
		);
	}

	private ProfileInstaller.LauncherType showLauncherTypeSelection() {
		Object[] options = {Utils.BUNDLE.getString("prompt.launcher.type.xbox"), Utils.BUNDLE.getString("prompt.launcher.type.win32")};

		int result = JOptionPane.showOptionDialog(null,
				Utils.BUNDLE.getString("prompt.launcher.type.body"),
				Utils.BUNDLE.getString("installer.title"),
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null,
				options,
				options[0]
		);

		if (result == JOptionPane.CLOSED_OPTION) {
			return null;
		}

		return result == JOptionPane.YES_OPTION ? ProfileInstaller.LauncherType.MICROSOFT_STORE : ProfileInstaller.LauncherType.WIN32;
	}

	private void showLauncherOpenMessage() {
		int result = JOptionPane.showConfirmDialog(null, Utils.BUNDLE.getString("prompt.launcher.open.body"), Utils.BUNDLE.getString("prompt.launcher.open.tile"), JOptionPane.YES_NO_OPTION);

		if (result == JOptionPane.YES_OPTION) {
			doInstall();
		} else {
			buttonInstall.setEnabled(true);
		}
	}

	@Override
	public void installCli(ArgumentParser args) throws Exception {
		Path path = Paths.get(args.getOrDefault("dir", () -> Utils.findDefaultInstallDir().toString()));

		if (!Files.exists(path)) {
			throw new FileNotFoundException("Launcher directory not found at " + path);
		}

		String gameVersion = getGameVersion();
		LoaderVersion loaderVersion = new LoaderVersion(getLoaderVersion());

		String profileName = ClientInstaller.install(path, gameVersion, loaderVersion, InstallerProgress.CONSOLE);

		if (args.has("noprofile")) {
			return;
		}

		ProfileInstaller profileInstaller = new ProfileInstaller(path);
		List<ProfileInstaller.LauncherType> types = profileInstaller.getInstalledLauncherTypes();
		ProfileInstaller.LauncherType launcherType = null;

		if (args.has("launcher")) {
			launcherType = ProfileInstaller.LauncherType.valueOf(args.get("launcher").toUpperCase(Locale.ROOT));
		}

		if (launcherType == null) {
			if (types.size() == 0) {
				throw new FileNotFoundException("Could not find a valid launcher profile .json");
			} else if (types.size() == 1) {
				// Only 1 launcher type found, install to that.
				launcherType = types.get(0);
			} else {
				throw new FileNotFoundException("Multiple launcher installations were found, please specify the target launcher using -launcher");
			}
		}

		profileInstaller.setupProfile(profileName, gameVersion, launcherType);
	}

	@Override
	public String cliHelp() {
		return "-dir <install dir> -mcversion <minecraft version, default latest> -loader <loader version, default latest> -launcher [win32, microsoft_store]";
	}

	@Override
	public void setupPane2(JPanel pane, GridBagConstraints c, InstallerGui installerGui) {
		installLocation.setText(Utils.findDefaultInstallDir().toString());
	}
}

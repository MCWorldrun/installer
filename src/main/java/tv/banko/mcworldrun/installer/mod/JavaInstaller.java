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

package tv.banko.mcworldrun.installer.mod;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import javax.swing.*;

import tv.banko.mcworldrun.installer.Handler;
import tv.banko.mcworldrun.installer.InstallerGui;
import tv.banko.mcworldrun.installer.util.InstallerProgress;
import tv.banko.mcworldrun.installer.util.Utils;

public class JavaInstaller {

	public static boolean INSTALLED = false;

	public static String install(InstallerProgress progress) throws JavaInstallationException {
		try {
			INSTALLED = false;
			System.out.println("Installing Java 17...");

			String osName = System.getProperty("os.name").toLowerCase();

			if (hasJava17OrHigher()) {
				return "java.success.already-installed";
			}

			if (osName.contains("win")) {
				progress.updateProgress(Utils.BUNDLE.getString("java.downloading"));
				return installJavaWindows(progress);
			}

			if (osName.contains("mac")) {
				progress.updateProgress(Utils.BUNDLE.getString("java.downloading"));
				return installJavaMacOS();
			}

			if (osName.contains("nux") || osName.contains("nix") || osName.contains("aix")) {
				progress.updateProgress(Utils.BUNDLE.getString("java.downloading"));
				return installJavaLinux();
			}

			throw new JavaInstallationException(Utils.BUNDLE.getString("java.error.unsupported"));
		} catch (JavaInstallationException e) {
			openWebpage();
			throw new RuntimeException(e);
		}
	}

	private static boolean hasJava17OrHigher() {
		String version = System.getProperty("java.version");
		if (version.startsWith("1.")) {
			version = version.substring(2, 3);
		} else {
			int dot = version.indexOf(".");
			if (dot != -1) {
				version = version.substring(0, dot);
			}
		}

		System.out.println("Found Java Version " + version);
		return Integer.parseInt(version) >= 17;
	}

	private static String installJavaWindows(InstallerProgress progress) throws JavaInstallationException {
		System.out.println("Installing Java 17 on Windows...");

		String name = "jdk.exe";
		String command = "cmd.exe /c curl -o " + name + " https://download.oracle.com/java/17/latest/jdk-17_windows-x64_bin.exe";
		runSystemCommand(command);

		progress.updateProgress(Utils.BUNDLE.getString("java.installing"));

		try {
			Runtime.getRuntime().exec("./" + name, null, new File("./"));
		} catch (IOException e) {
			openInstallJDKPrompt(name);
			throw new RuntimeException(Utils.BUNDLE.getString("prompt.follow-steps"));
		}
		return "java.success.installed";
	}

	private static String installJavaMacOS() throws JavaInstallationException {
		System.out.println("Installing Java 17 on macOS...");

		String url;

		switch (System.getProperty("os.arch")) {
			case "x86_64":
				url = "https://download.oracle.com/java/17/latest/jdk-17_macos-x64_bin.dmg";
				break;
			case "aarch64":
				url = "https://download.oracle.com/java/17/latest/jdk-17_macos-aarch64_bin.dmg";
				break;
			default:
				throw new JavaInstallationException(Utils.BUNDLE.getString("java.error.unknown-arch"));
		}

		String name = "jdk.dmg";
		String command = "/bin/bash -c \"curl -o " + name + " " + url + "\"";
		runSystemCommand(command);

		openInstallJDKPrompt(name);
		throw new RuntimeException(Utils.BUNDLE.getString("prompt.follow-steps"));
	}

	private static String installJavaLinux() throws JavaInstallationException {
		System.out.println("Installing Java 17 on Linux...");
		String command = "/bin/bash -c \"sudo apt-get update && sudo apt-get install openjdk-17-jdk\"";
		return runSystemCommand(command);
	}

	private static String runSystemCommand(String command) throws JavaInstallationException {
		try {
			Process process = Runtime.getRuntime().exec(command);
			process.waitFor();
			if (process.exitValue() == 0) {
				INSTALLED = true;
				return "java.success.installed";
			}

			InputStream stream = process.getErrorStream();

			StringBuilder error = new StringBuilder();
			try (Reader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
				int c;
				while ((c = reader.read()) != -1) {
					error.append((char) c);
				}
			}

			throw new JavaInstallationException(String.format(Utils.BUNDLE.getString("java.error.unknown"), process.exitValue(), error));
		} catch (Exception e) {
			throw new JavaInstallationException(e.getLocalizedMessage());
		}
	}

	private static void openInstallJDKPrompt(String name) {
		String html = String.format("<html><body style=\"%s\">%s</body></html>",
				Handler.buildEditorPaneStyle(),
				String.format(Utils.BUNDLE.getString("prompt.java.execute-file.body"), name)
						.replace("\n", "<br>")
						.replace("\t", "&ensp;"));
		JEditorPane textPane = new JEditorPane("text/html", html);
		textPane.setEditable(false);

		JOptionPane.showMessageDialog(InstallerGui.instance,
				textPane,
				Utils.BUNDLE.getString("prompt.java.execute-file.title"),
				JOptionPane.QUESTION_MESSAGE);
	}

	private static boolean openWebpage() {
		String html = String.format("<html><body style=\"%s\">%s</body></html>",
				Handler.buildEditorPaneStyle(),
				Utils.BUNDLE.getString("prompt.java.install-manually.body")
						.replace("\n", "<br>")
						.replace("\t", "&ensp;"));
		JEditorPane textPane = new JEditorPane("text/html", html);
		textPane.setEditable(false);

		JOptionPane.showMessageDialog(InstallerGui.instance,
				textPane,
				Utils.BUNDLE.getString("prompt.java.install-manually.title"),
				JOptionPane.QUESTION_MESSAGE);

		Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
		if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
			try {
				desktop.browse(URI.create("https://www.oracle.com/java/technologies/downloads/#java17"));
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	public static class JavaInstallationException extends RuntimeException {
		public JavaInstallationException(String message) {
			super(message);
		}
	}

}

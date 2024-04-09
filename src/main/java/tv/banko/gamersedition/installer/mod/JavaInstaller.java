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

package tv.banko.gamersedition.installer.mod;

import tv.banko.gamersedition.installer.InstallerGui;
import tv.banko.gamersedition.installer.util.Utils;

import javax.swing.*;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class JavaInstaller {

	public static boolean INSTALLED = false;

	public static String install() throws JavaInstallationException {
		INSTALLED = false;
		System.out.println("Installing Java 17...");

		String osName = System.getProperty("os.name").toLowerCase();

		if (hasJava17OrHigher()) {
			return "java.success.already-installed";
		}

		if (osName.contains("win")) {
			return installJavaWindows();
		}

		if (osName.contains("mac")) {
			return installJavaMacOS();
		}

		if (osName.contains("nux") || osName.contains("nix") || osName.contains("aix")) {
			return installJavaLinux();
		}

		throw new JavaInstallationException(Utils.BUNDLE.getString("java.error.unsupported"));
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

	private static String installJavaWindows() throws JavaInstallationException {
		System.out.println("Installing Java 17 on Windows...");
		String command = "cmd.exe /c curl -o jdk.exe https://download.oracle.com/java/17/latest/jdk-17_windows-x64_bin.exe";
		runSystemCommand(command);

        try {
            Runtime.getRuntime().exec("./jdk.exe", null, new File("./"));
        } catch (IOException e) {
			throw new JavaInstallationException(Utils.BUNDLE.getString("java.error.elevation"));
        }
		return "java.success.installed";
    }

	private static String installJavaMacOS() throws JavaInstallationException {
		System.out.println("Installing Java 17 on macOS...");

		JOptionPane.showMessageDialog(InstallerGui.instance,
				null,
				Utils.BUNDLE.getString("prompt.java.install-manually"),
				JOptionPane.INFORMATION_MESSAGE);

		return "";
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

	public static class JavaInstallationException extends RuntimeException {
		public JavaInstallationException(String message) {
			super(message);
		}
	}

}

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.xml.stream.XMLStreamException;

import tv.banko.mcworldrun.installer.client.ClientHandler;
import tv.banko.mcworldrun.installer.mod.ModInstaller;
import tv.banko.mcworldrun.installer.util.FabricService;
import tv.banko.mcworldrun.installer.util.OperatingSystem;
import tv.banko.mcworldrun.installer.util.ArgumentParser;

public class Main {

	public static final List<Handler> HANDLERS = new ArrayList<>();
	public static final ModInstaller MOD_MANAGER = new ModInstaller();

	public static void main(String[] args) throws IOException {
		if (OperatingSystem.CURRENT == OperatingSystem.WINDOWS) {
			System.setProperty("javax.net.ssl.trustStoreType", "WINDOWS-ROOT");
		}

		System.out.println("Loading Fabric Installer: " + Main.class.getPackage().getImplementationVersion());

		HANDLERS.add(new ClientHandler());

		ArgumentParser argumentParser = ArgumentParser.create(args);

		String metaUrl = argumentParser.has("metaurl") ? argumentParser.get("metaurl") : null;
		String mavenUrl = argumentParser.has("mavenurl") ? argumentParser.get("mavenurl") : null;

		if (metaUrl != null || mavenUrl != null) {
			FabricService.setFixed(metaUrl, mavenUrl);
		}

		try {
			InstallerGui.start();
		} catch (ClassNotFoundException | UnsupportedLookAndFeelException | InstantiationException |
				 IllegalAccessException | XMLStreamException e) {
			throw new RuntimeException(e);
		}
	}
}

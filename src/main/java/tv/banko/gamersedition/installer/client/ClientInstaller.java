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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;

import mjson.Json;
import tv.banko.gamersedition.installer.Handler;
import tv.banko.gamersedition.installer.InstallerGui;
import tv.banko.gamersedition.installer.LoaderVersion;
import tv.banko.gamersedition.installer.mod.JavaInstaller;
import tv.banko.gamersedition.installer.mod.ModInstaller;
import tv.banko.gamersedition.installer.util.FabricService;
import tv.banko.gamersedition.installer.util.InstallerProgress;
import tv.banko.gamersedition.installer.util.Library;
import tv.banko.gamersedition.installer.util.Reference;
import tv.banko.gamersedition.installer.util.Utils;

import javax.swing.*;

public class ClientInstaller {

	public static String install(Path mcDir, String gameVersion, LoaderVersion loaderVersion, InstallerProgress progress) throws IOException {
		System.out.println("Installing " + gameVersion + " with fabric " + loaderVersion.name);

		String profileName = String.format("%s-%s-%s", Reference.LOADER_NAME, loaderVersion.name, gameVersion);

		Path versionsDir = mcDir.resolve("versions");
		Path profileDir = versionsDir.resolve(profileName);
		Path profileJson = profileDir.resolve(profileName + ".json");

		if (!Files.exists(profileDir)) {
			Files.createDirectories(profileDir);
		}

		Path profileJar = profileDir.resolve(profileName + ".jar");
		Files.deleteIfExists(profileJar);

		Json json = FabricService.queryMetaJson(String.format("v2/versions/loader/%s/%s/profile/json", gameVersion, loaderVersion.name));
		Files.write(profileJson, json.toString().getBytes(StandardCharsets.UTF_8));

		progress.updateProgress(Utils.BUNDLE.getString("java.installing"));
		try {
			String message = JavaInstaller.install();
			progress.updateProgress(message);
		} catch (JavaInstaller.JavaInstallationException e) {
			progress.updateProgress(e.getLocalizedMessage());
			throw new RuntimeException(e);
		}

		progress.updateProgress(String.format(Utils.BUNDLE.getString("mod.installing"), ModInstaller.getModVersion()));
		try {
			progress.updateProgress(ModInstaller.install(mcDir));
		} catch (ModInstaller.ModInstallationException e) {
			progress.updateProgress(e.getLocalizedMessage());
			throw new RuntimeException(e);
		}

		/*
		Downloading the libraries isn't strictly necessary as the launcher will do it for us.
		Do it anyway in case the launcher fails, we know we have a working connection to maven here.
		 */
		Path libsDir = mcDir.resolve("libraries");

		for (Json libraryJson : json.at("libraries").asJsonList()) {
			Library library = new Library(libraryJson);
			Path libraryFile = libsDir.resolve(library.getPath());
			String url = library.getURL();

			//System.out.println("Downloading "+url+" to "+libraryFile);
			progress.updateProgress(new MessageFormat(Utils.BUNDLE.getString("progress.download.library.entry")).format(new Object[]{library.name}));
			FabricService.downloadSubstitutedMaven(url, libraryFile);
		}


		progress.updateProgress(Utils.BUNDLE.getString("progress.done"));

		return profileName;
	}
}

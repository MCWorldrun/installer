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

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import tv.banko.gamersedition.installer.util.Reference;
import tv.banko.gamersedition.installer.util.Utils;

public class ModInstaller {

	private static String MOD_VERSION = null;
	private static String MINECRAFT_VERSION = null;
	private static String LOADER_VERSION = null;

	public static String install(Path mcDir) throws ModInstallationException {
		Path mods = mcDir.resolve("mods");
		Path backupMods = mcDir.resolve("mods_" + System.currentTimeMillis());

		try {
			try (Stream<Path> stream = Files.list(mods)) {
				if (stream.findAny().isPresent()) {
					System.out.println("Backing up mods to " + backupMods);
					Files.move(mods, backupMods, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		} catch (IOException ignored) {
		}

		try {
			Path mod = mods.resolve("gamers-edition.jar");
			System.out.println("Downloading " + Reference.GAMERS_EDITION_DOWNLOAD + " to " + mod);
			Utils.downloadFile(new URL(Reference.GAMERS_EDITION_DOWNLOAD), mod);

			Path api = mods.resolve("fabric-api.jar");
			System.out.println("Downloading " + Reference.FABRIC_API_DOWNLOAD + " to " + api);
			Utils.downloadFile(new URL(Reference.FABRIC_API_DOWNLOAD), api);

			return String.format(Utils.BUNDLE.getString("mod.success"), getModVersion());
		} catch (IOException e) {
			throw new ModInstallationException(Utils.BUNDLE.getString("mod.error.download"));
		}
	}

	public static String getModVersion() {
		if (MOD_VERSION == null)
			loadVersions();
		return MOD_VERSION;
	}

	public static String getMinecraftVersion() {
		if (MINECRAFT_VERSION == null)
			loadVersions();
		return MINECRAFT_VERSION;
	}

	public static String getLoaderVersion() {
		if (LOADER_VERSION == null)
			loadVersions();
		return LOADER_VERSION;
	}

	private static void loadVersions() {
		try {
			String read = Utils.readString(new URL(Reference.GAMERS_EDITION_VERSION));

			JsonObject object = JsonParser.parseString(read).getAsJsonObject();

			if (!object.has("mod_version")) {
				throw new ModInstallationException("Mod version not found in version.json");
			}

			if (!object.has("minecraft_version")) {
				throw new ModInstallationException("Minecraft version not found in version.json");
			}

			if (!object.has("loader_version")) {
				throw new ModInstallationException("Loader version not found in version.json");
			}

			MOD_VERSION = object.get("mod_version").getAsString();
			MINECRAFT_VERSION = object.get("minecraft_version").getAsString();
			LOADER_VERSION = object.get("loader_version").getAsString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static class ModInstallationException extends RuntimeException {
		public ModInstallationException(String message) {
			super(message);
		}
	}

}

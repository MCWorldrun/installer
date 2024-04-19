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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import tv.banko.mcworldrun.installer.util.Reference;
import tv.banko.mcworldrun.installer.util.Utils;

public class ModInstaller {

	private static String MOD_VERSION = null;
	private static String MINECRAFT_VERSION = null;
	private static String LOADER_VERSION = null;

	public static List<Mod> MODS = new ArrayList<>();

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
			Path mod = mods.resolve("mcworldrun.jar");
			System.out.println("Downloading " + Reference.GAMERS_EDITION_DOWNLOAD + " to " + mod);
			Utils.downloadFile(new URL(Reference.GAMERS_EDITION_DOWNLOAD), mod);

			Arrays.stream(Mod.values()).filter(Mod::isRequired).forEach(MODS::add);

			for (Mod additionalMod : MODS) {
				Path modPath = mods.resolve(additionalMod.getFileName() + ".jar");
				System.out.println("Adding " + additionalMod.getFileName() + " to " + modPath);
				InputStream stream = ModInstaller.class.getClassLoader()
						.getResourceAsStream("mod/" + additionalMod.getFileName() + ".jar");
				FileOutputStream out = new FileOutputStream(modPath.toFile());
				byte[] buffer = new byte[1024];
				int length;
				while (true) {
                    assert stream != null;
                    if (!((length = stream.read(buffer)) > 0)) break;
                    out.write(buffer, 0, length);
				}
				out.close();
				stream.close();
				System.out.println("Added " + additionalMod.getName());
			}

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

	public enum Mod {
		SODIUM("Sodium", true, false),
		FULLBRIGHT("Fullbright", true, false),
		FABRIC_API("Fabric-API", true, true);

		private final String name;
		private final boolean defaultEnabled;
		private final boolean required;

		Mod(String name, boolean defaultEnabled, boolean required) {
			this.name = name;
			this.defaultEnabled = defaultEnabled;
            this.required = required;
        }

		public String getName() {
			return name;
		}

		public String getFileName() {
			return name.toLowerCase();
		}

		public boolean isDefaultEnabled() {
			return defaultEnabled;
		}

		public boolean isRequired() {
			return required;
		}

		public static List<Mod> getMods() {
			return Arrays.stream(values()).filter(mod -> !mod.isRequired())
					.collect(Collectors.toList());
		}
	}

}

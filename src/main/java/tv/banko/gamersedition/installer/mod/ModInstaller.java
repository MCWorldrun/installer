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

import tv.banko.gamersedition.installer.util.Reference;
import tv.banko.gamersedition.installer.util.Utils;

public class ModInstaller {

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
        } catch (IOException ignored) { }

        try {
			Path path = mods.resolve("gamers-edition.jar");
			System.out.println("Downloading " + Reference.GAMERS_EDITION_DOWNLOAD);
			Utils.downloadFile(new URL(Reference.GAMERS_EDITION_DOWNLOAD), path);
			System.out.println("Downloaded to " + path);
			return String.format(Utils.BUNDLE.getString("mod.success"), getModVersion());
		} catch (IOException e) {
			throw new ModInstallationException(Utils.BUNDLE.getString("mod.error.download"));
		}
	}

	public static String getModVersion() {
		return "0.0.1-ab21bac21";
	}

	public static String getMinecraftVersion() {
		return "1.20.4";
	}

	public static String getLoaderVersion() {
		return "0.15.3";
	}

	public static class ModInstallationException extends RuntimeException {
		public ModInstallationException(String message) {
			super(message);
		}
	}

}

package io.github.skepter.forgedetector;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class ModStorage {

	private File file;

	public ModStorage(JavaPlugin plugin) {
		file = new File(plugin.getDataFolder(), "playermods.txt");
		if(!file.exists()) {
			plugin.getDataFolder().mkdirs();
			try {
				file.createNewFile();
			} catch (IOException e) {
				Bukkit.getLogger().severe("Could not create mod storage file!");
			}
		}
	}

	public Map<String, TreeSet<Mod>> get() {
		Map<String, TreeSet<Mod>> players = new HashMap<String, TreeSet<Mod>>();
		if (getFromFile() == null) {
			return players;
		}
		for (String str : getFromFile()) {
			String player = str.split(":")[0];
			String[] mods = str.split(":")[1].split(";");
			TreeSet<Mod> modSet = new TreeSet<Mod>();
			for(String modStr : mods) {
				modSet.add(Mod.fromString(modStr));
			}
			players.put(player, modSet);
		}
		return players;
	}

	private String treeSetToString(TreeSet<Mod> set) {
		String str = "";
		for(Mod mod : set) {
			str = str + mod.toString() + ";";
		}
		return str.substring(0, str.length() - 1);
	}

	private List<String> getFromFile() {
		try {
			List<String> lines = new ArrayList<String>();
			final BufferedReader in = new BufferedReader(
					new InputStreamReader(new FileInputStream(file)));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				lines.add(inputLine);
			}
			in.close();
			return lines;
		} catch (Exception e) {
			Bukkit.getLogger().severe("Could not get mod list!");
		}
		return null;
	}

	public void store(Map<String, TreeSet<Mod>> players) {
		file.delete();
		try {
			file.createNewFile();
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			for(String str : players.keySet()) {
				bw.write(str + ":" + treeSetToString(players.get(str)));
				bw.newLine();
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			Bukkit.getLogger().severe("Could not store mod list!");
		}
	}

}

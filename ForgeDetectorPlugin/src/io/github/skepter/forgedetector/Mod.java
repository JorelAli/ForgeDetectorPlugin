package io.github.skepter.forgedetector;

public class Mod implements Comparable<Mod>{

	private String name;
	private String version;
	
	enum ModType {
		FORGE("Forge"), LITEMOD("Litemod");
		
		private String modTypeName;
		
		ModType(String str) {
			this.modTypeName = str;
		}
		
		public String getModTypeName() {
			return modTypeName;
		}
	}
	
	private ModType type;
	
	public Mod(String name, String version, ModType type) {
		this.name = name;
		this.version = version;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public ModType getType() {
		return type;
	}
	
	@Override
	public String toString() {
		return type.name() + "|" + name + "|" + version;
	}
	
	
	public static Mod fromString(String inputString) {
		String[] data = inputString.split("\\|");
		return new Mod(data[1], data[2], ModType.valueOf(data[0]));
	}

	@Override
	public int compareTo(Mod mod) {
		int i = (type == mod.getType() ? 0 : 1);
		if(i != 0) return i;
		
		i = name.compareTo(mod.getName());
		if(i != 0) return i;
		
		return 0;
	}
}

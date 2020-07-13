package io.github.skepter.forgedetector;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeSet;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerOptions;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketPostListener;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.MinecraftKey;
import com.comphenix.protocol.wrappers.WrappedServerPing;

import io.github.skepter.forgedetector.Mod.ModType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.server.v1_15_R1.PacketDataSerializer;

public class Main_Old extends JavaPlugin implements Listener {
	
	private Map<String, TreeSet<Mod>> players;
	private ModStorage storage;
	
	/**
	 * Gets the player's name from the players map
	 */
	private String getName(String name) {
		if(players.keySet().contains(name)) 
			return name;
		
		for(String player : players.keySet()) 
			if(name.equalsIgnoreCase(player)) 
				return player;
			
		for(String player : players.keySet()) 
			if(player.toLowerCase().startsWith(name.toLowerCase())) 
				return player;
			
		return name;
	}
	
	/**
	 * Displays the list of mods for a specific player to the sender
	 * @param sender - the sender to display the mods
	 * @param playerInput - the player to look up mods for
	 */
	private void displayMods(CommandSender sender, String playerInput) {
		for(Mod mod : players.getOrDefault(playerInput, new TreeSet<Mod>())) {
			if(mod.getType().equals(ModType.FORGE)) {
				sender.sendMessage(" > " + ChatColor.YELLOW + "Forge: " + mod.getName() + ChatColor.WHITE + " " + mod.getVersion());
			} else if(mod.getType().equals(ModType.LITEMOD)) {
				sender.sendMessage(" > " + ChatColor.GREEN + "Litemod: " + mod.getName() + ChatColor.WHITE + " " + mod.getVersion());
			}
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(label.equalsIgnoreCase("mods") || label.equalsIgnoreCase("mod")) {
			if(args.length == 0) {
				for(String str : players.keySet()) {
					sender.sendMessage("Looking up mods for: " + ChatColor.GREEN + str);
					displayMods(sender, str);
				}
			} else if(args.length == 1) {
				sender.sendMessage("Looking up mods for: " + ChatColor.GREEN + getName(args[0]));
				if(!players.containsKey(getName(args[0]))) {
					sender.sendMessage(" No mods found");
				} else {
					displayMods(sender, getName(args[0]));
				}
			}
			
			return true;
		}
		return false;
	}
	
	@EventHandler
	public void onConnect(PlayerLoginEvent e) {
		Player p = e.getPlayer();
//		p.sendPluginMessage(this, "fml:handshake", new byte[] {0, 2, 0});
//		p.sendPluginMessage(this, "REGISTER", "FML|HS FML FML|MP FML FORGE".getBytes());
		p.sendPluginMessage(this, "fml:handshake", new byte[] {0, 2, 0, 0, 0, 0});
	}
	
	public Object getPacketDataSerializerOf(byte[] bytes) {
		ByteBuf buffer = PacketContainer.createPacketBuffer();
		buffer.writeBytes(bytes);
		return MinecraftReflection.getPacketDataSerializer(buffer);
	}
	
	@Override
	public void onEnable() {	
		storage = new ModStorage(this);
		players = storage.get();
//		getServer().getMessenger().registerOutgoingPluginChannel(this, "REGISTER");
//		getServer().getMessenger().registerOutgoingPluginChannel(this, "fml:handshake");
//		getServer().getPluginManager().registerEvents(this, this);
		
		getCommand("mods").setExecutor(this);
		//Protocol manager
		ProtocolManager protManager = ProtocolLibrary.getProtocolManager();
//		
//		
		
		
		
		protManager.addPacketListener(new PacketAdapter(this, PacketType.Status.Server.SERVER_INFO) {
			@Override
			public void onPacketSending(PacketEvent event) {
								
//				event.getNetworkMarker().addPostListener(new PacketPostListener() {
//
//					@Override
//					public Plugin getPlugin() {
//						return Main.getInstance();
//					}
//
//					@Override
//					public void onPostEvent(PacketEvent arg0) {
//
//						{
//							PacketContainer cPacket = new PacketContainer(PacketType.Play.Server.CUSTOM_PAYLOAD);
//							cPacket.getMinecraftKeys().write(0, new MinecraftKey("fml", "netversion"));
//							cPacket.getModifier().write(1, getPacketDataSerializerOf("FML2".getBytes()));
//							try {
//								ProtocolLibrary.getProtocolManager().sendServerPacket(event.getPlayer(), cPacket);
//							} catch (InvocationTargetException e) {
//								e.printStackTrace();
//							}
//						}
//						
//					}
//				});
				
				WrappedServerPing serverPing = event.getPacket().getServerPings().read(0);
				String json = serverPing.toJson();
				System.out.println(json);
				
				try {
					JSONObject jsonObject = (JSONObject) new JSONParser().parse(json);
					
					JSONObject forgeData = new JSONObject();
					
					forgeData.put("fmlNetworkVersion", 2);
					forgeData.put("mods", new JSONArray() {
						{
							add(new JSONObject() {
							
								{
									put("modId", "forge");
									put("modmarker", "ANY");
								}
							});
						}
					});
					forgeData.put("channels", new JSONArray());
					jsonObject.put("forgeData", forgeData);
					System.out.println(jsonObject.toJSONString());
					event.getPacket().getServerPings().write(0, WrappedServerPing.fromJson(jsonObject.toJSONString()));
					
				} catch (ParseException e) {
					e.printStackTrace();
				}
				
				
				
				
				/*
				 * "forgeData": {
    "channels": [
      {
        "res": "ironchest:main_channel",
        "version": "1",
        "required": false
      }
    ],
    "mods": [
      {
        "modId": "forge",
        "modmarker": "ANY"
      },
      {
        "modId": "ironchest",
        "modmarker": "1.13.2-8.0.2"
      }
    ],
    "fmlNetworkVersion": 2
  }
				 */
			}
		});
		
//protManager.addPacketListener(new PacketAdapter(this, PacketType.Login.Server.SUCCESS) {
//			
//			@Override
//			public void onPacketSending(PacketEvent event) {
//				
//				event.getNetworkMarker().addPostListener(new PacketPostListener() {
//
//					@Override
//					public Plugin getPlugin() {
//						return Main.getInstance();
//					}
//
//					@Override
//					public void onPostEvent(PacketEvent event) {
//						if(!players.containsKey(event.getPlayer().getName())) {
//									
//							{
//								PacketContainer cPacket = new PacketContainer(PacketType.Play.Server.CUSTOM_PAYLOAD);
//								cPacket.getMinecraftKeys().write(0, new MinecraftKey("minecraft", "register"));
//								cPacket.getModifier().write(1, getPacketDataSerializerOf("fml:loginwrapper\\0fml:handshake\\0fml:play\\0".getBytes()));
//								try {
//									ProtocolLibrary.getProtocolManager().sendServerPacket(event.getPlayer(), cPacket);
//								} catch (InvocationTargetException e) {
//									e.printStackTrace();
//								}
//							}
//							
//							{
//								PacketContainer cPacket = new PacketContainer(PacketType.Play.Server.CUSTOM_PAYLOAD);
//								cPacket.getMinecraftKeys().write(0, new MinecraftKey("fml", "handshake"));
//								//2,0,0,0,0
//								cPacket.getModifier().write(1, getPacketDataSerializerOf(new byte[] {0, 2, 1, 3, 4, 6}));
////								cPacket.getModifier().write(1, getPacketDataSerializerOf(new byte[] {0, 2, 0, 0, 0, 0}));
//								try {
//									ProtocolLibrary.getProtocolManager().sendServerPacket(event.getPlayer(), cPacket);
//								} catch (InvocationTargetException e) {
//									e.printStackTrace();
//								}
//							}
//						}
//					}
//					
//				});
//			}
//		});
		
		
//		
		

		
		
		
		
		protManager.addPacketListener(new PacketAdapter(this, PacketType.Play.Server.CUSTOM_PAYLOAD) {
			@Override
			public void onPacketSending(PacketEvent event) {
				MinecraftKey key = event.getPacket().getMinecraftKeys().read(0);
				if(key.getPrefix().equals("minecraft") && key.getKey().contentEquals("brand")) {
					PacketContainer cPacket = new PacketContainer(PacketType.Play.Server.CUSTOM_PAYLOAD);
					cPacket.getMinecraftKeys().write(0, new MinecraftKey("minecraft", "brand"));
					cPacket.getModifier().write(1, getPacketDataSerializerOf(new byte[] {5, 102, 111, 114, 103, 101}));
					event.setPacket(cPacket);
				}
//				{
//					PacketContainer cPacket = new PacketContainer(PacketType.Play.Server.CUSTOM_PAYLOAD);
//					cPacket.getMinecraftKeys().write(0, new MinecraftKey("minecraft", "brand"));
//					cPacket.getModifier().write(1, getPacketDataSerializerOf(new byte[] {5, 102, 111, 114, 103, 101}));
//					try {
//						ProtocolLibrary.getProtocolManager().sendServerPacket(event.getPlayer(), cPacket);
//					} catch (InvocationTargetException e) {
//						e.printStackTrace();
//					}
//				}
			}
		});
		
		
		/**
		 * When the player is about to join the server (after the login success packet), begin the Forge handshake
		 * @see http://wiki.vg/Minecraft_Forge_Handshake#Forge_handshake
		 */
		protManager.addPacketListener(new PacketAdapter(this, PacketType.Login.Server.SUCCESS) {
			
			@Override
			public void onPacketSending(PacketEvent event) {
				
				event.getNetworkMarker().addPostListener(new PacketPostListener() {

					@Override
					public Plugin getPlugin() {
						return Main_Old.getInstance();
					}

					@Override
					public void onPostEvent(PacketEvent event) {
						if(!players.containsKey(event.getPlayer().getName())) {
							
//							{
//								PacketContainer cPacket = new PacketContainer(PacketType.Play.Server.CUSTOM_PAYLOAD);
//								cPacket.getMinecraftKeys().write(0, new MinecraftKey("fml", "netversion"));
//								cPacket.getModifier().write(1, getPacketDataSerializerOf("FML2".getBytes()));
//								try {
//									ProtocolLibrary.getProtocolManager().sendServerPacket(event.getPlayer(), cPacket);
//								} catch (InvocationTargetException e) {
//									e.printStackTrace();
//								}
//							}
							
							{
								PacketContainer cPacket = new PacketContainer(PacketType.Play.Server.CUSTOM_PAYLOAD);
								cPacket.getMinecraftKeys().write(0, new MinecraftKey("minecraft", "register"));
								cPacket.getModifier().write(1, getPacketDataSerializerOf(new byte[] {102, 109, 108, 58, 108, 111, 103, 105, 110, 119, 114, 97, 112, 112, 101, 114, 0, 102, 109, 108, 58, 104, 97, 110, 100, 115, 104, 97, 107, 101, 0, 97, 112, 112, 108, 101, 115, 107, 105, 110, 58, 115, 121, 110, 99, 0, 102, 109, 108, 58, 112, 108, 97, 121, 0}));
								try {
									ProtocolLibrary.getProtocolManager().sendServerPacket(event.getPlayer(), cPacket);
								} catch (InvocationTargetException e) {
									e.printStackTrace();
								}
							}
							
							

									
//							{
//								PacketContainer cPacket = new PacketContainer(PacketType.Play.Server.CUSTOM_PAYLOAD);
//								cPacket.getMinecraftKeys().write(0, new MinecraftKey("minecraft", "register"));
//								cPacket.getModifier().write(1, getPacketDataSerializerOf("fml:loginwrapper\\0fml:handshake\\0fml:play\\0".getBytes()));
//								try {
//									ProtocolLibrary.getProtocolManager().sendServerPacket(event.getPlayer(), cPacket);
//								} catch (InvocationTargetException e) {
//									e.printStackTrace();
//								}
//							}
//							
//							{
//								PacketContainer cPacket = new PacketContainer(PacketType.Play.Server.CUSTOM_PAYLOAD);
//								cPacket.getMinecraftKeys().write(0, new MinecraftKey("fml", "handshake"));
//								//2,0,0,0,0
//								cPacket.getModifier().write(1, getPacketDataSerializerOf(new byte[] {3, 2, 1, 3, 4, 6}));
////								cPacket.getModifier().write(1, getPacketDataSerializerOf(new byte[] {0, 2, 0, 0, 0, 0}));
//								try {
//									ProtocolLibrary.getProtocolManager().sendServerPacket(event.getPlayer(), cPacket);
//								} catch (InvocationTargetException e) {
//									e.printStackTrace();
//								}
//							}
						}
					}
					
				});
			}
		});
		
		//Output when a payload is found
		protManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, Arrays.asList(PacketType.Play.Client.CUSTOM_PAYLOAD), ListenerOptions.INTERCEPT_INPUT_BUFFER) {
			
			@SuppressWarnings("unused")
			private String getByteArrayString(byte[] bytes) {
				String[] arr = new String[bytes.length];
				for (int i = 0; i < bytes.length; i++) {
					byte b = bytes[i];
					arr[i] = String.valueOf(b);
				}
				return Arrays.deepToString(arr);
			}
			
			@Override
			public void onPacketReceiving(PacketEvent event) {
				System.out.println(">>> New incoming packet: " + event.getPacket().getMinecraftKeys().read(0).getFullKey());
				PacketDataSerializer data = (PacketDataSerializer) event.getPacket().getModifier().read(1);
				byte[] bytes = new byte[data.readableBytes()];
				data.readBytes(bytes);
				System.out.println("    packet data: " + new String(bytes) + " (" + Arrays.toString(bytes) + ")");
//				byte[] bytes = getBytesFromPacket(event.getPacket());
//				switch(event.getPacket().getStrings().read(0)) {
////					case "MC|Brand": {
////						ByteBuf buffer = Unpooled.copiedBuffer(bytes);
////						System.out.println(ByteBufUtils.readUTF8String(buffer));
////						break;
////					}
//					case "FML|HS": {
//						//FML Mod list discriminator
//						if(bytes[0] == 2) {
//						
//							//copy bytes for parsing
//							ByteBuf buffer = Unpooled.copiedBuffer(bytes);
//							
//							//read the discriminator to prevent AIOOBs
//							buffer.readByte();
//							
//							//read mod count
//							int modCount = ByteBufUtils.readVarInt(buffer, 2);
//							
//							//put mod data from packet directly into local list
//			            	TreeSet<Mod> modList = new TreeSet<Mod>();
//				            for (int i = 0; i < modCount; i++)    {
//				            	modList.add(new Mod(ByteBufUtils.readUTF8String(buffer), ByteBufUtils.readUTF8String(buffer), ModType.FORGE));
//				            }
//							players.put(event.getPlayer().getName(), modList);
//						}
//						event.setCancelled(true);
//						/*
//						 * Client will crash here because we don't send a server modlist packet.
//						 * This is normal.
//						 */
//						break;
//					}
//					case "WECUI": {
//						//write WECUI to mod list
//						TreeSet<Mod> currentMods = players.getOrDefault(event.getPlayer().getName(), new TreeSet<Mod>());
//						currentMods.add(new Mod("WorldEdit CUI", " ", ModType.LITEMOD));
//						players.put(event.getPlayer().getName(), currentMods);
//						break;
//					}
//					case "WDL|INIT": {
//						//write WDL to mod list
//						TreeSet<Mod> currentMods = players.getOrDefault(event.getPlayer().getName(), new TreeSet<Mod>());
//						try {
//							//receive version from the WDL|INIT packet
//							JSONObject obj = (JSONObject) new JSONParser().parse(new String(bytes));
//							currentMods.add(new Mod("World Downloader Mod", String.valueOf(obj.get("Version")), ModType.LITEMOD));
//						} catch (ParseException e) {
//							currentMods.add(new Mod("World Downloader Mod", " ", ModType.LITEMOD));
//						}
//						players.put(event.getPlayer().getName(), currentMods);
//						break;
//					}
//					case "PERMISSIONSREPL": {
//						//write other mods to mod list
//						ReplicatedPermissionsContainer permissionsContainer = ReplicatedPermissionsContainer.fromBytes(bytes);
//						
//						TreeSet<Mod> currentMods = players.getOrDefault(event.getPlayer().getName(), new TreeSet<Mod>());
//						currentMods.add(new Mod(permissionsContainer.modName, String.valueOf(permissionsContainer.modVersion), ModType.LITEMOD));
//						players.put(event.getPlayer().getName(), currentMods);
//						break;
//					}
//						
//				}
			}
		});
		
	}
	
	@Override
	public void onDisable() {
		storage.store(players);
	}
	
	private byte[] getBytesFromPacket(PacketContainer packet) {
		if(packet.getType().equals(PacketType.Play.Client.CUSTOM_PAYLOAD)) {
			Object serializerObject = MinecraftReflection.getPacketDataSerializer(packet.getModifier().read(1));
			try {
				return (byte[]) serializerObject.getClass().getDeclaredMethod("array").invoke(serializerObject);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
					| NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public static Main_Old getInstance() {
		return JavaPlugin.getPlugin(Main_Old.class);
	}

	public void title(String str) {
		Bukkit.getLogger().info("################################################# " + str + " #################################################");
	}

}

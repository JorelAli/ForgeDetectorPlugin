package io.github.skepter.forgedetector;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class Main extends JavaPlugin {

	//pathetic simple implementation
	private Map<String, Map<String, String>> players;
	
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(label.equalsIgnoreCase("mods") || label.equalsIgnoreCase("mod")) {
			//do stuff here
			return true;
		}
		return false;
	}
	
	@Override
	public void onEnable() {	
		players = new HashMap<String, Map<String, String>>();
		getCommand("mods").setExecutor(this);
		//Protocol manager
		ProtocolManager protManager = ProtocolLibrary.getProtocolManager();
		
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
						return Main.getInstance();
					}

					@Override
					public void onPostEvent(PacketEvent event) {
						if(!players.containsKey(event.getPlayer().getName())) {
							/* Send the register packet and server hello packet */
							PacketSender.sendRegisterPacket(event.getPlayer());
							PacketSender.sendServerHelloPacket(event.getPlayer());
						}
						
					}
				
				});
				
			}
		});
		
		
		
		
		//Output when a payload is found
		protManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, Arrays.asList(PacketType.Play.Client.CUSTOM_PAYLOAD), ListenerOptions.INTERCEPT_INPUT_BUFFER) {
			
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
				if(event.getPacket().getStrings().read(0).equals("FML|HS")) {
					title("Found a custom payload packet");
					Bukkit.getLogger().info("Found custom payload packet from client:");
					Bukkit.getLogger().info("\tChannel name: " + event.getPacket().getStrings().read(0));
					
					byte[] bytes = getBytesFromPacket(event.getPacket());
					System.out.println(getByteArrayString(bytes));
					if(bytes[0] == 2) {
						//they sent their mod list
						ByteBuf buffer = Unpooled.copiedBuffer(bytes);
						Map<String, String> modTags = new HashMap<String, String>();
						buffer.readByte();
						int modCount = ByteBufUtils.readVarInt(buffer, 2);
						System.out.println(modCount);
			            for (int i = 0; i < modCount; i++)    {
			                modTags.put(ByteBufUtils.readUTF8String(buffer), ByteBufUtils.readUTF8String(buffer));
			            }
			            for(Entry<String, String> entry : modTags.entrySet()) {
			            	System.out.println(entry.getKey() + " " + entry.getValue());
			            }
						players.put(event.getPlayer().getName(), modTags);
					}

					
					title("End of custom payload packet");
				}
				event.setCancelled(true);
			}
		});
		
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
	
	public static Main getInstance() {
		return JavaPlugin.getPlugin(Main.class);
	}

	public void title(String str) {
		Bukkit.getLogger().info("################################################# " + str + " #################################################");
	}

}

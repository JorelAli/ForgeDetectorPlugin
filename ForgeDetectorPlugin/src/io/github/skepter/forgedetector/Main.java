package io.github.skepter.forgedetector;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerOptions;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketPostListener;

import net.minecraft.server.v1_11_R1.PacketDataSerializer;

public class Main extends JavaPlugin {

	//pathetic simple implementation
	private Set<Player> players;
	
	
	@Override
	public void onEnable() {	
		players = new HashSet<Player>();
		
		
		
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
						if(!players.contains(event.getPlayer())) {
							/* Send the register packet and server hello packet */
							PacketSender.sendRegisterPacket(event.getPlayer());
							PacketSender.sendServerHelloPacket(event.getPlayer());
							players.add(event.getPlayer());
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
				title("Found a custom payload packet");
				Bukkit.getLogger().info("Found custom payload packet from client:");
				Bukkit.getLogger().info("\tChannel name: " + event.getPacket().getStrings().read(0));
				PacketDataSerializer serializer = (PacketDataSerializer) event.getPacket().getModifier().read(1);
				Bukkit.getLogger().info("\tReceived data (text): " + new String(serializer.array()));
				
				
				
				Bukkit.getLogger().info("\tRaw bytes: " + getByteArrayString(serializer.array()));
				title("End of custom payload packet");
				//handshake will crash 
				event.setCancelled(true);
			}
		});
		
	}
	
	public static Main getInstance() {
		return JavaPlugin.getPlugin(Main.class);
	}

	public void title(String str) {
		Bukkit.getLogger().info("################################################# " + str + " #################################################");
	}

}

package io.github.skepter.forgedetector;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
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
import com.comphenix.protocol.wrappers.MinecraftKey;

import io.netty.buffer.ByteBuf;
import net.minecraft.server.v1_15_R1.PacketDataSerializer;

public class Main extends JavaPlugin implements Listener {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (label.equalsIgnoreCase("mods") || label.equalsIgnoreCase("mod")) {
			return true;
		}
		return false;
	}

	public Object getPacketDataSerializerOf(byte[] bytes) {
		ByteBuf buffer = PacketContainer.createPacketBuffer();
		buffer.writeBytes(bytes);
		return MinecraftReflection.getPacketDataSerializer(buffer);
	}

	public String readPacketDataSerializer(Object packetDataSerializer) {
		PacketDataSerializer data = (PacketDataSerializer) packetDataSerializer;
		byte[] bytes = new byte[data.readableBytes()];
		data.readBytes(bytes);
		return new String(bytes) + " (" + Arrays.toString(bytes) + ")";
	}

	@Override
	public void onEnable() {
		getCommand("mods").setExecutor(this);

		ProtocolManager protManager = ProtocolLibrary.getProtocolManager();

		protManager.addPacketListener(new PacketAdapter(this, PacketType.Play.Server.CUSTOM_PAYLOAD) {

			@Override
			public void onPacketSending(PacketEvent event) {
				MinecraftKey key = event.getPacket().getMinecraftKeys().read(0);
				if (key.getPrefix().equals("minecraft") && key.getKey().contentEquals("brand")) {
					PacketContainer cPacket = new PacketContainer(PacketType.Play.Server.CUSTOM_PAYLOAD);
					cPacket.getMinecraftKeys().write(0, new MinecraftKey("minecraft", "brand"));
					cPacket.getModifier().write(1,
							getPacketDataSerializerOf(new byte[] { 5, 102, 111, 114, 103, 101 }));
					event.setPacket(cPacket);
				}
			}

		});

		/**
		 * When the player is about to join the server (after the login success packet),
		 * begin the Forge handshake
		 * 
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

						PacketContainer cPacket = new PacketContainer(PacketType.Play.Server.CUSTOM_PAYLOAD);
						cPacket.getMinecraftKeys().write(0, new MinecraftKey("minecraft", "register"));
						cPacket.getModifier().write(1,
//								getPacketDataSerializerOf(new byte[] { 102, 109, 108, 58, 108, 111, 103, 105, 110, 119,
//										114, 97, 112, 112, 101, 114, 0, 102, 109, 108, 58, 104, 97, 110, 100, 115, 104,
//										97, 107, 101, 0, 97, 112, 112, 108, 101, 115, 107, 105, 110, 58, 115, 121, 110,
//										99, 0, 102, 109, 108, 58, 112, 108, 97, 121, 0 }));
								getPacketDataSerializerOf(new byte[] { 102, 109, 108, 58, 108, 111, 103, 105, 110, 119,
										114, 97, 112, 112, 101, 114, 0, 102, 109, 108, 58, 104, 97, 110, 100, 115, 104,
										97, 107, 101, 0, 102, 109, 108, 58, 112, 108, 97, 121, 0 }));

						try {
							ProtocolLibrary.getProtocolManager().sendServerPacket(event.getPlayer(), cPacket);
						} catch (InvocationTargetException e) {
							e.printStackTrace();
						}

					}

				});
			}
		});

		// Output when a payload is found
		protManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL,
				Arrays.asList(PacketType.Play.Client.CUSTOM_PAYLOAD), ListenerOptions.INTERCEPT_INPUT_BUFFER) {

			@Override
			public void onPacketReceiving(PacketEvent event) {
				System.out.println(
						">>> New incoming packet: " + event.getPacket().getMinecraftKeys().read(0).getFullKey());
				System.out.println(
						"    packet data: " + readPacketDataSerializer(event.getPacket().getModifier().read(1)));
			}
		});

	}

	public static Main getInstance() {
		return JavaPlugin.getPlugin(Main.class);
	}

}

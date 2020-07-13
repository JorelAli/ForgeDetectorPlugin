package io.github.skepter.forgedetector;

import java.lang.reflect.InvocationTargetException;

import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.utility.MinecraftReflection;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class PacketSender {

	public static void sendCustomPayloadPacket(Player player, String channel, ByteBuf bytebuf) {
		try {
			
			PacketContainer cPacket = new PacketContainer(PacketType.Play.Server.CUSTOM_PAYLOAD);
			cPacket.getStrings().write(1, channel);
			//minecraft:registerfml:loginwrapper\0fml:handshake\0fml:play\0
			cPacket.getByteArrays().write(2, "minecraft:registerfml:loginwrapper\\0fml:handshake\\0fml:play\\0".getBytes());
//			cPacket.getModifier().write(1, MinecraftReflection.getPacketDataSerializer(bytebuf));
			ProtocolLibrary.getProtocolManager().sendServerPacket(player, cPacket);
			
		} catch (InvocationTargetException | SecurityException | IllegalArgumentException e) {
			e.printStackTrace();
		}
	}
	
	public static void sendRegisterPacket(Player player) {
		//Bukkit.getLogger().info("Sending register packet");
		
		ByteBuf bytebuf = Unpooled.buffer();
		bytebuf.writeBytes("FML|HS FML FML|MP FML FORGE".getBytes());
		sendCustomPayloadPacket(player, "REGISTER", bytebuf);

		//Bukkit.getLogger().info("Register packet sent!");
	}
	
	public static void sendServerHelloPacket(Player player){
		//Bukkit.getLogger().info("Sending server hello packet...");
		
		ByteBuf bytebuf = Unpooled.wrappedBuffer(new byte[] {0, 2, 0, 0, 0, 0});
		sendCustomPayloadPacket(player, "FML|HS", bytebuf);
		
		//Bukkit.getLogger().info("Server hello packet sent!");
	}
}

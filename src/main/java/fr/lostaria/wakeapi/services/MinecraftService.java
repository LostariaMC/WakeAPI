package fr.lostaria.wakeapi.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.lostaria.wakeapi.core.MinecraftStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Service
public class MinecraftService {

    @Value("${minecraft.host}")
    private String host;

    @Value("${minecraft.port:25565}")
    private int port;

    private static final int DEFAULT_TIMEOUT_MILLIS = 3000;
    private static final int PROTOCOL_VERSION = 47;

    private final ObjectMapper mapper = new ObjectMapper();

    public boolean isServerOnline() {
        return isServerOnline(DEFAULT_TIMEOUT_MILLIS);
    }

    public boolean isServerOnline(int timeoutMs) {
        try {
            MinecraftStatus s = fetchStatus(timeoutMs);
            return s.online();
        } catch (IOException e) {
            return false;
        }
    }

    public int getOnlinePlayersCount() {
        return getOnlinePlayersCount(DEFAULT_TIMEOUT_MILLIS);
    }

    public int getOnlinePlayersCount(int timeoutMs) {
        try {
            MinecraftStatus s = fetchStatus(timeoutMs);
            return s.online() ? s.playersOnline() : 0;
        } catch (IOException e) {
            return 0;
        }
    }

    private MinecraftStatus fetchStatus(int timeoutMs) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            ByteArrayOutputStream handshakeBytes = new ByteArrayOutputStream();
            DataOutputStream handshake = new DataOutputStream(handshakeBytes);
            handshake.writeByte(0x00);
            writeVarInt(handshake, PROTOCOL_VERSION);
            writeString(handshake, host);
            handshake.writeShort(port);
            writeVarInt(handshake, 1);

            writeVarInt(out, handshakeBytes.size());
            out.write(handshakeBytes.toByteArray());

            writeVarInt(out, 1);
            out.writeByte(0x00);

            readVarInt(in);
            int packetId = readVarInt(in);
            if (packetId != 0x00) throw new IOException("Invalid packetId: " + packetId);

            int jsonLength = readVarInt(in);
            byte[] jsonBytes = new byte[jsonLength];
            in.readFully(jsonBytes);
            String json = new String(jsonBytes, StandardCharsets.UTF_8);

            JsonNode root = mapper.readTree(json);
            int onlinePlayers = root.path("players").path("online").asInt(0);

            return new MinecraftStatus(true, onlinePlayers);
        }
    }

    private void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & -128) != 0) {
            out.writeByte(value & 127 | 128);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    private int readVarInt(DataInputStream in) throws IOException {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            read = in.readByte();
            int value = (read & 0b0111_1111);
            result |= (value << (7 * numRead));

            numRead++;
            if (numRead > 5) throw new IOException("VarInt too big");
        } while ((read & 0b1000_0000) != 0);
        return result;
    }

    private void writeString(DataOutputStream out, String s) throws IOException {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }
}

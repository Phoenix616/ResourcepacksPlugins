package de.themoep.BungeeResourcepacks.bungee.packets;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import de.themoep.BungeeResourcepacks.core.ResourcePack;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.md_5.bungee.connection.DownstreamBridge;
import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.PacketWrapper;

import java.beans.ConstructorProperties;
import java.nio.ByteBuffer;

/**
 * Created by Phoenix616 on 24.03.2015.
 */
public class ResourcePackSendPacket extends DefinedPacket {
    
    private String url;
    private String hash;

    public ResourcePackSendPacket() {};
    
    @ConstructorProperties({"url", "hash"})
    public ResourcePackSendPacket(String url, String hash) {
        this.url = url;
        if(hash != null) {
            this.hash = hash.substring(0, (hash.length() > 39) ? 39 : hash.length()).toLowerCase();
        } else {
            this.hash = Hashing.sha1().hashString(url, Charsets.UTF_8).toString().substring(0, 39).toLowerCase();
        }
    }

    @ConstructorProperties({"ResourcePack"})
    public ResourcePackSendPacket(ResourcePack pack) {
        this.url = pack.getUrl();
        this.hash = pack.getHash();
    }
    
    @Override
    public void handle(AbstractPacketHandler handler) throws Exception {
        if(handler instanceof DownstreamBridge) {
            ((DownstreamBridge) handler).handle(new PacketWrapper(this, Unpooled.copiedBuffer(ByteBuffer.allocate(Integer.toString(this.getUrl().length()).length()))));
        } else {
            throw new UnsupportedOperationException("Only players can receive ResourcePackSend packets!");
        }
    }

    public void read(ByteBuf buf) {
        this.url = readString(buf);
        this.hash = readString(buf);
    }

    public void write(ByteBuf buf) {
        writeString(this.url, buf);
        writeString(this.hash, buf);
    }

    public String getUrl() {
        return this.url;
    }

    public String getHash() {
        return this.hash;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setHash(String hash) {
        if(hash != null) {
            this.hash = hash.substring(0, 39).toLowerCase();
        } else {
            this.hash = Hashing.sha1().hashString(this.getUrl(), Charsets.UTF_8).toString().substring(0, 39).toLowerCase();
        }
    }
    
    public String toString() {
        return "ResourcePackSend(url=" + this.getUrl() + ", hash=" + this.getHash() + ")";
    }

    public boolean equals(Object o) {
        if(o == this) {
            return true;
        } else if(!(o instanceof ResourcePackSendPacket)) {
            return false;
        } else {
            ResourcePackSendPacket other = (ResourcePackSendPacket)o;
            String this$url = this.getUrl();
            String other$url = other.getUrl();
            if (this$url == null) {
                if (other$url != null) {
                    return false;
                }
            } else if (!this$url.equals(other$url)) {
                return false;
            }

            String this$hash = this.getHash();
            String other$hash = other.getHash();
            if (this$hash == null) {
                if (other$hash != null) {
                    return false;
                }
            } else if (!this$hash.equals(other$hash)) {
                return false;
            }

            return true;
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof ResourcePackSendPacket;
    }

    public int hashCode() {
        boolean PRIME = true;
        byte result = 1;
        String $url = this.getUrl();
        int result1 = result * 59 + ($url == null?0:$url.hashCode());
        String $hash = this.getHash();
        result1 = result1 * 59 + ($hash == null?0:$hash.hashCode());
        return result1;
    }
}

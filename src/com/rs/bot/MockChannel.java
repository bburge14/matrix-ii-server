package com.rs.bot;

import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;

public class MockChannel implements Channel {

    private static final AtomicInteger ID_GEN = new AtomicInteger(-1);
    private final Integer id = ID_GEN.getAndDecrement();
    private final ChannelPipeline pipeline = Channels.pipeline();
    private final ChannelFuture closeFuture = Channels.future(this);
    private Object attachment;
    private volatile boolean closed = false;

    @Override public Integer getId()             { return id; }
    @Override public ChannelFactory getFactory() { return null; }
    @Override public Channel getParent()         { return null; }
    @Override public ChannelConfig getConfig()   { return null; }
    @Override public ChannelPipeline getPipeline() { return pipeline; }
    @Override public boolean isOpen()      { return !closed; }
    @Override public boolean isBound()     { return false; }
    @Override public boolean isConnected() { return false; }
    @Override public SocketAddress getLocalAddress()  { return null; }
    @Override public SocketAddress getRemoteAddress() { return null; }
    @Override public ChannelFuture write(Object message) { return Channels.succeededFuture(this); }
    @Override public ChannelFuture write(Object message, SocketAddress remoteAddress) { return Channels.succeededFuture(this); }
    @Override public ChannelFuture bind(SocketAddress localAddress) { return Channels.succeededFuture(this); }
    @Override public ChannelFuture connect(SocketAddress remoteAddress) { return Channels.succeededFuture(this); }
    @Override public ChannelFuture disconnect() { return Channels.succeededFuture(this); }
    @Override public ChannelFuture unbind()     { return Channels.succeededFuture(this); }
    @Override public ChannelFuture close() {
        if (!closed) { closed = true; closeFuture.setSuccess(); }
        return closeFuture;
    }
    @Override public ChannelFuture getCloseFuture() { return closeFuture; }
    @Override public int getInterestOps() { return Channel.OP_READ_WRITE; }
    @Override public boolean isReadable() { return false; }
    @Override public boolean isWritable() { return false; }
    @Override public ChannelFuture setInterestOps(int interestOps) { return Channels.succeededFuture(this); }
    @Override public ChannelFuture setReadable(boolean readable) { return Channels.succeededFuture(this); }
    @Override public Object getAttachment() { return attachment; }
    @Override public void setAttachment(Object attachment) { this.attachment = attachment; }
    @Override public int compareTo(Channel o) {
        if (this == o) return 0;
        return Integer.compare(this.id, o.getId());
    }
}

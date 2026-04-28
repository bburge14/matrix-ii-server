package com.rs.net;

import java.net.InetSocketAddress;

import javax.swing.JOptionPane;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import com.rs.Settings;
import com.rs.cores.CoresManager;
import com.rs.io.InputStream;
import com.rs.net.decoders.WorldPacketsDecoder;
import com.rs.utils.Logger;

public final class ServerChannelHandler extends SimpleChannelHandler {

	private static ChannelGroup channels;
	private static ServerBootstrap bootstrap;

	public static final void init() {
		new ServerChannelHandler();
	}

	public static int getConnectedChannelsSize() {
		return channels == null ? 0 : channels.size();
	}

	/*
	 * throws exeption so if cant handle channel server closes
	 */
	private ServerChannelHandler() {
		channels = new DefaultChannelGroup();
		bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(
				CoresManager.serverBossChannelExecutor,
				CoresManager.serverWorkerChannelExecutor,
				CoresManager.serverWorkersCount));
		bootstrap.getPipeline().addLast("handler", this);
		bootstrap.setOption("reuseAddress", true); // reuses adress for bind
		bootstrap.setOption("child.tcpNoDelay", true);
		bootstrap.setOption("child.TcpAckFrequency", true);
		bootstrap.setOption("child.keepAlive", true);
		bootstrap.bind(new InetSocketAddress(43594));
		System.out.println("[" + Settings.SERVER_NAME + "] Hosting on world 1.");	
		/*String world = JOptionPane.showInputDialog("Select a world to run");
		int parsedWorld = Integer.parseInt(world);
		if (parsedWorld > 3 || parsedWorld < 0) {
			bootstrap.bind(new InetSocketAddress(43594));
			JOptionPane.showMessageDialog(null, "The world you selected is invalid. Hosting world 1.");
			return;
		}
		switch (parsedWorld) {
			case 1: 			
				bootstrap.bind(new InetSocketAddress(43594));
				System.out.println("[" + Settings.SERVER_NAME + "] Hosting on world 1.");	
				return;
			case 2:
				bootstrap.bind(new InetSocketAddress(43595));
				System.out.println("[" + Settings.SERVER_NAME + "] Hosting on world 2.");
				return;
			case 3:
				bootstrap.bind(new InetSocketAddress(43596));
				System.out.println("[" + Settings.SERVER_NAME + "] Hosting on world 3.");
				return;
		}*/
	}

	@Override
	public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
		channels.add(e.getChannel());
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
		channels.remove(e.getChannel());
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
		ctx.setAttachment(new Session(e.getChannel()));
	}

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx,
			ChannelStateEvent e) {
		Object sessionObject = ctx.getAttachment();
		if (sessionObject != null && sessionObject instanceof Session) {
			Session session = (Session) sessionObject;
			if (session.getDecoder() == null)
				return;
			if (session.getDecoder() instanceof WorldPacketsDecoder)
				session.getWorldPackets().getPlayer().finish();
		}
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
		if (!(e.getMessage() instanceof ChannelBuffer))
			return;
		Object sessionObject = ctx.getAttachment();
		if (sessionObject != null && sessionObject instanceof Session) {
			Session session = (Session) sessionObject;
			if (session.getDecoder() == null)
				return;
			ChannelBuffer buf = (ChannelBuffer) e.getMessage();
			buf.markReaderIndex();
			int avail = buf.readableBytes();
			if (avail < 1 || avail > Settings.RECEIVE_DATA_LIMIT) {
				return;
			}
			byte[] buffer = new byte[avail];
			buf.readBytes(buffer);
			try {
				session.getDecoder().decode(new InputStream(buffer));
			} catch (Throwable er) {
				Logger.handle(er);
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent ee)
			throws Exception {

	}

	public static final void shutdown() {
		channels.close().awaitUninterruptibly();
		bootstrap.releaseExternalResources();
	}

}

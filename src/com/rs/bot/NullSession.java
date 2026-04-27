package com.rs.bot;

import com.rs.net.Session;

public class NullSession extends Session {
    public NullSession() {
        super(new MockChannel());
    }

    @Override
    public String getIP() {
        return "ai.local";
    }
}

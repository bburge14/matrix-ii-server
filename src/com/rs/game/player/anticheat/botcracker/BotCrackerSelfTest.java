package com.rs.game.player.anticheat.botcracker;

import java.util.*;

/**
 * Offline sanity check for the detectors: feeds a synthetic human (log-normal
 * timing, aim scatter, slow fatigue drift, occasional breaks) and three
 * synthetic bots (rigid, jittered, short session) through the engine and
 * prints the verdicts. Run with:
 * <pre>java -cp bin com.rs.game.player.anticheat.botcracker.BotCrackerSelfTest</pre>
 * Expected: humans score 0, bots score ~1.0 with several agreeing sources.
 */
public final class BotCrackerSelfTest {
    static Random rnd = new Random(42);
    static double gauss(double m, double sd){ return m + rnd.nextGaussian()*sd; }

    static PlayerTelemetry human(long hours){
        PlayerTelemetry t = new PlayerTelemetry("human", 0);
        long now = 1000; long end = hours*3600000L;
        double fatigue = 1.0;
        while (now < end) {
            // human: lognormal-ish IAT around 2.4s (4 ticks) with drift, occasional pauses
            double base = 2400 * fatigue;
            long d = (long) Math.max(700, Math.exp(gauss(Math.log(base), 0.35)));
            if (rnd.nextDouble() < 0.01) d += 5000 + rnd.nextInt(40000);
            if (rnd.nextDouble() < 0.0005) d += 200000; // break
            now += d;
            long tickStart = now - (now % 600) - rnd.nextInt(5);
            t.recordAction(ActionKind.ITEM_ON_ITEM, now, tickStart);
            t.recordAction(ActionKind.SKILL_ACTION, now + 600, tickStart);
            t.recordXp(25, now + 600);
            t.recordClick((int) gauss(600, 4), (int) gauss(400, 4), 0, now);
            if (rnd.nextDouble() < 0.5) t.recordClick((int) gauss(640, 5), (int) gauss(380, 5), 0, now + 100);
            fatigue += 0.00002 * d / 1000; // slows with time
        }
        return t;
    }

    static PlayerTelemetry bot(long hours, boolean jitter){
        PlayerTelemetry t = new PlayerTelemetry("bot", 0);
        long now = 1000; long end = hours*3600000L;
        while (now < end) {
            long d = jitter ? 2400 + rnd.nextInt(120) - 60 : 2400; // hits every 4th tick with small jitter
            now += d;
            long tickStart = now - (now % 600);
            t.recordAction(ActionKind.ITEM_ON_ITEM, now, tickStart);
            t.recordAction(ActionKind.SKILL_ACTION, now + 600, tickStart);
            t.recordXp(25, now + 600);
            // uniform box jitter on a fixed hitbox
            t.recordClick(600 + (jitter ? rnd.nextInt(7) - 3 : 0), 400 + (jitter ? rnd.nextInt(7) - 3 : 0), 0, now);
        }
        return t;
    }

    public static void main(String[] a){
        HeuristicEngine e = new HeuristicEngine().register(new IATAnomalyDetector()).register(new FatigueInvarianceDetector()).register(new SpatialClusterDetector());
        for (Object[] c : new Object[][]{{"human 1h", human(1)},{"human 4h", human(4)},{"bot rigid 4h", bot(4,false)},{"bot jitter 4h", bot(4,true)},{"bot jitter 1h", bot(1,true)}}) {
            PlayerTelemetry t=(PlayerTelemetry)c[1];
            HeuristicEngine.Verdict v = e.evaluate(t, t.getLastAnyActionTime()+1000);
            System.out.printf("%-14s score=%.3f sources=%d%n", c[0], v.score, v.sources);
            for (Finding f : v.findings) System.out.println("    " + f);
        }
    }
}

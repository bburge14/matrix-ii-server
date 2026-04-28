package com.rs.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BeastiaryDumper {

	    /**
	     * The path bestiary data will be dumped to.
	     * (MUST END IN TRAILING SLASH)
	     */

	    private static String DUMP_PATH = "C:/Users/Andrew/Desktop/bestiary/";

	    /**
	     * When true, dumped data will be formatted to the "formatted" folder.
	     */

	    private static final boolean FORMAT = true;

	    /**
	     * A thread pool used to quickly execute data dumping.
	     */

	    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(16);

	    /**
	     * Identifiers used for formatting order.
	     */

	    private static final String[] IDENTIFIERS = {
	            "id", "name", "description", "size", "xp", "lifepoints", "level",
	            "attack", "defence", "ranged", "magic", "animations", "attackable",
	            "poisonous", "weakness", "slayerLevel", "slayercat", "abilities",
	            "areas", "aggressive", "members"
	    };

	    /**
	     * Starts this program.
	     * @param args not being used.
	     */

	    public static void main(String args) {
	        File dumpFolder = new File(DUMP_PATH);
	        if(!dumpFolder.exists()) {
	            System.err.println("Invalid dump directory provided: " + DUMP_PATH);
	            return;
	        }
	        if(FORMAT) {
	            File formatFolder = new File(DUMP_PATH + "formatted");
	            if(formatFolder.exists()) {
	                File[] files = formatFolder.listFiles();
	                if(files != null) {
	                    for(File file : files)
	                        file.delete();
	                }
	            } else {
	                formatFolder.mkdir();
	            }
	        }
	        for(int i = 0; i < 65000; i++) {
	            final int id = i;
	            EXECUTOR.execute(() -> dump(id));
	        }
	        EXECUTOR.shutdown();
	    }

	    private static void dump(int id) {
	        BufferedReader br = null;
	        BufferedWriter bw = null;
	        try {
	            URL url = new URL("http://services.runescape.com/m=itemdb_rs/bestiary/beastData.json?beastid=" + id);
	            br = new BufferedReader(new InputStreamReader(url.openStream()));
	            String line = br.readLine();
	            if(line == null || line.isEmpty()) {
	                /* no bestiary data */
	                return;
	            }
	            bw = new BufferedWriter(new FileWriter(DUMP_PATH + id + ".txt"));
	            bw.write(line);
	            System.out.println("Successfully dumped bestiary data for npc: " + id);
	        } catch(Exception e) {
	            System.err.println("Failed to dump bestiary data for npc: " + id);
	            e.printStackTrace();
	        } finally {
	            close(br, bw);
	            if(FORMAT)
	                format(id);
	        }
	    }

	    /**
	     * Formats the already dumped file with the given id.
	     * @param id the id of the dumped npc file.
	     */

	    private static void format(int id) {
	        BufferedReader br = null;
	        BufferedWriter bw = null;
	        try {
	            File dumped = new File(DUMP_PATH + id + ".txt");
	            if(!dumped.exists())
	                return;
	            br = new BufferedReader(new FileReader(dumped));
	            String line = br.readLine();
	            if(line == null) {
	                System.err.println("Failed to format dumped npc: " + id);
	                return;
	            }
	            /* remove starting and trailing brackets */
	            line = line.substring(1, line.length() - 1);

	            String s = "";
	            Character enclosedBy = null;
	            ArrayList<String> lines = new ArrayList<>(20);
	            for(char c : line.toCharArray()) {
	                if(enclosedBy == null) {
	                    if(c == '{' || c == '[' || c == '"')
	                        enclosedBy = c;
	                } else {
	                    if(enclosedBy == '{' && c == '}')
	                        enclosedBy = null;
	                    else if(enclosedBy == '[' && c == ']')
	                        enclosedBy = null;
	                    else if(enclosedBy == '"' && c == '"')
	                        enclosedBy = null;
	                }
	                if(c == ',' && enclosedBy == null) {
	                    /**
	                     * Split line
	                     */
	                    s = s.replaceFirst("\"", "").replaceFirst("\":", "=");
	                    if(!s.startsWith("weakness=") && !s.startsWith("areas=") && !s.startsWith("abilities=")) {
	                        if(s.contains("\""))
	                            s = s.replaceAll("\"", "");
	                    }
	                    if(s.isEmpty() || !s.contains("="))
	                        System.err.println("Invalid line read for npc: " + id);
	                    else
	                        lines.add(s);
	                    s = "";
	                } else {
	                    /**
	                     * Append character
	                     */
	                    s += c;
	                }
	            }
	            /* sorts dumped data by identifiers */
	            lines.sort((s1, s2) -> getOrder(s1) - getOrder(s2));

	            bw = new BufferedWriter(new FileWriter(DUMP_PATH + "formatted" + System.getProperty("file.separator") + id + ".txt"));
	            for(String l : lines) {
	                bw.write(l);
	                bw.newLine();
	            }
	            System.out.println("Successfully formatted npc: " + id);
	        } catch(Exception e) {
	            System.err.println("Failed to format npc: " + id);
	            e.printStackTrace();
	        } finally {
	            close(br, bw);
	        }
	    }

	    /**
	     * Retrieves the ascending order of the given identifier.
	     * @param identifier the identifier being read.
	     * @return the ascending order of the given identifier.
	     */

	    private static int getOrder(String identifier) {
	        identifier = identifier.substring(0, identifier.indexOf("="));
	        for(int i = 0; i < IDENTIFIERS.length; i++) {
	            if(identifier.equalsIgnoreCase(IDENTIFIERS[i]))
	                return i;
	        }
	        System.err.println("Unhandled Identifier: " + identifier);
	        return 0;
	    }

	    /**
	     * Closes the given buffers.
	     * @param br the <code>BufferedReader</code> to close.
	     * @param bw the <code>BufferedWriter</code> to close.
	     */

	    private static void close(BufferedReader br, BufferedWriter bw) {
	        if(br != null) {
	            try {
	                br.close();
	            } catch(IOException e) {
	                e.printStackTrace();
	            }
	        }
	        if(bw != null) {
	            try {
	                bw.close();
	            } catch(IOException e) {
	                e.printStackTrace();
	            }
	        }
	    }

}

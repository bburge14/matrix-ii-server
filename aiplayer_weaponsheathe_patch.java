// ADD this field after the archetype field around line 15 in AIPlayer.java:
    
    /** Whether this bot spawns with weapons sheathed (true) or unsheathed (false). */
    private boolean weaponSheathe = true;  // default sheathed

    public boolean getWeaponSheathe() { return weaponSheathe; }
    public void setWeaponSheathe(boolean sheathe) { this.weaponSheathe = sheathe; }

// ADD this code to the end of hydrate() method around line 570:
        
        // Apply weapon sheathe preference
        getCombatDefinitions().setSheathe(weaponSheathe);

// FIXED applySkiller method - replace around line 320 in BotEquipment.java
private static void applySkiller(Player bot, int cb) {
    // Skillcape (matched to a randomly-chosen skill) + matching robes
    int[] skillcapes = {
        9774, 9777, 9780, 9783, 9786, 9792, 9795, 9798, 9801, 9804, 9807, 9810,  // melee skills
        9762, 9765, 9771, 9759, 9948, 12169, 15706, 29185                         // skill capes incl summoning, dung, divin
    };
    equip(bot, Equipment.SLOT_CAPE, pick(skillcapes));

    // FIXED: Coordinated robe sets instead of random mix
    int robeSet = Utils.random(6);  // 6 different color sets
    int[][] robes = {
        { 577, 1011 },    // Blue wizard (top, bottom) - FIXED: moved 1011 to bottom
        { 581, 1015 },    // Black wizard  
        { 638, 639 },     // Green
        { 640, 641 },     // Blue
        { 642, 643 },     // Cream
        { 644, 645 }      // Turquoise
    };
    
    equip(bot, Equipment.SLOT_CHEST, robes[robeSet][0]);  // coordinated top
    equip(bot, Equipment.SLOT_LEGS,  robes[robeSet][1]);  // matching bottom
    
    if (chance(40)) equip(bot, Equipment.SLOT_HAT, 579);  // wizard hat
    if (chance(60)) equip(bot, Equipment.SLOT_FEET, pick(new int[]{2579, 88}));
    // No weapon - they're skillers
}

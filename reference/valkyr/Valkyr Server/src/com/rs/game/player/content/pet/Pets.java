package com.rs.game.player.content.pet;

import java.util.HashMap;
import java.util.Map;

import com.rs.game.player.Player;

/**
 * An enum containing all the pets and their info.
 * @author Emperor
 *
 */
public enum Pets {
	
	/**
	 * A cat/kitten pet.
	 */
	CAT(-1, 1555, 1561, 1567, 761, 768, 774, 0.0154320987654321, 0, 321, 363, 341, 15264, 345, 377, 353, 389, 7944, 349, 331, 327, 395, 383, 317, 371, 335, 359, 15264, 15270, 1927),
	CAT_1(-1, 1556, 1562, 1568, 762, 769, 775, 0.0154320987654321, 0, 321, 363, 341, 15264, 345, 377, 353, 389, 7944, 349, 331, 327, 395, 383, 317, 371, 335, 359, 15264, 15270, 1927),
	CAT_2(-1, 1557, 1563, 1569, 763, 770, 776, 0.0154320987654321, 0, 321, 363, 341, 15264, 345, 377, 353, 389, 7944, 349, 331, 327, 395, 383, 317, 371, 335, 359, 15264, 15270, 1927),
	CAT_3(-1, 1558, 1564, 1570, 764, 771, 777, 0.0154320987654321, 0, 321, 363, 341, 15264, 345, 377, 353, 389, 7944, 349, 331, 327, 395, 383, 317, 371, 335, 359, 15264, 15270, 1927),
	CAT_4(-1, 1559, 1565, 1571, 765, 772, 778, 0.0154320987654321, 0, 321, 363, 341, 15264, 345, 377, 353, 389, 7944, 349, 331, 327, 395, 383, 317, 371, 335, 359, 15264, 15270, 1927),
	CAT_5(-1, 1560, 1566, 1572, 766, 773, 779, 0.0154320987654321, 0, 321, 363, 341, 15264, 345, 377, 353, 389, 7944, 349, 331, 327, 395, 383, 317, 371, 335, 359, 15264, 15270, 1927),
	HELLCAT(-1, 7583, 7582, 7581, 3505, 3504, 3503, 0.0154320987654321, 0, 321, 363, 341, 15264, 345, 377, 353, 389, 7944, 349, 331, 327, 395, 383, 317, 371, 335, 359, 15264, 15270, 1927),
	CAT_7(-1, 14089, 14090, 15092, 8217, 8214, 8216, 0.0154320987654321, 0, 321, 363, 341, 15264, 345, 377, 353, 389, 7944, 349, 331, 327, 395, 383, 317, 371, 335, 359, 15264, 15270, 1927),
	
	/**
	 * A clockwork cat.
	 */
	CLOCKWORK_CAT(-1, 7771, 7772, -1, 3598, -1, -1, 0.0, 0),
	
	/**
	 * The firemaker's curse pets.
	 */
	SEARING_FLAME(-1, 22994, -1, -1, 14769, -1, -1, 0.0, 0),
	GLOWING_EMBER(-1, 22993, -1, -1, 14768, -1, -1, 0.0, 0),
	TWISTED_FIRESTARTER(-1, 22995, -1, -1, 14770, -1, -1, 0.0, 0),
	WARMING_FLAME(-1, 22992, -1, -1, 14767, -1, -1, 0.0, 0),
	
	/**
	 * Troll baby pet.
	 */
	TROLL_BABY(-1, 23030, 23030, -1, 14846, -1, -1, 0.0, 0),

	/**
	 * A bulldog pet.
	 */
	BULLDOG(-1, 12522, 12523, -1, 6969, 6968, -1, 0.0033333333333333, 4, 2132, 2134, 2136, 2138, 10816, 9986, 9978, 526),
	BULLDOG_1(-1, 12720, 12721, -1, 7259, 7257, -1, 0.0033333333333333, 4, 2132, 2134, 2136, 2138, 10816, 9986, 9978, 526),
	BULLDOG_2(-1, 12722, 12723, -1, 7260, 7258, -1, 0.0033333333333333, 4, 2132, 2134, 2136, 2138, 10816, 9986, 9978, 526),
	
	/**
	 * A dalmation pet.
	 */
	DALMATIAN(-1, 12518, 12519, -1, 6964, 6965, -1, 0.0033333333333333, 4, 2132, 2134, 2136, 2138, 10816, 9986, 9978, 526),
	DALMATIAN_1(-1, 12712, 12713, -1, 7249, 7250, -1, 0.0033333333333333, 4, 2132, 2134, 2136, 2138, 10816, 9986, 9978, 526),
	DALMATIAN_2(-1, 12714, 12715, -1, 7251, 7252, -1, 0.0033333333333333, 4, 2132, 2134, 2136, 2138, 10816, 9986, 9978, 526),
	
	/**
	 * A greyhound pet.
	 */
	GREYHOUND(-1, 12514, 12515, -1, 6960, 6961, -1, 0.0033333333333333, 4, 2132, 2134, 2136, 2138, 10816, 9986, 9978, 526),
	GREYHOUND_1(-1, 12704, 12705, -1, 7241, 7242, -1, 0.0033333333333333, 4, 2132, 2134, 2136, 2138, 10816, 9986, 9978, 526),
	GREYHOUND_2(-1, 12706, 12707, -1, 7243, 7244, -1, 0.0033333333333333, 4, 2132, 2134, 2136, 2138, 10816, 9986, 9978, 526),

	/**
	 * A labrador pet.
	 */
	LABRADOR(-1, 12516, 12517, -1, 6962, 6963, -1, 0.0033333333333333, 4, 2132, 2134, 2136, 2138, 10816, 9986, 9978, 526),
	LABRADOR_1(-1, 12708, 12709, -1, 7245, 7246, -1, 0.0033333333333333, 4, 2132, 2134, 2136, 2138, 10816, 9986, 9978, 526),
	LABRADOR_2(-1, 12710, 12711, -1, 7247, 7248, -1, 0.0033333333333333, 4, 2132, 2134, 2136, 2138, 10816, 9986, 9978, 526),

	/**
	 * A sheepdog pet.
	 */
	SHEEPDOG(-1, 12520, 12521, -1, 6966, 6967, -1, 0.0033333333333333, 4, 2132, 2134, 2136, 2138, 10816, 9986, 9978, 526),
	SHEEPDOG_1(-1, 12716, 12717, -1, 7253, 7254, -1, 0.0033333333333333, 4, 2132, 2134, 2136, 2138, 10816, 9986, 9978, 526),
	SHEEPDOG_2(-1, 12718, 12719, -1, 7255, 7256, -1, 0.0033333333333333, 4, 2132, 2134, 2136, 2138, 10816, 9986, 9978, 526),
	
	/**
	 * A terrier pet.
	 */
	TERRIER(-1, 2512, 12513, -1, 6958, 6859, -1, 0.0033333333333333, 4, 2132, 2134, 2136, 2138, 10816, 9986, 9978, 526),
	TERRIER_1(-1, 12700, 12701, -1, 7237, 7238, -1, 0.0033333333333333, 4, 2132, 2134, 2136, 2138, 10816, 9986, 9978, 526),
	TERRIER_2(-1, 12702, 12703, -1, 7239, 7240, -1, 0.0033333333333333, 4, 2132, 2134, 2136, 2138, 10816, 9986, 9978, 526),
	
	/**
	 * A creeping hand pet.
	 */
	CREEPING_HAND(-1, 14652, -1, -1, 8619, -1, -1, 0.0033333333333333, 4, 1059),
	
	/**
	 * Minitrice pet.
	 */
	MINITRICE(-1, 14653, -1, -1, 8620, -1, -1, 0.0033333333333333, 4, 225),
	
	/**
	 * Baby basilisk pet.
	 */
	BABY_BASILISK(-1, 14654, -1, -1, 8621, -1, -1, 0.0033333333333333, 4, 221),

	/**
	 * Baby kurask pet.
	 */
	BABY_KURASK(-1, 14655, -1, -1, 8622, -1, -1, 0.0033333333333333, 4, 526),
	
	/**
	 * Abyssal minion pet.
	 */
	ABYSSAL_MINION(-1, 14651, -1, -1, 8624, -1, -1, 0.0033333333333333, 4, 592),
	
	/**
	 * Rune guardian pets.
	 */
	RUNE_GUARDIAN(-1, 15626, -1, -1, 9656, -1, -1, 0.0033333333333333, 4),
	RUNE_GUARDIAN_1(-1, 15627, -1, -1, 9657, -1, -1, 0.0033333333333333, 4),
	RUNE_GUARDIAN_2(-1, 15628, -1, -1, 9658, -1, -1, 0.0033333333333333, 4),
	RUNE_GUARDIAN_3(-1, 15629, -1, -1, 9659, -1, -1, 0.0033333333333333, 4),
	RUNE_GUARDIAN_4(-1, 15630, -1, -1, 9660, -1, -1, 0.0033333333333333, 4),
	RUNE_GUARDIAN_5(-1, 15631, -1, -1, 9661, -1, -1, 0.0033333333333333, 4),
	RUNE_GUARDIAN_6(-1, 15632, -1, -1, 9662, -1, -1, 0.0033333333333333, 4),
	RUNE_GUARDIAN_7(-1, 15633, -1, -1, 9663, -1, -1, 0.0033333333333333, 4),
	RUNE_GUARDIAN_8(-1, 15634, -1, -1, 9664, -1, -1, 0.0033333333333333, 4),
	RUNE_GUARDIAN_9(-1, 15635, -1, -1, 9665, -1, -1, 0.0033333333333333, 4),
	RUNE_GUARDIAN_10(-1, 15636, -1, -1, 9666, -1, -1, 0.0033333333333333, 4),
	RUNE_GUARDIAN_11(-1, 15637, -1, -1, 9667, -1, -1, 0.0033333333333333, 4),
	RUNE_GUARDIAN_12(-1, 15638, -1, -1, 9668, -1, -1, 0.0033333333333333, 4),
	RUNE_GUARDIAN_13(-1, 15639, -1, -1, 9669, -1, -1, 0.0033333333333333, 4),

	/**
	 * Gecko pet.
	 */
	GECKO(-1, 12488, 12489, -1, 6915, 6916, -1, 0.005, 10, 12125, 12127), 
	GECKO_1(-1, 12738, 12742, -1, 7277, 7281, -1, 0.005, 10, 12125, 12127), 
	GECKO_2(-1, 12739, 12743, -1, 7278, 7282, -1, 0.005, 10, 12125, 12127), 
	GECKO_3(-1, 12740, 12744, -1, 7279, 7283, -1, 0.005, 10, 12125, 12127), 
	GECKO_4(-1, 12741, 12745, -1, 7280, 7284, -1, 0.005, 10, 12125, 12127), 

	/**
	 * The platypus pet.
	 */
	PLATYPUS(-1, 12551, 12548, -1, 7018, 7015, -1, 0.0046296296296296, 10, 321, 363, 341, 15264, 345, 377, 353, 389, 7944, 349, 331, 327, 395, 383, 317, 371, 335, 359, 15264, 15270, 313, 12129),
	PLATYPUS_1(-1, 12552, 12549, -1, 7019, 7016, -1, 0.0046296296296296, 10, 321, 363, 341, 15264, 345, 377, 353, 389, 7944, 349, 331, 327, 395, 383, 317, 371, 335, 359, 15264, 15270, 313, 12129),
	PLATYPUS_2(-1, 12553, 12550, -1, 7020, 7017, -1, 0.0046296296296296, 10, 321, 363, 341, 15264, 345, 377, 353, 389, 7944, 349, 331, 327, 395, 383, 317, 371, 335, 359, 15264, 15270, 313, 12129),

	/**
	 * The broav pet.
	 */
	BROAV(-1, 14533, -1, -1, 8491, -1, -1, 0.0, 23, 2970),
	
	/**
	 * The penguin pet.
	 */
	PENGUIN(-1, 12481, 12482, -1, 6908, 6909, -1, 0.0046296296296296, 30, 321, 363, 341, 15264, 345, 377, 353, 389, 7944, 349, 331, 327, 395, 383, 317, 371, 335, 359, 15264, 15270),
	PENGUIN_1(-1, 12763, 12762, -1, 7313, 7314, -1, 0.0046296296296296, 30, 321, 363, 341, 15264, 345, 377, 353, 389, 7944, 349, 331, 327, 395, 383, 317, 371, 335, 359, 15264, 15270),
	PENGUIN_2(-1, 12765, 12764, -1, 7316, 7317, -1, 0.0046296296296296, 30, 321, 363, 341, 15264, 345, 377, 353, 389, 7944, 349, 331, 327, 395, 383, 317, 371, 335, 359, 15264, 15270),

	/**
	 * A tooth creature pet.
	 */
	TOOTH_CREATURE(-1, 18671, 18669, -1, 11411, 11413, -1, 0.075757575757576, 37, 1927, 1977),
	
	/**
	 * A giant crab pet.
	 */
	GIANT_CRAB(-1, 12500, 12501, -1, 6947, 6948, -1, 0.0069444444444444, 40, 321, 363, 341, 15264, 345, 377, 353, 389, 7944, 349, 331, 327, 395, 383, 317, 371, 335, 359, 15264, 15270),
	GIANT_CRAB_1(-1, 12746, 12747, -1, 7293, 7294, -1, 0.0069444444444444, 40, 321, 363, 341, 15264, 345, 377, 353, 389, 7944, 349, 331, 327, 395, 383, 317, 371, 335, 359, 15264, 15270),
	GIANT_CRAB_2(-1, 12748, 12749, -1, 7295, 7296, -1, 0.0069444444444444, 40, 321, 363, 341, 15264, 345, 377, 353, 389, 7944, 349, 331, 327, 395, 383, 317, 371, 335, 359, 15264, 15270),
	GIANT_CRAB_3(-1, 12750, 12751, -1, 7297, 7298, -1, 0.0069444444444444, 40, 321, 363, 341, 15264, 345, 377, 353, 389, 7944, 349, 331, 327, 395, 383, 317, 371, 335, 359, 15264, 15270),
	GIANT_CRAB_4(-1, 12752, 12753, -1, 7299, 7300, -1, 0.0069444444444444, 40, 321, 363, 341, 15264, 345, 377, 353, 389, 7944, 349, 331, 327, 395, 383, 317, 371, 335, 359, 15264, 15270),

	/**
	 * A Raven pet.
	 */
	RAVEN(-1, 12484, 12485, -1, 6911, 6912, -1, 0.00698888, 50, 313, 12129),
	RAVEN_1(-1, 12724, 12725, -1, 7261, 7262, -1, 0.00698888, 50, 313, 12129),
	RAVEN_2(-1, 12726, 12727, -1, 7263, 7264, -1, 0.00698888, 50, 313, 12129),
	RAVEN_3(-1, 12728, 12729, -1, 7265, 7266, -1, 0.00698888, 50, 313, 12129),
	RAVEN_4(-1, 12730, 12731, -1, 7267, 7268, -1, 0.00698888, 50, 313, 12129),
	RAVEN_5(-1, 12732, 12733, -1, 7269, 7270, -1, 0.00698888, 50, 313, 12129),

	/**
	 * A squirrel pet.
	 */
	SQUIRREL(-1, 12490, 12491, -1, 6919, 6920, -1, 0.0071225071225071, 60, 12130),
	SQUIRREL_1(-1, 12754, 12755, -1, 7301, 7302, -1, 0.0071225071225071, 60, 12130),
	SQUIRREL_2(-1, 12756, 12757, -1, 7303, 7304, -1, 0.0071225071225071, 60, 12130),
	SQUIRREL_3(-1, 12758, 12759, -1, 7305, 7306, -1, 0.0071225071225071, 60, 12130),
	SQUIRREL_4(-1, 12760, 12761, -1, 7307, 7308, -1, 0.0071225071225071, 60, 12130),
	
	/**
	 * Godbirds.
	 */
	SARADOMIN_OWL(-1, 12503, 12504, 12505, 6949, 6950, 6951, 0.0069444444444444, 70, 313, 12129),
	ZAMORAK_HAWK(-1, 12506, 12507, 12508, 6952, 6953, 6954, 0.0069444444444444, 70, 313, 12129),
	GUTHIX_RAPTOR(-1, 12509, 12510, 12511, 6955, 6956, 6957, 0.0069444444444444, 70, 313, 12129),

	/**
	 * Ex-ex parrot
	 */
	EX_EX_PARROT(-1, 13335, -1, -1, 7844, -1, -1, 0.0, 71, 13379),
	
	/**
	 * The phoenix eggling pets.
	 */
	CUTE_PHOENIX_EGGLING(-1, 14627, -1, -1, 8578, -1, -1, 0.0, 72, 592),
	MEAN_PHOENIX_EGGLING(-1, 14626, -1, -1, 8577, -1, -1, 0.0, 72, 592),
	
	/**
	 * A raccoon pet.
	 */
	RACCOON(-1, 12486, 12487, -1, 6913, 6914, -1, 0.0029444444444444, 80, 321, 363, 341, 15264, 345, 377, 353, 389, 7944, 349, 331, 327, 395, 383, 317, 371, 335, 359, 15264, 15270, 2132, 2134, 2136, 2138, 10816, 9986, 9978),
	RACCOON_1(-1, 12734, 12735, -1, 7271, 7272, -1, 0.0029444444444444, 80, 321, 363, 341, 15264, 345, 377, 353, 389, 7944, 349, 331, 327, 395, 383, 317, 371, 335, 359, 15264, 15270, 2132, 2134, 2136, 2138, 10816, 9986, 9978),
	RACCOON_2(-1, 12736, 12737, -1, 7273, 7274, -1, 0.0029444444444444, 80, 321, 363, 341, 15264, 345, 377, 353, 389, 7944, 349, 331, 327, 395, 383, 317, 371, 335, 359, 15264, 15270, 2132, 2134, 2136, 2138, 10816, 9986, 9978),
	
	/**
	 * A sneaker peeper pet.
	 */
	SNEAKER_PEEPER(-1, 19894, 19895, -1, 13089, 13090, -1, 0.05, 80, 221),

	/**
	 * A vulture pet.
	 */
	VULTURE(-1, 12498, 12499, -1, 6945, 6946, -1, 0.0078, 85, 313, 12129),
	VULTURE_1(-1, 12766, 12767, -1, 7319, 7320, -1, 0.0078, 85, 313, 12129),
	VULTURE_2(-1, 12768, 12769, -1, 7321, 7322, -1, 0.0078, 85, 313, 12129),
	VULTURE_3(-1, 12770, 12771, -1, 7323, 7324, -1, 0.0078, 85, 313, 12129),
	VULTURE_4(-1, 12772, 12773, -1, 7325, 7326, -1, 0.0078, 85, 313, 12129),
	VULTURE_5(-1, 12774, 12775, -1, 7327, 7328, -1, 0.0078, 85, 313, 12129),

	/**
	 * A chameleon pet.
	 */
	CHAMELEON(-1, 12492, 12493, -1, 6922, 6923, -1, 0.0069444444444444, 90, 12125), 

	/**
	 * A monkey pet.
	 */
	MONKEY(-1, 12496, 12497, -1, 6942, 6943, -1, 0.0069444444444444, 95, 1963),
	MONKEY_1(-1, 12682, 12683, -1, 7210, 7211, -1, 0.0069444444444444, 95, 1963),
	MONKEY_2(-1, 12684, 12685, -1, 7212, 7213, -1, 0.0069444444444444, 95, 1963),
	MONKEY_3(-1, 12686, 12687, -1, 7214, 7215, -1, 0.0069444444444444, 95, 1963),
	MONKEY_4(-1, 12688, 12689, -1, 7216, 7217, -1, 0.0069444444444444, 95, 1963),
	MONKEY_5(-1, 12690, 12691, -1, 7218, 7219, -1, 0.0069444444444444, 95, 1963),
	MONKEY_6(-1, 12692, 12693, -1, 7220, 7221, -1, 0.0069444444444444, 95, 1963),
	MONKEY_7(-1, 12694, 12695, -1, 7222, 7223, -1, 0.0069444444444444, 95, 1963),
	MONKEY_8(-1, 12696, 12697, -1, 7224, 7225, -1, 0.0069444444444444, 95, 1963),
	MONKEY_9(-1, 12698, 12699, -1, 7226, 7227, -1, 0.0069444444444444, 95, 1963),
	
	/**
	 * A baby dragon pet.
	 */
	BABY_DRAGON(-1, 12469, 12470, -1, 6900, 6901, -1, 0.0052, 99, 2132, 2134, 2136, 2138, 10816, 9986, 9978, 321, 363, 341, 15264, 345, 377, 353, 389, 7944, 349, 331, 327, 395, 383, 317, 371, 335, 359, 15264, 15270),
	BABY_DRAGON_1(-1, 12471, 12472, -1, 6902, 6903, -1, 0.0052, 99, 2132, 2134, 2136, 2138, 10816, 9986, 9978, 321, 363, 341, 15264, 345, 377, 353, 389, 7944, 349, 331, 327, 395, 383, 317, 371, 335, 359, 15264, 15270),
	BABY_DRAGON_2(-1, 12473, 12474, -1, 6904, 6905, -1, 0.0052, 99, 2132, 2134, 2136, 2138, 10816, 9986, 9978, 321, 363, 341, 15264, 345, 377, 353, 389, 7944, 349, 331, 327, 395, 383, 317, 371, 335, 359, 15264, 15270),
	BABY_DRAGON_3(-1, 12475, 12476, -1, 6906, 6907, -1, 0.0052, 99, 2132, 2134, 2136, 2138, 10816, 9986, 9978, 321, 363, 341, 15264, 345, 377, 353, 389, 7944, 349, 331, 327, 395, 383, 317, 371, 335, 359, 15264, 15270),

	/**
	 * TzRek-Jad pet.
	 */
	TZREK_JAD(-1, 21512, -1, -1, 3604, -1, -1, 0.0, 99),
	
	/**
	 * Dragonkk's pet, as requested.
	 */
	FERRET(-1, 10092, -1, -1, 5559, -1, -1, 0.0, 99),
	
	HOE(-1, 0, -1, -1, 10, -1, -1, 0.0, 1),
	
	/**
	 * Emperor's (me) pet.
	 */
	GIANT_WOLPERTINGER(-1, 8888, -1, -1, 6990, -1, -1, 0.0, 99),
	
	/**
	 * Danny's (me) pet.
	 */
	QUEEN_BLACK_DRAGON(-1, 24385, -1, -1, 15454, -1, -1, 0.0, 99),
	
	
	/* New Pets*/
	CHICKARRA(0, 33778,-1,-1,20530,-1,-1,0.0,1), //Familiar DONE
	
	COMMANDERMINIANA(0, 33807,-1,-1,20533,-1,-1,0.0,1), //Familiar DONE
	
	GENERALAWWDOR(0, 33806,-1,-1,20532,-1,-1,0.0,1),
	
	KRILTINYROTH(0, 33805,-1,-1,20531,-1,-1,0.0,1),
	
	NEXTERMINATOR(0, 33808,-1,-1,20534,-1,-1,0.0, 1, /* Foods */ 1), //Familiar DONE
	
	MOLLY(0, 33813,-1,-1,20539,-1,-1,0.0,1),
	
	SHRIMPY(0, 33788, -1, -1, 20540, -1, -1, 0.0, 1), //Familiar DONE
	
	KINGBLACKDRAGONLING(0, 33792,-1,-1,20544,-1,-1,0.0,1),
	
	QUEENBLACKDRAGONLING(0, 33799,-1,-1,20551,-1,-1,0.0,1),
	
	KALPHITEGRUBLET(0, 33790,-1,-1,20543,-1,-1,0.0,1),
	
	KALPHITEGRUBLING(0, 33789,-1,-1,20541,-1,-1,0.0,1),
	
	CORPORALPUPPY(0, 33786,-1,-1,20538,-1,-1,0.0,1),
	
	ELLIE(0, 33785,-1,-1,20537,-1,-1,0.0,1),
	
	LEGIOPRIMULUS(0, 33819,-1,-1,20545,-1,-1,0.0,1),
	
	LEGIOSECUNDULUS(0, 33820,-1,-1,20546,-1,-1,0.0,1),
	
	LEGIOTERTIOLUS(0, 33821,-1,-1,20547,-1,-1,0.0,1),
	
	LEGIOGUARTULUS(0, 33822,-1,-1,20548,-1,-1,0.0,1),
	
	LEGIOQUINTULUS(0, 33823,-1,-1,20549,-1,-1,0.0,1),
	
	LEGIOSEXTULUS(0, 33824,-1,-1,20550,-1,-1,0.0,1),
	
	REXHATCHLING(0, 33801,-1,-1,20553,-1,-1,0.0,1),
	
	PRIMEHATCHLING(0, 33800,-1,-1,20552,-1,-1,0.0,1),
	
	SUPREMEHATCHLING(0, 33802,-1,-1,20554,-1,-1,0.0,1),
	
	DUCKSSSSSSS(0, 33803,-1,-1,20555,-1,-1,0.0,1),
	
	BOMBI(0, 33717,-1,-1,20461,-1,-1,0.0,1),
	
	BABY_JADINKO(-1, 19983, -1, -1, 13165, -1, -1, 0.0, 1),
	
	BARRY(0, 33809,-1,-1,20535,-1,-1,0.0,1),
	
	DAVE(0, 31748,-1,-1,19475,-1,-1,0.0,1),
	
	STEVE(0, 31749,-1,-1,19476,-1,-1,0.0,1),
	
	PETE(0, 31750,-1,-1,19477,-1,-1,0.0,1),
	
	GAVIN(0, 31751,-1,-1,19478,-1,-1,0.0,1),
	
	LANA(0, 31752,-1,-1,19479,-1,-1,0.0,1),
	
	BILL(0, 31753,-1,-1,19480,-1,-1,0.0,1),
	
	VITALIS(0, 28630,-1,-1,17186,-1,-1,0.0,1),
	
	VORAGO_SHARD(0, 28685,-1,-1,17397,-1,-1,0.0,1),
	
	SKYPOUNCER(-1, 26568,-1,-1,16674,-1,-1,0.0,1),
	
	BLOODPOUNCER(-1, 26569,-1,-1,16677,-1,-1,0.0,1),
	
	DRAGONWOLF(-1, 27215,-1,-1,16738,-1,-1,0.0,1),
	
	BLAZEHOUND(-1, 26567,-1,-1,16671,-1,-1,0.0,1),
	
	PROTOTYPE_COLOSSUS(-1, 28828,-1,-1,17785,-1,-1,0.0,1),
	
	WARBORN_BEHEMOTH(-1, 28833,-1,-1,17791,-1,-1,0.0,1),
	
	RORY_THE_REINDEER(-1, 30368,-1,-1,18819,-1,-1,0.0,1),
	
	DAWN(-1, 30064,-1,-1,18571,-1,-1,0.0,1),
	
	DUSK(-1, 30065,-1,-1,18573,-1,-1,0.0,1),
	
	MALLORY(0, 33784,-1,-1,20536,-1,-1,0.0,1);
	
	/* End of new Pets*/
	
	//BABY_JADINKO(19983, -1, -1, 13165, -1, -1, 0.0, 1);
	
	/**
	 * The baby pets mapping.
	 */
	private static final Map<Integer, Pets> babyPets = new HashMap<Integer, Pets>();
	
	/**
	 * The grown pets mapping.
	 */
	private static final Map<Integer, Pets> grownPets = new HashMap<Integer, Pets>();
	
	/**
	 * The overgrown pets mapping.
	 */
	private static final Map<Integer, Pets> overgrownPets = new HashMap<Integer, Pets>();
	
	/**
	 * Populates the mappings.
	 */
	static {
		for (Pets pet : Pets.values()) {
			babyPets.put(pet.babyItemId, pet);
			if (pet.grownItemId > 0) {
				grownPets.put(pet.grownItemId, pet);
				if (pet.getOvergrownItemId() > 0) {
					overgrownPets.put(pet.overgrownItemId, pet);
				}
			}
		}
	}
	
	/**
	 * Gets the pet object for the item id.
	 * @param itemId The item id.
	 * @return The pet object.
	 */
	public static Pets forId(int itemId) {
		Pets pet = babyPets.get(itemId);
		if (pet == null) {
			pet = grownPets.get(itemId);
			if (pet == null) {
				return overgrownPets.get(itemId);
			}
			return pet;
		}
		return pet;
	}
	
	/**
	 * Checks if a player has a pet.
	 * @param player The player.
	 * @return {@code True} if so.
	 */
	public static boolean hasPet(Player player) {
		for (int itemId : babyPets.keySet()) {
			if (player.getInventory().containsOneItem(itemId)) {
				return true;
			}
		}
		for (int itemId : grownPets.keySet()) {
			if (player.getInventory().containsOneItem(itemId)) {
				return true;
			}
		}
		for (int itemId : overgrownPets.keySet()) {
			if (player.getInventory().containsOneItem(itemId)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * The baby item id.
	 */
	private final int babyItemId;
	
	/**
	 * The grown pet's item id.
	 */
	private final int grownItemId;
	
	/**
	 * The overgrown pet's item id.
	 */
	private final int overgrownItemId;
	
	/**
	 * The baby pet NPC id.
	 */
	private final int babyNpcId;
	
	/**
	 * The grown pet NPC id.
	 */
	private final int grownNpcId;
	
	/**
	 * The overgrown pet NPC id.
	 */
	private final int overgrownNpcId;
	
	/**
	 * The growth rate.
	 */
	private final double growthRate;
	
	/**
	 * The summoning level required.
	 */
	private final int summoningLevel;
	
	/**
	 * The food this pet uses.
	 */
	private final int[] food;
	
	/**
	 * The pets default level.
	 */
	public int petLevel;
	
	/**
	 * Constructs a new {@code Pets} {@code Object}.
	 * @param babyItemId The baby pet item id.
	 * @param grownItemId The grown pet item id.
	 * @param overgrownItemId The overgrown item id.
	 * @param babyNpcId The baby pet npc id.
	 * @param grownNpcId The grown pet npc id.
	 * @param overgrownNpcId The overgrown npc id.
	 * @param growthRate The growth rate (amount to increase growth with every tick).
	 * @param summoningLevel The summoning level required.
	 * @param food The food item ids the pet uses.
	 */
	private Pets(int petLevel, int babyItemId, int grownItemId, int overgrownItemId, int babyNpcId, int grownNpcId, int overgrownNpcId, double growthRate, int summoningLevel, int...food) {
		this.petLevel = petLevel;
		this.babyItemId = babyItemId;
		this.grownItemId = grownItemId;
		this.overgrownItemId = overgrownItemId;
		this.babyNpcId = babyNpcId;
		this.grownNpcId = grownNpcId;
		this.overgrownNpcId = overgrownNpcId;
		this.growthRate = growthRate;
		this.summoningLevel = summoningLevel;
		this.food = food;
	}

	/**
	 * Gets the babyItemId.
	 * @return The babyItemId.
	 */
	public int getBabyItemId() {
		return babyItemId;
	}

	/**
	 * Gets the grownItemId.
	 * @return The grownItemId.
	 */
	public int getGrownItemId() {
		return grownItemId;
	}

	/**
	 * Gets the overgrownItemId.
	 * @return The overgrownItemId.
	 */
	public int getOvergrownItemId() {
		return overgrownItemId;
	}

	/**
	 * Gets the babyNpcId.
	 * @return The babyNpcId.
	 */
	public int getBabyNpcId() {
		return babyNpcId;
	}

	/**
	 * Gets the grownNpcId.
	 * @return The grownNpcId.
	 */
	public int getGrownNpcId() {
		return grownNpcId;
	}

	/**
	 * Gets the overgrownNpcId.
	 * @return The overgrownNpcId.
	 */
	public int getOvergrownNpcId() {
		return overgrownNpcId;
	}

	/**
	 * Gets the growthRate.
	 * @return The growthRate.
	 */
	public double getGrowthRate() {
		return growthRate;
	}

	/**
	 * Gets the summoningLevel.
	 * @return The summoningLevel.
	 */
	public int getSummoningLevel() {
		return summoningLevel;
	}

	/**
	 * Gets the food.
	 * @return The food.
	 */
	public int[] getFood() {
		return food;
	}
	
	/**
	 * Gets the pets level.
	 * @return The Pet's level.
	 */
	public int getPetLevel() {
		return petLevel;
	}

	/**
	 * Gets the NPC id for this pet.
	 * @param stage The stage of the pet.
	 * @return The NPc id.
	 */
	public int getNpcId(int stage) {
		switch (stage) {
		case 0:
			return babyNpcId;
		case 1:
			return grownNpcId;
		case 2:
			return overgrownNpcId;
		}
		return 0;
	}
	
	/**
	 * Gets the item id for this pet.
	 * @param stage The stage of the pet.
	 * @return The item id.
	 */
	public int getItemId(int stage) {
		switch (stage) {
		case 0:
			return babyItemId;
		case 1:
			return grownItemId;
		case 2:
			return overgrownItemId;
		}
		return 0;
	}

	@SuppressWarnings("incomplete-switch")
	public boolean isBossPet() {
		switch (Pets.valueOf(Pets.class, name())) {
			case NEXTERMINATOR:
			case SHRIMPY:
			case VORAGO_SHARD:
			case BOMBI:
			case VITALIS:
			case MALLORY:
			case MOLLY:
			case GENERALAWWDOR:
			case COMMANDERMINIANA:
			case KRILTINYROTH:
			case CHICKARRA:
			case GAVIN:
			case BARRY:
			case BILL:
			case STEVE:
			case DAVE:
			case PETE:
			case LANA:
			case KINGBLACKDRAGONLING:
			case QUEENBLACKDRAGONLING:
			case CORPORALPUPPY:
			case KALPHITEGRUBLET:
			case KALPHITEGRUBLING:
			case ELLIE:
			case LEGIOGUARTULUS:
			case LEGIOPRIMULUS:
			case LEGIOQUINTULUS:
			case LEGIOSECUNDULUS:
			case LEGIOSEXTULUS:
			case LEGIOTERTIOLUS:
				return true;
		}
		return false;
	}
}
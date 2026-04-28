package com.rs.game.player;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.rs.utils.Utils;

public final class QuestManager implements Serializable {

	private static final long serialVersionUID = -8085932531253271252L;
	
	public static enum Quests {
		THE_RESTLESS_GHOST,
		COOKS_ASSISTANT,
		IMP_CATCHER
	}
	
	
	private transient Player player;
	private List<Quests> completedQuests;
	private HashMap<Quests, Integer> questStages;
	
	public QuestManager() {
		completedQuests = new ArrayList<Quests>();
	}
	
	public void setPlayer(Player player) {
		this.player = player;
		if(questStages == null)
			questStages = new HashMap<Quests, Integer>();
	}
	
	public int getQuestStage(Quests quest) {
		if(completedQuests.contains(quest))
			return -1;
		Integer stage = questStages.get(quest);
		return stage == null ? -2 : stage;
	}
	
	public void setQuestStageAndRefresh(Quests quest, int stage) {
		setQuestStage(quest, stage);
		sendStageData(quest);
	}
	
	public void setQuestStage(Quests quest, int stage) {
		if(completedQuests.contains(quest))
			return;
		questStages.put(quest, stage);
	}
	
	public void init() {
		checkCompleted(); //temporary
		for(Quests quest : completedQuests)
			sendCompletedQuestsData(quest);
		for(Quests quest : questStages.keySet())
			sendStageData(quest);
	}

	public void checkCompleted() {

	}
	
	public void completeQuest(Quests quest) {
		completedQuests.add(quest);
		questStages.remove(quest);
		sendCompletedQuestsData(quest); 
		player.getPackets().sendGameMessage("<col=ff0000>You have completed quest: " + Utils.formatPlayerNameForDisplay(quest.toString()) + ".");
	}
	
	public void sendCompletedQuestsData(Quests quest) {
		switch(quest) {
		
			case THE_RESTLESS_GHOST:
				player.getPackets().sendConfig(107, 5);
			break;
			
			case COOKS_ASSISTANT:
				player.getPackets().sendConfig(29, 2);
			break;
			
			case IMP_CATCHER:
				player.getPackets().sendConfig(160, 2);
			break;
			
		}
	}
	
	private void sendStageData(Quests quest) {
		switch(quest) {
		
			case THE_RESTLESS_GHOST:
				if (!completedQuest(Quests.THE_RESTLESS_GHOST) && getQuestStage(Quests.THE_RESTLESS_GHOST) > 0) {
					player.getPackets().sendConfig(107, 1);
				}
			break;
			
			case COOKS_ASSISTANT:
				if (!completedQuest(Quests.COOKS_ASSISTANT) && getQuestStage(Quests.COOKS_ASSISTANT) > 0) {
					player.getPackets().sendConfig(29, 1);
				}
			break;
			
			case IMP_CATCHER:
				if (!completedQuest(Quests.IMP_CATCHER) && getQuestStage(Quests.IMP_CATCHER) > 0) {
					player.getPackets().sendConfig(160, 1);
				}
			break;
		}
	}
	
	public boolean completedQuest(Quests quest) {
		return completedQuests.contains(quest);
	}
}

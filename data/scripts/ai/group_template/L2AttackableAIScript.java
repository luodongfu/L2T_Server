/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package ai.group_template;

import static l2server.gameserver.ai.CtrlIntention.AI_INTENTION_ATTACK;

import l2server.Config;
import l2server.gameserver.ai.CtrlEvent;
import l2server.gameserver.ai.CtrlIntention;
import l2server.gameserver.datatables.NpcTable;
import l2server.gameserver.model.L2Object;
import l2server.gameserver.model.L2Skill;
import l2server.gameserver.model.actor.L2Attackable;
import l2server.gameserver.model.actor.L2Character;
import l2server.gameserver.model.actor.L2Npc;
import l2server.gameserver.model.actor.instance.L2MonsterInstance;
import l2server.gameserver.model.actor.instance.L2PcInstance;
import l2server.gameserver.model.quest.Quest;
import l2server.gameserver.model.quest.jython.QuestJython;
import l2server.gameserver.templates.chars.L2NpcTemplate;
import l2server.gameserver.util.MinionList;
import l2server.gameserver.util.Util;
import l2server.log.Log;
import l2server.util.Rnd;

/**
 * Overarching Superclass for all mob AI
 *
 * @author Fulminus
 */
public class L2AttackableAIScript extends QuestJython
{

    /**
     * This is used to register all monsters contained in mobs for a particular script<BR>
     * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method register ID for all QuestEventTypes<BR>
     * Do not use for group_template AIs</B></FONT><BR>
     *
     * @param mobs
     * @see #registerMobs(int[], QuestEventType...)
     */
    public void registerMobs(int[] mobs)
    {
        for (int id : mobs)
        {
            addEventId(id, QuestEventType.ON_ATTACK);
            addEventId(id, QuestEventType.ON_KILL);
            addEventId(id, QuestEventType.ON_SPAWN);
            addEventId(id, QuestEventType.ON_SPELL_FINISHED);
            addEventId(id, QuestEventType.ON_SKILL_SEE);
            addEventId(id, QuestEventType.ON_FACTION_CALL);
            addEventId(id, QuestEventType.ON_AGGRO_RANGE_ENTER);
        }
    }

    /**
     * This is used to register all monsters contained in mobs for a particular script
     * event types defined in types.
     *
     * @param mobs
     * @param types
     */
    public void registerMobs(int[] mobs, QuestEventType... types)
    {
        for (int id : mobs)
        {
            for (QuestEventType type : types)
            {
                addEventId(id, type);
            }
        }
    }

    public L2AttackableAIScript(int questId, String name, String descr)
    {
        super(questId, name, descr);
    }

    @Override
    public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
    {
        return null;
    }

    @Override
    public String onSpellFinished(L2Npc npc, L2PcInstance player, L2Skill skill)
    {
        return null;
    }

    @Override
    public String onSkillSee(L2Npc npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isPet)
    {
        if (caster == null)
        {
            return null;
        }
        if (!(npc instanceof L2Attackable))
        {
            return null;
        }

        L2Attackable attackable = (L2Attackable) npc;

        int skillAggroPoints = skill.getAggroPoints();

        if (caster.getPet() != null)
        {
            if (targets.length == 1 && Util.contains(targets, caster.getPet()))
            {
                skillAggroPoints = 0;
            }
        }

        if (skillAggroPoints > 0)
        {
            if (attackable.hasAI() && attackable.getAI().getIntention() == AI_INTENTION_ATTACK)
            {
                L2Object npcTarget = attackable.getTarget();
                for (L2Object skillTarget : targets)
                {
                    if (npcTarget == skillTarget || npc == skillTarget)
                    {
                        L2Character originalCaster = isPet ? caster.getPet() : caster;
                        attackable
                                .addDamageHate(originalCaster, 0, skillAggroPoints * 150 / (attackable.getLevel() + 7));
                    }
                }
            }
        }

        return null;
    }

    @Override
    public String onFactionCall(L2Npc npc, L2Npc caller, L2PcInstance attacker, boolean isPet)
    {
        if (attacker == null)
        {
            return null;
        }

        L2Character originalAttackTarget = isPet ? attacker.getPet() : attacker;

        // Preventing some strange behavior with CALLS (1 calls 2, 2 calls 1, 1 calls 2, etc)
        if (npc.getAI().getAttackTarget() != null)
        {
            return null;
        }

        // By default, when a faction member calls for help, attack the caller's attacker.
        // Notify the AI with EVT_AGGRESSION
        npc.getAI().notifyEvent(CtrlEvent.EVT_AGGRESSION, originalAttackTarget, 1);

        return null;
    }

    @Override
    public String onAggroRangeEnter(L2Npc npc, L2PcInstance player, boolean isPet)
    {
        if (player == null)
        {
            return null;
        }

        L2Character target = isPet ? player.getPet() : player;

        ((L2Attackable) npc).addDamageHate(target, 0, 1);

        // Set the intention to the L2Attackable to AI_INTENTION_ACTIVE
        if (npc.getAI().getIntention() == CtrlIntention.AI_INTENTION_IDLE)
        {
            npc.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
        }
        return null;
    }

    @Override
    public String onSpawn(L2Npc npc)
    {
        return null;
    }

    @Override
    public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet)
    {
        if (attacker != null && npc instanceof L2Attackable)
        {
            L2Attackable attackable = (L2Attackable) npc;

            L2Character originalAttacker = isPet ? attacker.getPet() : attacker;
            if (isPet && originalAttacker == null && attacker.getSummons().size() > 0)
            {
                originalAttacker = attacker.getSummon(Rnd.get(attacker.getSummons().size()));
            }
            attackable.getAI().notifyEvent(CtrlEvent.EVT_ATTACKED, originalAttacker);
            attackable.addDamageHate(originalAttacker, damage, damage * 100 / (attackable.getLevel() + 7));

            //if (npc.isMinion() && ((L2MonsterInstance) npc).getLeader().isDead())
            //	((L2MonsterInstance) npc).getLeader().getMinionList().deleteSpawnedMinions();

        }
        return null;
    }

    @Override
    public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
    {
        if (npc instanceof L2MonsterInstance)
        {
            final L2MonsterInstance mob = (L2MonsterInstance) npc;
            final L2MonsterInstance leader = mob.getLeader();

            if (leader != null)
            {
                MinionList minionList = leader.getMinionList();

                if (minionList != null)
                {
                    final int respawnTime = Config.MINIONS_RESPAWN_TIME
                            .get(mob.getNpcId()) > 0 ? Config.MINIONS_RESPAWN_TIME.get(mob.getNpcId()) * 1000 : -1;
                    minionList.onMinionDie(mob, respawnTime);
                }
            }

            if (mob.hasMinions())
            {
                mob.getMinionList().onMasterDie(false);
            }
        }
        return null;
    }

    public static void main(String[] args)
    {
        L2AttackableAIScript ai = new L2AttackableAIScript(-1, "L2AttackableAIScript", "L2AttackableAIScript");
        // register all mobs here...
        for (int level = 1; level < 120; level++)
        {
            L2NpcTemplate[] templates = NpcTable.getInstance().getAllOfLevel(level);
            if (templates != null && templates.length > 0)
            {
                for (L2NpcTemplate t : templates)
                {
                    try
                    {
                        if (L2Attackable.class.isAssignableFrom(Class
                                .forName("l2server.gameserver.model.actor.instance." + t.Type + "Instance")))
                        {
                            ai.addEventId(t.NpcId, Quest.QuestEventType.ON_ATTACK);
                            ai.addEventId(t.NpcId, Quest.QuestEventType.ON_KILL);
                            ai.addEventId(t.NpcId, Quest.QuestEventType.ON_SPAWN);
                            ai.addEventId(t.NpcId, Quest.QuestEventType.ON_SKILL_SEE);
                            ai.addEventId(t.NpcId, Quest.QuestEventType.ON_FACTION_CALL);
                            ai.addEventId(t.NpcId, Quest.QuestEventType.ON_AGGRO_RANGE_ENTER);
                        }
                    }
                    catch (ClassNotFoundException ex)
                    {
                        Log.info("Class not found " + t.Type + "Instance");
                    }
                }
            }
        }
    }
}

package l2tserver.gameserver.events;

import l2tserver.gameserver.Announcements;
import l2tserver.gameserver.GeoData;
import l2tserver.gameserver.ThreadPoolManager;
import l2tserver.gameserver.TimeController;
import l2tserver.gameserver.datatables.NpcTable;
import l2tserver.gameserver.datatables.SkillTable;
import l2tserver.gameserver.datatables.SpawnTable;
import l2tserver.gameserver.instancemanager.ZoneManager;
import l2tserver.gameserver.model.L2Spawn;
import l2tserver.gameserver.model.actor.L2Npc;
import l2tserver.gameserver.model.actor.instance.L2PcInstance;
import l2tserver.gameserver.model.zone.type.L2ClanHallZone;
import l2tserver.gameserver.network.serverpackets.MagicSkillLaunched;
import l2tserver.gameserver.network.serverpackets.MagicSkillUse;
import l2tserver.gameserver.network.serverpackets.NpcHtmlMessage;
import l2tserver.gameserver.network.serverpackets.SetupGauge;
import l2tserver.gameserver.templates.chars.L2NpcTemplate;
import l2tserver.gameserver.util.Util;
import l2tserver.log.Log;
import l2tserver.util.Rnd;

/**
 * @author Pere
 */
public class HiddenChests
{
	public static HiddenChests _instance = null;

	private static final int SPECIAL_CHEST_COUNT = 5;
	private L2Spawn[] _specialChestSpawns = new L2Spawn[SPECIAL_CHEST_COUNT];
	private HiddenChestsTask[] _specialChestTasks = new HiddenChestsTask[SPECIAL_CHEST_COUNT];
	
	public static HiddenChests getInstance()
	{
		if (_instance == null)
			_instance = new HiddenChests();
		return _instance;
	}
	
	public void spawnChests()
	{
		L2NpcTemplate tmpl = NpcTable.getInstance().getTemplate(50101);
		try
		{
			int i;
			for (i = 0; i < SPECIAL_CHEST_COUNT; i++)
			{
				L2Spawn chestSpawn = new L2Spawn(tmpl);
				
				L2Spawn loc = SpawnTable.getInstance().getRandomMonsterSpawn();
				L2ClanHallZone zone = ZoneManager.getInstance().getZone(loc.getX(), loc.getY(), loc.getZ(), L2ClanHallZone.class);
				while (loc.getNpc().getX() < 150000 || loc.getNpc().getY() > 227000 || zone != null)
				{
					loc = SpawnTable.getInstance().getRandomMonsterSpawn();
					zone = ZoneManager.getInstance().getZone(loc.getX(), loc.getY(), loc.getZ(), L2ClanHallZone.class);
				}
				
				chestSpawn.setX(loc.getNpc().getX() + Rnd.get(300));
				chestSpawn.setY(loc.getNpc().getY() + Rnd.get(300));
				chestSpawn.setZ(loc.getNpc().getZ() + 10);
				chestSpawn.setHeading(Rnd.get(65536));
				chestSpawn.setRespawnDelay(10);
				chestSpawn.setInstanceId(0);
				
				SpawnTable.getInstance().addNewSpawn(chestSpawn, false);

				chestSpawn.stopRespawn();
				chestSpawn.doSpawn();
				
				while (!GeoData.getInstance().canSeeTarget(loc.getNpc(), chestSpawn.getNpc()))
					chestSpawn.getNpc().setXYZ(loc.getNpc().getX() + Rnd.get(300), loc.getNpc().getY() + Rnd.get(300), loc.getNpc().getZ() + 10);
				
				String name = "";
				for (int j = 0; j < 10; j++)
				{
					int rnd = (int)(Math.random() * 36);
					char c = (char)('0' + rnd);
					if (rnd >= 10)
						c = (char)('a' + rnd - 10);
					name += c;
				}
				
				chestSpawn.getNpc().setName(name);
				
				_specialChestTasks[i] = new HiddenChestsTask(i, System.currentTimeMillis() + 3600000L * 5 + Rnd.get(3600000 * 3));
				ThreadPoolManager.getInstance().executeTask(_specialChestTasks[i]);
				
				_specialChestSpawns[i] = chestSpawn;
			}
		}
		catch (Exception e)
		{
			Log.warning("Chest event exception:");
			e.printStackTrace();
		}
	}

	public void moveChest(L2Npc chest, final boolean delayed)
	{
		if (chest == null)
			return;
		
		final L2NpcTemplate tmpl = NpcTable.getInstance().getTemplate(50101);
		if (tmpl == null)
		{
			Log.warning("ERROR: NPC " + chest.getObjectId() + " has a null template.");
			return;
		}
		
		int index = 0;
		for (L2Spawn scs : _specialChestSpawns)
		{
			if ( scs != null && scs.getNpc() != null
					&& scs.getNpc() == chest)
				break;
			index++;
		}
		
		if (index >= _specialChestSpawns.length)
		{
			Log.warning("ERROR: NPC " + chest.getObjectId() + " is not in the chest spawns list.");
			chest.deleteMe();
			chest.getSpawn().stopRespawn();
			return;
		}
		
		final int respawnTime = _specialChestSpawns[index].getRespawnDelay() / 1000;
		chest.deleteMe();
		_specialChestSpawns[index].stopRespawn();
		SpawnTable.getInstance().deleteSpawn(_specialChestSpawns[index], false);
		_specialChestSpawns[index] = null;
		
		final int fIndex = index;
		
		ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
		{
			public void run()
			{
				try
				{
					//L2MonsterInstance mob = new L2MonsterInstance(monsterTemplate, template1);
					
					L2Spawn chestSpawn = new L2Spawn(tmpl);
					L2Spawn loc = SpawnTable.getInstance().getRandomMonsterSpawn();
					while (loc.getNpc().getX() < 150000 || loc.getNpc().getY() > 227000)
						loc = SpawnTable.getInstance().getRandomMonsterSpawn();
					chestSpawn.setX(loc.getNpc().getX() + Rnd.get(300));
					chestSpawn.setY(loc.getNpc().getY() + Rnd.get(300));
					chestSpawn.setZ(loc.getNpc().getZ() + 10);
					chestSpawn.setHeading(Rnd.get(65536));
					chestSpawn.setRespawnDelay(respawnTime);
					chestSpawn.setInstanceId(0);
					SpawnTable.getInstance().addNewSpawn(chestSpawn, false);
					chestSpawn.stopRespawn();
					chestSpawn.doSpawn();
					
					while (!GeoData.getInstance().canSeeTarget(loc.getNpc(), chestSpawn.getNpc()))
						chestSpawn.getNpc().setXYZ(loc.getNpc().getX() + Rnd.get(300), loc.getNpc().getY() + Rnd.get(300), loc.getNpc().getZ() + 10);
					
					//_specialChestSpawns[fIndex].getNpc().setName(tmpl.getName() + " #" + (fIndex + 1));
					
					_specialChestTasks[fIndex].setStartTime(System.currentTimeMillis() + 3600000L * 5);
					
					if (delayed)
						Announcements.getInstance().announceToAll("The treasure chest #" + (fIndex + 1) + " has respawned! Use .treasure for hints to find it.");
					
					_specialChestSpawns[fIndex] = chestSpawn;
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}, delayed ? 6 * 3600 * 1000 : 1000);
	}
	
	public void showInfo(L2PcInstance activeChar)
	{
		String html = "<html>" +
		"<body>" +
		"<center><table><tr><td><img src=icon.etc_alphabet_h_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_i_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_d_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_d_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_e_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_n_i00 width=32 height=32></td></tr></table></center>"+
		"<table><tr><td><img src=icon.etc_alphabet_t_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_r_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_e_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_a_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_s_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_u_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_r_i00 width=32 height=32></td><td><img src=icon.etc_alphabet_e_i00 width=32 height=32></td></tr></table>"+
		"<br><br>" +
		"<table width=300><tr><td>These are all the treasures in the world with hints for you to find them:</td></tr></table><br>" +
		"<center>";
		int x = 0;
		boolean someChest = false;
		for (int i = 0; i < SPECIAL_CHEST_COUNT; i++)
		{
			L2Spawn chest = _specialChestSpawns[i];
			if (chest == null)
				continue;
			
			long dx = activeChar.getX() - chest.getX();
			long dy = activeChar.getY() - chest.getY();
			long dz = activeChar.getZ() - chest.getZ();
			int distance = (int)Math.sqrt(dx * dx + dy * dy + dz * dz);
			String hint = "<font color=00FF00>Far far away</font>";
			if (distance < 500)
				hint = "<font color=9B0000>Extremely hot!</font>";
			else if (distance < 1000)
				hint = "<font color=FF0000>Very hot!</font>";
			else if (distance < 2000)
				hint = "<font color=EA0000>Hot!</font>";
			else if (distance < 5000)
				hint = "<font color=FF2020>Very warm</font>";
			else if (distance < 10000)
				hint = "<font color=FF4646>Warm</font>";
			else if (distance < 20000)
				hint = "<font color=379BFF>Cold</font>";
			else if (distance < 50000)
				hint = "<font color=0074E8>Very cold</font>";
			html += "<table height=50 width=300 " + (x % 2 == 0 ? "bgcolor=131210" : "")+  "><tr>";
			html += "<td><img src=icon.etc_treasure_box_i08 width=32 height=32></td><td fixwidth=\"70\">Chest #" + (i + 1) + "</td>";
			html += "<td fixwidth=\"100\">" + (activeChar.isGM() ? "<a action=\"bypass -h admin_move_to "+chest.getX()+" "+chest.getY()+" "+chest.getZ()+"\"> "+ hint +"</a>" : hint) + "</td>";
			html += "</tr></table>";
			someChest = true;
			x++;
		}
		if (!someChest)
			html += "<table width=280><tr><td align=center><font color=LEVEL>There are no chests at this moment!</font></a></td></tr></table>";
		html += "</center><br>";
		
		html += "<center><button value=\"Reload!\" width=103 height=24 action=\"bypass -h treasure\" fore=L2UI_CT1.Button_DF_Calculator back=L2UI_CT1.Button_DF_Calculator_Over></center>";
		html += "</body></html>";
		activeChar.sendPacket(new NpcHtmlMessage(0, html));
	}
	
	class HiddenChestsTask implements Runnable
	{
		private int _index;
		private long _startTime;
		
		public HiddenChestsTask(int index, long startTime)
		{
			_index = index;
			_startTime = startTime;
		}
		
		public void setStartTime(long startTime)
		{
			_startTime = startTime;
		}
		
		/**
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			long delay = _startTime - System.currentTimeMillis();
			
			if (delay < 1000 && _specialChestSpawns[_index] != null)
			{
				moveChest(_specialChestSpawns[_index].getNpc(), false);
				ThreadPoolManager.getInstance().scheduleGeneral(this, System.currentTimeMillis() + 3600000L * 6);
			}
			else
				ThreadPoolManager.getInstance().scheduleGeneral(this, delay);
		}
	}
	
	class OpenChestCastFinalizer implements Runnable
	{
		private L2PcInstance _player;
		private L2Npc _chest;
		
		OpenChestCastFinalizer(L2PcInstance player, L2Npc chest)
		{
			_player = player;
			_chest = chest;
		}
		
		public void run()
		{
			if (_player.isCastingNow())
			{
				_player.sendPacket(new MagicSkillLaunched(_player, 11030, 1));
				_player.setIsCastingNow(false);
				
				if (_player.getTarget() == _chest && !_chest.isDead()
						&& Util.checkIfInRange(1000, _player, _chest, true))
				{
					String name = _player.getName();
					if (_player.getActingPlayer() != null)
						name = _player.getActingPlayer().getName();
					Announcements.getInstance().announceToAll(name + " has opened a treasure chest!");
					_chest.reduceCurrentHp(_chest.getMaxHp() + 1, _player, null);
					
					ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
					{
						public void run()
						{
							HiddenChests.getInstance().moveChest(_chest, !_player.isGM());
						}
					}, 5000L);
				}
			}
		}
	}

	public void tryOpenChest(L2PcInstance activeChar, L2Npc npc)
	{
		if (activeChar == null)
			return;
		
		activeChar.stopMove(null, false);
		int castingMillis = 30000;
		activeChar.broadcastPacket(new MagicSkillUse(activeChar, 11030, 1, castingMillis, 0));
		activeChar.sendPacket(new SetupGauge(0, castingMillis));
		activeChar.sendMessage("Opening chest...");
		activeChar.setLastSkillCast(SkillTable.getInstance().getInfo(11030, 1));
		OpenChestCastFinalizer fcf = new OpenChestCastFinalizer(activeChar, npc);
		activeChar.setSkillCast(ThreadPoolManager.getInstance().scheduleEffect(fcf, castingMillis));
		activeChar.forceIsCasting(TimeController.getGameTicks() + castingMillis / TimeController.MILLIS_IN_TICK);
	}
}

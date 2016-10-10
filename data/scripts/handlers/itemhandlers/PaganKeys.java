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

package handlers.itemhandlers;

import l2server.gameserver.handler.IItemHandler;
import l2server.gameserver.model.L2ItemInstance;
import l2server.gameserver.model.L2Object;
import l2server.gameserver.model.actor.L2Playable;
import l2server.gameserver.model.actor.instance.L2DoorInstance;
import l2server.gameserver.model.actor.instance.L2PcInstance;
import l2server.gameserver.network.SystemMessageId;
import l2server.gameserver.network.serverpackets.ActionFailed;
import l2server.gameserver.network.serverpackets.SystemMessage;

/**
 * @author chris
 */
public class PaganKeys implements IItemHandler
{
    public static final int INTERACTION_DISTANCE = 100;

    /**
     * @see l2server.gameserver.handler.IItemHandler#useItem(l2server.gameserver.model.actor.L2Playable, l2server.gameserver.model.L2ItemInstance, boolean)
     */
    @Override
    public void useItem(L2Playable playable, L2ItemInstance item, boolean forceUse)
    {
        if (!(playable instanceof L2PcInstance))
        {
            return;
        }
        L2PcInstance activeChar = (L2PcInstance) playable;
        L2Object target = activeChar.getTarget();

        if (!(target instanceof L2DoorInstance))
        {
            activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.INCORRECT_TARGET));
            activeChar.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }
        L2DoorInstance door = (L2DoorInstance) target;

        if (!activeChar.isInsideRadius(door, INTERACTION_DISTANCE, false, false))
        {
            activeChar.sendMessage("Too far.");
            activeChar.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }
        if (!activeChar.getAbnormalEffect().isEmpty() || activeChar.isInCombat())
        {
            activeChar.sendMessage("You cannot use the key now.");
            activeChar.sendPacket(ActionFailed.STATIC_PACKET);
            return;
        }

        if (!playable.destroyItem("Consume", item.getObjectId(), 1, null, false))
        {
            return;
        }

        door.openMe();
    }
}

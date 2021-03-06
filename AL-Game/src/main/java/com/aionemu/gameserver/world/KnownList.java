/**
 * This file is part of aion-emu <aion-emu.com>.
 *
 *  aion-emu is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  aion-emu is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with aion-emu.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.world;

import java.util.Map;

import com.aionemu.commons.utils.SingletonMap;
import com.aionemu.gameserver.model.gameobjects.AionObject;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.utils.MathUtil;

/**
 * KnownList.
 * 
 * @author -Nemesiss-, kosyachok, lord_rex 
 * 		based on l2j-free engines.
 */
public class KnownList
{
	// how far player will see visible object
	private static final float					visibilityDistance	= 95;

	// maxZvisibleDistance
	private static final float					maxZvisibleDistance	= 95;

	private final VisibleObject					owner;

	private final Map<Integer, VisibleObject>	knownObjects		= new SingletonMap<Integer, VisibleObject>().setShared();

	private long								lastUpdate;

	/**
	 * Constructor.
	 * 
	 * @param owner
	 */
	public KnownList(VisibleObject owner)
	{
		this.owner = owner;
	}

	/**
	 * Owner of this KnownList.
	 */
	public VisibleObject getOwner()
	{
		return owner;
	}

	/**
	 * List of objects that this KnownList owner known
	 */
	public final Map<Integer, VisibleObject> getKnownObjects()
	{
		return knownObjects;
	}

	/**
	 * Do KnownList update.
	 */
	public synchronized final void updateKnownList()
	{
		if((System.currentTimeMillis() - lastUpdate) < 100 || !owner.getActiveRegion().isActive())
			return;

		updateKnownListImpl();

		lastUpdate = System.currentTimeMillis();
	}

	protected void updateKnownListImpl()
	{
		forgetVisibleObjects();
		findVisibleObjects();
	}

	/**
	 * Clear known list. Used when object is despawned.
	 */
	public final void clearKnownList()
	{
		for(VisibleObject object : getKnownObjects().values())
		{
			removeKnownObject(object, false);
			object.getKnownList().removeKnownObject(getOwner(), false);
		}

		getKnownObjects().clear();
	}

	/**
	 * Check if object is known
	 * 
	 * @param object
	 * @return true if object is known
	 */
	public boolean knowns(AionObject object)
	{
		return getKnownObjects().containsKey(object.getObjectId());
	}

	/**
	 * Add VisibleObject to this KnownList. Object is unknown.
	 * 
	 * @param object
	 * @return
	 */
	protected boolean addKnownObject(VisibleObject object)
	{
		if(object == null || object == getOwner())
			return false;

		if(!checkObjectInRange(getOwner(), object))
			return false;

		if(getKnownObjects().put(object.getObjectId(), object) != null)
			return false;

		getOwner().getController().see(object);

		return true;
	}

	/**
	 * Remove VisibleObject from this KnownList. Object was known.
	 * 
	 * @param object
	 * @return
	 */
	private final boolean removeKnownObject(VisibleObject object, boolean isOutOfRange)
	{
		if(object == null)
			return false;

		if(getKnownObjects().remove(object.getObjectId()) == null)
			return false;

		getOwner().getController().notSee(object, isOutOfRange);

		return true;
	}

	private final void forgetVisibleObjects()
	{
		for(VisibleObject object : getKnownObjects().values())
		{
			forgetVisibleObject(object);
			object.getKnownList().forgetVisibleObject(getOwner());
		}
	}

	private final boolean forgetVisibleObject(VisibleObject object)
	{
		if(checkObjectInRange(getOwner(), object))
			return false;

		return removeKnownObject(object, true);
	}

	/**
	 * Find objects that are in visibility range.
	 */
	protected void findVisibleObjects()
	{
		if(getOwner() == null || !getOwner().isSpawned())
			return;

		for(MapRegion region : getOwner().getActiveRegion().getNeighbours())
		{
			for(VisibleObject object : region.getVisibleObjects().values())
			{
				addKnownObject(object);
				object.getKnownList().addKnownObject(getOwner());
			}
		}
	}

	protected final boolean checkObjectInRange(VisibleObject owner, VisibleObject newObject)
	{
		// check if Z distance is greater than maxZvisibleDistance
		if(Math.abs(owner.getZ() - newObject.getZ()) > maxZvisibleDistance)
			return false;

		return MathUtil.isInRange(owner, newObject, visibilityDistance);
	}
}

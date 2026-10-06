package dev.rm20.thankmasvault.utils

import com.hypixel.hytale.builtin.points.PointsPlugin
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

fun Store<EntityStore>.getPoint(pointId: String) =
    this.getResource(PointsPlugin.get().managerResourceType).getPoint(pointId)
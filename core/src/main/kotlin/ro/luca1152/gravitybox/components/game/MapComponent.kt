/*
 * This file is part of Gravity Box.
 *
 * Gravity Box is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Gravity Box is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Gravity Box.  If not, see <https://www.gnu.org/licenses/>.
 */

package ro.luca1152.gravitybox.components.game

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonWriter
import com.badlogic.gdx.utils.Pool.Poolable
import com.badlogic.gdx.utils.TimeUtils
import ktx.inject.Context
import ro.luca1152.gravitybox.components.ComponentResolver
import ro.luca1152.gravitybox.components.editor.*
import ro.luca1152.gravitybox.entities.editor.DashedLineEntity
import ro.luca1152.gravitybox.entities.editor.MovingMockPlatformEntity
import ro.luca1152.gravitybox.entities.game.CollectiblePointEntity
import ro.luca1152.gravitybox.entities.game.PlatformEntity
import ro.luca1152.gravitybox.entities.game.TextEntity
import ro.luca1152.gravitybox.events.EventQueue
import ro.luca1152.gravitybox.events.Events
import ro.luca1152.gravitybox.screens.LevelEditorScreen
import ro.luca1152.gravitybox.utils.assets.json.*
import ro.luca1152.gravitybox.utils.assets.loaders.Text
import ro.luca1152.gravitybox.utils.kotlin.createComponent
import ro.luca1152.gravitybox.utils.kotlin.removeAndResetEntity
import ro.luca1152.gravitybox.utils.kotlin.tryGet
import ro.luca1152.gravitybox.utils.ui.Colors
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

/** Pixels per meter. */
const val PPM = 64f

val Int.pixelsToMeters: Float
    get() = this / PPM

val Float.pixelsToMeters: Float
    get() = this / PPM

val Float.metersToPixels: Float
    get() = this * PPM

@Suppress("PrivatePropertyName")
/** Contains map information. */
class MapComponent : Component, Poolable {
    companion object : ComponentResolver<MapComponent>(MapComponent::class.java) {
        const val GRAVITY = -25f
    }

    // Injected objects
    private lateinit var engine: PooledEngine
    private lateinit var manager: AssetManager
    private lateinit var world: World
    private lateinit var eventQueue: EventQueue

    var levelId = 1
    var hue = 180

    var pointsCount = 0
    var collectedPointsCount = 0

    var mapLeft = Float.POSITIVE_INFINITY
    var mapRight = Float.NEGATIVE_INFINITY
    var mapBottom = Float.POSITIVE_INFINITY
    var mapTop = Float.NEGATIVE_INFINITY

    var forceCenterCameraOnPlayer = false

    var paddingLeft = 2f
    var paddingRight = 2f
    var paddingTop = 5f
    var paddingBottom = 5f

    fun set(context: Context, levelId: Int, hue: Int) {
        this.levelId = levelId
        this.hue = hue
        injectObjects(context)
    }

    private fun injectObjects(context: Context) {
        engine = context.inject()
        manager = context.inject()
        world = context.inject()
        eventQueue = context.inject()
    }

    fun updateMapBounds() {
        mapLeft = Float.POSITIVE_INFINITY
        mapRight = Float.NEGATIVE_INFINITY
        mapBottom = Float.POSITIVE_INFINITY
        mapTop = Float.NEGATIVE_INFINITY
        engine.getEntitiesFor(Family.all(PolygonComponent::class.java).get()).forEach {
            if ((it.tryGet(EditorObjectComponent) == null || !it.editorObject.isDeleted) && !it.isScheduledForRemoval) {
                it.polygon.run {
                    mapLeft = Math.min(mapLeft, leftmostX)
                    mapRight = Math.max(mapRight, rightmostX)
                    mapBottom = Math.min(mapBottom, bottommostY)
                    mapTop = Math.max(mapTop, topmostY)
                }
            }
        }
    }

    /** @param forceSave If true, it will override any previous map, even if the new file's content is the same. */
    fun saveMap(forceSave: Boolean = false) {
        val json = getJsonFromMap()
        writeJsonToFile(json, forceSave)
    }

    private fun getJsonFromMap(): Json {
        var player: Entity? = null
        var finishPoint: Entity? = null
        val objects = Array<Entity>()
        engine.getEntitiesFor(Family.all(MapObjectComponent::class.java).get()).forEach {
            if (it.tryGet(EditorObjectComponent) == null || !it.editorObject.isDeleted) {
                when {
                    it.tryGet(PlayerComponent) != null -> {
                        check(player == null) { "A map can't have more than one player." }
                        player = it
                    }
                    it.tryGet(FinishComponent) != null -> {
                        check(finishPoint == null) { " A map can't have more than one finish point." }
                        finishPoint = it
                    }
                    it.tryGet(PlatformComponent) != null || it.tryGet(DestroyablePlatformComponent) != null ||
                            it.tryGet(CollectiblePointComponent) != null || it.tryGet(TextComponent) != null -> {
                        objects.add(it)
                    }
                }
            }
        }
        check(player != null) { "A map must have a player." }
        check(finishPoint != null) { "A map must have a finish point." }

        return Json().apply {
            setOutputType(JsonWriter.OutputType.json)
            setWriter(JsonWriter(StringWriter()))
            writeObjectStart()

            // Map properties
            writeValue("id", levelId)
            writeValue("hue", hue)
            writeObjectStart("padding")
            writeValue("left", paddingLeft)
            writeValue("right", paddingRight)
            writeValue("top", paddingTop)
            writeValue("bottom", paddingBottom)
            writeObjectEnd()

            // Objects
            player!!.json.writeToJson(this)
            finishPoint!!.json.writeToJson(this)
            writeArrayStart("objects")
            objects.forEach {
                it.json.writeToJson(this)
            }
            writeArrayEnd()

            writeObjectEnd()
        }
    }

    private fun writeJsonToFile(json: Json, forceSave: Boolean) {
        val fileFolder = "maps/editor"
        val existentFileName = getMapFileNameForId(levelId)
        if (existentFileName != "") {
            val oldJson = Gdx.files.local("$fileFolder/$existentFileName").readString()
            if (!forceSave && oldJson == json.prettyPrint(json.writer.writer.toString())) {
                return
            } else {
                Gdx.files.local("$fileFolder/$existentFileName").delete()
            }
        }
        val fileHandle = Gdx.files.local("$fileFolder/${getNewFileName()}")
        fileHandle.writeString(json.prettyPrint(json.writer.writer.toString()), false)
    }

    fun loadMap(
        context: Context,
        mapFactory: MapFactory,
        playerEntity: Entity,
        finishEntity: Entity,
        isLevelEditor: Boolean = false
    ) {
        resetPoints()
        destroyAllBodies()
        removeObjects()
        createMap(mapFactory.id, mapFactory.hue, mapFactory.padding)
        createPlayer(mapFactory.player, playerEntity)
        createFinish(mapFactory.finish, finishEntity)
        createObjects(context, mapFactory.objects, isLevelEditor)
        updateMapBounds()
        if (isLevelEditor) {
            makeObjectsTransparent()
        }
        eventQueue.add(Events.UPDATE_ROUNDED_PLATFORMS)
    }

    private fun resetPoints() {
        collectedPointsCount = 0
        pointsCount = 0
    }

    private fun makeObjectsTransparent() {
        engine.getEntitiesFor(Family.all(EditorObjectComponent::class.java).get()).forEach {
            it.scene2D.color.a = LevelEditorScreen.OBJECTS_COLOR_ALPHA
        }
    }

    private fun removeObjects() {
        val entitiesToRemove = Array<Entity>()
        engine.getEntitiesFor(
            Family.one(
                PlatformComponent::class.java,
                CombinedBodyComponent::class.java,
                DestroyablePlatformComponent::class.java,
                RotatingObjectComponent::class.java,
                ExplosionComponent::class.java,
                RotatingIndicatorComponent::class.java,
                DashedLineComponent::class.java,
                MockMapObjectComponent::class.java,
                TextComponent::class.java,
                BulletComponent::class.java,
                CollectiblePointComponent::class.java,
                ExtendedTouchComponent::class.java
            ).get()
        ).forEach {
            entitiesToRemove.add(it)
        }
        entitiesToRemove.forEach {
            engine.removeAndResetEntity(it)
        }
    }

    private fun createMap(id: Int, hue: Int, padding: PaddingPrototype) {
        levelId = id

        this.hue = hue
        Colors.hue = hue
        Colors.LightTheme.resetAllColors(Colors.hue)
        Colors.DarkTheme.resetAllColors(Colors.hue)

        paddingLeft = padding.left.toFloat()
        paddingRight = padding.right.toFloat()
        paddingTop = padding.top.toFloat()
        paddingBottom = padding.bottom.toFloat()
    }

    private fun createPlayer(player: PlayerPrototype, playerEntity: Entity) {
        playerEntity.run {
            scene2D.run {
                centerX = player.position.x.pixelsToMeters
                centerY = player.position.y.pixelsToMeters
                rotation = player.rotation.toFloat()
            }
        }
    }

    private fun createFinish(finish: FinishPrototype, finishEntity: Entity) {
        finishEntity.run {
            scene2D.run {
                centerX = finish.position.x.pixelsToMeters
                centerY = finish.position.y.pixelsToMeters
                rotation = finish.rotation.toFloat()
            }
        }
    }

    private fun createObjects(
        context: Context,
        objects: ArrayList<ObjectPrototype>,
        isLevelEditor: Boolean
    ) {
        objects.forEach {
            when {
                it.type == "platform" -> createPlatform(context, it, isLevelEditor)
                it.type == "point" -> createPoint(context, it)
                it.type == "text" -> createText(context, it)
            }
        }
    }

    private fun createPlatform(context: Context, platform: ObjectPrototype, isLevelEditor: Boolean) {
        val newPlatform = PlatformEntity.createEntity(
            context,
            platform.position.x.pixelsToMeters,
            platform.position.y.pixelsToMeters,
            platform.width.pixelsToMeters,
            rotation = platform.rotation.toFloat(),
            isDestroyable = platform.isDestroyable,
            isRotating = platform.isRotating,
            targetX = platform.movingTo.x.pixelsToMeters,
            targetY = platform.movingTo.y.pixelsToMeters
        )
        if (isLevelEditor && platform.movingTo.x != Float.POSITIVE_INFINITY && platform.movingTo.y != Float.POSITIVE_INFINITY) {
            val mockPlatform = MovingMockPlatformEntity.createEntity(
                context, newPlatform,
                newPlatform.scene2D.centerX + 1f, newPlatform.scene2D.centerY + 1f,
                newPlatform.scene2D.width, newPlatform.scene2D.rotation
            )
            newPlatform.linkedEntity(context, "mockPlatform", mockPlatform)
            newPlatform.movingObject(context, mockPlatform.scene2D.centerX, mockPlatform.scene2D.centerY)

            val dashedLine = DashedLineEntity.createEntity(context, newPlatform, mockPlatform)
            mockPlatform.linkedEntity.add("dashedLine", dashedLine)
            newPlatform.linkedEntity.add("dashedLine", dashedLine)
        }
        if (isLevelEditor && platform.isRotating) {
            newPlatform.rotatingIndicator(context)
        }
    }

    private fun createPoint(context: Context, point: ObjectPrototype) {
        pointsCount++
        CollectiblePointEntity.createEntity(
            context,
            point.position.x.pixelsToMeters,
            point.position.y.pixelsToMeters,
            point.rotation.toFloat()
        )
    }

    private fun createText(context: Context, text: ObjectPrototype) {
        TextEntity.createEntity(
            context,
            text.string,
            text.position.x,
            text.position.y
        )
    }

    private fun getMapFileNameForId(mapId: Int): String {
        Gdx.files.local("maps/editor").list().forEach {
            val jsonData = if (manager.isLoaded(it.path())) {
                manager.get<Text>(it.path()).string
            } else {
                Gdx.files.local(it.path()).readString()
            }
            val mapFactory = Json().fromJson(MapFactory::class.java, jsonData)
            if (mapFactory.id == mapId)
                return it.name()
        }
        return ""
    }

    private fun getNewFileName(): String {
        val date = Date(TimeUtils.millis())
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z'.json'", Locale.getDefault())
        return formatter.format(date)
    }

    override fun reset() {
        destroyAllBodies()
        levelId = 1
        hue = 180
        pointsCount = 0
        collectedPointsCount = 0
        mapLeft = Float.POSITIVE_INFINITY
        mapRight = Float.NEGATIVE_INFINITY
        mapBottom = Float.POSITIVE_INFINITY
        mapTop = Float.NEGATIVE_INFINITY
        paddingLeft = 2f
        paddingRight = 2f
        paddingTop = 5f
        paddingBottom = 5f
        forceCenterCameraOnPlayer = false
    }

    fun destroyAllBodies() {
        val bodiesToRemove = Array<Body>()
        world.getBodies(bodiesToRemove)
        bodiesToRemove.forEach {
            world.destroyBody(it)
        }
    }
}

val Entity.map: MapComponent
    get() = MapComponent[this]

fun Entity.map(context: Context, levelId: Int, hue: Int) =
    add(createComponent<MapComponent>(context).apply {
        set(context, levelId, hue)
    })!!
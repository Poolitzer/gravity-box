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

package ro.luca1152.gravitybox.components.editor

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.Pool.Poolable
import ro.luca1152.gravitybox.components.ComponentResolver
import ro.luca1152.gravitybox.components.game.*
import ro.luca1152.gravitybox.engine
import ro.luca1152.gravitybox.utils.kotlin.tryGet

/** Translates the entity's information to JSON. */
class JsonComponent : Component, Poolable {
    private var parentEntity: Entity? = null
    private var jsonObjectName = ""
    private var jsonWillBeInArray = false

    fun setObject(parentEntity: Entity, jsonObjectName: String = "") {
        this.parentEntity = parentEntity
        this.jsonObjectName = jsonObjectName
    }

    fun setArrayObject(parentEntity: Entity) {
        jsonWillBeInArray = true
        setObject(parentEntity, "")
    }

    fun writeToJson(json: Json) {
        if (jsonWillBeInArray) {
            json.writeObjectStart()
        } else {
            json.writeObjectStart(jsonObjectName)
        }
        parentEntity.run {
            this?.let {
                if (tryGet(PlatformComponent) != null) json.run {
                    writeValue("type", "platform")
                }
                if (tryGet(MapObjectComponent) != null) json.run {
                    writeValue("id", mapObject.id)
                }
                if (tryGet(ImageComponent) != null) json.run {
                    writeObjectStart("position")
                    writeValue("x", image.centerX.metersToPixels)
                    writeValue("y", image.centerY.metersToPixels)
                    writeObjectEnd()

                    if (tryGet(PlatformComponent) != null) json.run {
                        writeValue("width", image.width.metersToPixels)
                    }
                    writeValue("rotation", image.img.rotation.toInt())
                }
            }
        }
        json.writeObjectEnd()
    }

    override fun reset() {
        jsonObjectName = ""
        jsonWillBeInArray = false
        parentEntity = null
    }

    companion object : ComponentResolver<JsonComponent>(JsonComponent::class.java)
}

val Entity.json: JsonComponent
    get() = JsonComponent[this]

fun Entity.json() =
    add(engine.createComponent(JsonComponent::class.java))!!

fun Entity.json(parentEntity: Entity, jsonObjectName: String) =
    add(engine.createComponent(JsonComponent::class.java).apply {
        setObject(parentEntity, jsonObjectName)
    })!!

fun Entity.json(parentEntity: Entity) =
    add(engine.createComponent(JsonComponent::class.java).apply {
        setArrayObject(parentEntity)
    })!!
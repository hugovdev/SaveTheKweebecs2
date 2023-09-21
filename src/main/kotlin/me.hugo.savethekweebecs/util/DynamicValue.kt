package me.hugo.savethekweebecs.util

class DynamicValue<T>(value: T, val onChange: MutableList<() -> Unit> = mutableListOf()) {

    var value: T = value
        set(newValue) {
            field = newValue
            onChange.toMutableList().forEach { it.invoke() }
        }

}
package com.jht.interfaces



/** Common interfacing for an interfaces model class */
interface BaseModel : AutoCloseable {
    // Keep to interface rather than class? Is there any common functionality that would require abstract class instead of interface?

    /** Stops, clears, or cleans up anything that was started or allocated within the model object. Should not throw exceptions. */
    override fun close()
}

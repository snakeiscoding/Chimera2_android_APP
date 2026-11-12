package com.jht.sprint8.sprint8immersive.components

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@Composable
internal fun Sprint8NewForwardMotionCompGL(
    modifier: Modifier,
    sprint8NewColor: Color,
    numLanes: Int,
    runningSpeed: Long,
    isRunning: Boolean
) {
    val roadWidth = 0.5f
    val roadHeight = 0.99f
    AndroidView(
        modifier = modifier.clipToBounds(), // on older consoles the opengl can draw over rest of the window if we do not clip it
        factory = { context ->
            Sprint8GLSurfaceView(context).apply {
                renderer.apply {
                    setColor(sprint8NewColor)
                    this.numLanes = numLanes
                    this.roadWidth = roadWidth
                    this.roadHeight = roadHeight
                    this.runningSpeed = runningSpeed.toFloat() * 80f // tuned to match Compose speed
                    this.isRunning = isRunning
                }
            }
        },
        update = { view ->
            view.renderer.apply {
                setColor(sprint8NewColor)
                this.numLanes = numLanes
                this.roadWidth = roadWidth
                this.roadHeight = roadHeight
                this.runningSpeed = runningSpeed.toFloat() * 80f
                this.isRunning = isRunning
            }

            view.renderMode = if (isRunning) {
                GLSurfaceView.RENDERMODE_CONTINUOUSLY
            } else {
                GLSurfaceView.RENDERMODE_WHEN_DIRTY
            }
        }
    )
}

private class Sprint8GLSurfaceView(context: Context) : GLSurfaceView(context) {
    val renderer = RoadRenderer()

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}

private class RoadRenderer : GLSurfaceView.Renderer {

    var numLanes: Int = 6
    var roadWidth: Float = 0.4f
    var roadHeight: Float = 0.8f
    var runningSpeed: Float = 1f
    var isRunning: Boolean = true

    private var program = 0
    private var time = 0f

    private var uTime = 0
    private var uSpeed = 0
    private var uWidth = 0
    private var uHeight = 0
    private var uLanes = 0
    private var uColor = 0

    private val color = floatArrayOf(1f, 0.9f, 0.2f, 1f)

    fun setColor(c: Color) {
        color[0] = c.red
        color[1] = c.green
        color[2] = c.blue
        color[3] = c.alpha
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)

        uTime = GLES20.glGetUniformLocation(program, "u_time")
        uSpeed = GLES20.glGetUniformLocation(program, "u_speed")
        uWidth = GLES20.glGetUniformLocation(program, "u_roadWidth")
        uHeight = GLES20.glGetUniformLocation(program, "u_roadHeight")
        uLanes = GLES20.glGetUniformLocation(program, "u_laneCount")
        uColor = GLES20.glGetUniformLocation(program, "u_color")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        if (isRunning) time += 0.016f // smooth, frame-rate independent

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)

        GLES20.glUniform1f(uTime, time)
        GLES20.glUniform1f(uSpeed, runningSpeed * 0.001f)
        GLES20.glUniform1f(uWidth, roadWidth)
        GLES20.glUniform1f(uHeight, roadHeight)
        GLES20.glUniform1f(uLanes, numLanes.toFloat())
        GLES20.glUniform4f(uColor, color[0], color[1], color[2], color[3])

        drawQuad(program)
    }
}

private fun drawQuad(program: Int) {
    val verts = floatArrayOf(
        -1f, -1f,
        1f, -1f,
        -1f,  1f,
        1f,  1f
    )

    val buffer = java.nio.ByteBuffer.allocateDirect(verts.size * 4)
        .order(java.nio.ByteOrder.nativeOrder())
        .asFloatBuffer().apply {
            put(verts)
            position(0)
        }

    val pos = GLES20.glGetAttribLocation(program, "a_position")
    GLES20.glEnableVertexAttribArray(pos)
    GLES20.glVertexAttribPointer(pos, 2, GLES20.GL_FLOAT, false, 0, buffer)
    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
    GLES20.glDisableVertexAttribArray(pos)
}

private fun loadShader(type: Int, code: String): Int {
    return GLES20.glCreateShader(type).also {
        GLES20.glShaderSource(it, code)
        GLES20.glCompileShader(it)
    }
}

private fun createProgram(v: String, f: String): Int {
    val vs = loadShader(GLES20.GL_VERTEX_SHADER, v)
    val fs = loadShader(GLES20.GL_FRAGMENT_SHADER, f)

    return GLES20.glCreateProgram().also {
        GLES20.glAttachShader(it, vs)
        GLES20.glAttachShader(it, fs)
        GLES20.glLinkProgram(it)
    }
}

private const val VERTEX_SHADER = """
attribute vec2 a_position;
varying vec2 v_uv;
void main() {
    v_uv = (a_position + 1.0) * 0.5;
    gl_Position = vec4(a_position, 0.0, 1.0);
}
"""

private const val FRAGMENT_SHADER = """
precision highp float;

varying vec2 v_uv;

uniform highp float u_time;
uniform highp float u_speed;
uniform highp float u_roadWidth;
uniform highp float u_roadHeight;
uniform highp float u_laneCount;
uniform vec4 u_color;

// Bézier helper
vec2 bezier(vec2 p0, vec2 p1, vec2 p2, float t) {
    float u = 1.0 - t;
    return (u*u)*p0 + (2.0*u*t)*p1 + (t*t)*p2;
}

void main() {
    float roadTop = 1.0 - u_roadHeight;
    if (v_uv.y < roadTop) { gl_FragColor = vec4(0.0); return; }

    float localY = (v_uv.y - roadTop) / u_roadHeight;
    float cx = 0.5;

    float topOffset = 0.03;
    vec2 topL = vec2(cx - topOffset, roadTop);
    vec2 topR = vec2(cx + topOffset, roadTop);
    vec2 botL = vec2(cx - u_roadWidth, 1.0);
    vec2 botR = vec2(cx + u_roadWidth, 1.0);

    float cY = roadTop + u_roadHeight * 0.6;
    vec2 cL = vec2(cx - (u_roadWidth * 0.2), cY);
    vec2 cR = vec2(cx + (u_roadWidth * 0.2), cY);

    vec2 leftEdge = bezier(botL, cL, topL, localY);
    vec2 rightEdge = bezier(botR, cR, topR, localY);

    if (v_uv.x < leftEdge.x || v_uv.x > rightEdge.x) { gl_FragColor = vec4(0.0); return; }

    vec3 roadColor = vec3(0.0);

    // Road edges with gradient
    float edgeWidth = 0.004;
    if (abs(v_uv.x - leftEdge.x) < edgeWidth || abs(v_uv.x - rightEdge.x) < edgeWidth) {
        float t = 1.0 - (v_uv.y - roadTop) / u_roadHeight;
        roadColor = mix(u_color.rgb * 0.0, u_color.rgb, t);
    }

    // Lane markers with reversed global vertical gradient
    float laneHeight = u_roadHeight / u_laneCount;
    float markerFraction = 0.08; // short like Compose
    float offset = mod(u_time * u_speed, u_roadHeight);
    float relativeY = localY * u_roadHeight + offset;

    float markerPhase = mod(relativeY, laneHeight);

    vec3 finalColor = roadColor;

    if(markerPhase < laneHeight * markerFraction) {
        float t = 1.0 - (v_uv.y - roadTop) / u_roadHeight;
        vec3 markerColor = mix(u_color.rgb * 0.0, u_color.rgb, t);

        // Narrower marker width
        float roadWidthAtY = (rightEdge.x - leftEdge.x) * 0.47; // think this needs to be less than 0.5 or it will hit the edges
        if(abs(v_uv.x - cx) < roadWidthAtY) {
            // Smooth fade-in/out
            float markerPhaseNorm = markerPhase / (laneHeight * markerFraction);
            float alpha = smoothstep(0.0, 1.0, 1.0 - markerPhaseNorm);
            finalColor = mix(finalColor, markerColor, alpha);
        }
    }

    gl_FragColor = vec4(finalColor, 1.0);
}
"""

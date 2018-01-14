package ms.domwillia.jvmemory.visualisation

import java.awt.*
import java.awt.image.BufferStrategy
import java.awt.image.BufferedImage
import javax.swing.JFrame
import javax.swing.JPanel

class SwingTest(private val dims: Pair<Int, Int>) : JPanel() {

    private lateinit var bi: BufferedImage
    private lateinit var buffer: BufferStrategy
    private val canvas: Canvas

    private val balls: Array<Ball>

    init {
        this.size = Dimension(dims.first, dims.second)
        layout = GridLayout()

        canvas = Canvas()
        canvas.ignoreRepaint = true
        canvas.size = size
        add(canvas)

        balls = arrayOf(
                Ball(1, Color(40, 123, 222), dims.first / 2, dims.second, 0),
                Ball(-1, Color(200, 100, 150), dims.first / 2, dims.second, dims.first / 2)
        )
    }

    private fun init() {
        canvas.createBufferStrategy(2)
        buffer = canvas.bufferStrategy

        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val gd = ge.defaultScreenDevice
        val gc = gd.defaultConfiguration
        bi = gc.createCompatibleImage(dims.first, dims.second)
    }

    private fun render(fps: Int) {
        var g2d: Graphics2D? = null
        var g: Graphics? = null
        try {
            g2d = bi.createGraphics()
            g2d.addRenderingHints(mutableMapOf(
                    RenderingHints.KEY_ANTIALIASING to RenderingHints.VALUE_ANTIALIAS_ON
            ))

            balls.forEach { it.render(g2d) }
            g2d.color = Color.BLACK
            g2d.drawString("${fps}FPS", 20, 20)

            g = buffer.drawGraphics
            g.drawImage(bi, 0, 0, null)
            if (!buffer.contentsLost())
                buffer.show()

            Thread.yield()
        } finally {
            g?.apply { dispose() }
            g2d?.apply { dispose() }
        }


    }

    private class Ball(
            val direction: Int,
            val bg: Color,
            val width: Int,
            val height: Int,
            val xOffset: Int
    ) {
        private val ballSize = 30
        private val ballColour = Color(35, 30, 30)
        private var ballY = height / 2

        fun render(g: Graphics2D) {
            g.color = bg
            g.fillRect(xOffset, 0, width, height)

            // move ball
            ballY += direction
            if (ballY >= height) ballY = 0
            else if (ballY <= 0) ballY = height

            g.color = ballColour
            g.fillOval(xOffset + width / 2, ballY, ballSize, ballSize)
        }
    }

    fun go() {
        val frame = JFrame()
        frame.ignoreRepaint = true
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

        frame.add(this)
        frame.pack()
        frame.isVisible = true

        this.init()

        var fps = 0
        var frames = 0
        var totalTime = 0L
        var curTime = System.currentTimeMillis()
        var lastTime: Long

        while (true) {
            lastTime = curTime
            curTime = System.currentTimeMillis()
            totalTime += curTime - lastTime
            if (totalTime > 1000) {
                totalTime -= 1000
                fps = frames
                frames = 0
            }
            ++frames

            render(fps)
        }
    }


}

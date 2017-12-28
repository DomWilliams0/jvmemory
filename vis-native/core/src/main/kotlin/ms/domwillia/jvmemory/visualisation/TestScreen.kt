package ms.domwillia.jvmemory.visualisation

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import ktx.app.KtxScreen
import ktx.app.use
import ms.domwillia.jvmemory.parser.parseLog

class TestScreen : KtxScreen {
    private val batch: SpriteBatch = SpriteBatch()
    private val img: Texture = Texture("badlogic.jpg")

    init {
        parseLog("../../../monitor-agent/jvmemory.log")
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(1f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        batch.use {
            it.draw(img, 0f, 0f)
        }
    }

    override fun dispose() {
        batch.dispose()
        img.dispose()
    }


}

package ms.domwillia.jvmemory.visualisation

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import ktx.app.KtxScreen
import ms.domwillia.jvmemory.parser.parseLog

class TestScreen : KtxScreen {
    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.19f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    }
}

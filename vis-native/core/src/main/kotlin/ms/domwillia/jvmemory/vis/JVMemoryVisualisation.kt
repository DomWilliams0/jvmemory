package ms.domwillia.jvmemory.vis

import ktx.app.KtxGame
import ktx.app.KtxScreen

class JVMemoryVisualisation : KtxGame<KtxScreen>() {

    override fun create() {
        addScreen(TestScreen())
        setScreen<TestScreen>()
    }


}

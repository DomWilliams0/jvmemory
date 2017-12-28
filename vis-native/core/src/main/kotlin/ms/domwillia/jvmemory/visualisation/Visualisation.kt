package ms.domwillia.jvmemory.visualisation

import ktx.app.KtxGame
import ktx.app.KtxScreen

class Visualisation : KtxGame<KtxScreen>() {

    override fun create() {
        addScreen(TestScreen())
        setScreen<TestScreen>()
    }


}

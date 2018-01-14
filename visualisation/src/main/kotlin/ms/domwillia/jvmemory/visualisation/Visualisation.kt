package ms.domwillia.jvmemory.visualisation

object Visualisation {
    @JvmStatic
    fun main(args: Array<String>) {
        val size = Pair(600, 600)
        SwingTest(size).go()
    }
}
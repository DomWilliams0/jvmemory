package ms.domwillia.jvmemory.visualisation

import edu.uci.ics.jung.algorithms.layout.AbstractLayout
import edu.uci.ics.jung.algorithms.layout.FRLayout2
import edu.uci.ics.jung.graph.DirectedSparseGraph
import edu.uci.ics.jung.graph.Graph
import edu.uci.ics.jung.graph.util.Graphs
import edu.uci.ics.jung.visualization.GraphZoomScrollPane
import edu.uci.ics.jung.visualization.VisualizationViewer
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse
import edu.uci.ics.jung.visualization.control.ModalGraphMouse
import ms.domwillia.jvmemory.preprocessor.ObjectID
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.JComponent
import javax.swing.ToolTipManager

typealias Vertex = Long
typealias Edge = HeapEdge

data class HeapEdge(val field: String, val from: ObjectID)

class HeapGraph {

    private val graph: Graph<Vertex, Edge>
    private val nodeTypes = mutableMapOf<ObjectID, String>()

    private var layout: AbstractLayout<Vertex, Edge>
    private val vis: VisualizationViewer<Vertex, Edge>

    private val visContainer: JComponent

    private val timer = Timer()

    init {
        graph = Graphs.synchronizedDirectedGraph(
                DirectedSparseGraph())
//        (graph as ObservableGraph).addGraphEventListener { println("hiya $it") }

        layout = FRLayout2(graph)

        vis = VisualizationViewer(layout)
        vis.renderContext.setVertexLabelTransformer { nodeTypes[it] }
        vis.renderContext.setEdgeLabelTransformer { it?.field }
        vis.setVertexToolTipTransformer { v -> v?.toString() }
        ToolTipManager.sharedInstance().initialDelay = 200


        vis.graphMouse = DefaultModalGraphMouse<Vertex, Edge>().apply {
            setZoomAtMouse(true)
            setMode(ModalGraphMouse.Mode.TRANSFORMING)
        }

        vis.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_Q)
                    System.exit(2)
            }
        })

        visContainer = GraphZoomScrollPane(vis)
        timer.schedule(TickerTask(), 300, 300)
    }

    val guiPanel: JComponent
        get() = visContainer

    inner class TickerTask : TimerTask() {

        private var i = 400L
        override fun run() {
            layout.lock(true)
            val relaxer = vis.model.relaxer
            relaxer.pause()

            val v = i++
            graph.addVertex(v)
            if (v > 400)
                graph.addEdge(Edge("hiya-$v", v - 1), v - 1, v)
            if (v > 401)
                graph.addEdge(Edge("hiya-$v-2", v - 2), v - 2, v)

            layout.initialize()
            relaxer.resume()
            layout.lock(false)
        }
    }

}

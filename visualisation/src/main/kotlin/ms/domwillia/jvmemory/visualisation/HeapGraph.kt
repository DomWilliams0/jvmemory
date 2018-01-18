package ms.domwillia.jvmemory.visualisation

import edu.uci.ics.jung.algorithms.layout.AbstractLayout
import edu.uci.ics.jung.algorithms.layout.SpringLayout
import edu.uci.ics.jung.graph.DirectedSparseGraph
import edu.uci.ics.jung.graph.Graph
import edu.uci.ics.jung.graph.util.EdgeType
import edu.uci.ics.jung.graph.util.Graphs
import edu.uci.ics.jung.visualization.GraphZoomScrollPane
import edu.uci.ics.jung.visualization.VisualizationViewer
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse
import edu.uci.ics.jung.visualization.control.ModalGraphMouse
import ms.domwillia.jvmemory.preprocessor.ObjectID
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.ToolTipManager
import kotlin.system.exitProcess

typealias Vertex = Long
typealias Edge = HeapEdge

data class HeapEdge(val field: String, val from: ObjectID)

class HeapGraph : GUIPanel {
    private val graph: Graph<Vertex, Edge>
    private val nodeTypes = mutableMapOf<ObjectID, String>()

    private var layout: AbstractLayout<Vertex, Edge>
    private val vis: VisualizationViewer<Vertex, Edge>

    private val visContainer: JComponent

    init {
        graph = Graphs.synchronizedDirectedGraph(
                DirectedSparseGraph())

        layout = SpringLayout(graph) { 100 }

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
                    exitProcess(2)
            }
        })
        vis.model.relaxer.setSleepTime(50)

        visContainer = GraphZoomScrollPane(vis)
    }

    override fun getGUIPanel(): JComponent = visContainer

    private fun modifyGraph(func: () -> Unit) {
//        layout.lock(true)
        val relaxer = vis.model.relaxer
        relaxer.pause()

        func()

        layout.initialize()
        relaxer.resume()
//        layout.lock(false)
    }

    fun addVertex(id: ObjectID, type: String) = modifyGraph {
        graph.addVertex(id)
        nodeTypes[id] = type
    }

    fun deleteVertex(id: ObjectID) = modifyGraph {
        graph.removeVertex(id)
        nodeTypes.remove(id)
    }

    fun setLink(src: ObjectID, dst: ObjectID, name: String) {
        val edge = Edge(name, src)
        graph.removeEdge(edge)

        if (dst > 0L) {
            modifyGraph {
                graph.addEdge(edge, src, dst, EdgeType.DIRECTED)
            }
        }
    }
}

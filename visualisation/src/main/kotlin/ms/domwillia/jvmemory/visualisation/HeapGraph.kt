package ms.domwillia.jvmemory.visualisation

import edu.uci.ics.jung.algorithms.layout.AbstractLayout
import edu.uci.ics.jung.algorithms.layout.SpringLayout
import edu.uci.ics.jung.graph.DirectedSparseGraph
import edu.uci.ics.jung.graph.Graph
import edu.uci.ics.jung.graph.util.EdgeType
import edu.uci.ics.jung.graph.util.Graphs
import edu.uci.ics.jung.visualization.VisualizationViewer
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse
import edu.uci.ics.jung.visualization.control.ModalGraphMouse
import javafx.embed.swing.SwingNode
import javafx.scene.Node
import ms.domwillia.jvmemory.preprocessor.ObjectID
import ms.domwillia.jvmemory.protobuf.Definitions
import java.awt.Color
import java.awt.Dimension
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.ToolTipManager
import kotlin.collections.ArrayList
import kotlin.system.exitProcess

typealias Vertex = Long
typealias Edge = HeapEdge

data class HeapEdge(val field: String, val from: ObjectID)

class HeapGraph(
        classDefinitions: ArrayList<Definitions.ClassDefinition>,
        width: Double,
        height: Double
) : GUIPanel {
    private val graph: Graph<Vertex, Edge>
    private val nodeTypes = mutableMapOf<ObjectID, String>()

    private var layout: AbstractLayout<Vertex, Edge>
    private val vis: VisualizationViewer<Vertex, Edge>

    private val visNode: SwingNode

    private val classColours: Map<String, Color> =
            classDefinitions.associate { it.name to generatePersistentRandomColour(it) }

    init {
        graph = Graphs.synchronizedDirectedGraph(
                DirectedSparseGraph())

        layout = SpringLayout(graph) { 100 }

        vis = VisualizationViewer(layout, Dimension(width.toInt(), height.toInt()))

//        vis.renderContext.setVertexLabelTransformer { nodeTypes[it] }
        vis.renderContext.setEdgeLabelTransformer { it?.field }
        vis.setVertexToolTipTransformer { nodeTypes[it] }
        ToolTipManager.sharedInstance().initialDelay = 200

        vis.renderContext.setVertexFillPaintTransformer {
            classColours[nodeTypes[it]] ?: Color.GRAY
        }

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

        visNode = SwingNode().apply {
//            content = GraphZoomScrollPane(vis)
            content = vis
        }
    }

    override val guiPanel: Node
        get() = visNode

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

    companion object {
        private val random = Random()
        private fun generatePersistentRandomColour(clazz: Definitions.ClassDefinition): Color {
            println("clazz: $clazz")
            val hash = Objects.hash(clazz.name)
            random.setSeed(hash.toLong())
            val hue = random.nextFloat()
            return Color.getHSBColor(hue, 0.7f, 0.7f)
        }
    }
}

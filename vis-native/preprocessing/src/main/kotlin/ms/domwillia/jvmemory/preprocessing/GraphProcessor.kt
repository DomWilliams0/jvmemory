package ms.domwillia.jvmemory.preprocessing

import edu.uci.ics.jung.graph.DirectedSparseGraph
import edu.uci.ics.jung.graph.ObservableGraph
import edu.uci.ics.jung.graph.util.EdgeType
import edu.uci.ics.jung.visualization.BasicVisualizationServer
import ms.domwillia.jvmemory.protobuf.Access
import ms.domwillia.jvmemory.protobuf.Allocations
import java.awt.Color
import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.SwingUtilities

class GraphProcessor(threadId: Long) : Processor(threadId) {

    data class Edge(val field: String, val from: Long)

    private val graph = DirectedSparseGraph<Long, Edge>()
    private val nodeTypes = mutableMapOf<Long, String>()

    override fun allocate(message: Allocations.Allocation) {
        if (graph.addVertex(message.id)) {
            nodeTypes[message.id] = message.type
        }

        // TODO recalculate layout
    }

    override fun putField(message: Access.PutField) {
        if (message.valueId == 0L) return

        graph.getOutEdges(message.id).find { it.field == message.field }?.let {
            graph.removeEdge(it)
            println("Removing old ${message.id} ${message.field}")
        }

        graph.addEdge(
                Edge(message.field, message.id),
                message.id,
                message.valueId,
                EdgeType.DIRECTED
        )
        println("Adding edge: ${message.id}.${message.field} = ${message.valueId}")
    }

    override fun finish() {
        // TODO render graph

        val layout = edu.uci.ics.jung.algorithms.layout.ISOMLayout(graph)
        val vis = BasicVisualizationServer(
                layout,
                Dimension(600, 600)
        )
        vis.renderContext.setVertexLabelTransformer {
            val type = run {
                val ty = nodeTypes[it]!!
                ty.substring(ty.lastIndexOf('/') + 1, ty.length - 1)
            }
            "$type:$it"
        }
        vis.renderContext.setEdgeLabelTransformer { it?.field }
        vis.renderContext.setVertexFillPaintTransformer {
            when (nodeTypes[it]!!) {
                "Lms/domwillia/specimen/AllocationsTracking;" -> Color.GREEN
                "Lms/domwillia/specimen/AllocationsTracking\$Link;" -> Color.BLUE
                else -> {Color.RED}
            }
        }

        val frame = JFrame()
        frame.size = Dimension(600, 600)
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.add(vis)
        frame.isVisible = true
    }
}

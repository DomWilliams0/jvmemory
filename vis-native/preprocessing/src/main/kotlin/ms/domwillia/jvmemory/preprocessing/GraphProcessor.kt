package ms.domwillia.jvmemory.preprocessing

import edu.uci.ics.jung.graph.DirectedSparseGraph
import edu.uci.ics.jung.graph.util.EdgeType
import ms.domwillia.jvmemory.protobuf.Access
import ms.domwillia.jvmemory.protobuf.Allocations

class GraphProcessor(threadId: Long) : Processor(threadId) {

    data class Edge(val field: String)

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
                Edge(message.field),
                message.id,
                message.valueId,
                EdgeType.DIRECTED
        )
        println("Adding new ${message.id} ${message.field} to ${message.valueId}")
    }

    override fun finish() {
        // TODO render graph
    }
}

package ms.domwillia.jvmemory.server

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.hubspot.jackson.datatype.protobuf.ProtobufModule
import io.javalin.Javalin
import io.javalin.embeddedserver.Location
import io.javalin.embeddedserver.jetty.EmbeddedJettyFactory
import io.javalin.translator.json.JavalinJacksonPlugin
import ms.domwillia.jvmemory.preprocessor.Preprocessor
import org.eclipse.jetty.server.Server
import java.io.File
import java.net.InetSocketAddress
import kotlin.system.exitProcess

var addr = "localhost"
val port = 52933

object Server {
    @JvmStatic
    fun main(args: Array<String>) {
        val paths = args.take(3)
        if (paths.size < 2) {
            System.err.println("Expected args: <input log file> <output log dir> <vis. web src dir")
            exitProcess(1)
        }

        val (inFile, outDir, visSrc) = paths

        val events = Preprocessor.runPreprocessor(File(inFile), File(outDir))

        JavalinJacksonPlugin.configure(ObjectMapper()
                .registerModule(ProtobufModule())
                .setSerializationInclusion(JsonInclude.Include.NON_NULL))

        val app = Javalin.create().apply {
            enableStaticFiles(visSrc, Location.EXTERNAL)
            embeddedServer(EmbeddedJettyFactory({
                Server(InetSocketAddress.createUnresolved(addr, port))
            }))
            enableCorsForAllOrigins()
        }.start()

        app.get("/thread") { ctx ->
            ctx.json(events.threads)
        }

        app.get("/thread/:id") { ctx ->
            ctx.param("id")!!.toLongOrNull()?.let { threadId ->
                val evts = events.getEventsForThread(threadId)
                if (evts.isNotEmpty()) {
                    val out = ctx.response().outputStream
                    evts.forEach { it.writeDelimitedTo(out) }
                    return@get
                }
            }

            ctx.status(404)
        }

        app.get("/definitions") { ctx ->
            ctx.json(events.definitions)
        }
    }
}

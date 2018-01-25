package ms.domwillia.jvmemory.server

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.hubspot.jackson.datatype.protobuf.ProtobufModule
import io.javalin.Javalin
import io.javalin.embeddedserver.Location
import io.javalin.embeddedserver.jetty.EmbeddedJettyFactory
import io.javalin.translator.json.JavalinJacksonPlugin
import ms.domwillia.jvmemory.preprocessor.EventsLoader
import ms.domwillia.jvmemory.preprocessor.Preprocessor
import ms.domwillia.jvmemory.preprocessor.protobuf.Event
import ms.domwillia.jvmemory.protobuf.Definitions
import org.eclipse.jetty.server.Server
import java.io.File
import java.net.InetSocketAddress

var addr = "localhost"
val port = 52933

object Server {
    @JvmStatic
    fun main(args: Array<String>) {
        val useCached = args.getOrNull(0)?.equals("cached") ?: false
        val events = if (!useCached) {
            Preprocessor.runPreprocessor()
        } else {
            EventsLoader(File(Preprocessor.defaultOutputDirPath))
        }

        JavalinJacksonPlugin.configure(ObjectMapper()
                .registerModule(ProtobufModule())
                .setSerializationInclusion(JsonInclude.Include.NON_NULL))

        val app = Javalin.create().apply {
            enableStaticFiles("../visualisation/src", Location.EXTERNAL)
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
                    ctx.json(evts)
                    return@get
                }
            }

            ctx.status(404)
        }

        app.get("/definitions") { ctx ->
            val map = events.definitions.associateBy { it.name }
            ctx.json(map)
        }
    }
}

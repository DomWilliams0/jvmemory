package ms.domwillia.jvmemory.server

import io.javalin.Javalin
import io.javalin.embeddedserver.Location
import io.javalin.embeddedserver.jetty.EmbeddedJettyFactory
import ms.domwillia.jvmemory.preprocessor.EventsLoader
import ms.domwillia.jvmemory.preprocessor.Preprocessor
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

        val app = Javalin.create().apply {
            enableStaticFiles("../visualisation/src", Location.EXTERNAL)
            embeddedServer(EmbeddedJettyFactory({
                Server(InetSocketAddress.createUnresolved(addr, port))
            }))
        }.start()

        app.get("/thread") { ctx ->
            ctx.result(events.threads.toString())
        }

        app.get("/thread/:id") { ctx ->
            ctx.param("id")!!.toLongOrNull()?.let { threadId ->
                events.getRawEventsForThread(threadId)?.let { stream ->
                    ctx.result(stream)
                    return@get
                }
            }

            ctx.status(404)
        }

        app.get("/definitions") { ctx ->
            events.rawDefinitions?.let { defs ->
                ctx.result(defs)
            } ?: ctx.status(404)
        }
    }
}

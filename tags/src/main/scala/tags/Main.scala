package tags

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import neotypes.GraphDatabase
import org.neo4j.driver.v1.AuthTokens
import service.MyRejectionHandler.myRejectionHandler
import service.TagsService

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.io.StdIn

object Main {
  def main(args: Array[String]) {

      implicit val system: ActorSystem = ActorSystem("tags-system")
      implicit val materializer: ActorMaterializer = ActorMaterializer()
      // needed for the future flatMap/onComplete in the end
      implicit val executionContext: ExecutionContextExecutor = system.dispatcher

      val port = Integer.parseInt(Option(System.getenv("PORT")).getOrElse("4013") )

      val url = "bolt://localhost:7687"

      val driver = GraphDatabase.driver[Future](url,
        AuthTokens.basic("neo4j",
          "neo4j"))

    val tagsService = new TagsService(driver)

    Await.ready(tagsService.createConstraints(),Duration(10,TimeUnit.MINUTES))

    val routes = new Routes(tagsService)

      val bindingFuture = Http().bindAndHandle(routes.routes, "localhost", port)

      println(s"Server online at http://localhost:$port/\nPress RETURN to stop...")
      StdIn.readLine() // let it run until user presses return
      bindingFuture
        .flatMap(_.unbind()) // trigger unbinding from the port
        .onComplete(_ => system.terminate()) // and shutdown when done
    }
}

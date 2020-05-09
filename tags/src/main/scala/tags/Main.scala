package tags

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import neotypes.GraphDatabase
import org.neo4j.driver.v1.{AuthTokens, Config}
import service.MyRejectionHandler.myRejectionHandler
import service.TagsService

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.io.StdIn
import scala.util.{Failure, Success, Try}

object Main {
  def main(args: Array[String]) {

    implicit val system: ActorSystem = ActorSystem("tags-system")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    val port = Integer.parseInt(System.getenv().getOrDefault("PORT","4014"))

    val url = System.getenv().getOrDefault("NEO4J_URI", "bolt://localhost:7687")

    val user = System.getenv().getOrDefault("NEO4J_USER", "neo4j")

    val password = System.getenv().getOrDefault("NEO4J_PASSWORD", "neo4j")

    val driver = Try(GraphDatabase.driver[Future](url,
      AuthTokens.basic(user,
        password),
      Config.builder().withoutEncryption().build())) match {
      case Success(s) => s
      case Failure(t) => t.printStackTrace()
        println("Could not create driver, crashing!")
        System.exit(255)
        throw t
    }

    val tagsService = new TagsService(driver)

    Await.ready(tagsService.createConstraints(), Duration(10, TimeUnit.MINUTES))

    val routes = new Routes(tagsService)

    val bindingFuture = Http().bindAndHandle(routes.routes, "0.0.0.0", port)

    println(s"Server online at http://0.0.0.0:$port/\nPress RETURN to stop...")

    var worked = true

    while(worked) {
      Thread.sleep(10 * 1000)
      val t = tagsService.keepAlive().map {
        t =>
          println("t is? " + t)
          true
      }.recover {
        case t: Throwable => t.printStackTrace()
          println("Recover is false!")
          false
      }
      worked = Try(Await.result(t,Duration(30,TimeUnit.SECONDS))) match {
        case Success(false) =>
          println(s"Worked is false")
          false
        case Success(true) =>
          println("Worked is true")
          true
        case Failure(t) =>
          t.printStackTrace()
          println("Worked was really false!")
          false
      }
      println("Working? " + worked)
    }
    println("Failed to contact neo4j, crashing!")
    Try(driver.close)
    bindingFuture.flatMap(_.terminate(Duration(20, TimeUnit.SECONDS)))
    println("Shutting down.")
    System.exit(255)
  }
}

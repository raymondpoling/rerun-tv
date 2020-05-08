package service

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._

object MyRejectionHandler {

  val notFound = """{"status":"not found"}"""

  implicit def myRejectionHandler: RejectionHandler =
    RejectionHandler.newBuilder()
      .handleAll[MethodRejection] { _ =>
        complete((NotFound, notFound))
      }
      .handleNotFound { complete((NotFound, notFound)) }
      .result()
}

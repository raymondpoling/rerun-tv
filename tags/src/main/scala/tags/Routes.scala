package tags

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.GenericMarshallers._
import akka.http.scaladsl.marshalling.PredefinedToResponseMarshallers._
import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.`Content-Type`
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import service.{TagsService, _}
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class Routes(tagsService: TagsService) extends SprayJsonSupport with DefaultJsonProtocol {

    private val params =
        parameters(Symbol("author").as[String], Symbol("tags").as[String].?)

    def makeResponse(code: Int, out: String): HttpResponse = {
        HttpResponse(code, Seq(), HttpEntity(`application/json`, out))
    }

    def compute(author: String, nodeType: NodeType, tags: Option[String], ids: List[String]): Future[HttpResponse] =
        Future.sequence(tags.map(_.split(",").toList).getOrElse(List()).map(tag => Try(tagsService.addTag(
            AddTags(Author(author), Is, ID(ids.last, nodeType), Tags(List(tag))))) match {
            case Success(s) => s
            case Failure(t) => t.printStackTrace()
                throw t
        }))
          .map { _ =>
              val idString = ids.mkString("[\"", "\",\"", "\"]")
              makeResponse(200,
                  s"""{"status":"ok","catalog_ids":$idString}""")
          }
          .recover {
              t: Throwable =>
                  makeResponse(400,
                      s"""{"status":"failed","messages":"${t.getMessage}"}""")
          }

    val routes: Route = concat(
        pathPrefix("add" / ".*".r) {
            series_id =>
                params {
                    (author, tags) =>
                        concat(pathEnd {
                            put {
                                complete(
                                    tagsService.ensureSeries(series_id).flatMap(
                                        _ => compute(author, SERIES, tags, List(series_id))))
                            }

                        }, pathPrefix(".*".r) {
                            season_id =>
                                concat(pathEnd {
                                    put {
                                        complete(
                                            tagsService.bindSeriesToSeason(series_id, season_id).flatMap(
                                                _ => compute(author, SEASON, tags, List(series_id, season_id))))
                                    }

                                },
                                    pathPrefix(".*".r) {
                                        episode_id =>
                                            pathEnd {
                                                put {
                                                    complete(
                                                        tagsService.bindSeriesSeasonEpisode(series_id, season_id, episode_id).flatMap(_ =>
                                                            compute(author, EPISODE, tags, List(series_id, season_id, episode_id))))
                                                }
                                            }
                                    })
                        })

                }
        },
        path("find-by-tags") {
            parameters(Symbol("type").as[String].?, Symbol("tags").as[String], Symbol("author").as[String].?) {
                (resultType, tags, author) =>
                    get {
                        complete(tagsService.lookupByTags(FindByTags(resultType.map(a => NodeType(a)).getOrElse(All),
                            Tags(tags),
                            author.map(Author)))
                          .map { ids =>
                              val quotedIds = ids.map(id => "\"" + id + "\"")
                              makeResponse(200,
                                  s"""{"status":"ok","catalog_ids":[${quotedIds.mkString(",")}]}""")
                          }
                          .recover {
                              t: Throwable =>
                                  makeResponse(400,
                                      s"""{"status":"failed","messages":["${t.getMessage}"]}""")
                          })
                    }
            }
        }, {
            path("find-by-catalog-id" / ".*".r) {
                catalog_id =>
                    parameters(Symbol("type").as[String].?("all"), Symbol("author").as[String].?) {
                        (nodeType, author) =>
                            get {
                                complete(tagsService.findTagsById(FindTagsById(ID(catalog_id, NodeType(nodeType)), author.map(Author)))
                                  .map { tags =>
                                      val quotedTags = tags.map(t => "\"" + t + "\"")
                                      makeResponse(
                                          200,
                                          s"""{"status":"ok","tags":[${quotedTags.mkString(",")}]}""")
                                  }
                                  .recover {
                                      t: Throwable =>
                                          makeResponse(
                                              400,
                                              s"""{"status":"failed","messages":["${t.getMessage}"]}""")
                                  })
                            }
                    }
            }
        }, {
            path("constraints") {
                post {
                    complete(Try(tagsService.createConstraints()) match {
                        case Success(_) => makeResponse(200,
                            s"""{"status":"ok"}""")
                        case Failure(t) => makeResponse(400,
                            s"""{"status":"failed","messages":[${t.getMessage}]}""")
                    })
                }
            }
        }, {
            path("all-tags") {
                parameters(Symbol("author").?) {
                    author =>
                        get {
                            complete(tagsService.findAll(FindAll(author.map(Author)))
                              .map { tags =>
                                  val quotedTags = tags.map(t => "\"" + t + "\"")
                                  makeResponse(
                                      200,
                                      s"""{"status":"ok","tags":[${quotedTags.mkString(",")}]}""")
                              }
                              .recover {
                                  t: Throwable =>
                                      makeResponse(
                                          400,
                                          s"""{"status":"failed","messages":["${t.getMessage}"]}""")
                              })
                        }
                }
            }
        }
    )
}
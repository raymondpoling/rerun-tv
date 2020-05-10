package service

import java.util.concurrent.TimeUnit

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.Specs2RouteTest
import neotypes.{Driver, GraphDatabase}
import org.neo4j.driver.v1.{AuthTokens, Config}
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll
import tags.Routes

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

class TagsServiceTest extends Specification with NowAndLater {
  this.sequential

  "The tags service when creating" should {

    "Attach a series to a tag" in {
      Put("/add/BGATA01?author=ME&tags=sexy") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","catalog_ids":["BGATA01"]}"""
      }
    }

    "Attach a season to a tag" in {
      Put("/add/BGATA01/BGATA0101?author=JAMES&tags=less-sexy") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","catalog_ids":["BGATA01","BGATA0101"]}"""
      }
    }

    "Attach an episode to a tag" in {
      Put("/add/BGATA01/BGATA0101/BGATA0101001?author=NOVICE&tags=not-sexy") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","catalog_ids":["BGATA01","BGATA0101","BGATA0101001"]}"""
      }
    }

    "Attach multiple tags to an episode" in {
      Put("/add/HOUSE01/HOUSE0101/HOUSE0101002?author=TOM&tags=medical,procedural") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","catalog_ids":["HOUSE01","HOUSE0101","HOUSE0101002"]}"""
      }
    }
    "Add the series/season/episode relationship without tags" in {
      Put("/add/KANON01/KANON0101/KANON0101001?author=testing") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","catalog_ids":["KANON01","KANON0101","KANON0101001"]}"""
        Try(testUtil.findANode(SERIES,"KANON01")) must beAFailedTry
        Try(testUtil.findANode(SEASON,"KANON0101")) must beAFailedTry
        Try(testUtil.findANode(EPISODE,"KANON0101001")) must beAFailedTry
      }
    }
    "Add tag to kanon season for lookup" in {
      Put("/add/KANON01/KANON0101?author=stew&tag=jimminy") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","catalog_ids":["KANON01","KANON0101"]}"""
        testUtil.countAll(SERIES,"KANON01") must be_==(1)
        testUtil.countAll(SEASON,"KANON0101") must be_==(1)

      }
    }
  }
  "Tags service looking up tags" should {
    "find a series based on a tag" in {
      Get("/find-by-tags?tags=sexy&type=SERIES") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","catalog_ids":["BGATA01"]}"""
      }
    }
    "find a season based on a tag" in {
      Get("/find-by-tags?tags=less-sexy&type=SEASON") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","catalog_ids":["BGATA0101"]}"""
      }
    }
    "find a series based on a tag" in {
      Get("/find-by-tags?tags=not-sexy&type=EPISODE") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","catalog_ids":["BGATA0101001"]}"""
      }
    }
    "without a result type, get all items for tags to series" in {
      Get("/find-by-tags?tags=sexy") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","catalog_ids":["BGATA01","BGATA0101","BGATA0101001"]}"""
      }
    }
    "without a result type, get season and episode for tags to series" in {
      Get("/find-by-tags?tags=less-sexy") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","catalog_ids":["BGATA0101","BGATA0101001"]}"""
      }
    }
    "without a result type, get season and episode for tags to series" in {
      Get("/find-by-tags?tags=not-sexy") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","catalog_ids":["BGATA0101001"]}"""
      }
    }
    "can find the intersection of tags" in {
      Get("/find-by-tags?tags=procedural,medical") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","catalog_ids":["HOUSE0101002"]}"""
      }
    }
    "can find the intersection of tags" in {
      Get("/find-by-tags?tags=not-sexy,less-sexy,sexy") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","catalog_ids":["BGATA0101001"]}"""
      }
    }
  }
  "Tags service looking up tags with authors" should {
    "find a series based on a tag by ME" in {
      Get("/find-by-tags?tags=sexy&type=SERIES&author=ME") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","catalog_ids":["BGATA01"]}"""
      }
    }
    "find a series based on a tag by Miss" in {
      Get("/find-by-tags?tags=sexy&type=SERIES&author=Miss") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","catalog_ids":[]}"""
      }
    }
    "find a season based on a tag by JAMES" in {
      Get("/find-by-tags?tags=less-sexy&type=SEASON&author=JAMES") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","catalog_ids":["BGATA0101"]}"""
      }
    }
    "find a season based on a tag by Miss" in {
      Get("/find-by-tags?tags=less-sexy&type=SEASON&author=Miss") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","catalog_ids":[]}"""
      }
    }
    "find a series based on a tag by NOVICE" in {
      Get("/find-by-tags?tags=not-sexy&type=EPISODE&author=NOVICE") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","catalog_ids":["BGATA0101001"]}"""
      }
    }
    "find a series based on a tag by Miss" in {
      Get("/find-by-tags?tags=not-sexy&type=EPISODE&author=Miss") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","catalog_ids":[]}"""
      }
    }
    "without a result type, get all items for tags to series by ME" in {
      Get("/find-by-tags?tags=sexy&author=ME") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","catalog_ids":["BGATA01","BGATA0101","BGATA0101001"]}"""
      }
    }
    "without a result type, get all items for tags to series by Miss" in {
      Get("/find-by-tags?tags=sexy&author=Miss") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","catalog_ids":[]}"""
      }
    }
    "without a result type, get season and episode for tags to series by JAMES" in {
      Get("/find-by-tags?tags=less-sexy&author=JAMES") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","catalog_ids":["BGATA0101","BGATA0101001"]}"""
      }
    }
    "without a result type, get season and episode for tags to series by NOVICE" in {
      Get("/find-by-tags?tags=not-sexy&author=NOVICE") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","catalog_ids":["BGATA0101001"]}"""
      }
    }
    "can find the intersection of tags by TOM" in {
      Get("/find-by-tags?tags=procedural,medical&author=TOM") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","catalog_ids":["HOUSE0101002"]}"""
      }
    }
    "can't find the intersection of tags when author not the same" in {
      Get("/find-by-tags?tags=not-sexy,less-sexy,sexy&author=Me") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","catalog_ids":[]}"""
      }
    }
  }
  "Find tags by various ids" should {
    "find all tags associated with a series" in {
      Get("/find-by-catalog-id/BGATA01") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","tags":["sexy"]}"""
      }
    }
    "find all tags associated with a season" in {
      Get("/find-by-catalog-id/BGATA0101") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","tags":["less-sexy","sexy"]}"""
      }
    }
    "find all tags associated with a episode" in {
      Get("/find-by-catalog-id/BGATA0101001") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","tags":["less-sexy","not-sexy","sexy"]}"""
      }
    }
  }
  "Find tags by various ids with author" should {
    "find all tags associated with a series by author Miss" in {
      Get("/find-by-catalog-id/BGATA01?author=Miss") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","tags":[]}"""
      }
    }
    "find all tags associated with a series by author ME" in {
      Get("/find-by-catalog-id/BGATA01?author=ME") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","tags":["sexy"]}"""
      }
    }
    "find all tags associated with a season" in {
      Get("/find-by-catalog-id/BGATA0101") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","tags":["less-sexy","sexy"]}"""
      }
    }
    "find all tags associated with a season by Miss" in {
      Get("/find-by-catalog-id/BGATA0101?author=Miss") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","tags":[]}"""
      }
    }
    "find all tags associated with a season by ME" in {
      Get("/find-by-catalog-id/BGATA0101?author=ME") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","tags":["sexy"]}"""
      }
    }
    "find all tags associated with a season by JAMES" in {
      Get("/find-by-catalog-id/BGATA0101?author=JAMES") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","tags":["less-sexy"]}"""
      }
    }
    "find all tags associated with a episode by JAMES" in {
      Get("/find-by-catalog-id/BGATA0101001?author=JAMES") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","tags":["less-sexy"]}"""
      }
    }
    "find all tags associated with a episode by ME" in {
      Get("/find-by-catalog-id/BGATA0101001?author=ME") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","tags":["sexy"]}"""
      }
    }
    "find all tags associated with a episode by MISS" in {
      Get("/find-by-catalog-id/BGATA0101001?author=MISS") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","tags":[]}"""
      }
    }
    "find all tags associated with a episode by NOVICE" in {
      Get("/find-by-catalog-id/BGATA0101001?author=NOVICE") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","tags":["not-sexy"]}"""
      }
    }
  }
  "Ensure properties are true" should {
    "ensure series and distinct series are the same" in {
      testUtil.countAll(SERIES) must be_==(testUtil.countAllDistinct(SERIES))
    }
    "ensure seasons and distinct seasons are the same" in {
      testUtil.countAll(SEASON) must be_==(testUtil.countAllDistinct(SEASON))
    }
    "ensure episodes and distinct episodes are the same" in {
      testUtil.countAll(EPISODE) must be_==(testUtil.countAllDistinct(EPISODE))
    }
    "ensure tags and distinct distinct are the same" in {
      testUtil.countAll(Tag) must be_==(testUtil.countAllDistinct(Tag))
    }
  }
  "Getting all tags" should {
    "return all distinct tags" in {
      Get("/all-tags") ~> routes ~> check {
        val results = List("less-sexy","medical","not-sexy","procedural",
          "sexy")
          .map(t => "\"" + t + "\"")
          .mkString(",")
        responseAs[String] shouldEqual s"""{"status":"ok","tags":[$results]}"""
      }
    }
    "return all distinct tags a particular user has attached" in {
      Get("/all-tags?author=TOM") ~> routes ~> check {
        responseAs[String] shouldEqual """{"status":"ok","tags":["medical","procedural"]}"""
      }
    }
  }
}
trait NowAndLater extends Specs2RouteTest with BeforeAfterAll {
  val url = "bolt://localhost:7687"

  protected val driver = GraphDatabase.driver[Future](url,
    AuthTokens.basic("neo4j",
      "testing"),Config.build().withoutEncryption().build())

  val tagsService: TagsService = new TagsService(driver)

  val testUtil: TestUtil = new TestUtil(driver)

  val routes: Route = new Routes(tagsService).routes

  override def beforeAll() : Unit = {
    println("Preparing constraints for testing!")
    Try(Await.result(tagsService.createConstraints(),Duration(20,TimeUnit.SECONDS))) match {
      case Failure(t) => t.printStackTrace()
      case Success(s) =>
    }
  }

  override def afterAll(): Unit = {
    driver.close
  }
}
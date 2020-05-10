package service

import neotypes.Driver
import neotypes.implicits.all._
import scala.concurrent.Future
import neotypes.implicits._

import scala.concurrent.ExecutionContext.Implicits.global

class TagsService(driver:Driver[Future]) {

  def ensureSeries(series: String): Future[Unit] = {
    driver.writeSession { session =>
      (c"" + s"""MERGE (series$SERIES {catalog_id:"$series"})""")
        .query[Unit].execute(session)
    }
  }

  def bindSeriesToSeason(series: String, season: String): Future[Unit] = {
    driver.writeSession { session =>
      (c"" +
        s"""MERGE (series$SERIES {catalog_id:"$series"})
                MERGE (season$SEASON {catalog_id:"$season"})
                MERGE (series)<-[$BELONGS_TO_SERIES]-(season)""")
        .query[Unit].execute(session)
    }
  }

  def bindSeriesSeasonEpisode(series: String, season: String, episode: String): Future[Unit] = {
    driver.writeSession { session =>
      (c"" +
        s"""MERGE (series$SERIES {catalog_id:"$series"})
            MERGE (season$SEASON {catalog_id:"$season"})
            MERGE (episode$EPISODE {catalog_id:"$episode"})
            MERGE (series)<-[$BELONGS_TO_SERIES]-(season)
            MERGE (season)<-[$BELONGS_TO_SEASON]-(episode)""")
        .query[Unit].execute(session)
    }
  }

  def createConstraints(): Future[List[_]] = {
    Future.sequence(TagsService.constraints.map { constraint =>
      driver.writeSession { session =>
        constraint.query[Unit].list(session)
      }
    })
  }


  def addTag(addTag: AddTags): Future[Unit] = {
    // aliasing to work with cypher query interpolator
    driver.writeSession { session =>
      (c"" +
        s"""MERGE (value${addTag.id.nodeType.toString} {catalog_id:"${addTag.id.id}"})
               MERGE (tag:TAG {tag:"${addTag.tags.tags.last}"})
               MERGE (value)-[${addTag.relationship.toString} {author:"${addTag.author.author}"}]->(tag)""")
        .query[Unit].execute(session)
    }
  }

  def lookupByTags(findTag: FindByTags): Future[List[String]] = {
    val author = findTag.author.map(a => s"is.author = '${a.author}' AND ").getOrElse("")
    val first: String =
      s"""MATCH p=(start${findTag.nodeType})-[*]->(:TAG {tag:'${findTag.tags.tags.head}'})
          WITH last(relationships(p)) AS is, start
          WHERE $author type(is) = "Is"
          WITH start
          """
    val rest: String = findTag.tags.tags.drop(1).map { t =>
      s"""MATCH p=(start${findTag.nodeType})-[*]->(:TAG {tag:'$t'})
          WITH last(relationships(p)) AS is, start
          WHERE $author type(is) = "Is"
          WITH start
          """
    }.fold("")((a, b) => a + b)
    val string = first + rest +
      """ORDER BY start.catalog_id
         RETURN DISTINCT start.catalog_id"""
    driver.readSession { session =>
      (c"" + string).query[String].list(session)
    }
  }

  def findTagsById(findTagsById: FindTagsById): Future[List[String]] = {
    val author = findTagsById.author.map(a => s"{author:'${a.author}'}").getOrElse("")
    val string: String =
      s"""MATCH ({catalog_id:'${findTagsById.id.id}'})-[:Is ${author}]->(tag:TAG)
          WITH collect({tag:tag.tag}) AS direct_match
          OPTIONAL MATCH ({catalog_id:'${findTagsById.id.id}'})-[*]->()-[:Is ${author}]->(tag:TAG)
          WITH direct_match + collect({tag:tag.tag}) AS result_set
          UNWIND result_set AS row
          WITH row.tag AS tag
          WHERE tag IS NOT NULL
          RETURN tag
          ORDER BY tag"""
    driver.readSession { session =>
      (c"" + string).query[String].list(session)
    }
  }

  def findAll(findAll: FindAll) : Future[List[String]] = {
    val author = findAll.author.map(a => s""", r.author AS author
      WHERE author = '${a.author}'
      """).getOrElse("")
    val query =
      s"""MATCH ()-[r:Is]->(tags:TAG)
          WITH DISTINCT tags.tag AS tag$author
          WITH tag
          ORDER BY tag
          RETURN tag"""
    driver.readSession { session =>
      (c"" + query).query[String].list(session)
    }
  }

  def checkConnection(): Future[List[String]] = {
    driver.readSession { session =>
      c"""MATCH (n1:TESTING )-->() RETURN n1.name""".query[String].list(session)
    }
  }

}

object TagsService {
  val constraints = List(
    """CREATE CONSTRAINT series_key ON (node:SERIES) ASSERT node.catalog_id IS UNIQUE""",
    """CREATE CONSTRAINT season_key ON (node:SEASON) ASSERT node.catalog_id IS UNIQUE""",
    """CREATE CONSTRAINT episode_key ON (node:EPISODE) ASSERT node.catalog_id IS UNIQUE""",
    """CREATE CONSTRAINT tag_key ON (node:TAG) ASSERT node.tag IS UNIQUE"""
  )
}
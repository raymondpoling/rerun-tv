package service

import neotypes.Driver
import neotypes.implicits.all._
import scala.concurrent.Future
import neotypes.implicits._

import scala.concurrent.ExecutionContext.Implicits.global

class TagsService(driver:Driver[Future]) {

  def ensureSeries(series:String) : Future[Unit] = {
    driver.writeSession { session =>
      (c"" + s"""MERGE (series$SERIES {catalog_id:"$series"})""").query[Unit].execute(session)
    }
  }

  def bindSeriesToSeason(series:String, season:String) : Future[Unit] = {
    driver.writeSession { session =>
      (c"" + s"""MERGE (series$SERIES {catalog_id:"$series"})
                MERGE (season$SEASON {catalog_id:"$season"})
                MERGE (series)<-[$BELONGS_TO_SERIES]-(season)""").query[Unit].execute(session)
    }
  }

  def bindSeriesSeasonEpisode(series:String, season:String, episode:String) : Future[Unit] = {
    driver.writeSession { session =>
      (c"" + s"""MERGE (series$SERIES {catalog_id:"$series"})
                MERGE (season$SEASON {catalog_id:"$season"})
                MERGE (episode$EPISODE {catalog_id:"$episode"})
                MERGE (series)<-[$BELONGS_TO_SERIES]-(season)
                MERGE (season)<-[$BELONGS_TO_SEASON]-(episode)""")
        .query[Unit].execute(session)
    }
  }

  def createConstraints() : Future[List[_]] = {
    Future.sequence(TagsService.constraints.map { constraint =>
      driver.writeSession { session =>
        constraint.query[Unit].list(session)
      }
    })
  }


  def addTag(addTag:AddTags) : Future[Unit] = {
    // aliasing to work with cypher query interpolator
    driver.writeSession { session =>
      (c"" + s"""MERGE (value${addTag.id.nodeType.toString} {catalog_id:"${addTag.id.id}"})
               MERGE (tag:TAG {tag:"${addTag.tags.tags.last}"})
               MERGE (value)-[${addTag.relationship.toString} {author:"${addTag.author.author}"}]->(tag)""")
        .query[Unit].execute(session)
    }
  }

  def lookupByTags(findTag:FindByTags) : Future[List[String]] = {
    val first :String = s"""MATCH (tag:TAG {tag:'${findTag.tags.tags.head}'}), (start${findTag.nodeType})-[*]->(tag)
                    WITH start
                    """
    val rest :String = findTag.tags.tags.drop(1).map{t =>
              s"""MATCH (tag:TAG {tag:'$t'}), (start)-[*]->(tag)
                   WITH start
                   """}.fold("")((a,b) => a + b)
    val string = first + rest +
          """ORDER BY start.catalog_id
              RETURN DISTINCT start.catalog_id"""
    driver.writeSession { session =>
      (c"" + string).query[String].list(session)
    }
  }

  def findTagsById(findTagsById: FindTagsById) : Future[List[String]] = {
    val string :String = s"""MATCH (start {catalog_id:'${findTagsById.id.id}'})-[*]->(tag:TAG) WITH tag ORDER BY tag.tag RETURN DISTINCT tag.tag"""
    driver.writeSession { session =>
      (c"" + string).query[String].list(session)
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
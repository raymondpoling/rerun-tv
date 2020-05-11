package service

import java.util.concurrent.TimeUnit

import neotypes.Driver
import neotypes.implicits.all._

import scala.concurrent.{Await, Future}
import neotypes.implicits._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Success, Try}

class TestUtil(driver:Driver[Future]) {
  def findANode(nodeType: NodeType, duplicateId: String): List[String] = {
    Await.result(driver.writeSession { session =>
      (c"" + s"""CREATE (node$nodeType {catalog_id:'$duplicateId'}) RETURN node.catalog_id""").query[String].list(session)
    }, Duration(200, TimeUnit.MILLISECONDS))
  }

  def countAll(nodeType: NodeType) : Int = {
    Await.result(driver.readSession { session =>
      val s = s"""MATCH (node$nodeType) RETURN count(node.catalog_id)"""
      (c"" + s).query[Int].list(session)
    }, Duration(200, TimeUnit.MILLISECONDS)).map(identity).sum
  }

  def countAll(nodeType: NodeType,catalog_id:String) : Int = {
    Await.result(driver.readSession { session =>
      val s = s"""MATCH (node$nodeType {catalog_id:'$catalog_id'}) RETURN count(node.catalog_id)"""
      (c"" + s).query[Int].list(session)
    }, Duration(200, TimeUnit.MILLISECONDS)).map(identity).sum
  }

  def countAllDistinct(nodeType: NodeType) : Int = {
    Await.result(driver.readSession { session =>
      (c"" + s"""MATCH (node$nodeType) RETURN count(DISTINCT node.catalog_id)""").query[Int].list(session)
    }, Duration(200, TimeUnit.MILLISECONDS)).sum
  }
}

package service

sealed trait Label {
  override val toString: String = ":" + {
    val t = this.getClass.getSimpleName
      t.take(t.length-1)
  }
}

sealed trait Relationship extends Label {}
case object Is extends Relationship
case object BELONGS_TO_SERIES extends Relationship
case object BELONGS_TO_SEASON extends Relationship

sealed trait NodeType  extends Label {}
case object SERIES extends NodeType
case object SEASON extends NodeType
case object EPISODE extends NodeType

case object All extends NodeType {
  override val toString: String = ""
}
case object Tag extends NodeType


object NodeType {
  def apply(nodeType:String) : NodeType = nodeType.toLowerCase match {
    case "series" => SERIES
    case "season" => SEASON
    case "episode" => EPISODE
    case "all" => All
  }
}

// Represents metadata
case class Author(author:String)
case class Tags(tags:List[String])
object Tags {
  def apply(tags:String) : Tags = Tags(tags.split(",").toList)
}
case class ID(id:String,nodeType: NodeType)

// Represents searches
case class AddTags(author:Author, relationship:Relationship, id:ID, tags:Tags)

case class FindByTags(nodeType: NodeType, tags: Tags, author:Option[Author] = None)

case class FindTagsById(id:ID,author:Option[Author]=None)

case class FindAll(author: Option[Author])
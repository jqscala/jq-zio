package jqzio

import zio._
import zio.json.ast._

object JsonExtractors: 
        
    object IsArrayZ: 
        def unapply(v: Json): Option[Chunk[Json]] = 
            v.asArray

    object IsObjectZ: 
        def unapply(v: Json): Option[Json.Obj] = 
            v.asObject

    object IsStringZ: 
        def unapply(v: Json): Option[String] = 
            v.asString

    object IsNumZ: 
        def unapply(v: Json): Option[Json.Num] = 
            v.asNumber

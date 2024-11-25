package jqzio

import zio._
import zio.json.ast._

object JsonExtractors: 
        
    object IsArrayZ: 
        def unapply(v: Json): Option[Chunk[Json]] = 
            v.as[Json.Arr].map(_.elements).toOption

    object IsObjectZ: 
        def unapply(v: Json): Option[Json.Obj] = 
            v.as[Json.Obj].toOption

    object IsStringZ: 
        def unapply(v: Json): Option[Json.Str] = 
            v.as[Json.Str].toOption

    object IsNumZ: 
        def unapply(v: Json): Option[Json.Num] = 
            v.as[Json.Num].toOption

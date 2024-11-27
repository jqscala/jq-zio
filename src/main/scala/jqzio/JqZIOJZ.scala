package jqzio

import jq.{Json => _, _}
import zio._
import zio.stream._
import zio.json.ast._
import JsonExtractors._

type Filter[R, E] = 
    ZPipeline[R, E, Json, Json | TypeError[Json]]

given JQZioJZ[R, E]: Jq[Filter[R, E]] with 

    val isJson: PartialFunction[Json | TypeError[Json], Json] = 
        case j: Json => j

    def id: Filter[R, E] = 
        ZPipeline.identity

    def str(s: String): Filter[R, E] = 
        ZPipeline.map: _ => 
            Json.Str(s)

    def error(msg: String): Filter[R, E] = 
        ZPipeline.map: _ => 
            TypeError.Custom(msg)

    def iterator: Filter[R, E] = 
        ZPipeline.map:
                case IsObjectZ(v) => v.values
                case IsArrayZ(v) => v
                case j: Json => Chunk(TypeError.CannotIterateOver(j))
        .flattenIterables
   
    def array(f: Filter[R, E]): Filter[R, E] =
        ZPipeline.fromFunction:  
            _ flatMap: json => 
                f(ZStream(json))
                    .transduce(ZSink.collectAll)
                    .map: chunk => 
                        chunk.headOption match 
                            case Some(error: TypeError[Json]) => error
                            case _ => Json.Arr(chunk.collect(isJson)*)

    extension (f1: Filter[R, E])
        def |(f2: Filter[R, E]): Filter[R, E] = 
            ZPipeline.fromFunction: 
                _ flatMap: v => 
                    f1(ZStream(v)) flatMap:
                        case e: TypeError[Json] => ZStream(e)
                        case j: Json => 
                            f2(ZStream(j)).collectWhile(isJson)
                
        infix def concat(f2: Filter[R, E]): Filter[R, E] = 
            ZPipeline.fromFunction:  
                _ flatMap: json => 
                    (f1(ZStream(json)) ++ f2(ZStream(json)))
                        .collectWhile(isJson)

        def index(f2: Filter[R, E]): Filter[R, E] = 
            ZPipeline fromFunction: 
                _ flatMap: v => 
                    (f1(ZStream(v)) cross f2(ZStream(v)))
                        .map:
                            case (IsObjectZ(obj), IsStringZ(key)) => 
                                obj.get(key).getOrElse(Json.Null)
                            case (o: Json, k: Json) => 
                                TypeError.CannotIndex(???, ???)
                            case (e: TypeError[Json], _) => 
                                e
                            case (_, e: TypeError[Json]) => 
                                e
                        .collectWhile(isJson)

        def `catch`(f2: Filter[R, E]): Filter[R, E] = 
            ZPipeline.fromFunction:
                _ flatMap: json => 
                    f1(ZStream(json)) flatMap:
                        case j: Json => ZStream(j)
                        case e: TypeError[Json] => 
                            f2(ZStream(Json.Str(e.toString))) 

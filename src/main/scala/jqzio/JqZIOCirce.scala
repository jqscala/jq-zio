package jqzio

import jq._
import zio._
import zio.stream._
import io.circe._

type Filter[R, E] = 
    ZPipeline[R, E, Json, Json | jq.TypeError]

given JqZioCirce[R, E]: Jq[Filter[R, E]] with 

    val isJson: PartialFunction[Json | TypeError, Json] = 
        case j: Json => j

    def id: Filter[R, E] = 
        ZPipeline.identity

    def str(s: String): Filter[R, E] = 
        ZPipeline.map: _ => 
            Json.fromString(s)

    def error(msg: String): Filter[R, E] = 
        ZPipeline.map: _ => 
            TypeError.Custom(msg)

    def iterator: Filter[R, E] = 
        ZPipeline.map:
                case IsObject(v) => v.values
                case IsArray(v) => v
                case j: Json => Chunk(TypeError.CannotIterateOver(j))
        .flattenIterables
   
    def array(f: Filter[R, E]): Filter[R, E] =
        ZPipeline.fromFunction:  
            _ flatMap: json => 
                f(ZStream(json))
                    .transduce(ZSink.collectAll)
                    .map: chunk => 
                        chunk.headOption match 
                            case Some(error: TypeError) => error
                            case _ => Json.arr(chunk.collect(isJson).toSeq*)

    extension (f1: Filter[R, E])
        def |(f2: Filter[R, E]): Filter[R, E] = 
            ZPipeline.fromFunction: 
                _ flatMap: v => 
                    f1(ZStream(v)) flatMap:
                        case e: TypeError => ZStream(e)
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
                            case (IsObject(obj), IsString(key)) => 
                                obj(key).getOrElse(Json.Null)
                            case (o: Json, k: Json) => 
                                TypeError.CannotIndex(???, ???)
                            case (e: TypeError, _) => 
                                e
                            case (_, e: TypeError) => 
                                e
                        .collectWhile(isJson)

        def `catch`(f2: Filter[R, E]): Filter[R, E] = 
            ZPipeline.fromFunction:
                _ flatMap: json => 
                    f1(ZStream(json)) flatMap:
                        case j: Json => ZStream(j)
                        case e: TypeError => 
                            f2(ZStream(Json.fromString(e.toString))) 

package jqzio

import jq._
import zio._
import zio.stream._
import zio.json.ast._
import JsonExtractors._

type FilterJZ[R, E] = 
    ZPipeline[R, E, Json, Json | TypeError]

enum TypeError: 
    case CannotIterateOver(j: Json)
    case CannotIndexObjectWith(j: Json)
    case CannotIndex(what: Json, _with: Json)
    case Custom(msg: String = "")

given JQZioJZ[R, E]: Jq[FilterJZ[R, E]] with 

    val isJson: PartialFunction[Json | TypeError, Json] = 
        case j: Json => j

    def id: FilterJZ[R, E] = 
        ZPipeline.identity

    def str(s: String): FilterJZ[R, E] = 
        ZPipeline.map: _ => 
            Json.Str(s)

    def error(msg: String): FilterJZ[R, E] = 
        ZPipeline.map: _ => 
            TypeError.Custom(msg)

    def iterator: FilterJZ[R, E] = 
        ZPipeline.map:
                case IsObjectZ(v) => v.values
                case IsArrayZ(v) => v
                case j: Json => Chunk(TypeError.CannotIterateOver(j))
        .flattenIterables
   
    def array(f: FilterJZ[R, E]): FilterJZ[R, E] =
        ZPipeline.fromFunction:  
            _ flatMap: json => 
                f(ZStream(json))
                    .transduce(ZSink.collectAll)
                    .map: chunk => 
                        chunk.headOption match 
                            case Some(error: TypeError) => error
                            case _ => Json.Arr(chunk.collect(isJson)*)

    extension (f1: FilterJZ[R, E])
        def |(f2: FilterJZ[R, E]): FilterJZ[R, E] = 
            ZPipeline.fromFunction: 
                _ flatMap: v => 
                    f1(ZStream(v)) flatMap:
                        case e: TypeError => ZStream(e)
                        case j: Json => 
                            f2(ZStream(j)).collectWhile(isJson)
                
        infix def concat(f2: FilterJZ[R, E]): FilterJZ[R, E] = 
            ZPipeline.fromFunction:  
                _ flatMap: json => 
                    (f1(ZStream(json)) ++ f2(ZStream(json)))
                        .collectWhile(isJson)

        def index(f2: FilterJZ[R, E]): FilterJZ[R, E] = 
            ZPipeline fromFunction: 
                _ flatMap: v => 
                    (f1(ZStream(v)) cross f2(ZStream(v)))
                        .map:
                            case (IsObjectZ(obj), IsStringZ(key)) => 
                                obj.get(key).getOrElse(Json.Null)
                            case (o: Json, k: Json) => 
                                TypeError.CannotIndex(???, ???)
                            case (e: TypeError, _) => 
                                e
                            case (_, e: TypeError) => 
                                e
                        .collectWhile(isJson)

        def `catch`(f2: FilterJZ[R, E]): FilterJZ[R, E] = 
            ZPipeline.fromFunction:
                _ flatMap: json => 
                    f1(ZStream(json)) flatMap:
                        case j: Json => ZStream(j)
                        case e: TypeError => 
                            f2(ZStream(Json.Str(e.toString))) 

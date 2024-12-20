package jqzio

import jq.{Json => _, _}
import jq.std._
import zio._
import zio.stream._
import zio.json.ast._

given ZIORun[E]: RunnableFilter[Filter[Any, E], Json] with

    extension [A](st: ZStream[Any, E, A])
        def runS: List[A] = 
            Unsafe.unsafe{ implicit unsafe =>
                Runtime.default.unsafe.run(
                    st.run(ZSink.collectAll[A].map(_.toList))
                ).getOrThrowFiberFailure()
            }
            
    extension (input: List[Json])
        def throughJson(r: Filter[Any, E]): List[Json | TypeError[Json]] = 
            r(ZStream(input*)).runS

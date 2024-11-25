package jqzio

import jq._
import zio._
import zio.stream._
import io.circe.Json

given ZIORun[E]: RunnableFilter[Filter[Any, E]] with

    extension [A](st: ZStream[Any, E, A])
        def runS: List[A] = 
            Unsafe.unsafe{ implicit unsafe =>
                Runtime.default.unsafe.run(
                    st.run(ZSink.collectAll[A].map(_.toList))
                ).getOrThrowFiberFailure()
            }
            
    extension (input: List[Json])
        def throughJson(r: Filter[Any, E]): List[Json | TypeError] = 
            r(ZStream(input*)).runS

package jqzio

import jq._
import zio.json._
import zio.json.ast.{Json => ZJson, _}

given Json[ZJson] with

    extension (i: Int)
        def num: ZJson = ZJson.Num(i)
    
    extension (i: String)
        def str: ZJson = ZJson.Str(i)

    def obj(kv: (String, ZJson)*): ZJson = 
        ZJson.Obj(kv*)

    def arr(elems: ZJson*): ZJson =
        ZJson.Arr(elems*)